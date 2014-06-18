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

package org.gridgain.loadtests.dsi;

import org.gridgain.grid.cache.affinity.*;

import java.io.*;

/**
 *
 */
public class GridDsiRequest implements Serializable {
    /** */
    private Long id;

    /** */
    @SuppressWarnings({"UnusedDeclaration", "FieldCanBeLocal"})
    private long msgId;

    /** */
    @SuppressWarnings("UnusedDeclaration")
    private long txId;

    /**
     * @param id ID.
     */
    public GridDsiRequest(long id) {
        this.id = id;
    }

    /**
     * @param msgId Message ID.
     */
    public void setMessageId(long msgId) {
        this.msgId = msgId;
    }

    /**
     * @param terminalId Terminal ID.
     * @return Cache key.
     */
    public Object getCacheKey(String terminalId){
        return new RequestKey(id, terminalId);
    }

    /**
     *
     */
    @SuppressWarnings("PackageVisibleInnerClass")
    static class RequestKey implements Serializable {
        /** */
        private Long key;

        /** */
        @SuppressWarnings("UnusedDeclaration")
        @GridCacheAffinityKeyMapped
        private String terminalId;

        /**
         * @param key Key.
         * @param terminalId Terminal ID.
         */
        RequestKey(long key, String terminalId) {
            this.key = key;
            this.terminalId = terminalId;
        }

        /** {@inheritDoc} */
        @Override public int hashCode() {
            return key.hashCode();
        }

        /** {@inheritDoc} */
        @Override public boolean equals(Object obj) {
            return obj instanceof RequestKey && key.equals(((RequestKey)obj).key);
        }
    }
}
