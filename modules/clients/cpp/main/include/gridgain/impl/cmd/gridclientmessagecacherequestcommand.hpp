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

#ifndef GRID_CLIENT_MESSAGE_CACHE_COMMAND_HPP
#define GRID_CLIENT_MESSAGE_CACHE_COMMAND_HPP

#include <string>
#include <exception>
#include <map>
#include <set>
#include <cassert>

#include "gridgain/gridclientvariant.hpp"
#include "gridgain/gridclientcacheflag.hpp"
#include "gridgain/impl/cmd/gridclientmessagecommand.hpp"

/**
 * Cache request command.
 */
class GridCacheRequestCommand  : public GridClientMessageCommand {
public:
    /** Typedef for cache request. */
    typedef std::map<GridClientVariant, GridClientVariant> TKeyValueMap;

    /**
     * Available cache operations
     */
    enum GridCacheOperation {
        /** Cache put. */
        PUT= 0x01,

        /** Cache put all. */
        PUT_ALL= 0x02,

        /** Cache get. */
        GET = 0x03,

        /** Cache get all. */
        GET_ALL = 0x04,

        /** Cache remove. */
        RMV = 0x05,

        /** Cache remove all. */
        RMV_ALL = 0x06,

        /** Cache add (put only if not exists). */
        ADD = 0x07,

        /** Cache replace (put only if exists).  */
        REPLACE = 0x08,

        /** Cache compare and set. */
        CAS = 0x09,

        /** Cache metrics request. */
        METRICS = 0x0A
    };

    /**
     * Tries to find enum value by operation code.
     *
     * @param val Operation code value.
     * @return Enum value.
    */
    static GridCacheOperation findByOperationCode(const int& val) {
        switch (val) {
            case 1:
                return PUT;
            case 2:
                return PUT_ALL;
            case 3:
                return GET;
            case 4:
                return GET_ALL;
            case 5:
                return RMV;
            case 6:
                return RMV_ALL;
            case 7:
                return ADD;
            case 8:
                return REPLACE;
            case 9:
                return CAS;
            case 10:
                return METRICS;
            default: {
                assert(false);

                throw new std::exception();
            }break;
        }
        return PUT;
    }

    /**
     * Creates grid cache request.
     *
     * @param op Requested operation.
    */
    GridCacheRequestCommand(GridCacheOperation op) {
        this->op = op;
    }

    /**
     * @return Requested operation.
    */
    GridCacheOperation getOperation() const {
        return op;
    }

    /**
     * Gets cache name.
     *
     * @return Cache name, or null if not set.
     */
    std::string getCacheName() const {
        return cacheName;
    }

    /**
     * Sets cache name.
     *
     * @param cacheName Cache name.
     */
    void setCacheName(const std::string& cacheName) {
        this->cacheName = cacheName;
    }

    /**
     * @return Key.
     */
    GridClientVariant getKey() const {
        return key;
    }

    /**
     * @param key Key.
     */
    void setKey(const GridClientVariant& key) {
        this->key = key;
    }

    /**
     * @return Value1.
     */
    GridClientVariant getValue() const {
        return val;
    }

    /**
     * @param val Value1.
     */
    void setValue(const GridClientVariant& val) {
        this->val = val;
    }

    /**
     * @return Value 2.
     */
    GridClientVariant getValue2() const {
        return val2;
    }

    /**
     * @param val2 Value 2.
     */
    void setValue2(const GridClientVariant& val2) {
        this->val2 = val2;
    }

    /**
     * @return Values map for batch operations.
     */
    TKeyValueMap getValues() const {
        return vals;
    }

    /**
     * @param vals Values map for batch operations.
     */
    void setValues(const TKeyValueMap& vals) {
        this->vals = vals;
    }

    /**
     * @param flags Cache flags for this command.
     */
    void setFlags(const std::set<GridClientCacheFlag>& flags) {
        this->flags = flags;
    }

    /**
     * @return Cache flags for this command.
     */
    std::set<GridClientCacheFlag> getFlags() const {
        return flags;
    }

private:
    /** Requested cache operation. */
    GridCacheOperation op;

    /** Cache name. */
    std::string cacheName;

    /** Key */
    GridClientVariant key;

    /** Value (expected value for CAS). */
    GridClientVariant val;

    /** New value for CAS. */
    GridClientVariant val2;

    /** Keys and values for put all, get all, remove all operations. */
    TKeyValueMap vals;

    /** Cache flags. */
    std::set<GridClientCacheFlag> flags;
};

#endif
