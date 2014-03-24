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

namespace GridGain.Client.Impl.Marshaller {
    using System;

    /** <summary>Marshaller for binary protocol messages.</summary> */
    internal interface IGridClientMarshaller {
        /**
         * <summary>
         * Marshals object to byte array.</summary>
         *
         * <param name="val">Object to marshal.</param>
         * <returns>Byte array.</returns>
         * <exception cref="System.IO.IOException">If marshalling failed.</exception>
         */
        byte[] Marshal(Object val);

        /**
         * <summary>
         * Unmarshalls object from byte array.</summary>
         *
         * <param name="data">Byte array.</param>
         * <returns>Unmarshalled object.</returns>
         * <exception cref="System.IO.IOException">If unmarshalling failed.</exception>
         */
        T Unmarshal<T>(byte[] data);
    }
}
