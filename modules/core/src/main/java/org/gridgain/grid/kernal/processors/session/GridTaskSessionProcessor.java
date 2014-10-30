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

package org.gridgain.grid.kernal.processors.session;

import org.gridgain.grid.*;
import org.gridgain.grid.compute.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.managers.deployment.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.util.typedef.*;
import org.jdk8.backport.*;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.concurrent.*;

/**
 */
public class GridTaskSessionProcessor extends GridProcessorAdapter {
    /** Sessions (initialized to 2K number of concurrent sessions). */
    private final ConcurrentMap<GridUuid, GridTaskSessionImpl> sesMap =
        new ConcurrentHashMap8<>(2048);

    /**
     * @param ctx Grid kernal context.
     */
    public GridTaskSessionProcessor(GridKernalContext ctx) {
        super(ctx);
    }

    /**
     * Starts session processor.
     */
    @Override public void start() throws GridException {
        if (log.isDebugEnabled())
            log.debug("Session processor started.");
    }

    /**
     * Stops session processor.
     */
    @Override public void stop(boolean cancel) throws GridException {
        if (log.isDebugEnabled())
            log.debug("Session processor stopped.");
    }

    /**
     * @param sesId Session ID.
     * @param taskNodeId Task node ID.
     * @param taskName Task name.
     * @param dep Deployment.
     * @param taskClsName Task class name.
     * @param top Topology.
     * @param startTime Execution start time.
     * @param endTime Execution end time.
     * @param siblings Collection of siblings.
     * @param attrs Map of attributes.
     * @param fullSup {@code True} to enable distributed session attributes
     *      and checkpoints.
     * @param subjId Subject ID.
     * @return New session if one did not exist, or existing one.
     */
    public GridTaskSessionImpl createTaskSession(
        GridUuid sesId,
        UUID taskNodeId,
        String taskName,
        @Nullable GridDeployment dep,
        String taskClsName,
        @Nullable Collection<UUID> top,
        long startTime,
        long endTime,
        Collection<GridComputeJobSibling> siblings,
        Map<Object, Object> attrs,
        boolean fullSup,
        UUID subjId) {
        if (!fullSup) {
            return new GridTaskSessionImpl(
                taskNodeId,
                taskName,
                dep,
                taskClsName,
                sesId,
                top,
                startTime,
                endTime,
                siblings,
                attrs,
                ctx,
                false,
                subjId);
        }

        while (true) {
            GridTaskSessionImpl ses = sesMap.get(sesId);

            if (ses == null) {
                GridTaskSessionImpl old = sesMap.putIfAbsent(
                    sesId,
                    ses = new GridTaskSessionImpl(
                        taskNodeId,
                        taskName,
                        dep,
                        taskClsName,
                        sesId,
                        top,
                        startTime,
                        endTime,
                        siblings,
                        attrs,
                        ctx,
                        true,
                        subjId));

                if (old != null)
                    ses = old;
                else
                    // Return without acquire.
                    return ses;
            }

            if (ses.acquire())
                return ses;
            else
                sesMap.remove(sesId, ses);
        }
    }

    /**
     * @param sesId Session ID.
     * @return Session for a given session ID.
     */
    @Nullable public GridTaskSessionImpl getSession(GridUuid sesId) {
        return sesMap.get(sesId);
    }

    /**
     * Removes session for a given session ID.
     *
     * @param sesId ID of session to remove.
     * @return {@code True} if session was removed.
     */
    public boolean removeSession(GridUuid sesId) {
        GridTaskSessionImpl ses = sesMap.get(sesId);

        assert ses == null || ses.isFullSupport();

        if (ses != null && ses.release()) {
            sesMap.remove(sesId, ses);

            return true;
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public void printMemoryStats() {
        X.println(">>>");
        X.println(">>> Task session processor memory stats [grid=" + ctx.gridName() + ']');
        X.println(">>>  sesMapSize: " + sesMap.size());
    }
}
