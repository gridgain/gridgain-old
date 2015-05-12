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

/*
 * ___    _________________________ ________
 * __ |  / /____  _/__  ___/__  __ \___  __ \
 * __ | / /  __  /  _____ \ _  / / /__  /_/ /
 * __ |/ /  __/ /   ____/ / / /_/ / _  _, _/
 * _____/   /___/   /____/  \____/  /_/ |_|
 *
 */

package org.gridgain.visor.plugin;

import ro.fortsoft.pf4j.*;

/**
 * Abstract base class for Visor plugin extension point.
 */
public abstract class VisorExtensionPoint implements ExtensionPoint {
    /** */
    private final VisorPluginModel model;

    /**
     * @param model Visor model.
     */
    protected VisorExtensionPoint(VisorPluginModel model) {
        this.model = model;
    }

    /**
     * @return Visor model.
     */
    public VisorPluginModel model() {
        return model;
    }

    /**
     * @return Plugin name.
     */
    public abstract String name();

    /**
     * Will be executed on Visor model changed.
     */
    public void onModelChanged() {
        // No-op.
    }

    /**
     * Will be executed on Visor events changed.
     */
    public void onEventsChanged() {
        // No-op.
    }

    /**
     * Will be executed on Visor connect to grid.
     */
    public void onConnected() {
        // No-op.
    }

    /**
     * Will be executed on Visor disconnect from grid.
     */
    public void onDisconnected() {
        // No-op.
    }
}
