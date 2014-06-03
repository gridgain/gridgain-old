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
#include "gridgain/impl/utils/gridclientdebug.hpp"

#include <cassert>
#include <cstddef>

#include "gridgain/impl/hash/gridclientbytearrayshasheableobject.hpp"

/**
 * Public constructor.
 *
 * @param bytes Value to hold.
 */
GridByteArrayHasheableObject::GridByteArrayHasheableObject(std::vector<int8_t> buf) {
    hashCodeVal = 1;

    for (size_t i = 0; i < buf.size(); ++i)
        hashCodeVal = 31 * hashCodeVal + buf[i];

    data = buf;
}

/**
 * Public constructor.
 *
 * @param bytes Value to hold.
 */
int32_t GridByteArrayHasheableObject::hashCode() const {
    return hashCodeVal;
}

/**
 * Converts contained object to byte vector.
 *
 * @param bytes Vector to fill.
 */
void GridByteArrayHasheableObject::convertToBytes(std::vector<int8_t>& bytes) const {
    bytes.clear();

    bytes = data;
}
