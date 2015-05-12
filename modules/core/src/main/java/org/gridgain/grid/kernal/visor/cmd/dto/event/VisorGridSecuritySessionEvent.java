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

package org.gridgain.grid.kernal.visor.cmd.dto.event;

import org.gridgain.grid.*;
import org.gridgain.grid.security.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.util.*;

/**
 * Lightweight counterpart for {@link VisorGridSecuritySessionEvent}.
 */
public class VisorGridSecuritySessionEvent extends VisorGridEvent {
    /** */
    private static final long serialVersionUID = 0L;

    /**  Subject type. */
    private final GridSecuritySubjectType subjType;

    /** Subject ID. */
    private final UUID subjId;

    /**
     * Create event with given parameters.
     *
     * @param typeId Event type.
     * @param id Event id.
     * @param name Event name.
     * @param nid Event node ID.
     * @param timestamp Event timestamp.
     * @param message Event message.
     * @param shortDisplay Shortened version of {@code toString()} result.
     * @param subjType Subject type.
     * @param subjId Subject ID.
     */
    public VisorGridSecuritySessionEvent(
        int typeId,
        GridUuid id,
        String name,
        UUID nid,
        long timestamp,
        String message,
        String shortDisplay,
        GridSecuritySubjectType subjType,
        UUID subjId
    ) {
        super(typeId, id, name, nid, timestamp, message, shortDisplay);

        this.subjType = subjType;
        this.subjId = subjId;
    }

    /**
     * Gets subject ID that triggered the event.
     *
     * @return Subject ID that triggered the event.
     */
    public UUID subjId() {
        return subjId;
    }

    /**
     * Gets subject type that triggered the event.
     *
     * @return Subject type that triggered the event.
     */
    public GridSecuritySubjectType subjType() {
        return subjType;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(VisorGridSecuritySessionEvent.class, this);
    }
}
