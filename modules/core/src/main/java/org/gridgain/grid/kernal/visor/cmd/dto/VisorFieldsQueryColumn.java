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

package org.gridgain.grid.kernal.visor.cmd.dto;

import org.gridgain.grid.util.typedef.internal.*;

import java.io.*;

/**
 * Data transfer object for query column type description.
 */
public class VisorFieldsQueryColumn implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Column type. */
    private final String type;

    /** Field name. */
    private final String field;

    /**
     * Create data transfer object with given parameters.
     *
     * @param type Column type.
     * @param field Field name.
     */
    public VisorFieldsQueryColumn(String type, String field) {
        this.type = type;
        this.field = field;
    }

    /**
     * @return Column type.
     */
    public String type() {
        return type;
    }

    /**
     * @return Field name.
     */
    public String field() {
        return field;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorFieldsQueryColumn.class, this);
    }
}
