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

package org.gridgain.grid.gridify;

import org.gridgain.grid.compute.gridify.*;

import java.util.*;

/**
 * Test set-to-set target interface.
 */
public interface GridifySetToSetTargetInterface {
    /**
     * Find prime numbers in collection.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget", threshold = 2, splitSize = 2)
    public Collection<Long> findPrimes(Collection<Long> input);

    /**
     * Find prime numbers in collection.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget", threshold = 2)
    public Collection<Long> findPrimesWithoutSplitSize(Collection<Long> input);

    /**
     * Find prime numbers in collection.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget")
    public Collection<Long> findPrimesWithoutSplitSizeAndThreshold(Collection<Long> input);

    /**
     * Find prime numbers in collection.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget")
    public Collection<Long> findPrimesInListWithoutSplitSizeAndThreshold(List<Long> input);

    /**
     * Find prime numbers in collection.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @SuppressWarnings({"CollectionDeclaredAsConcreteClass"})
    @GridifySetToSet(gridName = "GridifySetToSetTarget")
    public Collection<Long> findPrimesInArrayListWithoutSplitSizeAndThreshold(ArrayList<Long> input);

    /**
     * Find prime numbers in array.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget", threshold = 2, splitSize = 2)
    public Long[] findPrimesInArray(Long[] input);

    /**
     * Find prime numbers in primitive array.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget", threshold = 2, splitSize = 2)
    public long[] findPrimesInPrimitiveArray(long[] input);

    /**
     * Find prime numbers in iterator.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget", threshold = 2, splitSize = 2)
    public Iterator<Long> findPrimesWithIterator(Iterator<Long> input);

    /**
     * Find prime numbers in enumeration.
     *
     * @param input Input collection.
     * @return Prime numbers.
     */
    @GridifySetToSet(gridName = "GridifySetToSetTarget", threshold = 2, splitSize = 2)
    public Enumeration<Long> findPrimesWithEnumeration(Enumeration<Long> input);
}
