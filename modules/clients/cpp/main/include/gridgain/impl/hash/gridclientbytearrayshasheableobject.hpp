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

#ifndef GRID_CLIENT_ARRAYGHASHEABLEOBJECT_HPP_INCLUDED
#define GRID_CLIENT_ARRAYGHASHEABLEOBJECT_HPP_INCLUDED

#include <vector>

#include "gridgain/gridhasheableobject.hpp"

/**
 * Hashable object wrapping byte array.
 */
class GridByteArrayHasheableObject : public GridClientHasheableObject {
public:
    /**
     * Public constructor.
     *
     * @param bytes Value to hold.
     */
    GridByteArrayHasheableObject(std::vector<int8_t> bytes);

    /** Destructor. */
    ~GridByteArrayHasheableObject() {};

    /**
     * Calculates hash code for the contained object.
     *
     * @return Hash code.
     */
    virtual int32_t hashCode() const;

    /**
     * Converts contained object to byte vector.
     *
     * @param bytes Vector to fill.
     */
    virtual void convertToBytes(std::vector<int8_t>& bytes) const;
private:
    /** Stored calculated hash code value. */
    int32_t hashCodeVal;

    /** Wrapped value. */
    std::vector<int8_t> data;
};

#endif
