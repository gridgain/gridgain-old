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

package org.gridgain.grid;

import org.gridgain.grid.util.mbean.*;

import java.io.*;

/**
 * This class represents runtime information on a node. Apart from obvious
 * statistical value, this information is used for implementation of
 * load balancing, failover, and collision SPIs. For example, collision SPI
 * in combination with fail-over SPI could check if other nodes don't have
 * any active or waiting jobs and fail-over some jobs to those nodes.
 * <p>
 * Node metrics for any node can be accessed via {@link GridNode#metrics()}
 * method. Keep in mind that there will be a certain network delay (usually
 * equal to heartbeat delay) for the accuracy of node metrics. However, when accessing
 * metrics on local node {@link Grid#localNode() Grid.localNode().getMetrics()}
 * the metrics are always accurate and up to date.
 * <p>
 * Local node metrics are registered as {@code MBean} and can be accessed from
 * any JMX management console. The simplest way is to use standard {@code jconsole}
 * that comes with JDK as it also provides ability to view any node parameter
 * as a graph.
 * @see GridProjectionMetrics
 */
public interface GridNodeMetrics extends Serializable {
    /**
     * Gets last update time of this node metrics.
     *
     * @return Last update time.
     */
    @GridMBeanDescription("Last update time of this node metrics.")
    public long getLastUpdateTime();

    /**
     * Gets maximum number of jobs that ever ran concurrently on this node.
     * Note that this different from {@link #getTotalExecutedJobs()}
     * metric and only reflects maximum number of jobs that ran at the same time.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Maximum number of jobs that ever ran concurrently on this node.
     */
    @GridMBeanDescription("Maximum number of jobs that ever ran concurrently on this node.")
    public int getMaximumActiveJobs();

    /**
     * Gets number of currently active jobs concurrently executing on the node.
     *
     * @return Number of currently active jobs concurrently executing on the node.
     */
    @GridMBeanDescription("Number of currently active jobs concurrently executing on the node.")
    public int getCurrentActiveJobs();

    /**
     * Gets average number of active jobs concurrently executing on the node.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Average number of active jobs.
     */
    @GridMBeanDescription("Average number of active jobs concurrently executing on the node.")
    public float getAverageActiveJobs();

    /**
     * Gets maximum number of waiting jobs this node had.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Maximum number of waiting jobs.
     */
    @GridMBeanDescription("Maximum number of waiting jobs this node had.")
    public int getMaximumWaitingJobs();

    /**
     * Gets number of queued jobs currently waiting to be executed.
     *
     * @return Number of queued jobs currently waiting to be executed.
     */
    @GridMBeanDescription("Number of queued jobs currently waiting to be executed.")
    public int getCurrentWaitingJobs();

    /**
     * Gets average number of waiting jobs this node had queued.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Average number of waiting jobs.
     */
    @GridMBeanDescription("Average number of waiting jobs this node had queued.")
    public float getAverageWaitingJobs();

    /**
     * Gets maximum number of jobs rejected at once during a single collision resolution
     * operation.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Maximum number of jobs rejected at once.
     */
    @GridMBeanDescription("Maximum number of jobs rejected at once during a single collision resolution operation.")
    public int getMaximumRejectedJobs();

    /**
     * Gets number of jobs rejected after more recent collision resolution operation.
     *
     * @return Number of jobs rejected after more recent collision resolution operation.
     */
    @GridMBeanDescription("Number of jobs rejected after more recent collision resolution operation.")
    public int getCurrentRejectedJobs();

    /**
     * Gets average number of jobs this node rejects during collision resolution operations.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of grid configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Average number of jobs this node rejects during collision resolution operations.
     */
    @GridMBeanDescription("Average number of jobs this node rejects during collision resolution operations.")
    public float getAverageRejectedJobs();

    /**
     * Gets total number of jobs this node rejects during collision resolution operations since node startup.
     * <p>
     * <b>Note:</b> Unlike most of other aggregation metrics this metric is not calculated over history
     * but over the entire node life.
     *
     * @return Total number of jobs this node rejects during collision resolution
     *      operations since node startup.
     */
    @GridMBeanDescription(
        "Total number of jobs this node rejects during collision resolution operations since node startup.")
    public int getTotalRejectedJobs();

    /**
     * Gets maximum number of cancelled jobs this node ever had running
     * concurrently.
     *
     * @return Maximum number of cancelled jobs.
     */
    @GridMBeanDescription("Maximum number of cancelled jobs this node ever had running concurrently.")
    public int getMaximumCancelledJobs();

