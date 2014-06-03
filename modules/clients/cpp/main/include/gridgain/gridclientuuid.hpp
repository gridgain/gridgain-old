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

#ifndef GRID_UUID_HPP_INCLUDED
#define GRID_UUID_HPP_INCLUDED

#include <iostream>
#include <string>
#include <functional>

#include <boost/uuid/uuid.hpp>

#include <gridgain/gridhasheableobject.hpp>

/**
 * Globally unique universal identifier.
 */
class GRIDGAIN_API GridClientUuid: public GridClientHasheableObject {
    class Impl {
    public:
        /** Wrapped Boost uuid. */
        boost::uuids::uuid uuid_;
    };
public:
    /**
     * Public default constructor. Creates an empty UUID.
     */
    GridClientUuid();

    /**
     * Reconstructs UUID from string.
     *
     * @param str String representation of UUID.
     */
    GridClientUuid(const char* str);

    /**
     * Reconstructs UUID from string.
     *
     * @param str String representation of UUID.
     */
    GridClientUuid(const std::string& str);

    /**
     * Copy constructor.
     *
     * @param other Another instance of UUID.
     */
    GridClientUuid(const GridClientUuid& other);

    /**
     * Constructs a new UUID from raw bytes.
     *
     * @param bytes Raw bytes.
     * @return A new UUID.
     */
    static GridClientUuid fromBytes(const std::string& bytes);

    /**
     * Generates a random UUID.
     *
     * @return New random UUID.
     */
    static GridClientUuid randomUuid();

    /**
     * Assignment operator override.
     *
     * @param rhs Right-hand side of the assignment operator.
     * @return This instance of the class.
     */
    GridClientUuid& operator=(const GridClientUuid& rhs);

    /** Destructor. */
    virtual ~GridClientUuid();

    /**
     * Comparison operator for UUID.
     *
     * @param other UUID to compare this UUID to.
     * @return <tt>true</tt> if this UUID is less than other, <tt>false</tt> otherwise.
     */
    bool operator <(const GridClientUuid& other) const;

    /**
     * Comparison operator for UUID.
     *
     * @param other UUID to compare this UUID to.
     * @return <tt>true</tt> if this UUID equals another, <tt>false</tt> otherwise.
     */
    bool operator ==(const GridClientUuid& other) const;

    /**
     * Returns hash code for this UUID following Java conventions.
     *
     * @return Hash code.
     */
    virtual int32_t hashCode() const;

    /**
     * Converts UUID to string.
     *
     * @return String representation iof UUID.
     */
    const std::string uuid() const;

    /**
     * Least significant bits in UUID.
     *
     * @return 4 least significant bits packed in 64-bit integer.
     */
    int64_t leastSignificantBits() const;

    /**
     * Most significant bits in UUID.
     *
     * @return 4 most significant bits packed in 64-bit integer.
     */
    int64_t mostSignificantBits() const;

    /**
     * Converts this UUID to byte array.
     *
     * This method is used for hash calculation and uses
     * special byte ordering.
     *
     * @param bytes Vector to fill.
     */
    virtual void convertToBytes(std::vector<int8_t>& bytes) const;

    /**
     * Reads the UUID raw bytes to byte vector.
     *
     * Byte order is not changed.
     *
     * @param bytes Vector to fill.
     */
    void rawBytes(std::vector<int8_t>& bytes) const;
private:
    Impl pimpl;

    /**
     * Prints UUID to stream
     *
     * @param out Stream to output UUID to.
     * @param u UUID.
     */
    friend std::ostream& operator<<(std::ostream &out, const GridClientUuid& u);
};

inline std::ostream& operator<<(std::ostream &out, const GridClientUuid& u) {
    return out << u.uuid();
}

namespace std {

/** Hash code for UUID for unordered_map. */
#ifdef _MSC_VER
template<> inline
size_t hash<GridClientUuid>::operator()(const GridClientUuid& u) const {
    return u.hashCode();
}
#else
template<> struct hash<GridClientUuid> {
    size_t operator()(GridClientUuid u) const {
        return u.hashCode();
    }
};
#endif

}

#endif
