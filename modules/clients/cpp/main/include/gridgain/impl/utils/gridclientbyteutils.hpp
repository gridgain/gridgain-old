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

#ifndef GRID_CLEINT_BYTE_UTILS_HPP_INCLUDED
#define GRID_CLEINT_BYTE_UTILS_HPP_INCLUDED

#include <vector>
#include <set>
#include <cassert>
#include <cstdint>
#include <cstddef>
#include <cstring>

/**
 *
 * Utilities for converting simple types to byte array and back.
 */
namespace GridClientByteUtils {

    using std::size_t;

    /** Byte order on current platform. */
    enum ByteOrder { BIG_ENDIAN_ORDER = 1, LITTLE_ENDIAN_ORDER = 2 };

    /**
     * Convert byte array to a primitive value.
     *
     * @param buf Byte array.
     * @param nBytes Number of bytes in array.
     * @param res Resulting value.
     * @param order Byte order.
     */
    template <class T> void bytesToValue(const int8_t* buf, size_t nBytes, T& res, const ByteOrder& order =
            BIG_ENDIAN_ORDER) {
        assert(buf != NULL);
        assert(order == BIG_ENDIAN_ORDER || order == LITTLE_ENDIAN_ORDER);

        size_t bytesCnt = sizeof(T) / sizeof(int8_t);

        assert(nBytes >= bytesCnt);

        res = 0;

        for (size_t i = 0; i < bytesCnt; i++) {
            int shift = (order == BIG_ENDIAN_ORDER ? bytesCnt - i - 1 : i) << 3;

            res |= (0xffL & buf[i]) << shift;
        }
    }

    /**
     * Convert a primitive value to a vector of bytes.
     *
     * @param val Value to convert to bytes.
     * @param bytes Resulting array.
     * @param order Byte order.
     */
    template <class T> void valueToBytes(const T& val, std::vector<int8_t>& bytes, const ByteOrder& order =
            BIG_ENDIAN_ORDER) {
        assert(order == BIG_ENDIAN_ORDER || order == LITTLE_ENDIAN_ORDER);

        bytes.clear();

        size_t bytesCnt = sizeof(T) / sizeof(int8_t);

        for (size_t i = 0; i < bytesCnt; i++) {
            int shift = (order == BIG_ENDIAN_ORDER ? bytesCnt - i - 1 : i) << 3;

            bytes.push_back((int8_t)((val >> shift) & 0xff));
        }
    }

    /**
     * Convert a primitive value to byte array.
     *
     * @param val Value to convert to bytes.
     * @param buf Resulting array.
     * @param nBytes Byte array length.
     * @param order Byte order.
     */
    template <class T> void valueToBytes(const T& val, int8_t* buf, size_t nBytes, const ByteOrder& order =
            BIG_ENDIAN_ORDER) {
        assert(buf != NULL);
        assert(order == BIG_ENDIAN_ORDER || order == LITTLE_ENDIAN_ORDER);

        size_t bytesCnt = sizeof(T) / sizeof(int8_t);

        assert(nBytes >= bytesCnt);

        for (size_t i = 0; i < bytesCnt; i++) {
           int shift = (order == BIG_ENDIAN_ORDER ? bytesCnt - i - 1 : i) << 3;

           buf[i] = ((int8_t)((val >> shift) & 0xff));
        }
    }

    /**
     * Calculates a bitwise OR result for members of a given range.
     *
     * @param first First range element, inclusive.
     * @param last Last range element, exclusive.
     * @param initVal Initial value for the calculation.
     * @return A bitwise OR result.
     */
    template <class IT, class T> T bitwiseOr(IT first, IT last, T initVal) {
        T ret = initVal;

        for (IT i = first; i != last; i++)
            ret |= *i;

        return ret;
    }
}
#endif
