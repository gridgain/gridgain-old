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

package org.gridgain.grid.kernal.visor.cmd.tasks;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.kernal.visor.cmd.*;
import org.gridgain.grid.kernal.visor.cmd.dto.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.portables.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Task that collects portables metadata.
 */
@GridInternal
public class VisorPortableCollectMetadataTask extends VisorOneNodeTask<Long, GridBiTuple<Long, Collection<VisorPortableMetadata>>> {
    /** */
    private static final long serialVersionUID = 0L;

    /** {@inheritDoc} */
    @Override protected VisorPortableCollectMetadataJob job(Long lastUpdate) {
        return new VisorPortableCollectMetadataJob(lastUpdate);
    }

    /** Job that collect portables metadata on node. */
    private static class VisorPortableCollectMetadataJob extends VisorJob<Long, GridBiTuple<Long, Collection<VisorPortableMetadata>>> {
        /** */
        private static final long serialVersionUID = 0L;

        /** Create job with given argument. */
        private VisorPortableCollectMetadataJob(Long lastUpdate) {
            super(lastUpdate);
        }

        /** {@inheritDoc} */
        @Override protected GridBiTuple<Long, Collection<VisorPortableMetadata>> run(Long lastUpdate) throws GridException {
            final GridPortables p = g.portables();

            final Collection<VisorPortableMetadata> data = new ArrayList<>(p.metadata().size());

            for(GridPortableMetadata metadata: p.metadata()) {
                final VisorPortableMetadata type = new VisorPortableMetadata();

                type.typeName(metadata.typeName());

                type.typeId(p.typeId(metadata.typeName()));

                final Collection<VisorPortableMetadataField> fields = new ArrayList<>(metadata.fields().size());

                for (String fieldName: metadata.fields()) {
                    final VisorPortableMetadataField field = new VisorPortableMetadataField();

                    field.fieldName(fieldName);
                    field.fieldTypeName(metadata.fieldTypeName(fieldName));

                    fields.add(field);
                }

                type.fields(fields);

                data.add(type);
            }

            return new GridBiTuple<>(0L, data);
        }

        /** {@inheritDoc} */
        @Override public String toString() {
            return S.toString(VisorPortableCollectMetadataJob.class, this);
        }
    }
}
