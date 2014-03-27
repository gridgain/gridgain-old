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

    /** <summary>Base implementation for cache metrics.</summary> */
    internal class GridClientDataMetrics : IGridClientDataMetrics {
        /** <summary>Constructs empty cache metrics.</summary> */
        public GridClientDataMetrics() {
            var now = DateTime.Now;

            CreateTime = now;
            WriteTime = now;
            ReadTime = now;
        }

        /** <summary>Gets create time of the owning entity (either cache or entry).</summary> */
        public DateTime CreateTime {
            get;
            set;
        }

        /** <summary>Gets last write time of the owning entity (either cache or entry).</summary> */
        public DateTime WriteTime {
            get;
            set;
        }

        /** <summary>Gets last read time of the owning entity (either cache or entry).</summary> */
        public DateTime ReadTime {
            get;
            set;
        }

        /** <summary>Gets total number of reads of the owning entity (either cache or entry).</summary> */
        public long Reads {
            get;
            set;
        }

        /** <summary>Gets total number of writes of the owning entity (either cache or entry).</summary> */
        public long Writes {
            get;
            set;
        }

        /** <summary>Gets total number of hits for the owning entity (either cache or entry).</summary> */
        public long Hits {
            get;
            set;
        }

        /** <summary>Gets total number of misses for the owning entity (either cache or entry).</summary> */
        public long Misses {
            get;
            set;
        }

        /** <inheritDoc/> */
        override public String ToString() {
            return "GridClientDataMetrics [" +
                "createTime=" + CreateTime +
                ", hits=" + Hits +
                ", misses=" + Misses +
                ", reads=" + Reads +
                ", readTime=" + ReadTime +
                ", writes=" + Writes +
                ", writeTime=" + WriteTime +
                ']';
        }
    }
}
