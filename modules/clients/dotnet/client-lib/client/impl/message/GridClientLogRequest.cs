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

namespace GridGain.Client.Impl.Message {
    using System;
    using System.Text;

    /** <summary>Request for a log file.</summary> */
    internal class GridClientLogRequest : GridClientRequest {
        /** 
         * <summary>
         * Constructs log request.</summary>
         * 
         * <param name="destNodeId">Node ID to route request to.</param>
         */
        public GridClientLogRequest(Guid destNodeId)
            : base(destNodeId) {
            From = -1;
            To = -1;
        }

        /** <summary>Path to log file.</summary> */
        public String Path {
            get;
            set;
        }

        /** <summary>From line, inclusive, indexing from 0.</summary> */
        public int From {
            get;
            set;
        }

        /** <summary>To line, inclusive, indexing from 0, can exceed count of lines in log.</summary> */
        public int To {
            get;
            set;
        }
    }
}
