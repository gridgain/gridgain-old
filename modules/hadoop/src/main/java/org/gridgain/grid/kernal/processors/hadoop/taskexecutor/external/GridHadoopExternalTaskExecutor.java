/* 
 Copyright (C) GridGain Systems. All Rights Reserved.
 
 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0
 
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal.processors.hadoop.taskexecutor.external;

import org.gridgain.grid.*;
import org.gridgain.grid.hadoop.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.jobtracker.*;
import org.gridgain.grid.kernal.processors.hadoop.message.*;
import org.gridgain.grid.kernal.processors.hadoop.taskexecutor.*;
import org.gridgain.grid.kernal.processors.hadoop.taskexecutor.external.child.*;
import org.gridgain.grid.kernal.processors.hadoop.taskexecutor.external.communication.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;

import static org.gridgain.grid.kernal.processors.hadoop.taskexecutor.GridHadoopTaskState.*;

/**
 * External process registry. Handles external process lifecycle.
 */
public class GridHadoopExternalTaskExecutor extends GridHadoopTaskExecutorAdapter {
    /** Hadoop context. */
    private GridHadoopContext ctx;

    /** */
    private String javaCmd;

    /** Logger. */
    private GridLogger log;

    /** Node process descriptor. */
    private GridHadoopProcessDescriptor nodeDesc;

    /** Output base. */
    private File outputBase;

    /** Path separator. */
    private String pathSep;

    /** Hadoop external communication. */
    private GridHadoopExternalCommunication comm;

    /** Starting processes. */
    private final ConcurrentMap<UUID, HadoopProcess> runningProcsByProcId = new ConcurrentHashMap8<>();

    /** Starting processes. */
    private final ConcurrentMap<GridHadoopJobId, HadoopProcess> runningProcsByJobId = new ConcurrentHashMap8<>();

    /** Busy lock. */
    private final GridSpinReadWriteLock busyLock = new GridSpinReadWriteLock();

    /** Job tracker. */
    private GridHadoopJobTracker jobTracker;

    /** {@inheritDoc} */
    @Override public void start(GridHadoopContext ctx) throws GridException {
        this.ctx = ctx;

        log = ctx.kernalContext().log(GridHadoopExternalTaskExecutor.class);

        outputBase = U.resolveWorkDirectory("hadoop", false);

        pathSep = System.getProperty("path.separator", U.isWindows() ? ";" : ":");

        initJavaCommand();

        comm = new GridHadoopExternalCommunication(
            ctx.localNodeId(),
            UUID.randomUUID(),
            ctx.kernalContext().config().getMarshaller(),
            log,
            ctx.kernalContext().config().getSystemExecutorService(),
            ctx.kernalContext().gridName());

        comm.setListener(new MessageListener());

        comm.start();

        nodeDesc = comm.localProcessDescriptor();

        ctx.kernalContext().ports().registerPort(nodeDesc.tcpPort(), GridPortProtocol.TCP,
            GridHadoopExternalTaskExecutor.class);

        if (nodeDesc.sharedMemoryPort() != -1)
            ctx.kernalContext().ports().registerPort(nodeDesc.sharedMemoryPort(), GridPortProtocol.TCP,
                GridHadoopExternalTaskExecutor.class);

        jobTracker = ctx.jobTracker();
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) {
        busyLock.writeLock();

        try {
            comm.stop();
        }
        catch (GridException e) {
            U.error(log, "Failed to gracefully stop external hadoop communication server (will shutdown anyway)", e);
        }
    }