    /**
     * Gets number of cancelled jobs that are still running. Just like
     * regular java threads, jobs will receive cancel notification, but
     * it's ultimately up to the job itself to gracefully exit.
     *
     * @return Number of cancelled jobs that are still running.
     */
    @GridMBeanDescription("Number of cancelled jobs that are still running.")
    public int getCurrentCancelledJobs();

    /**
     * Gets average number of cancelled jobs this node ever had running
     * concurrently.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Average number of cancelled jobs.
     */
    @GridMBeanDescription("Average number of cancelled jobs this node ever had running concurrently.")
    public float getAverageCancelledJobs();

    /**
     * Gets number of cancelled jobs since node startup.
     * <p>
     * <b>Note:</b> Unlike most of other aggregation metrics this metric is not calculated over history
     * but over the entire node life.
     *
     * @return Total number of cancelled jobs since node startup.
     */
    @GridMBeanDescription("Total number of cancelled jobs since node startup.")
    public int getTotalCancelledJobs();

    /**
     * Gets total number of jobs handled by the node since node startup.
     * <p>
     * <b>Note:</b> Unlike most of other aggregation metrics this metric is not calculated over history
     * but over the entire node life.
     *
     * @return Total number of jobs handled by the node since node startup.
     */
    @GridMBeanDescription("Total number of jobs handled by the node.")
    public int getTotalExecutedJobs();

    /**
     * Gets maximum time a job ever spent waiting in a queue to be executed.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Maximum waiting time.
     */
    @GridMBeanDescription("Maximum time a job ever spent waiting in a queue to be executed.")
    public long getMaximumJobWaitTime();

    /**
     * Gets current time an oldest jobs has spent waiting to be executed.
     *
     * @return Current wait time of oldest job.
     */
    @GridMBeanDescription("Current wait time of oldest job.")
    public long getCurrentJobWaitTime();

    /**
     * Gets average time jobs spend waiting in the queue to be executed.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Average job wait time.
     */
    @GridMBeanDescription("Average time jobs spend waiting in the queue to be executed.")
    public double getAverageJobWaitTime();

    /**
     * Gets time it took to execute the longest job on the node.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Time it took to execute the longest job on the node.
     */
    @GridMBeanDescription("Time it took to execute the longest job on the node.")
    public long getMaximumJobExecuteTime();

    /**
     * Gets longest time a current job has been executing for.
     *
     * @return Longest time a current job has been executing for.
     */
    @GridMBeanDescription("Longest time a current job has been executing for.")
    public long getCurrentJobExecuteTime();

    /**
     * Gets average time a job takes to execute on the node.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Average job execution time.
     */
    @GridMBeanDescription("Average time a job takes to execute on the node.")
    public double getAverageJobExecuteTime();

    /**
     * Gets total number of tasks handled by the node.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Total number of jobs handled by the node.
     */
    @GridMBeanDescription("Total number of tasks handled by the node.")
    public int getTotalExecutedTasks();

    /**
     * Gets total time this node spent executing jobs.
     *
     * @return Total time this node spent executing jobs.
     */
    @GridMBeanDescription("Total time this node spent executing jobs.")
    public long getTotalBusyTime();

    /**
     * Gets total time this node spent idling (not executing any jobs).
     *
     * @return Gets total time this node spent idling.
     */
    @GridMBeanDescription("Total time this node spent idling (not executing any jobs).")
    public long getTotalIdleTime();

    /**
     * Gets time this node spend idling since executing last job.
     *
     * @return Time this node spend idling since executing last job.
     */
    @GridMBeanDescription("Time this node spend idling since executing last job.")
    public long getCurrentIdleTime();

    /**
     * Gets percentage of time this node is busy executing jobs vs. idling.
     *
     * @return Percentage of time this node is busy (value is less than
     *      or equal to {@code 1} and greater than or equal to {@code 0})
     */
    @GridMBeanDescription("Percentage of time this node is busy executing jobs vs. idling.")
    public float getBusyTimePercentage();

    /**
     * Gets percentage of time this node is idling vs. executing jobs.
     *
     * @return Percentage of time this node is idle (value is less than
     *      or equal to {@code 1} and greater than or equal to {@code 0})
     */
    @GridMBeanDescription("Percentage of time this node is idling vs. executing jobs.")
    public float getIdleTimePercentage();

