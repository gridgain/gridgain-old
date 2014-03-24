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

    /** <summary>Available cache operations.</summary> */
    internal enum GridClientCacheRequestOperation {
        /** <summary>Cache put.</summary> */
        Put = 0x01,

        /** <summary>Cache put all.</summary> */
        PutAll = 0x02,

        /** <summary>Cache get.</summary> */
        Get = 0x03,

        /** <summary>Cache get all.</summary> */
        GetAll = 0x04,

        /** <summary>Cache remove.</summary> */
        Rmv = 0x05,

        /** <summary>Cache remove all.</summary> */
        RmvAll = 0x06,

        /** <summary>Cache replace (put only if exists).</summary> */
        Replace = 0x08,

        /** <summary>Append requested value to already cached one.</summary> */
        Append = 0x0B,

        /** <summary>Prepend requested value to already cached one.</summary> */
        Prepend = 0x0C,

        /** <summary>Cache compare and set.</summary> */
        Cas = 0x09,

        /** <summary>Cache metrics request.</summary> */
        Metrics = 0x0A
    }
}
