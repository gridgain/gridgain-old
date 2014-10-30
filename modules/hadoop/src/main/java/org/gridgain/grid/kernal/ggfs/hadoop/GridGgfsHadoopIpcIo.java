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

package org.gridgain.grid.kernal.ggfs.hadoop;

import org.apache.commons.logging.*;
import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.ggfs.common.*;
import org.gridgain.grid.util.*;
import org.gridgain.grid.util.ipc.*;
import org.gridgain.grid.util.ipc.shmem.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.concurrent.locks.*;

/**
 * IO layer implementation based on blocking IPC streams.
 */
@SuppressWarnings("FieldAccessedSynchronizedAndUnsynchronized")
public class GridGgfsHadoopIpcIo implements GridGgfsHadoopIo {
    /** Logger. */
    private Log log;

    /** Request futures map. */
    private ConcurrentMap<Long, GridGgfsHadoopFuture> reqMap =
        new ConcurrentHashMap8<>();

    /** Request ID counter. */
    private AtomicLong reqIdCnt = new AtomicLong();

    /** Endpoint. */
    private GridIpcEndpoint endpoint;

    /** Endpoint output stream. */
    private GridGgfsDataOutputStream out;

    /** Protocol. */
    private final GridGgfsMarshaller marsh;

    /** Client reader thread. */
    private Thread reader;

    /** Lock for graceful shutdown. */
    private final ReadWriteLock busyLock = new ReentrantReadWriteLock();

    /** Stopping flag. */
    private volatile boolean stopping;

    /** Server endpoint address. */
    private final String endpointAddr;

    /** Number of open file system sessions. */
    private final AtomicInteger activeCnt = new AtomicInteger(1);

    /** Event listeners. */
    private final Collection<GridGgfsHadoopIpcIoListener> lsnrs =
        new GridConcurrentHashSet<>();

    /** Cached connections. */
    private static final ConcurrentMap<String, GridGgfsHadoopIpcIo> ipcCache =
        new ConcurrentHashMap8<>();

    /** Striped lock that prevents multiple instance creation in {@link #get(Log, String)}. */
    private static final GridStripedLock initLock = new GridStripedLock(32);

    /**
     * @param endpointAddr Endpoint.
     * @param marsh Protocol.
     * @param log Logger to use.
     */
    public GridGgfsHadoopIpcIo(String endpointAddr, GridGgfsMarshaller marsh, Log log) {
        assert endpointAddr != null;
        assert marsh != null;

        this.endpointAddr = endpointAddr;
        this.marsh = marsh;
        this.log = log;
    }

    /**
     * Returns a started and valid instance of this class
     * for a given endpoint.
     *
     * @param log Logger to use for new instance.
     * @param endpoint Endpoint string.
     * @return New or existing cached instance, which is started and operational.
     * @throws IOException If new instance was created but failed to start.
     */
    public static GridGgfsHadoopIpcIo get(Log log, String endpoint) throws IOException {
        while (true) {
            GridGgfsHadoopIpcIo clientIo = ipcCache.get(endpoint);

            if (clientIo != null) {
                if (clientIo.acquire())
                    return clientIo;
                else
                    // If concurrent close.
                    ipcCache.remove(endpoint, clientIo);
            }
            else {
                Lock lock = initLock.getLock(endpoint);

                lock.lock();

                try {
                    clientIo = ipcCache.get(endpoint);

                    if (clientIo != null) { // Perform double check.
                        if (clientIo.acquire())
                            return clientIo;
                        else
                            // If concurrent close.
                            ipcCache.remove(endpoint, clientIo);
                    }

                    // Otherwise try creating a new one.
                    clientIo = new GridGgfsHadoopIpcIo(endpoint, new GridGgfsMarshaller(), log);

                    try {
                        clientIo.start();
                    }
                    catch (GridException e) {
                        throw new IOException(e.getMessage(), e);
                    }

                    GridGgfsHadoopIpcIo old = ipcCache.putIfAbsent(endpoint, clientIo);

                    // Put in exclusive lock.
                    assert old == null;

                    return clientIo;
                }
                finally {
                    lock.unlock();
                }
            }
        }
    }

