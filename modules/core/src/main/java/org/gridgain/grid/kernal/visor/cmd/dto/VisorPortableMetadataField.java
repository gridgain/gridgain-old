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
 * Portable object metadata field information.
 */
public class VisorPortableMetadataField implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Field name. */
    private String fieldName;

    /** Field type name. */
    private String fieldTypeName;

    /** Field id. */
    private Integer fieldId;

    /** Field name. */
    public String fieldName() {
        return fieldName;
    }

    public void fieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    /** Field type name. */
    public String fieldTypeName() {
        return fieldTypeName;
    }

    public void fieldTypeName(String fieldTypeName) {
        this.fieldTypeName = fieldTypeName;
    }

    /** Field id. */
    public Integer fieldId() {
        return fieldId;
    }

    public void fieldId(Integer fieldId) {
        this.fieldId = fieldId;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorPortableMetadataField.class, this);
    }
}
