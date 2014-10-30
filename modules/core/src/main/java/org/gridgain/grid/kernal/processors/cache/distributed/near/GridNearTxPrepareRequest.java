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

package org.gridgain.grid.kernal.processors.cache.distributed.near;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.cache.distributed.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Near transaction prepare request.
 */
public class GridNearTxPrepareRequest<K, V> extends GridDistributedTxPrepareRequest<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Future ID. */
    private GridUuid futId;

    /** Mini future ID. */
    private GridUuid miniId;

    /** Synchronous commit flag. */
    private boolean syncCommit;

    /** Synchronous rollback flag. */
    private boolean syncRollback;

    /** Topology version. */
    private long topVer;

    /** {@code True} if this last prepare request for node. */
    private boolean last;

    /** IDs of backup nodes receiving last prepare request during this prepare. */
    @GridDirectCollection(UUID.class)
    private Collection<UUID> lastBackups;

    /** Subject ID. */
    @GridDirectVersion(1)
    private UUID subjId;

    /** Task name hash. */
    @GridDirectVersion(2)
    private int taskNameHash;

    /**
     * Empty constructor required for {@link Externalizable}.
     */
    public GridNearTxPrepareRequest() {
        // No-op.
    }

    /**
     * @param futId Future ID.
     * @param topVer Topology version.
     * @param tx Transaction.
     * @param reads Read entries.
     * @param writes Write entries.
     * @param grpLockKey Group lock key if preparing group-lock transaction.
     * @param partLock {@code True} if preparing group-lock transaction with partition lock.
     * @param syncCommit Synchronous commit.
     * @param syncRollback Synchronous rollback.
     * @param txNodes Transaction nodes mapping.
     * @param last {@code True} if this last prepare request for node.
     * @param lastBackups IDs of backup nodes receiving last prepare request during this prepare.
     */
    public GridNearTxPrepareRequest(GridUuid futId, long topVer, GridCacheTxEx<K, V> tx,
        Collection<GridCacheTxEntry<K, V>> reads, Collection<GridCacheTxEntry<K, V>> writes, Object grpLockKey,
        boolean partLock, boolean syncCommit, boolean syncRollback,
        Map<UUID, Collection<UUID>> txNodes, boolean last, Collection<UUID> lastBackups, @Nullable UUID subjId,
        int taskNameHash) {
        super(tx, reads, writes, grpLockKey, partLock, txNodes);

        assert futId != null;

        this.futId = futId;
        this.topVer = topVer;
        this.syncCommit = syncCommit;
        this.syncRollback = syncRollback;
        this.last = last;
        this.lastBackups = lastBackups;
        this.subjId = subjId;
        this.taskNameHash = taskNameHash;
    }

    /**
     * @return IDs of backup nodes receiving last prepare request during this prepare.
     */
    public Collection<UUID> lastBackups() {
        return lastBackups;
    }

    /**
     * @return {@code True} if this last prepare request for node.
     */
    public boolean last() {
        return last;
    }

    /**
     * @return Future ID.
     */
    public GridUuid futureId() {
        return futId;
    }

    /**
     * @return Mini future ID.
     */
    public GridUuid miniId() {
        return miniId;
    }

    /**
     * @param miniId Mini future ID.
     */
    public void miniId(GridUuid miniId) {
        this.miniId = miniId;
    }

    /**
     * @return Subject ID.
     */
    @Nullable public UUID subjectId() {
        return subjId;
    }

    /**
     * @return Task name hash.
     */
    public int taskNameHash() {
        return taskNameHash;
    }

    /**
     * @return Synchronous commit.
     */
    public boolean syncCommit() {
        return syncCommit;
    }

    /**
     * @return Synchronous rollback.
     */
    public boolean syncRollback() {
        return syncRollback;
    }

    /**
     * @return Topology version.
     */
    @Override public long topologyVersion() {
        return topVer;
    }

    /**
     * @param ctx Cache context.
     */
    void cloneEntries(GridCacheContext<K, V> ctx) {
        reads(cloneEntries(ctx, reads()));
        writes(cloneEntries(ctx, writes()));
    }

    /**
     * @param ctx Cache context.
     * @param c Collection of entries to clone.
     * @return Cloned collection.
     */
    private Collection<GridCacheTxEntry<K, V>> cloneEntries(GridCacheContext<K, V> ctx,
        Collection<GridCacheTxEntry<K, V>> c) {
        if (F.isEmpty(c))
            return c;

        Collection<GridCacheTxEntry<K, V>> cp = new ArrayList<>(c.size());

        for (GridCacheTxEntry<K, V> e : c)
            cp.add(e.cleanCopy(ctx));

        return cp;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridNearTxPrepareRequest _clone = new GridNearTxPrepareRequest();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        super.clone0(_msg);

        GridNearTxPrepareRequest _clone = (GridNearTxPrepareRequest)_msg;

        _clone.futId = futId;
        _clone.miniId = miniId;
        _clone.syncCommit = syncCommit;
        _clone.syncRollback = syncRollback;
        _clone.topVer = topVer;
        _clone.last = last;
        _clone.lastBackups = lastBackups;
        _clone.subjId = subjId;
        _clone.taskNameHash = taskNameHash;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
    @Override public boolean writeTo(ByteBuffer buf) {
        commState.setBuffer(buf);

        if (!super.writeTo(buf))
            return false;

        if (!commState.typeWritten) {
            if (!commState.putByte(directType()))
                return false;

            commState.typeWritten = true;
        }

        switch (commState.idx) {
            case 20:
                if (!commState.putGridUuid(futId))
                    return false;

                commState.idx++;

            case 21:
                if (!commState.putBoolean(last))
                    return false;

                commState.idx++;

            case 22:
                if (lastBackups != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(lastBackups.size()))
                            return false;

                        commState.it = lastBackups.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putUuid((UUID)commState.cur))
                            return false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

                commState.idx++;

            case 23:
                if (!commState.putGridUuid(miniId))
                    return false;

                commState.idx++;

            case 24:
                if (!commState.putBoolean(syncCommit))
                    return false;

                commState.idx++;

            case 25:
                if (!commState.putBoolean(syncRollback))
                    return false;

                commState.idx++;

            case 26:
                if (!commState.putLong(topVer))
                    return false;

                commState.idx++;

            case 27:
                if (!commState.putUuid(subjId))
                    return false;

                commState.idx++;

            case 28:
                if (!commState.putInt(taskNameHash))
                    return false;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @SuppressWarnings("all")
    @Override public boolean readFrom(ByteBuffer buf) {
        commState.setBuffer(buf);

        if (!super.readFrom(buf))
            return false;

        switch (commState.idx) {
            case 20:
                GridUuid futId0 = commState.getGridUuid();

                if (futId0 == GRID_UUID_NOT_READ)
                    return false;

                futId = futId0;

                commState.idx++;

            case 21:
                if (buf.remaining() < 1)
                    return false;

                last = commState.getBoolean();

                commState.idx++;

            case 22:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (lastBackups == null)
                        lastBackups = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        UUID _val = commState.getUuid();

                        if (_val == UUID_NOT_READ)
                            return false;

                        lastBackups.add((UUID)_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

            case 23:
                GridUuid miniId0 = commState.getGridUuid();

                if (miniId0 == GRID_UUID_NOT_READ)
                    return false;

                miniId = miniId0;

                commState.idx++;

            case 24:
                if (buf.remaining() < 1)
                    return false;

                syncCommit = commState.getBoolean();

                commState.idx++;

            case 25:
                if (buf.remaining() < 1)
                    return false;

                syncRollback = commState.getBoolean();

                commState.idx++;

            case 26:
                if (buf.remaining() < 8)
                    return false;

                topVer = commState.getLong();

                commState.idx++;

            case 27:
                UUID subjId0 = commState.getUuid();

                if (subjId0 == UUID_NOT_READ)
                    return false;

                subjId = subjId0;

                commState.idx++;

            case 28:
                if (buf.remaining() < 4)
                    return false;

                taskNameHash = commState.getInt();

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 54;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridNearTxPrepareRequest.class, this, super.toString());
    }
}