    /**
     * Increases usage count for this instance.
     *
     * @return {@code true} if usage count is greater than zero.
     */
    private boolean acquire() {
        while (true) {
            int cnt = activeCnt.get();

            if (cnt == 0) {
                if (log.isDebugEnabled())
                    log.debug("IPC IO not acquired (count was 0): " + this);

                return false;
            }

            // Need to make sure that no-one decremented count in between.
            if (activeCnt.compareAndSet(cnt, cnt + 1)) {
                if (log.isDebugEnabled())
                    log.debug("IPC IO acquired: " + this);

                return true;
            }
        }
    }

    /**
     * Releases this instance, decrementing usage count.
     * <p>
     * If usage count becomes zero, the instance is stopped
     * and removed from cache.
     */
    public void release() {
        while (true) {
            int cnt = activeCnt.get();

            if (cnt == 0) {
                if (log.isDebugEnabled())
                    log.debug("IPC IO not released (count was 0): " + this);

                return;
            }

            if (activeCnt.compareAndSet(cnt, cnt - 1)) {
                if (cnt == 1) {
                    ipcCache.remove(endpointAddr, this);

                    if (log.isDebugEnabled())
                        log.debug("IPC IO stopping as unused: " + this);

                    stop();
                }
                else if (log.isDebugEnabled())
                    log.debug("IPC IO released: " + this);

                return;
            }
        }
    }

    /**
     * Closes this IO instance, removing it from cache.
     */
    public void forceClose() {
        if (ipcCache.remove(endpointAddr, this))
            stop();
    }

    /**
     * Starts the IO.
     *
     * @throws GridException If failed to connect the endpoint.
     */
    private void start() throws GridException {
        boolean success = false;

        try {
            endpoint = GridIpcEndpointFactory.connectEndpoint(
                endpointAddr, new GridLoggerProxy(new GridGgfsHadoopJclLogger(log), null, null, ""));

            out = new GridGgfsDataOutputStream(new BufferedOutputStream(endpoint.outputStream()));

            reader = new ReaderThread();

            // Required for Hadoop 2.x
            reader.setDaemon(true);

            reader.start();

            success = true;
        }
        catch (GridException e) {
            GridIpcOutOfSystemResourcesException resEx = e.getCause(GridIpcOutOfSystemResourcesException.class);

            if (resEx != null)
                throw new GridException(GridIpcSharedMemoryServerEndpoint.OUT_OF_RESOURCES_MSG, resEx);

            throw e;
        }
        finally {
            if (!success)
                stop();
        }
    }

