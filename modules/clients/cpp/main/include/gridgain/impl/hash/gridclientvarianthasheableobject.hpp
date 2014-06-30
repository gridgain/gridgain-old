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

#ifndef GRID_CLIENT_VARIANT_HASHEABLE_OBJECT_HPP_INCLUDED
#define GRID_CLIENT_VARIANT_HASHEABLE_OBJECT_HPP_INCLUDED

#include <vector>

#include "gridgain/gridhasheableobject.hpp"
#include "gridgain/gridclientvariant.hpp"

/**
 * Provide the implementation of hash-code for GridClientVariant.
 */
class GridClientVariantHasheableObject : public GridClientHasheableObject {
public:
    /**
     * Public constructor.
     *
     * @param var Value to hold.
     */
    GridClientVariantHasheableObject(const GridClientVariant& var);

    /**
     * Public constructor.
     *
     * @param var Value to hold.
     * @param calculatedHashCode Pre-calculated hashcode.
     */
    GridClientVariantHasheableObject(const GridClientVariant& var, int calculatedHashCode);

    /**
     * Calculates hash code for the contained object.
     *
     * @return Hash code.
     */
    virtual int32_t hashCode() const { return hashCode_; }

    /**
     * Converts contained object to byte vector.
     *
     * @param opBytes Vector to fill.
     */
    virtual void convertToBytes(std::vector<int8_t>& opBytes) const {
        opBytes = bytes;
    }
protected:
    /**
     * Assign another value to same object.
     *
     * @param var New value.
     */
    void init(const GridClientVariant& var);

    /** Stored hash code. */
    int32_t hashCode_;

    /** Byte representation of the object. */
    std::vector<int8_t> bytes;
};

#endif
