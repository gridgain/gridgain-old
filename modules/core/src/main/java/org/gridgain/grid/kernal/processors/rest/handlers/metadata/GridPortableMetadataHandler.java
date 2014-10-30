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

package org.gridgain.grid.kernal.processors.rest.handlers.metadata;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.rest.*;
import org.gridgain.grid.kernal.processors.rest.client.message.*;
import org.gridgain.grid.kernal.processors.rest.handlers.*;
import org.gridgain.grid.kernal.processors.rest.request.*;
import org.gridgain.grid.portables.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

import static org.gridgain.grid.kernal.processors.rest.GridRestCommand.*;

/**
 * Portable metadata handler.
 */
public class GridPortableMetadataHandler extends GridRestCommandHandlerAdapter {
    /** Supported commands. */
    private static final Collection<GridRestCommand> SUPPORTED_COMMANDS = U.sealList(
        PUT_PORTABLE_METADATA,
        GET_PORTABLE_METADATA
    );

    /**
     * @param ctx Context.
     */
    public GridPortableMetadataHandler(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridRestCommand> supportedCommands() {
        return SUPPORTED_COMMANDS;
    }

    /** {@inheritDoc} */
    @Override public GridFuture<GridRestResponse> handleAsync(GridRestRequest req) {
        assert SUPPORTED_COMMANDS.contains(req.command()) : req.command();

        try {
            if (req.command() == GET_PORTABLE_METADATA) {
                GridRestPortableGetMetaDataRequest metaReq = (GridRestPortableGetMetaDataRequest)req;

                GridRestResponse res = new GridRestResponse();

                Map<Integer, GridPortableMetadata> meta = ctx.portable().metadata(metaReq.typeIds());

                GridClientMetaDataResponse metaRes = new GridClientMetaDataResponse();

                metaRes.metaData(meta);

                res.setResponse(metaRes);

                return new GridFinishedFuture<>(ctx, res);
            }
            else {
                assert req.command() == PUT_PORTABLE_METADATA;

                GridRestPortablePutMetaDataRequest metaReq = (GridRestPortablePutMetaDataRequest)req;

                for (GridClientPortableMetaData meta : metaReq.metaData())
                    ctx.portable().updateMetaData(meta.typeId(),
                        meta.typeName(),
                        meta.affinityKeyFieldName(),
                        meta.fields());

                GridRestResponse res = new GridRestResponse();

                res.setResponse(true);

                return new GridFinishedFuture<>(ctx, res);
            }
        }
        catch (GridRuntimeException e) {
            return new GridFinishedFuture<>(ctx, e);
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridPortableMetadataHandler.class, this, super.toString());
    }
}