    /**
     * Shuts down the IO. No send requests will be accepted anymore, all pending futures will be failed.
     * Close listeners will be invoked as if connection is closed by server.
     */
    private void stop() {
        close0(null);

        if (reader != null) {
            try {
                U.interrupt(reader);
                U.join(reader);

                reader = null;
            }
            catch (GridInterruptedException ignored) {
                Thread.currentThread().interrupt();

                log.warn("Got interrupted while waiting for reader thread to shut down (will return).");
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void addEventListener(GridGgfsHadoopIpcIoListener lsnr) {
        if (!busyLock.readLock().tryLock()) {
            lsnr.onClose();

            return;
        }

        boolean invokeNow = false;

        try {
            invokeNow = stopping;

            if (!invokeNow)
                lsnrs.add(lsnr);
        }
        finally {
            busyLock.readLock().unlock();

            if (invokeNow)
                lsnr.onClose();
        }
    }

    /** {@inheritDoc} */
    @Override public void removeEventListener(GridGgfsHadoopIpcIoListener lsnr) {
        lsnrs.remove(lsnr);
    }

    /** {@inheritDoc} */
    @Override public GridPlainFuture<GridGgfsMessage> send(GridGgfsMessage msg) throws GridException {
        return send(msg, null, 0, 0);
    }

    /** {@inheritDoc} */
    @Override public <T> GridPlainFuture<T> send(GridGgfsMessage msg, @Nullable byte[] outBuf, int outOff,
        int outLen) throws GridException {
        assert outBuf == null || msg.command() == GridGgfsIpcCommand.READ_BLOCK;

        if (!busyLock.readLock().tryLock())
            throw new GridGgfsHadoopCommunicationException("Failed to send message (client is being concurrently " +
                "closed).");

        try {
            if (stopping)
                throw new GridGgfsHadoopCommunicationException("Failed to send message (client is being concurrently " +
                    "closed).");

            long reqId = reqIdCnt.getAndIncrement();

            GridGgfsHadoopFuture<T> fut = new GridGgfsHadoopFuture<>();

            fut.outputBuffer(outBuf);
            fut.outputOffset(outOff);
            fut.outputLength(outLen);
            fut.read(msg.command() == GridGgfsIpcCommand.READ_BLOCK);

            GridGgfsHadoopFuture oldFut = reqMap.putIfAbsent(reqId, fut);

            assert oldFut == null;

            if (log.isDebugEnabled())
                log.debug("Sending GGFS message [reqId=" + reqId + ", msg=" + msg + ']');

            byte[] hdr = GridGgfsMarshaller.createHeader(reqId, msg.command());

            GridException err = null;

            try {
                synchronized (this) {
                    marsh.marshall(msg, hdr, out);

                    out.flush(); // Blocking operation + sometimes system call.
                }
            }
            catch (GridException e) {
                err = e;
            }
            catch (IOException e) {
                err = new GridGgfsHadoopCommunicationException(e);
            }

            if (err != null) {
                reqMap.remove(reqId, fut);

                fut.onDone(err);
            }

            return fut;
        }
        finally {
            busyLock.readLock().unlock();
        }
    }

    /** {@inheritDoc} */
    @Override public void sendPlain(GridGgfsMessage msg) throws GridException {
        if (!busyLock.readLock().tryLock())
            throw new GridGgfsHadoopCommunicationException("Failed to send message (client is being " +
                "concurrently closed).");

        try {
            if (stopping)
                throw new GridGgfsHadoopCommunicationException("Failed to send message (client is being concurrently closed).");

            assert msg.command() == GridGgfsIpcCommand.WRITE_BLOCK;

            GridGgfsStreamControlRequest req = (GridGgfsStreamControlRequest)msg;

            byte[] hdr = GridGgfsMarshaller.createHeader(-1, GridGgfsIpcCommand.WRITE_BLOCK);

            U.longToBytes(req.streamId(), hdr, 12);
            U.intToBytes(req.length(), hdr, 20);

            synchronized (this) {
                out.write(hdr);
                out.write(req.data(), (int)req.position(), req.length());

                out.flush();
            }
        }
        catch (IOException e) {
            throw new GridGgfsHadoopCommunicationException(e);
        }
        finally {
            busyLock.readLock().unlock();
        }
    }

    /**
     * Closes client but does not wait.
     *
     * @param err Error.
     */
    private void close0(@Nullable Throwable err) {
        busyLock.writeLock().lock();

        try {
            if (stopping)
                return;

            stopping = true;
        }
        finally {
            busyLock.writeLock().unlock();
        }

        if (err == null)
            err = new GridException("Failed to perform request (connection was concurrently closed before response " +
                "is received).");

        // Clean up resources.
        U.closeQuiet(out);

        if (endpoint != null)
            endpoint.close();

        // Unwind futures. We can safely iterate here because no more futures will be added.
        Iterator<GridGgfsHadoopFuture> it = reqMap.values().iterator();

        while (it.hasNext()) {
            GridGgfsHadoopFuture fut = it.next();

            fut.onDone(err);

            it.remove();
        }

        for (GridGgfsHadoopIpcIoListener lsnr : lsnrs)
            lsnr.onClose();
    }

    /**
     * Do not extend {@code GridThread} to minimize class dependencies.
     */
    private class ReaderThread extends Thread {
        /** {@inheritDoc} */
        @SuppressWarnings("unchecked")
        @Override public void run() {
            // Error to fail pending futures.
            Throwable err = null;

            try {
                InputStream in = endpoint.inputStream();

                GridGgfsDataInputStream dis = new GridGgfsDataInputStream(in);

                byte[] hdr = new byte[GridGgfsMarshaller.HEADER_SIZE];
                byte[] msgHdr = new byte[GridGgfsControlResponse.RES_HEADER_SIZE];

                while (!Thread.currentThread().isInterrupted()) {
                    dis.readFully(hdr);

                    long reqId = U.bytesToLong(hdr, 0);

                    // We don't wait for write responses, therefore reqId is -1.
                    if (reqId == -1) {
                        // We received a response which normally should not be sent. It must contain an error.
                        dis.readFully(msgHdr);

                        assert msgHdr[4] != 0;

                        String errMsg = dis.readUTF();

                        // Error code.
                        dis.readInt();

                        long streamId = dis.readLong();

                        for (GridGgfsHadoopIpcIoListener lsnr : lsnrs)
                            lsnr.onError(streamId, errMsg);
                    }
                    else {
                        GridGgfsHadoopFuture<Object> fut = reqMap.remove(reqId);

                        if (fut == null) {
                            String msg = "Failed to read response from server: response closure is unavailable for " +
                                "requestId (will close connection):" + reqId;

                            log.warn(msg);

                            err = new GridException(msg);

                            break;
                        }
                        else {
                            try {
                                GridGgfsIpcCommand cmd = GridGgfsIpcCommand.valueOf(U.bytesToInt(hdr, 8));

                                if (log.isDebugEnabled())
                                    log.debug("Received GGFS response [reqId=" + reqId + ", cmd=" + cmd + ']');

                                Object res = null;

                                if (fut.read()) {
                                    dis.readFully(msgHdr);

                                    boolean hasErr = msgHdr[4] != 0;

                                    if (hasErr) {
                                        String errMsg = dis.readUTF();

                                        // Error code.
                                        Integer errCode = dis.readInt();

                                        GridGgfsControlResponse.throwError(errCode, errMsg);
                                    }

                                    int blockLen = U.bytesToInt(msgHdr, 5);

                                    int readLen = Math.min(blockLen, fut.outputLength());

                                    if (readLen > 0) {
                                        assert fut.outputBuffer() != null;

                                        dis.readFully(fut.outputBuffer(), fut.outputOffset(), readLen);
                                    }

                                    if (readLen != blockLen) {
                                        byte[] buf = new byte[blockLen - readLen];

                                        dis.readFully(buf);

                                        res = buf;
                                    }
                                }
                                else
                                    res = marsh.unmarshall(cmd, hdr, dis);

                                fut.onDone(res);
                            }
                            catch (GridException e) {
                                if (log.isDebugEnabled())
                                    log.debug("Failed to apply response closure (will fail request future): " +
                                        e.getMessage());

                                fut.onDone(e);

                                err = e;
                            }
                        }
                    }
                }
            }
            catch (EOFException ignored) {
                err = new GridException("Failed to read response from server (connection was closed by remote peer).");
            }
            catch (IOException e) {
                if (!stopping)
                    log.error("Failed to read data (connection will be closed)", e);

                err = new GridGgfsHadoopCommunicationException(e);
            }
            catch (GridException e) {
                if (!stopping)
                    log.error("Failed to obtain endpoint input stream (connection will be closed)", e);

                err = e;
            }
            finally {
                close0(err);
            }
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return getClass().getSimpleName() + " [endpointAddr=" + endpointAddr + ", activeCnt=" + activeCnt +
            ", stopping=" + stopping + ']';
    }
}