    /** {@inheritDoc} */
    @Override public void onJobStateChanged(final GridHadoopJobMetadata meta) {
        final HadoopProcess proc = runningProcsByJobId.get(meta.jobId());

        // If we have a local process for this job.
        if (proc != null) {
            if (log.isDebugEnabled())
                log.debug("Updating job information for remote task process [proc=" + proc + ", meta=" + meta + ']');

            if (meta.phase() == GridHadoopJobPhase.PHASE_COMPLETE) {
                if (log.isDebugEnabled())
                    log.debug("Completed job execution, will terminate child process [jobId=" + meta.jobId() +
                        ", proc=" + proc + ']');

                runningProcsByJobId.remove(meta.jobId());
                runningProcsByProcId.remove(proc.descriptor().processId());

                proc.terminate();

                return;
            }

            if (proc.initFut.isDone()) {
                if (!proc.initFut.isFailed())
                    sendJobInfoUpdate(proc, meta);
                else if (log.isDebugEnabled())
                    log.debug("Failed to initialize child process (will skip job state notification) " +
                        "[jobId=" + meta.jobId() + ", meta=" + meta + ']');
            }
            else {
                proc.initFut.listenAsync(new CI1<GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>>>() {
                    @Override public void apply(GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>> f) {
                        try {
                            f.get();

                            sendJobInfoUpdate(proc, meta);
                        }
                        catch (GridException e) {
                            if (log.isDebugEnabled())
                                log.debug("Failed to initialize child process (will skip job state notification) " +
                                    "[jobId=" + meta.jobId() + ", meta=" + meta + ", err=" + e + ']');
                        }

                    }
                });
            }
        }
        else if (ctx.isParticipating(meta)) {
            GridHadoopJob job;

            try {
                job = jobTracker.job(meta.jobId(), meta.jobInfo());
            }
            catch (GridException e) {
                U.error(log, "Failed to get job: " + meta.jobId(), e);

                return;
            }

            startProcess(job, meta.mapReducePlan());
        }
    }

