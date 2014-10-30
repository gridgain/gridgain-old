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

/**
 * MBean that provides access to information about executor service.
 */
@GridMBeanDescription("MBean that provides access to information about executor service.")
public interface GridExecutorServiceMBean {
    /**
     * Returns the approximate number of threads that are actively executing tasks.
     *
     * @return The number of threads.
     */
    @GridMBeanDescription("Approximate number of threads that are actively executing tasks.")
    public int getActiveCount();

    /**
     * Returns the approximate total number of tasks that have completed execution.
     * Because the states of tasks and threads may change dynamically during
     * computation, the returned value is only an approximation, but one that
     * does not ever decrease across successive calls.
     *
     * @return The number of tasks.
     */
    @GridMBeanDescription("Approximate total number of tasks that have completed execution.")
    public long getCompletedTaskCount();

    /**
     * Returns the core number of threads.
     *
     * @return The core number of threads.
     */
    @GridMBeanDescription("The core number of threads.")
    public int getCorePoolSize();

    /**
     * Returns the largest number of threads that have ever
     * simultaneously been in the pool.
     *
     * @return The number of threads.
     */
    @GridMBeanDescription("Largest number of threads that have ever simultaneously been in the pool.")
    public int getLargestPoolSize();

    /**
     * Returns the maximum allowed number of threads.
     *
     * @return The maximum allowed number of threads.
     */
    @GridMBeanDescription("The maximum allowed number of threads.")
    public int getMaximumPoolSize();

    /**
     * Returns the current number of threads in the pool.
     *
     * @return The number of threads.
     */
    @GridMBeanDescription("Current number of threads in the pool.")
    public int getPoolSize();

    /**
     * Returns the approximate total number of tasks that have been scheduled
     * for execution. Because the states of tasks and threads may change dynamically
     * during computation, the returned value is only an approximation, but
     * one that does not ever decrease across successive calls.
     *
     * @return The number of tasks.
     */
    @GridMBeanDescription("Approximate total number of tasks that have been scheduled for execution.")
    public long getTaskCount();

    /**
     * Gets current size of the execution queue. This queue buffers local
     * executions when there are not threads available for processing in the pool.
     *
     * @return Current size of the execution queue.
     */
    @GridMBeanDescription("Current size of the execution queue.")
    public int getQueueSize();

    /**
     * Returns the thread keep-alive time, which is the amount of time which threads
     * in excess of the core pool size may remain idle before being terminated.
     *
     * @return Keep alive time.
     */
    @GridMBeanDescription("Thread keep-alive time, which is the amount of time which threads in excess of " +
        "the core pool size may remain idle before being terminated.")
    public long getKeepAliveTime();

    /**
     * Returns {@code true} if this executor has been shut down.
     *
     * @return {@code True} if this executor has been shut down.
     */
    @GridMBeanDescription("True if this executor has been shut down.")
    public boolean isShutdown();

    /**
     * Returns {@code true} if all tasks have completed following shut down. Note that
     * {@code isTerminated()} is never {@code true} unless either {@code shutdown()} or
     * {@code shutdownNow()} was called first.
     *
     * @return {@code True} if all tasks have completed following shut down.
     */
    @GridMBeanDescription("True if all tasks have completed following shut down.")
    public boolean isTerminated();

    /**
     * Returns {@code true} if this executor is in the process of terminating after
     * {@code shutdown()} or {@code shutdownNow()} but has not completely terminated.
     * This method may be useful for debugging. A return of {@code true} reported a
     * sufficient period after shutdown may indicate that submitted tasks have ignored
     * or suppressed interruption, causing this executor not to properly terminate.
     *
     * @return {@code True} if terminating but not yet terminated.
     */
    @GridMBeanDescription("True if terminating but not yet terminated.")
    public boolean isTerminating();

    /**
     * Returns the class name of current rejection handler.
     *
     * @return Class name of current rejection handler.
     */
    @GridMBeanDescription("Class name of current rejection handler.")
    public String getRejectedExecutionHandlerClass();

    /**
     * Returns the class name of thread factory used to create new threads.
     *
     * @return Class name of thread factory used to create new threads.
     */
    @GridMBeanDescription("Class name of thread factory used to create new threads.")
    public String getThreadFactoryClass();
}
