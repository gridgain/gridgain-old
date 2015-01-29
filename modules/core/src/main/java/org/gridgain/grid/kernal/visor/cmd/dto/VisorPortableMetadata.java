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
import java.util.*;

/**
 * Portable object metadata to show in Visor.
 */
public class VisorPortableMetadata implements Serializable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Type name */
    private String typeName;

    /** Type Id */
    private Integer typeId;

    /** Filed list */
    private Collection<VisorPortableMetadataField> fields;

    /** Type name */
    public String typeName() {
        return typeName;
    }

    public void typeName(String typeName) {
        this.typeName = typeName;
    }

    /** Type Id */
    public Integer typeId() {
        return typeId;
    }

    public void typeId(Integer typeId) {
        this.typeId = typeId;
    }

    /** Fields list */
    public Collection<VisorPortableMetadataField> fields() {
        return fields;
    }

    public void fields(Collection<VisorPortableMetadataField> fields) {
        this.fields = fields;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorPortableMetadata.class, this);
    }
}
