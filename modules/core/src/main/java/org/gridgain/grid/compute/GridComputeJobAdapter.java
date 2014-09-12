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

package org.gridgain.grid.compute;

import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.util.concurrent.*;

/**
 * Convenience adapter for {@link GridComputeJob} implementations. It provides the
 * following functionality:
 * <ul>
 * <li>
 *      Default implementation of {@link GridComputeJob#cancel()} method and ability
 *      to check whether cancellation occurred with {@link #isCancelled()} method.
 * </li>
 * <li>
 *      Ability to set and get job arguments via {@link #setArguments(Object...)}
 *      and {@link #argument(int)} methods.
 * </li>
 * </ul>
 */
public abstract class GridComputeJobAdapter implements GridComputeJob, Callable<Object> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Job argument. */
    private Object[] args;

    /** Cancellation flag. */
    private transient volatile boolean cancelled;

    /**
     * No-arg constructor.
     */
    protected GridComputeJobAdapter() {
        /* No-op. */
    }

    /**
     * Creates job with one arguments. This constructor exists for better
     * backward compatibility with internal GridGain 2.x code.
     *
     * @param arg Job argument.
     */
    protected GridComputeJobAdapter(@Nullable Object arg) {
        args = new Object[]{arg};
    }

    /**
     * Creates job with specified arguments.
     *
     * @param args Optional job arguments.
     */
    protected GridComputeJobAdapter(@Nullable Object... args) {
        this.args = args;
    }

    /**
     * Sets given arguments.
     *
     * @param args Optional job arguments to set.
     */
    public void setArguments(@Nullable Object... args) {
        this.args = args;
    }

    /** {@inheritDoc} */
    @Override public void cancel() {
        cancelled = true;
    }

    /**
     * This method tests whether or not this job was cancelled. This method
     * is thread-safe and can be called without extra synchronization.
     * <p>
     * This method can be periodically called in {@link GridComputeJob#execute()} method
     * implementation to check whether or not this job cancelled. Note that system
     * calls {@link #cancel()} method only as a hint and this is a responsibility of
     * the implementation of the job to properly cancel its execution.
     *
     * @return {@code true} if this job was cancelled, {@code false} otherwise.
     */
    protected final boolean isCancelled() {
        return cancelled;
    }

    /**
     * Gets job argument.
     *
     * @param idx Index of the argument.
     * @param <T> Type of the argument to return.
     * @return Job argument.
     * @throws NullPointerException Thrown in case when there no arguments set.
     * @throws IllegalArgumentException Thrown if index is invalid.
     */
    @SuppressWarnings("unchecked")
    @Nullable public <T> T argument(int idx) {
        A.notNull(args, "args");
        A.ensure(idx >= 0 && idx < args.length, "idx >= 0 && idx < args.length");

        return (T)args[idx];
    }

    /**
     * Gets array of job arguments. Note that changes to this array may
     * affect job execution.
     *
     * @return Array of job arguments.
     */
    @Nullable Object[] arguments() {
        return args;
    }

    /** {@inheritDoc} */
    @Nullable @Override public final Object call() throws Exception {
        return execute();
    }
}
