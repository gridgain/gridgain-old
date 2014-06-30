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

package org.gridgain.grid.kernal.processors.rest.client.message;

import java.util.*;

/**
 *
 */
public class GridRouterResponse extends GridClientAbstractMessage {
    /** */
    private static final long serialVersionUID = 0L;

    /** Raw message. */
    private final byte[] body;

    /** Error message. */
    private final String errMsg;

    /** Status. */
    private final int status;

    /**
     * @param body Message in raw form.
     * @param clientId Client id.
     * @param reqId Request id.
     * @param destId Destination where this message should be delivered.
     */
    public GridRouterResponse(byte[] body, Long reqId, UUID clientId, UUID destId) {
        this.body = body;
        errMsg = null;
        status = GridClientResponse.STATUS_SUCCESS;

        destinationId(destId);
        clientId(clientId);
        requestId(reqId);
    }

    /**
     * @return Response body.
     */
    public byte[] body() {
        return body;
    }

    /**
     * @return Error message.
     */
    public String errorMessage() {
        return errMsg;
    }

    /**
     * @return Status.
     */
    public int status() {
        return status;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return "GridRoutedResponse [" +
            "clientId=" + clientId() +
            ", reqId=" + requestId() +
            ", destId=" + destinationId() +
            ", status=" + status +
            ", errMsg=" + errorMessage() + "]";
    }
}