    /**
     * Returns the number of CPUs available to the Java Virtual Machine.
     * This method is equivalent to the {@link Runtime#availableProcessors()}
     * method.
     * <p>
     * Note that this value may change during successive invocations of the
     * virtual machine.
     *
     * @return The number of processors available to the virtual
     *      machine, never smaller than one.
     */
    @GridMBeanDescription("The number of CPUs available to the Java Virtual Machine.")
    public int getTotalCpus();

    /**
     * Returns the CPU usage usage in {@code [0, 1]} range.
     * The exact way how this number is calculated depends on SPI implementation.
     * <p>
     * If the CPU usage is not available, a negative value is returned.
     * <p>
     * This method is designed to provide a hint about the system load
     * and may be queried frequently. The load average may be unavailable on
     * some platform where it is expensive to implement this method.
     *
     * @return The estimated CPU usage in {@code [0, 1]} range.
     *      Negative value if not available.
     */
    @GridMBeanDescription("The system load average; or a negative value if not available.")
    public double getCurrentCpuLoad();

    /**
     * Gets average of CPU load values over all metrics kept in the history.
     * <p>
     * <b>Note:</b> all aggregated metrics like average, minimum, maximum, total, count are
     * calculated over all the metrics kept in history. The
     * history size is set via either one or both of configuration settings:
     * <ul>
     * <li>{@link GridConfiguration#getMetricsExpireTime()}</li>
     * <li>{@link GridConfiguration#getMetricsHistorySize()}</li>
     * </ul>
     *
     * @return Average of CPU load value in {@code [0, 1]} range over all metrics kept
     *      in the history.
     */
    @GridMBeanDescription("Average of CPU load values over all metrics kept in the history.")
    public double getAverageCpuLoad();

    /**
     * Returns average time spent in CG since the last update.
     *
     * @return Average time spent in CG since the last update.
     */
    @GridMBeanDescription("Average time spent in CG since the last update.")
    public double getCurrentGcCpuLoad();

    /**
     * Returns the amount of heap memory in bytes that the JVM
     * initially requests from the operating system for memory management.
     * This method returns {@code -1} if the initial memory size is undefined.
     * <p>
     * This value represents a setting of the heap memory for Java VM and is
     * not a sum of all initial heap values for all memory pools.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The initial size of memory in bytes; {@code -1} if undefined.
     */
    @GridMBeanDescription("The initial size of memory in bytes; -1 if undefined.")
    public long getHeapMemoryInitialized();

    /**
     * Returns the current heap size that is used for object allocation.
     * The heap consists of one or more memory pools. This value is
     * the sum of {@code used} heap memory values of all heap memory pools.
     * <p>
     * The amount of used memory in the returned is the amount of memory
     * occupied by both live objects and garbage objects that have not
     * been collected, if any.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return Amount of heap memory used.
     */
    @GridMBeanDescription("Current heap size that is used for object allocation.")
    public long getHeapMemoryUsed();

    /**
     * Returns the amount of heap memory in bytes that is committed for
     * the JVM to use. This amount of memory is
     * guaranteed for the JVM to use.
     * The heap consists of one or more memory pools. This value is
     * the sum of {@code committed} heap memory values of all heap memory pools.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The amount of committed memory in bytes.
     */
    @GridMBeanDescription("The amount of committed memory in bytes.")
    public long getHeapMemoryCommitted();

    /**
     * Returns the maximum amount of heap memory in bytes that can be
     * used for memory management. This method returns {@code -1}
     * if the maximum memory size is undefined.
     * <p>
     * This amount of memory is not guaranteed to be available
     * for memory management if it is greater than the amount of
     * committed memory. The JVM may fail to allocate
     * memory even if the amount of used memory does not exceed this
     * maximum size.
     * <p>
     * This value represents a setting of the heap memory for Java VM and is
     * not a sum of all initial heap values for all memory pools.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The maximum amount of memory in bytes; {@code -1} if undefined.
     */
    @GridMBeanDescription("The maximum amount of memory in bytes; -1 if undefined.")
    public long getHeapMemoryMaximum();

    /**
     * Returns the amount of non-heap memory in bytes that the JVM
     * initially requests from the operating system for memory management.
     * This method returns {@code -1} if the initial memory size is undefined.
     * <p>
     * This value represents a setting of non-heap memory for Java VM and is
     * not a sum of all initial heap values for all memory pools.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The initial size of memory in bytes; {@code -1} if undefined.
     */
    @GridMBeanDescription("The initial size of memory in bytes; -1 if undefined.")
    public long getNonHeapMemoryInitialized();

