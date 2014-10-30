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

package org.gridgain.grid.kernal.managers.deployment;

import org.gridgain.grid.*;
import org.jetbrains.annotations.*;

import java.util.*;

/**
 * Interface for all deployment stores.
 */
public interface GridDeploymentStore {
    /**
     * Starts store.
     *
     * @throws GridException If start failed.
     */
    public void start() throws GridException;

    /**
     * Stops store.
     */
    public void stop();

    /**
     * Kernal started callback.
     *
     * @throws GridException If callback execution failed.
     */
    public void onKernalStart() throws GridException;

    /**
     * Kernel stopping callback.
     */
    public void onKernalStop();

    /**
     * @param meta Deployment metadata.
     * @return Deployment.
     */
    @Nullable public GridDeployment getDeployment(GridDeploymentMetadata meta);

    /**
     * Gets class loader based on ID.
     *
     *
     * @param ldrId Class loader ID.
     * @return Class loader of {@code null} if not found.
     */
    @Nullable public GridDeployment getDeployment(GridUuid ldrId);

    /**
     * @return All current deployments.
     */
    public Collection<GridDeployment> getDeployments();

    /**
     * Explicitly deploys class.
     *
     * @param cls Class to explicitly deploy.
     * @param clsLdr Class loader.
     * @return Grid deployment.
     * @throws GridException Id deployment failed.
     */
    public GridDeployment explicitDeploy(Class<?> cls, ClassLoader clsLdr) throws GridException;

    /**
     * @param nodeId Optional ID of node that initiated request.
     * @param rsrcName Undeploys all deployments that have given
     */
    public void explicitUndeploy(@Nullable UUID nodeId, String rsrcName);

    /**
     * Adds participants to all deployments.
     *
     * @param allParticipants All participants to determine which deployments to add to.
     * @param addedParticipants Participants to add.
     */
    public void addParticipants(Map<UUID, GridUuid> allParticipants,
        Map<UUID, GridUuid> addedParticipants);
}
