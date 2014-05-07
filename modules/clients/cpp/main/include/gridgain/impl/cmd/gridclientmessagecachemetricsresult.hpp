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

#ifndef GRID_CLIENTMESSAGE_CACHEMETRICS_RESULT_HPP_INCLUDED
#define GRID_CLIENTMESSAGE_CACHEMETRICS_RESULT_HPP_INCLUDED

#include <map>
#include "gridgain/impl/cmd/gridclientmessagecacheresult.hpp"

/** Typedef for cache metrics map. */
typedef std::map<std::string, GridClientVariant> TCacheMetrics;

/**
 * Cache metrics result message.
 */
class GridClientMessageCacheMetricResult : public GridClientMessageCacheResult {
public:
   /**
    * Set the cache metrics values.
    *
    * @param pCacheMetrics The new value of cache-metrics.
    */
    void setCacheMetrics(const TCacheMetrics& pCacheMetrics) {
        cacheMetrics = pCacheMetrics;
    }

   /**
    * Get the cache values.
    *
    * @return  The key/value map of the cache metrics.
    */
    TCacheMetrics getCacheMetrics() const {
        return cacheMetrics;
    }
private:
    /** Cache metrics. */
    TCacheMetrics cacheMetrics;
};

#endif