    /**
     * Returns the current non-heap memory size that is used by Java VM.
     * The non-heap memory consists of one or more memory pools. This value is
     * the sum of {@code used} non-heap memory values of all non-heap memory pools.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return Amount of none-heap memory used.
     */
    @GridMBeanDescription("Current non-heap memory size that is used by Java VM.")
    public long getNonHeapMemoryUsed();

    /**
     * Returns the amount of non-heap memory in bytes that is committed for
     * the JVM to use. This amount of memory is
     * guaranteed for the JVM to use.
     * The non-heap memory consists of one or more memory pools. This value is
     * the sum of {@code committed} non-heap memory values of all non-heap memory pools.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The amount of committed memory in bytes.
     */
    @GridMBeanDescription("Amount of non-heap memory in bytes that is committed for the JVM to use.")
    public long getNonHeapMemoryCommitted();

    /**
     * Returns the maximum amount of non-heap memory in bytes that can be
     * used for memory management. This method returns {@code -1}
     * if the maximum memory size is undefined.
     * <p>
     * This amount of memory is not guaranteed to be available
     * for memory management if it is greater than the amount of
     * committed memory.  The JVM may fail to allocate
     * memory even if the amount of used memory does not exceed this
     * maximum size.
     * <p>
     * This value represents a setting of the non-heap memory for Java VM and is
     * not a sum of all initial non-heap values for all memory pools.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The maximum amount of memory in bytes; {@code -1} if undefined.
     */
    @GridMBeanDescription("Maximum amount of non-heap memory in bytes that can " +
        "be used for memory management. -1 if undefined.")
    public long getNonHeapMemoryMaximum();

    /**
     * Returns the uptime of the JVM in milliseconds.
     *
     * @return Uptime of the JVM in milliseconds.
     */
    @GridMBeanDescription("Uptime of the JVM in milliseconds.")
    public long getUpTime();

    /**
     * Returns the start time of the JVM in milliseconds.
     * This method returns the approximate time when the Java virtual
     * machine started.
     *
     * @return Start time of the JVM in milliseconds.
     */
    @GridMBeanDescription("Start time of the JVM in milliseconds.")
    public long getStartTime();

    /**
     * Returns the start time of grid node in milliseconds.
     * There can be several grid nodes started in one JVM, so JVM start time will be
     * the same for all of them, but node start time will be different.
     *
     * @return Start time of the grid node in milliseconds.
     */
    public long getNodeStartTime();

    /**
     * Returns the current number of live threads including both
     * daemon and non-daemon threads.
     *
     * @return Current number of live threads.
     */
    @GridMBeanDescription("Current number of live threads.")
    public int getCurrentThreadCount();

    /**
     * Returns the maximum live thread count since the JVM
     * started or peak was reset.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The peak live thread count.
     */
    @GridMBeanDescription("The peak live thread count.")
    public int getMaximumThreadCount();

    /**
     * Returns the total number of threads created and also started
     * since the JVM started.
     * <p>
     * <b>Note:</b> this is <b>not</b> an aggregated metric and it's calculated
     * from the time of the node's startup.
     *
     * @return The total number of threads started.
     */
    @GridMBeanDescription("The total number of threads started.")
    public long getTotalStartedThreadCount();

    /**
     * Returns the current number of live daemon threads.
     *
     * @return Current number of live daemon threads.
     */
    @GridMBeanDescription("Current number of live daemon threads.")
    public int getCurrentDaemonThreadCount();

    /**
     * In-Memory Data Grid assigns incremental versions to all cache operations. This method provides
     * the latest data version on the node.
     *
     * @return Last data version.
     */
    @GridMBeanDescription("Last data version.")
    public long getLastDataVersion();

    /**
     * Gets sent messages count.
     *
     * @return Sent messages count.
     */
    @GridMBeanDescription("Sent messages count.")
    public int getSentMessagesCount();

    /**
     * Gets sent bytes count.
     *
     * @return Sent bytes count.
     */
    @GridMBeanDescription("Sent bytes count.")
    public long getSentBytesCount();

    /**
     * Gets received messages count.
     *
     * @return Received messages count.
     */
    @GridMBeanDescription("Received messages count.")
    public int getReceivedMessagesCount();

    /**
     * Gets received bytes count.
     *
     * @return Received bytes count.
     */
    @GridMBeanDescription("Received bytes count.")
    public long getReceivedBytesCount();

    /**
     * Gets outbound messages queue size.
     *
     * @return Outbound messages queue size.
     */
    @GridMBeanDescription("Outbound messages queue size.")
    public int getOutboundMessagesQueueSize();
}
