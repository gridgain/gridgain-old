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

package org.gridgain.grid.util;

import java.io.*;
import java.util.*;

/**
 * Makes {@link AbstractSet} as {@link Serializable} and is
 * useful for making anonymous serializable sets. It has no extra logic or state in
 * addition to {@link AbstractSet}.
 * <p>
 * Note that methods {@link #contains(Object)} and {@link #remove(Object)} implemented
 * in {@link AbstractCollection} fully iterate through collection so you need to make
 * sure to override these methods if it's possible to create efficient implementations.
 */
public abstract class GridSerializableSet<E> extends AbstractSet<E> implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    // No-op.
}
