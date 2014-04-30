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

namespace GridGain.Client {
    using System;
    using GridGain.Client.Balancer;

    /** <summary>Data configuration bean.</summary> */
    public class GridClientDataConfiguration : IGridClientDataConfiguration {
        /** <summary>Creates empty configuration.</summary> */
        public GridClientDataConfiguration() {
            PinnedBalancer = new GridClientRandomBalancer();
        }

        /**
         * <summary>
         * Copy constructor.</summary>
         *
         * <param name="cfg">Configuration to copy.</param>
         */
        public GridClientDataConfiguration(IGridClientDataConfiguration cfg) {
            // Preserve alphabetic order for maintenance.
            Affinity = cfg.Affinity;
            Name = cfg.Name;
            PinnedBalancer = cfg.PinnedBalancer;
        }

        /** <summary>Client data affinity for this configuration.</summary> */
        public IGridClientDataAffinity Affinity {
            get;
            set;
        }

        /** <summary>Grid cache name for this configuration.</summary> */
        public String Name {
            get;
            set;
        }

        /** <summary>Balancer that will be used in pinned mode.</summary> */
        public IGridClientLoadBalancer PinnedBalancer {
            get;
            set;
        }
    }
}
