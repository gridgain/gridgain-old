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

#ifndef GRID_UTIL_HPP_INCLUDED
#define GRID_UTIL_HPP_INCLUDED
#include <string>

/**
 * Holder class for various utility functions.
 */
class GridUtil {
public:
    /**
     * Returns full path to file if the file was specified related to GridGain home.
     *
     * @param relPath Relative path.
     * @return Full path.
     */
    static std::string prependHome(std::string relPath);

    /**
     * Outputs string representation of a vector to stream.
     *
     * @param out Target stream.
     * @param v Vector to output.
     * @return Target stream (for chaining).
     */
    static std::ostream& toStream(std::ostream& out, const std::vector<std::string>& v);

    /**
     * Outputs GridClientSocketAddress representation of a vector to stream.
     *
     * @param out Target stream.
     * @param v Vector to output.
     * @return Target stream (for chaining).
     */
    static std::ostream& toStream(std::ostream& out, const std::vector<GridClientSocketAddress>& v);

    /**
     * Returns current system time in milliseconds.
     */
    static int64_t currentTimeMillis();

    /**
     * @return A vector of 4 elements, representing the client version
     * ([major, minor, revision, revision2]).
     */
    static std::vector<int8_t> getVersionNumeric();

private:
    /** Private constructor prohibits instantiation of this class. */
    GridUtil();
};

#endif
