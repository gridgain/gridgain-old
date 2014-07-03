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
    using System.Runtime.Serialization;

    /** <summary>Exception that represents client authentication failure for some reason.</summary> */
    [Serializable]
    public class GridClientAuthenticationException : GridClientException {
        /** <summary>Constructs an exception.</summary> */
        public GridClientAuthenticationException() {
        }

        /**
         * <summary>
         * Creates exception with given message.</summary>
         *
         * <param name="msg">Error message.</param>
         */
        public GridClientAuthenticationException(String msg)
            : base(msg) {
        }

        /**
         * <summary>
         * Creates exception with given message and cause.</summary>
         *
         * <param name="msg">Message.</param>
         * <param name="cause">Cause.</param>
         */
        public GridClientAuthenticationException(String msg, Exception cause)
            : base(msg, cause) {
        }

        /**
         * <summary>
         * Constructs an exception.</summary>
         *
         * <param name="info">Serialization info.</param>
         * <param name="ctx">Streaming context.</param>
         */
        protected GridClientAuthenticationException(SerializationInfo info, StreamingContext ctx)
            : base(info, ctx) {
        }
    }
}
