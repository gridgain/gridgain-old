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
import org.gridgain.grid.lang.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import java.util.*;

/**
 * Deployment metadata.
 */
class GridDeploymentMetadata {
    /** Deployment mode. */
    private GridDeploymentMode depMode;

    /** */
    private String alias;

    /** */
    private String clsName;

    /** */
    private String userVer;

    /** */
    private UUID sndNodeId;

    /** */
    private GridUuid clsLdrId;

    /** Class loader. */
    private ClassLoader clsLdr;

    /** Master node participants. */
    @GridToStringInclude
    private Map<UUID, GridUuid> participants;

    /** */
    private ClassLoader parentLdr;

    /** */
    private boolean record;

    /** */
    private GridPredicate<GridNode> nodeFilter;

    /**
     *
     */
    GridDeploymentMetadata() {
        // Meta.
    }

    /**
     * @param meta Meta to copy.
     */
    GridDeploymentMetadata(GridDeploymentMetadata meta) {
        assert meta != null;

        depMode = meta.deploymentMode();
        alias = meta.alias();
        clsName = meta.className();
        userVer = meta.userVersion();
        sndNodeId = meta.senderNodeId();
        clsLdr = meta.classLoader();
        clsLdrId = meta.classLoaderId();
        participants = meta.participants();
        parentLdr = meta.parentLoader();
        record = meta.record();
        nodeFilter = meta.nodeFilter();
    }

    /**
     * Gets property depMode.
     *
     * @return Property depMode.
     */
    GridDeploymentMode deploymentMode() {
        return depMode;
    }

    /**
     * Sets property depMode.
     *
     * @param depMode Property depMode.
     */
    void deploymentMode(GridDeploymentMode depMode) {
        this.depMode = depMode;
    }

    /**
     * Gets property alias.
     *
     * @return Property alias.
     */
    String alias() {
        return alias;
    }

    /**
     * Sets property alias.
     *
     * @param alias Property alias.
     */
    void alias(String alias) {
        this.alias = alias;
    }

    /**
     * Gets property clsName.
     *
     * @return Property clsName.
     */
    String className() {
        return clsName;
    }

    /**
     * Sets property clsName.
     *
     * @param clsName Property clsName.
     */
    void className(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Gets property seqNum.
     *
     * @return Property seqNum.
     */
    long sequenceNumber() {
        return clsLdrId.localId();
    }

    /**
     * Gets property userVer.
     *
     * @return Property userVer.
     */
    String userVersion() {
        return userVer;
    }

    /**
     * Sets property userVer.
     *
     * @param userVer Property userVer.
     */
    void userVersion(String userVer) {
        this.userVer = userVer;
    }

    /**
     * Gets property senderNodeId.
     *
     * @return Property senderNodeId.
     */
    UUID senderNodeId() {
        return sndNodeId;
    }

    /**
     * Sets property senderNodeId.
     *
     * @param sndNodeId Property senderNodeId.
     */
    void senderNodeId(UUID sndNodeId) {
        this.sndNodeId = sndNodeId;
    }

    /**
     * Gets property clsLdrId.
     *
     * @return Property clsLdrId.
     */
    GridUuid classLoaderId() {
        return clsLdrId;
    }

    /**
     * Sets property clsLdrId.
     *
     * @param clsLdrId Property clsLdrId.
     */
    void classLoaderId(GridUuid clsLdrId) {
        this.clsLdrId = clsLdrId;
    }

    /**
     * Gets parent loader.
     *
     * @return Parent loader.
     */
    public ClassLoader parentLoader() {
        return parentLdr;
    }

    /**
     * Sets parent loader.
     *
     * @param parentLdr Parent loader.
     */
    public void parentLoader(ClassLoader parentLdr) {
        this.parentLdr = parentLdr;
    }

    /**
     * Gets property record.
     *
     * @return Property record.
     */
    boolean record() {
        return record;
    }

    /**
     * Sets property record.
     *
     * @param record Property record.
     */
    void record(boolean record) {
        this.record = record;
    }

    /**
     * @return Node participants.
     */
    public Map<UUID, GridUuid> participants() {
        return participants;
    }

    /**
     * @param participants Node participants.
     */
    public void participants(Map<UUID, GridUuid> participants) {
        this.participants = participants;
    }

    /**
     * @return Class loader.
     */
    public ClassLoader classLoader() {
        return clsLdr;
    }

    /**
     * @param clsLdr Class loader.
     */
    public void classLoader(ClassLoader clsLdr) {
        this.clsLdr = clsLdr;
    }

    /**
     * @param nodeFilter Node filter.
     */
    public void nodeFilter(GridPredicate<GridNode> nodeFilter) {
        this.nodeFilter = nodeFilter;
    }

    /**
     * @return Node filter.
     */
    public GridPredicate<GridNode> nodeFilter() {
        return nodeFilter;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDeploymentMetadata.class, this, "seqNum", clsLdrId != null ? clsLdrId.localId() : "n/a");
    }
}
