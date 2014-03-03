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

    /** <summary>Cache metrics used to obtain statistics on cache itself or any of its entries.</summary> */
    public interface IGridClientDataMetrics {
        /** <summary>Gets create time of the owning entity (either cache or entry).</summary> */
        DateTime CreateTime {
            get;
        }

        /** <summary>Gets last write time of the owning entity (either cache or entry).</summary> */
        DateTime WriteTime {
            get;
        }

        /** <summary>Gets last read time of the owning entity (either cache or entry).</summary> */
        DateTime ReadTime {
            get;
        }

        /** <summary>Gets total number of reads of the owning entity (either cache or entry).</summary> */
        long Reads {
            get;
        }

        /** <summary>Gets total number of writes of the owning entity (either cache or entry).</summary> */
        long Writes {
            get;
        }

        /** <summary>Gets total number of hits for the owning entity (either cache or entry).</summary> */
        long Hits {
            get;
        }

        /** <summary>Gets total number of misses for the owning entity (either cache or entry).</summary> */
        long Misses {
            get;
        }
    }
}