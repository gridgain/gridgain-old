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

package org.gridgain.grid.dotnet;

import org.gridgain.grid.portables.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Mirror of .Net class GridDotNetConfiguration.cs
 */
public class GridDotNetConfiguration implements GridPortableMarshalAware {
    /** */
    private GridDotNetPortableConfiguration portableCfg;

    /** */
    private List<String> assemblies;

    /**
     * Default constructor.
     */
    public GridDotNetConfiguration() {
        // No-op.
    }

    /**
     * Copy constructor.
     * @param cfg configuration to copy.
     */
    public GridDotNetConfiguration(GridDotNetConfiguration cfg) {
        if (cfg.getPortableConfiguration() != null)
            portableCfg = new GridDotNetPortableConfiguration(cfg.getPortableConfiguration());

        if (cfg.getAssemblies() != null)
            assemblies = new ArrayList<>(cfg.getAssemblies());
    }

    /**
     * @return Configuration.
     */
    public GridDotNetPortableConfiguration getPortableConfiguration() {
        return portableCfg;
    }

    /**
     * @param portableCfg Configuration.
     */
    public void setPortableConfiguration(GridDotNetPortableConfiguration portableCfg) {
        this.portableCfg = portableCfg;
    }

    /**
     * @return Assemblies.
     */
    public List<String> getAssemblies() {
        return assemblies;
    }

    /**
     *
     * @param assemblies Assemblies.
     */
    public void setAssemblies(List<String> assemblies) {
        this.assemblies = assemblies;
    }

    /** {@inheritDoc} */
    @Override public void writePortable(GridPortableWriter writer) throws GridPortableException {
        GridPortableRawWriter rawWriter = writer.rawWriter();

        rawWriter.writeObject(portableCfg);
        rawWriter.writeCollection(assemblies);
    }

    /** {@inheritDoc} */
    @Override public void readPortable(GridPortableReader reader) throws GridPortableException {
        GridPortableRawReader rawReader = reader.rawReader();

        portableCfg = (GridDotNetPortableConfiguration)rawReader.readObject();
        assemblies = (List<String>)rawReader.<String>readCollection();
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridDotNetConfiguration.class, this);
    }
}
