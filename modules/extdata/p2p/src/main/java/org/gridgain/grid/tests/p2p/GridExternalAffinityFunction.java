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

package org.gridgain.grid.tests.p2p;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.affinity.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Mock affinity implementation that ensures constant key-to-node mapping based on {@code GridCacheModuloAffinity} The
 * partition selection is as follows: 0 maps to partition 1 and any other value maps to partition 1.
 */
public class GridExternalAffinityFunction implements GridCacheAffinityFunction {
    /** Node attribute for index. */
    public static final String IDX_ATTR = "nodeIndex";

    /** Number of backups. */
    private int backups;

    /** Number of partitions. */
    private int parts;

    /** Empty constructor. Equivalent for {@code new GridCacheModuloAffinity(2, 0)}. */
    public GridExternalAffinityFunction() {
        this(2, 0);
    }

    /**
     * @param parts   Number of partitions.
     * @param backups Number of backups.
     */
    public GridExternalAffinityFunction(int parts, int backups) {
        assert parts > 0;
        assert backups >= 0;

        this.parts = parts;
        this.backups = backups;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("unchecked")
    @Override public List<List<GridNode>> assignPartitions(GridCacheAffinityFunctionContext ctx) {
        List<List<GridNode>> res = new ArrayList<>(partitions());

        List<GridNode> topSnapshot = ctx.currentTopologySnapshot();

        for (int part = 0; part < parts; part++) {
            res.add(F.isEmpty(topSnapshot) ?
                Collections.<GridNode>emptyList() :
                // Wrap affinity nodes with unmodifiable list since unmodifiable generic collection
                // doesn't provide equals and hashCode implementations.
                U.sealList(nodes(part, topSnapshot)));
        }

        return res;
    }

    /** {@inheritDoc} */
    public Collection<GridNode> nodes(int part, Collection<GridNode> nodes) {
        List<GridNode> sorted = new ArrayList<>(nodes);

        Collections.sort(sorted, new Comparator<GridNode>() {
            @Override public int compare(GridNode n1, GridNode n2) {
                int idx1 = n1.<Integer>attribute(IDX_ATTR);
                int idx2 = n2.<Integer>attribute(IDX_ATTR);

                return idx1 < idx2 ? -1 : idx1 == idx2 ? 0 : 1;
            }
        });

        int max = 1 + backups;

        if (max > nodes.size())
            max = nodes.size();

        Collection<GridNode> ret = new ArrayList<>(max);

        Iterator<GridNode> it = sorted.iterator();

        for (int i = 0; i < max; i++) {
            GridNode n = null;

            if (i == 0) {
                while (it.hasNext()) {
                    n = it.next();

                    int nodeIdx = n.<Integer>attribute(IDX_ATTR);

                    if (part <= nodeIdx)
                        break;
                    else
                        n = null;
                }
            }
            else {
                if (it.hasNext())
                    n = it.next();
                else {
                    it = sorted.iterator();

                    assert it.hasNext();

                    n = it.next();
                }
            }

            assert n != null || nodes.size() < parts;

            if (n == null)
                n = (it = sorted.iterator()).next();

            ret.add(n);
        }

        return ret;
    }

    /** {@inheritDoc} */
    @Override public void reset() {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public int partitions() {
        return parts;
    }

    /** {@inheritDoc} */
    @Override public int partition(Object key) {
        return key instanceof Integer ? 0 == key ? 0 : 1 : 1;
    }

    /** {@inheritDoc}
     * @param nodeId*/
    @Override public void removeNode(UUID nodeId) {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridExternalAffinityFunction.class, this);
    }
}