    /** {@inheritDoc} */
    @SuppressWarnings("ConstantConditions")
    @Override public void run(final GridHadoopJob job, final Collection<GridHadoopTaskInfo> tasks) throws GridException {
        if (!busyLock.tryReadLock()) {
            if (log.isDebugEnabled())
                log.debug("Failed to start hadoop tasks (grid is stopping, will ignore).");

            return;
        }

        try {
            HadoopProcess proc = runningProcsByJobId.get(job.id());

            GridHadoopTaskType taskType = F.first(tasks).type();

            if (taskType == GridHadoopTaskType.SETUP || taskType == GridHadoopTaskType.ABORT ||
                taskType == GridHadoopTaskType.COMMIT) {
                if (proc == null || proc.terminated()) {
                    runningProcsByJobId.remove(job.id(), proc);

                    // Start new process for ABORT task since previous processes were killed.
                    proc = startProcess(job, jobTracker.plan(job.id()));

                    if (log.isDebugEnabled())
                        log.debug("Starting new process for maintenance task [jobId=" + job.id() +
                            ", proc=" + proc + ", taskType=" + taskType + ']');
                }
            }
            else
                assert proc != null : "Missing started process for task execution request: " + job.id() +
                    ", tasks=" + tasks;

            final HadoopProcess proc0 = proc;

            proc.initFut.listenAsync(new CI1<GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>>>() {
                @Override public void apply(
                    GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>> f) {
                    if (!busyLock.tryReadLock())
                        return;

                    try {
                        f.get();

                        proc0.addTasks(tasks);

                        if (log.isDebugEnabled())
                            log.debug("Sending task execution request to child process [jobId=" + job.id() +
                                ", proc=" + proc0 + ", tasks=" + tasks + ']');

                        sendExecutionRequest(proc0, job, tasks);
                    }
                    catch (GridException e) {
                        notifyTasksFailed(tasks, FAILED, e);
                    }
                    finally {
                        busyLock.readUnlock();
                    }
                }
            });
        }
        finally {
            busyLock.readUnlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelTasks(GridHadoopJobId jobId) {
        HadoopProcess proc = runningProcsByJobId.get(jobId);

        if (proc != null)
            proc.terminate();
    }

    /**
     * Sends execution request to remote node.
     *
     * @param proc Process to send request to.
     * @param job Job instance.
     * @param tasks Collection of tasks to execute in started process.
     */
    private void sendExecutionRequest(HadoopProcess proc, GridHadoopJob job, Collection<GridHadoopTaskInfo> tasks)
        throws GridException {
        // Must synchronize since concurrent process crash may happen and will receive onConnectionLost().
        proc.lock();

        try {
            if (proc.terminated()) {
                notifyTasksFailed(tasks, CRASHED, null);

                return;
            }

            GridHadoopTaskExecutionRequest req = new GridHadoopTaskExecutionRequest();

            req.jobId(job.id());
            req.jobInfo(job.info());
            req.tasks(tasks);

            comm.sendMessage(proc.descriptor(), req);
        }
        finally {
            proc.unlock();
        }
    }

    /**
     * @return External task metadata.
     */
    private GridHadoopExternalTaskMetadata buildTaskMeta() {
        GridHadoopExternalTaskMetadata meta = new GridHadoopExternalTaskMetadata();

        meta.classpath(Arrays.asList(System.getProperty("java.class.path").split(File.pathSeparator)));
        meta.jvmOptions(Arrays.asList("-Xmx1g", "-ea", "-XX:+UseConcMarkSweepGC", "-XX:+CMSClassUnloadingEnabled",
            "-DGRIDGAIN_HOME=" + U.getGridGainHome()));

        return meta;
    }

    /**
     * @param tasks Tasks to notify about.
     * @param state Fail state.
     * @param e Optional error.
     */
    private void notifyTasksFailed(Iterable<GridHadoopTaskInfo> tasks, GridHadoopTaskState state, Throwable e) {
        GridHadoopTaskStatus fail = new GridHadoopTaskStatus(state, e);

        for (GridHadoopTaskInfo task : tasks)
            jobTracker.onTaskFinished(task, fail);
    }

    /**
     * Starts process template that will be ready to execute Hadoop tasks.
     *
     * @param job Job instance.
     * @param plan Map reduce plan.
     */
    private HadoopProcess startProcess(final GridHadoopJob job, final GridHadoopMapReducePlan plan) {
        final UUID childProcId = UUID.randomUUID();

        GridHadoopJobId jobId = job.id();

        final GridHadoopProcessFuture fut = new GridHadoopProcessFuture(childProcId, jobId, ctx.kernalContext());

        final HadoopProcess proc = new HadoopProcess(jobId, fut, plan.reducers(ctx.localNodeId()));

        HadoopProcess old = runningProcsByJobId.put(jobId, proc);

        assert old == null;

        old = runningProcsByProcId.put(childProcId, proc);

        assert old == null;

        ctx.kernalContext().closure().runLocalSafe(new Runnable() {
            @Override public void run() {
                if (!busyLock.tryReadLock()) {
                    fut.onDone(new GridException("Failed to start external process (grid is stopping)."));

                    return;
                }

                try {
                    GridHadoopExternalTaskMetadata startMeta = buildTaskMeta();

                    if (log.isDebugEnabled())
                        log.debug("Created hadoop child process metadata for job [job=" + job +
                            ", childProcId=" + childProcId + ", taskMeta=" + startMeta + ']');

                    Process proc = startJavaProcess(childProcId, startMeta, job);

                    BufferedReader rdr = new BufferedReader(new InputStreamReader(proc.getInputStream()));

                    String line;

                    // Read up all the process output.
                    while ((line = rdr.readLine()) != null) {
                        if (log.isDebugEnabled())
                            log.debug("Tracing process output: " + line);

                        if ("Started".equals(line)) {
                            // Process started successfully, it should not write anything more to the output stream.
                            if (log.isDebugEnabled())
                                log.debug("Successfully started child process [childProcId=" + childProcId +
                                    ", meta=" + job + ']');

                            fut.onProcessStarted(proc);

                            break;
                        }
                        else if ("Failed".equals(line)) {
                            StringBuilder sb = new StringBuilder("Failed to start child process: " + job + "\n");

                            while ((line = rdr.readLine()) != null)
                                sb.append("    ").append(line).append("\n");

                            // Cut last character.
                            sb.setLength(sb.length() - 1);

                            log.warning(sb.toString());

                            fut.onDone(new GridException(sb.toString()));

                            break;
                        }
                    }
                }
                catch (Throwable e) {
                    fut.onDone(new GridException("Failed to initialize child process: " + job, e));
                }
                finally {
                    busyLock.readUnlock();
                }
            }
        }, true);

        fut.listenAsync(new CI1<GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>>>() {
            @Override public void apply(GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>> f) {
                try {
                    // Make sure there were no exceptions.
                    f.get();

                    prepareForJob(proc, job, plan);
                }
                catch (GridException ignore) {
                    // Exception is printed in future's onDone() method.
                }
            }
        });

        return proc;
    }

    /**
     * Checks that java local command is available.
     *
     * @throws GridException If initialization failed.
     */
    private void initJavaCommand() throws GridException {
        String javaHome = System.getProperty("java.home");

        if (javaHome == null)
            javaHome = System.getenv("JAVA_HOME");

        if (javaHome == null)
            throw new GridException("Failed to locate JAVA_HOME.");

        javaCmd = javaHome + File.separator + "bin" + File.separator + (U.isWindows() ? "java.exe" : "java");

        try {
            Process proc = new ProcessBuilder(javaCmd, "-version").redirectErrorStream(true).start();

            Collection<String> out = readProcessOutput(proc);

            int res = proc.waitFor();

            if (res != 0)
                throw new GridException("Failed to execute 'java -version' command (process finished with nonzero " +
                    "code) [exitCode=" + res + ", javaCmd='" + javaCmd + "', msg=" + F.first(out) + ']');

            if (log.isInfoEnabled()) {
                log.info("Will use java for external task execution: ");

                for (String s : out)
                    log.info("    " + s);
            }
        }
        catch (IOException e) {
            throw new GridException("Failed to check java for external task execution.", e);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new GridException("Failed to wait for process completion (thread got interrupted).", e);
        }
    }

    /**
     * Reads process output line-by-line.
     *
     * @param proc Process to read output.
     * @return Read lines.
     * @throws IOException If read failed.
     */
    private Collection<String> readProcessOutput(Process proc) throws IOException {
        BufferedReader rdr = new BufferedReader(new InputStreamReader(proc.getInputStream()));

        Collection<String> res = new ArrayList<>();

        String s;

        while ((s = rdr.readLine()) != null)
            res.add(s);

        return res;
    }

    /**
     * Builds process from metadata.
     *
     * @param childProcId Child process ID.
     * @param startMeta Metadata.
     * @param job Job.
     * @return Started process.
     */
    private Process startJavaProcess(UUID childProcId, GridHadoopExternalTaskMetadata startMeta,
        GridHadoopJob job) throws Exception {
        String outFldr = jobWorkFolder(job.id()) + File.separator + childProcId;

        if (log.isDebugEnabled())
            log.debug("Will write process log output to: " + outFldr);

        List<String> cmd = new ArrayList<>();

        File workDir = U.resolveWorkDirectory("", false);

        cmd.add(javaCmd);
        cmd.addAll(startMeta.jvmOptions());
        cmd.add("-cp");
        cmd.add(buildClasspath(startMeta.classpath()));
        cmd.add(GridHadoopExternalProcessStarter.class.getName());
        cmd.add("-cpid");
        cmd.add(String.valueOf(childProcId));
        cmd.add("-ppid");
        cmd.add(String.valueOf(nodeDesc.processId()));
        cmd.add("-nid");
        cmd.add(String.valueOf(nodeDesc.parentNodeId()));
        cmd.add("-addr");
        cmd.add(nodeDesc.address());
        cmd.add("-tport");
        cmd.add(String.valueOf(nodeDesc.tcpPort()));
        cmd.add("-sport");
        cmd.add(String.valueOf(nodeDesc.sharedMemoryPort()));
        cmd.add("-out");
        cmd.add(outFldr);
        cmd.add("-wd");
        cmd.add(workDir.getAbsolutePath());

        return new ProcessBuilder(cmd)
            .redirectErrorStream(true)
            .directory(workDir)
            .start();
    }

    /**
     * Gets job work folder.
     *
     * @param jobId Job ID.
     * @return Job work folder.
     */
    private String jobWorkFolder(GridHadoopJobId jobId) {
        return outputBase + File.separator + "Job_" + jobId;
    }

    /**
     * @param cp Classpath collection.
     * @return Classpath string.
     */
    private String buildClasspath(Collection<String> cp) {
        assert !cp.isEmpty();

        StringBuilder sb = new StringBuilder();

        for (String s : cp)
            sb.append(s).append(pathSep);

        sb.setLength(sb.length() - 1);

        return sb.toString();
    }

    /**
     * Sends job info update request to remote process.
     *
     * @param proc Process to send request to.
     * @param meta Job metadata.
     */
    private void sendJobInfoUpdate(HadoopProcess proc, GridHadoopJobMetadata meta) {
        Map<Integer, GridHadoopProcessDescriptor> rdcAddrs = meta.reducersAddresses();

        int rdcNum = meta.mapReducePlan().reducers();

        GridHadoopProcessDescriptor[] addrs = null;

        if (rdcAddrs != null && rdcAddrs.size() == rdcNum) {
            addrs = new GridHadoopProcessDescriptor[rdcNum];

            for (int i = 0; i < rdcNum; i++) {
                GridHadoopProcessDescriptor desc = rdcAddrs.get(i);

                assert desc != null : "Missing reducing address [meta=" + meta + ", rdc=" + i + ']';

                addrs[i] = desc;
            }
        }

        try {
            comm.sendMessage(proc.descriptor(), new GridHadoopJobInfoUpdateRequest(proc.jobId, meta.phase(), addrs));
        }
        catch (GridException e) {
            if (!proc.terminated()) {
                log.error("Failed to send job state update message to remote child process (will kill the process) " +
                    "[jobId=" + proc.jobId + ", meta=" + meta + ']', e);

                proc.terminate();
            }
        }
    }

    /**
     * Sends prepare request to remote process.
     *
     * @param proc Process to send request to.
     * @param job Job.
     * @param plan Map reduce plan.
     */
    private void prepareForJob(HadoopProcess proc, GridHadoopJob job, GridHadoopMapReducePlan plan) {
        try {
            comm.sendMessage(proc.descriptor(), new GridHadoopPrepareForJobRequest(job.id(), job.info(),
                plan.reducers(), plan.reducers(ctx.localNodeId())));
        }
        catch (GridException e) {
            U.error(log, "Failed to send job prepare request to remote process [proc=" + proc + ", job=" + job +
                ", plan=" + plan + ']', e);

            proc.terminate();
        }
    }

    /**
     * Processes task finished message.
     *
     * @param desc Remote process descriptor.
     * @param taskMsg Task finished message.
     */
    private void processTaskFinishedMessage(GridHadoopProcessDescriptor desc, GridHadoopTaskFinishedMessage taskMsg) {
        HadoopProcess proc = runningProcsByProcId.get(desc.processId());

        if (proc != null)
            proc.removeTask(taskMsg.taskInfo());

        jobTracker.onTaskFinished(taskMsg.taskInfo(), taskMsg.status());
    }

    /**
     *
     */
    private class MessageListener implements GridHadoopMessageListener {
        /** {@inheritDoc} */
        @Override public void onMessageReceived(GridHadoopProcessDescriptor desc, GridHadoopMessage msg) {
            if (!busyLock.tryReadLock())
                return;

            try {
                if (msg instanceof GridHadoopProcessStartedAck) {
                    HadoopProcess proc = runningProcsByProcId.get(desc.processId());

                    assert proc != null : "Missing child process for processId: " + desc;

                    GridHadoopProcessFuture fut = proc.initFut;

                    if (fut != null)
                        fut.onReplyReceived(desc);
                    // Safety.
                    else
                        log.warning("Failed to find process start future (will ignore): " + desc);
                }
                else if (msg instanceof GridHadoopTaskFinishedMessage) {
                    GridHadoopTaskFinishedMessage taskMsg = (GridHadoopTaskFinishedMessage)msg;

                    processTaskFinishedMessage(desc, taskMsg);
                }
                else
                    log.warning("Unexpected message received by node [desc=" + desc + ", msg=" + msg + ']');
            }
            finally {
                busyLock.readUnlock();
            }
        }

        /** {@inheritDoc} */
        @Override public void onConnectionLost(GridHadoopProcessDescriptor desc) {
            if (!busyLock.tryReadLock())
                return;

            try {
                if (desc == null) {
                    U.warn(log, "Handshake failed.");

                    return;
                }

                // Notify job tracker about failed tasks.
                HadoopProcess proc = runningProcsByProcId.get(desc.processId());

                if (proc != null) {
                    Collection<GridHadoopTaskInfo> tasks = proc.tasks();

                    if (!F.isEmpty(tasks)) {
                        log.warning("Lost connection with alive process (will terminate): " + desc);

                        GridHadoopTaskStatus status = new GridHadoopTaskStatus(CRASHED,
                            new GridException("Failed to run tasks (external process finished unexpectedly): " + desc));

                        for (GridHadoopTaskInfo info : tasks)
                            jobTracker.onTaskFinished(info, status);

                        runningProcsByJobId.remove(proc.jobId(), proc);
                    }

                    // Safety.
                    proc.terminate();
                }
            }
            finally {
                busyLock.readUnlock();
            }
        }
    }

    /**
     * Hadoop process.
     */
    private static class HadoopProcess extends ReentrantLock {
        /** */
        private static final long serialVersionUID = 0L;

        /** Job ID. */
        private final GridHadoopJobId jobId;

        /** Process. */
        private Process proc;

        /** Init future. Completes when process is ready to receive messages. */
        private final GridHadoopProcessFuture initFut;

        /** Process descriptor. */
        private GridHadoopProcessDescriptor procDesc;

        /** Reducers planned for this process. */
        private Collection<Integer> reducers;

        /** Tasks. */
        private final Collection<GridHadoopTaskInfo> tasks = new ConcurrentLinkedDeque8<>();

        /** Terminated flag. */
        private volatile boolean terminated;

        /**
         * @param jobId Job ID.
         * @param initFut Init future.
         */
        private HadoopProcess(GridHadoopJobId jobId, GridHadoopProcessFuture initFut,
            int[] reducers) {
            this.jobId = jobId;
            this.initFut = initFut;

            if (!F.isEmpty(reducers)) {
                this.reducers = new ArrayList<>(reducers.length);

                for (int r : reducers)
                    this.reducers.add(r);
            }
        }

        /**
         * @return Communication process descriptor.
         */
        private GridHadoopProcessDescriptor descriptor() {
            return procDesc;
        }

        /**
         * @return Job ID.
         */
        public GridHadoopJobId jobId() {
            return jobId;
        }

        /**
         * Initialized callback.
         *
         * @param proc Java process representation.
         * @param procDesc Process descriptor.
         */
        private void onInitialized(Process proc, GridHadoopProcessDescriptor procDesc) {
            this.proc = proc;
            this.procDesc = procDesc;
        }

        /**
         * Terminates process (kills it).
         */
        private void terminate() {
            // Guard against concurrent message sending.
            lock();

            try {
                terminated = true;

                if (!initFut.isDone())
                    initFut.listenAsync(new CI1<GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>>>() {
                        @Override public void apply(
                            GridFuture<GridBiTuple<Process, GridHadoopProcessDescriptor>> f) {
                            proc.destroy();
                        }
                    });
                else
                    proc.destroy();
            }
            finally {
                unlock();
            }
        }

        /**
         * @return Terminated flag.
         */
        private boolean terminated() {
            return terminated;
        }

        /**
         * Sets process tasks.
         *
         * @param tasks Tasks to set.
         */
        private void addTasks(Collection<GridHadoopTaskInfo> tasks) {
            this.tasks.addAll(tasks);
        }

        /**
         * Removes task when it was completed.
         *
         * @param task Task to remove.
         */
        private void removeTask(GridHadoopTaskInfo task) {
            if (tasks != null)
                tasks.remove(task);
        }

        /**
         * @return Collection of tasks.
         */
        private Collection<GridHadoopTaskInfo> tasks() {
            return tasks;
        }

        /**
         * @return Planned reducers.
         */
        private Collection<Integer> reducers() {
            return reducers;
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(HadoopProcess.class, this);
        }
    }

    /**
     *
     */
    private class GridHadoopProcessFuture extends GridFutureAdapter<GridBiTuple<Process, GridHadoopProcessDescriptor>> {
        /** */
        private static final long serialVersionUID = 0L;

        /** Child process ID. */
        private UUID childProcId;

        /** Job ID. */
        private GridHadoopJobId jobId;

        /** Process descriptor. */
        private GridHadoopProcessDescriptor desc;

        /** Running process. */
        private Process proc;

        /** Process started flag. */
        private volatile boolean procStarted;

        /** Reply received flag. */
        private volatile boolean replyReceived;

        /** Logger. */
        private final GridLogger log = GridHadoopExternalTaskExecutor.this.log;

        /**
         * Empty constructor.
         */
        public GridHadoopProcessFuture() {
            // No-op.
        }

        /**
         * @param ctx Kernal context.
         */
        private GridHadoopProcessFuture(UUID childProcId, GridHadoopJobId jobId, GridKernalContext ctx) {
            super(ctx);

            this.childProcId = childProcId;
            this.jobId = jobId;
        }

        /**
         * Process started callback.
         */
        public void onProcessStarted(Process proc) {
            this.proc = proc;

            procStarted = true;

            if (procStarted && replyReceived)
                onDone(F.t(proc, desc));
        }

        /**
         * Reply received callback.
         */
        public void onReplyReceived(GridHadoopProcessDescriptor desc) {
            assert childProcId.equals(desc.processId());

            this.desc = desc;

            replyReceived = true;

            if (procStarted && replyReceived)
                onDone(F.t(proc, desc));
        }

        /** {@inheritDoc} */
        @Override public boolean onDone(@Nullable GridBiTuple<Process, GridHadoopProcessDescriptor> res,
            @Nullable Throwable err) {
            if (err == null) {
                HadoopProcess proc = runningProcsByProcId.get(childProcId);

                assert proc != null;

                assert proc.initFut == this;

                proc.onInitialized(res.get1(), res.get2());

                if (!F.isEmpty(proc.reducers()))
                    jobTracker.onExternalMappersInitialized(jobId, proc.reducers(), desc);
            }
            else {
                // Clean up since init failed.
                runningProcsByJobId.remove(jobId);
                runningProcsByProcId.remove(childProcId);
            }

            if (super.onDone(res, err)) {
                if (err == null) {
                    if (log.isDebugEnabled())
                        log.debug("Initialized child process for external task execution [jobId=" + jobId +
                            ", desc=" + desc + ", initTime=" + duration() + ']');
                }
                else
                    U.error(log, "Failed to initialize child process for external task execution [jobId=" + jobId +
                        ", desc=" + desc + ']', err);

                return true;
            }

            return false;
        }
    }
}
