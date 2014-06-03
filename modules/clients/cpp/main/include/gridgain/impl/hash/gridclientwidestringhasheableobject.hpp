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

#ifndef GRIDCLIENTWIDESTRINGHASHEABLEOBJECT_HPP_
#define GRIDCLIENTWIDESTRINGHASHEABLEOBJECT_HPP_

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

#ifndef GRID_CLIENT_STRINGHASHEABLEOBJECT_HPP_INCLUDED
#define GRID_CLIENT_STRINGHASHEABLEOBJECT_HPP_INCLUDED

#include <cstdint>

#include <string>
#include <vector>

#include "gridgain/gridhasheableobject.hpp"

/**
 * Provides the implementation of hash code for wide strings.
 */
class GridWideStringHasheableObject : public GridClientHasheableObject {
public:
    /**
     * Public constructor.
     *
     * @param s Value to hold.
     */
    GridWideStringHasheableObject(const std::wstring& s);

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

    /**
     * Compares with another object of this type.
     *
     * @return True if the right operand is greater than this object.
     */
    virtual bool operator<(const GridWideStringHasheableObject& right) const {
        return str < const_cast<GridWideStringHasheableObject&>(right).str;
    }

protected:
    /** Wrapped value. */
    std::wstring str;
};

#endif



#endif /* GRIDCLIENTWIDESTRINGHASHEABLEOBJECT_HPP_ */
