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

#ifndef GRIDCLIENTMODEL_HPP_INCLUDED
#define GRIDCLIENTMODEL_HPP_INCLUDED

#include <string>

#include <gridgain/gridconf.hpp>

/**
 * Interface that will define the grid cache.
 */
class GRIDGAIN_API GridClientCache {
public:
    /** Cache modes. */
    enum GridClientCacheMode {
        /** Local cache. */
        LOCAL,

        /** Replicated cache. */
        REPLICATED,

        /** Partitioned cache. */
        PARTITIONED
    };

    virtual ~GridClientCache() {};

    /**
     * Retrieve the name of the cache.
     */
    virtual std::string name() const = 0;

     /**
     * Retrieve the mode of the cache.
     */
    virtual GridClientCacheMode mode() const = 0;
};

#endif // GRIDCLIENTMODEL_HPP_INCLUDED
