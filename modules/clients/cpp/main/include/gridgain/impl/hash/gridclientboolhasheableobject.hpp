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

#ifndef GRID_CLIENT_BOOL_HASHEABLE_OBJECT_HPP_INCLUDED
#define GRID_CLIENT_BOOL_HASHEABLE_OBJECT_HPP_INCLUDED

#include "gridgain/gridhasheableobject.hpp"
#include "gridgain/impl/utils/gridclientbyteutils.hpp"

/**
 * Hashable object wrapping boolean value.
 */
class GridBoolHasheableObject : public GridClientHasheableObject {
public:
    /**
     * Public constructor.
     *
     * @param pVal Value to hold.
     */
    GridBoolHasheableObject(bool pVal);

    /**
     * Return hash code for the contained value.
     *
     * @return Java-like boolean hash code.
     */
    virtual int32_t hashCode() const { return hashCode_; }

    /**
     * Converts contained object to byte vector.
     *
     * @param bytes Vector to fill.
     */
    virtual void convertToBytes(std::vector<int8_t>& opBytes) const { opBytes = bytes; }

protected:
    /** Stored hash code. */
    int32_t hashCode_;

    /** Byte representation of the object. */
    std::vector<int8_t> bytes;
};

/**
 * Public constructor.
 *
 * @param pVal Value to hold.
 */
inline GridBoolHasheableObject::GridBoolHasheableObject(bool pVal) {
    hashCode_ = pVal ? 1231 : 1237;

    bytes.resize(sizeof(hashCode_));
    memset(&bytes[0], 0, sizeof(hashCode_));

    GridClientByteUtils::valueToBytes(hashCode_, &bytes[0], sizeof(hashCode_), GridClientByteUtils::LITTLE_ENDIAN_ORDER);
}

#endif
