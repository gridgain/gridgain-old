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

package org.gridgain.grid.util.tostring;

/**
 * Simple field descriptor containing field name and its order in the class descriptor.
 */
class GridToStringFieldDescriptor {
    /** Field name. */
    private final String name;

    /** */
    private int order = Integer.MAX_VALUE;

    /**
     * @param name Field name.
     */
    GridToStringFieldDescriptor(String name) {
        assert name != null;

        this.name = name;
    }

    /**
     * @return Field order.
     */
    int getOrder() { return order; }

    /**
     * @param order Field order.
     */
    void setOrder(int order) { this.order = order; }

    /**
     * @return Field name.
     */
    String getName() { return name; }
}
