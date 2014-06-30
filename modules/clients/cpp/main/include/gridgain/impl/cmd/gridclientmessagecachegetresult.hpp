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

#ifndef GRID_CLIENTMESSAGE_CACHEGET_RESULT_HPP_INCLUDED
#define GRID_CLIENTMESSAGE_CACHEGET_RESULT_HPP_INCLUDED

#include "gridgain/impl/cmd/gridclientmessagecacheresult.hpp"

/** Typedef for cache result key/value map. */
typedef std::map<GridClientVariant, GridClientVariant> TCacheValuesMap;

/**
 * Cache get result message.
 */
class GridClientMessageCacheGetResult : public GridClientMessageCacheResult {
public:
   /**
    * Set the cache values.
    *
    * @param The new value for keys and values.
    */
    void setCacheValues (const TCacheValuesMap& pCacheValues) {
        cacheValues = pCacheValues;
    }

   /**
    * Get the cache values.
    *
    * @return Retrieved key/value map.
    */
    TCacheValuesMap getCacheValue() const {
        return cacheValues;
    }

   /**
    * Add the specified key and value to the existed cache value.
    *
    * @param pKey The added key.
    * @param pValue The added value.
    */
    void add(const GridClientVariant& pKey, const GridClientVariant& pValue) {
        cacheValues[pKey] = pValue;
    }

private:
    /** Stored values. */
    TCacheValuesMap cacheValues;
};
#endif
