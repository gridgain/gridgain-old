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

package org.gridgain.grid.kernal.processors.hadoop;

import org.apache.hadoop.conf.*;
import org.gridgain.grid.*;
import org.gridgain.grid.util.typedef.*;

/**
 * Hadoop node startup.
 */
public class GridHadoopStartup {
    /**
     * @param args Arguments.
     */
    public static void main(String[] args) throws GridException {
        G.start("config/hadoop/default-config.xml");
    }

    /**
     * @return Configuration for job run.
     */
    @SuppressWarnings("UnnecessaryFullyQualifiedName")
    public static Configuration configuration() {
        Configuration cfg = new Configuration();

        cfg.set("fs.defaultFS", "ggfs://ggfs@localhost");

        cfg.set("fs.ggfs.impl", org.gridgain.grid.ggfs.hadoop.v1.GridGgfsHadoopFileSystem.class.getName());
        cfg.set("fs.AbstractFileSystem.ggfs.impl", org.gridgain.grid.ggfs.hadoop.v2.GridGgfsHadoopFileSystem.class.getName());

        cfg.set("dfs.client.block.write.replace-datanode-on-failure.policy", "NEVER");

        cfg.set("mapreduce.framework.name", "gridgain");
        cfg.set("mapreduce.jobtracker.address", "localhost:11211");

        return cfg;
    }
}
