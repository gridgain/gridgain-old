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
    using System.Runtime.Serialization;

    /**
     * <summary>
     * This exception is thrown if client was closed by idle checker thread. This exception should be
     * handled internally and never rethrown to user.</summary>
     */
    [Serializable]
    internal class GridClientConnectionIdleClosedException : GridClientConnectionResetException {
        /** <summary>Constructs an exception.</summary> */
        public GridClientConnectionIdleClosedException() {
        }

        /**
         * <summary>
         * Creates exception with error message.</summary>
         *
         * <param name="msg">Error message.</param>
         */
        public GridClientConnectionIdleClosedException(String msg)
            : base(msg) {
        }


        /**
         * <summary>
         * Creates an exception with given message and error cause.</summary>
         *
         * <param name="msg">Exception message.</param>
         * <param name="cause">Exception cause.</param>
         */
        public GridClientConnectionIdleClosedException(String msg, Exception cause)
            : base(msg, cause) {
        }

        /**
         * <summary>
         * Constructs an exception.</summary>
         *
         * <param name="info">Serialization info.</param>
         * <param name="ctx">Streaming context.</param>
         */
        protected GridClientConnectionIdleClosedException(SerializationInfo info, StreamingContext ctx)
            : base(info, ctx) {
        }
    }
}
