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

import java.util.*;

/**
 * Simple class descriptor containing simple and fully qualified class names as well as
 * the list of class fields.
 */
class GridToStringClassDescriptor {
    /** */
    private final String sqn;

    /** */
    private final String fqn;

    /** */
    private List<GridToStringFieldDescriptor> fields = new ArrayList<>();

    /**
     * @param cls Class.
     */
    GridToStringClassDescriptor(Class<?> cls) {
        assert cls != null;

        fqn = cls.getName();
        sqn = cls.getSimpleName();
    }

    /**
     * @param field Field descriptor to be added.
     */
    void addField(GridToStringFieldDescriptor field) {
        assert field != null;

        fields.add(field);
    }

    /** */
    void sortFields() {
        Collections.sort(fields, new Comparator<GridToStringFieldDescriptor>() {
            /** {@inheritDoc} */
            @Override public int compare(GridToStringFieldDescriptor arg0, GridToStringFieldDescriptor arg1) {
                return arg0.getOrder() < arg1.getOrder() ? -1 : arg0.getOrder() > arg1.getOrder() ? 1 : 0;
            }
        });
    }

    /**
     * @return Simple class name.
     */
    String getSimpleClassName() {
        return sqn;
    }

    /**
     * @return Fully qualified class name.
     */
    String getFullyQualifiedClassName() {
        return fqn;
    }

    /**
     * @return List of fields.
     */
    List<GridToStringFieldDescriptor> getFields() {
        return fields;
    }
}
