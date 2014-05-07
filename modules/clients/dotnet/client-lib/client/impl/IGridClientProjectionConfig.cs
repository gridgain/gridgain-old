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

namespace GridGain.Client.Impl {
    using System;

    /** <summary>Interface provides configuration for the client projection(s).</summary> */
    internal interface IGridClientProjectionConfig {
        /** <summary>Connections manager.</summary> */
        GridClientConnectionManager ConnectionManager {
            get;
        }

        /** <summary>Topology instance to be used in this projection.</summary> */
        GridClientTopology Topology {
            get;
        }

        /**
         * <summary>
         * Gets data affinity for a given cache name.</summary>
         *
         * <param name="cacheName">Name of cache for which affinity is obtained. Data configuration with this name</param>
         *     must be configured at client startup.
         * <returns>Data affinity object.</returns>
         * <exception cref="ArgumentException">If client data with given name was not configured.</exception>
         */
        IGridClientDataAffinity Affinity(String cacheName);
    }
}
