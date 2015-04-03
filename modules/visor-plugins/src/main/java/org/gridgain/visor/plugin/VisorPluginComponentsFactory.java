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

package org.gridgain.visor.plugin;

import javax.swing.*;
import java.util.*;

/**
 * Factory for creating ready Visor UI blocks like nodes panel, log panel, etc.
 * Plugins will receive factory instance from
 * {@link VisorPluggableTab#createPanel(VisorPluginComponentsFactory, JPanel)} method.
 */
public interface VisorPluginComponentsFactory {
    /**
     * Create panel with nodes.
     *
     * @param pluginName Plugin name.
     * @param title Panel title.
     * @param ovrMsg Overlay message text.
     * @param showHostBtn Whether or not host button should be displayed.
     * @return Nodes panel.
     */
    public VisorPluginNodesPanel nodesPanel(String pluginName, String title, List<String> ovrMsg, boolean showHostBtn);

    /**
     * Create panel with list of log events.
     *
     * @param pluginName Plugin name.
     * @param title Panel title.
     * @param ovrMsg Overlay message text.
     * @return Log panel.
     */
    public VisorPluginLogPanel logPanel(String pluginName, String title, List<String> ovrMsg);
}
