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

package org.gridgain.grid.kernal.processors.cache.distributed;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.util.direct.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.nio.*;
import java.util.*;

/**
 * Transaction completion message.
 */
public class GridDistributedTxFinishRequest<K, V> extends GridDistributedBaseMessage<K, V> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Future ID. */
    private GridUuid futId;

    /** Thread ID. */
    private long threadId;

    /** Commit version. */
    private GridCacheVersion commitVer;

    /** Invalidate flag. */
    private boolean invalidate;

    /** Commit flag. */
    private boolean commit;

    /** Min version used as base for completed versions. */
    private GridCacheVersion baseVer;

    /** Transaction write entries. */
    @GridToStringInclude
    @GridDirectTransient
    private Collection<GridCacheTxEntry<K, V>> writeEntries;

    /** */
    @GridDirectCollection(byte[].class)
    private Collection<byte[]> writeEntriesBytes;

    /** Write entries which have not been transferred to nodes during lock request. */
    @GridToStringInclude
    @GridDirectTransient
    private Collection<GridCacheTxEntry<K, V>> recoveryWrites;

    /** */
    @GridDirectCollection(byte[].class)
    private Collection<byte[]> recoveryWritesBytes;

    /** */
    private boolean reply;

    /** Expected txSize. */
    private int txSize;

    /** Group lock key. */
    @GridDirectTransient
    private Object grpLockKey;

    /** Group lock key bytes. */
    private byte[] grpLockKeyBytes;

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridDistributedTxFinishRequest() {
        /* No-op. */
    }

    /**
     * @param xidVer Transaction ID.
     * @param futId future ID.
     * @param threadId Thread ID.
     * @param commitVer Commit version.
     * @param commit Commit flag.
     * @param invalidate Invalidate flag.
     * @param baseVer Base version.
     * @param committedVers Committed versions.
     * @param rolledbackVers Rolled back versions.
     * @param txSize Expected transaction size.
     * @param writeEntries Write entries.
     * @param recoveryWrites Recover entries. In pessimistic mode entries which were not transferred to remote nodes
     *      with lock requests. {@code Null} for optimistic mode.
     * @param reply Reply flag.
     * @param grpLockKey Group lock key if this is a group-lock transaction.
     */
    public GridDistributedTxFinishRequest(
        GridCacheVersion xidVer,
        GridUuid futId,
        @Nullable GridCacheVersion commitVer,
        long threadId,
        boolean commit,
        boolean invalidate,
        GridCacheVersion baseVer,
        Collection<GridCacheVersion> committedVers,
        Collection<GridCacheVersion> rolledbackVers,
        int txSize,
        Collection<GridCacheTxEntry<K, V>> writeEntries,
        Collection<GridCacheTxEntry<K, V>> recoveryWrites,
        boolean reply,
        @Nullable Object grpLockKey
    ) {
        super(xidVer, writeEntries == null ? 0 : writeEntries.size());
        assert xidVer != null;

        this.futId = futId;
        this.commitVer = commitVer;
        this.threadId = threadId;
        this.commit = commit;
        this.invalidate = invalidate;
        this.baseVer = baseVer;
        this.txSize = txSize;
        this.writeEntries = writeEntries;
        this.recoveryWrites = recoveryWrites;
        this.reply = reply;
        this.grpLockKey = grpLockKey;

        completedVersions(committedVers, rolledbackVers);
    }

    /**
     * @return Future ID.
     */
    public GridUuid futureId() {
        return futId;
    }

    /**
     * @return Thread ID.
     */
    public long threadId() {
        return threadId;
    }

    /**
     * @return Commit version.
     */
    public GridCacheVersion commitVersion() {
        return commitVer;
    }

    /**
     * @return Commit flag.
     */
    public boolean commit() {
        return commit;
    }

    /**
     *
     * @return Invalidate flag.
     */
    public boolean isInvalidate() {
        return invalidate;
    }

    /**
     * @return Base version.
     */
    public GridCacheVersion baseVersion() {
        return baseVer;
    }

    /**
     * @return Write entries.
     */
    public Collection<GridCacheTxEntry<K, V>> writes() {
        return writeEntries;
    }

    /**
     * @return Recover entries.
     */
    public Collection<GridCacheTxEntry<K, V>> recoveryWrites() {
        return recoveryWrites;
    }

    /**
     * @return Expected tx size.
     */
    public int txSize() {
        return txSize;
    }

    /**
     *
     * @return {@code True} if reply is required.
     */
    public boolean replyRequired() {
        return reply;
    }

    /**
     * @return {@code True} if group lock transaction.
     */
    public boolean groupLock() {
        return grpLockKey != null;
    }

    /**
     * @return Group lock key.
     */
    @Nullable public Object groupLockKey() {
        return grpLockKey;
    }

    /** {@inheritDoc} */
    @Override public void prepareMarshal(GridCacheContext<K, V> ctx) throws GridException {
        super.prepareMarshal(ctx);

        if (writeEntries != null) {
            marshalTx(writeEntries, ctx);

            writeEntriesBytes = new ArrayList<>(writeEntries.size());

            for (GridCacheTxEntry<K, V> e : writeEntries)
                writeEntriesBytes.add(ctx.marshaller().marshal(e));
        }

        if (recoveryWrites != null) {
            marshalTx(recoveryWrites, ctx);

            recoveryWritesBytes = new ArrayList<>(recoveryWrites.size());

            for (GridCacheTxEntry<K, V> e : recoveryWrites)
                recoveryWritesBytes.add(ctx.marshaller().marshal(e));
        }

        if (grpLockKey != null && grpLockKeyBytes == null) {
            if (ctx.deploymentEnabled())
                prepareObject(grpLockKey, ctx);

            grpLockKeyBytes = CU.marshal(ctx, grpLockKey);
        }
    }

    /** {@inheritDoc} */
    @Override public void finishUnmarshal(GridCacheContext<K, V> ctx, ClassLoader ldr) throws GridException {
        super.finishUnmarshal(ctx, ldr);

        if (writeEntriesBytes != null) {
            writeEntries = new ArrayList<>(writeEntriesBytes.size());

            for (byte[] arr : writeEntriesBytes)
                writeEntries.add(ctx.marshaller().<GridCacheTxEntry<K, V>>unmarshal(arr, ldr));

            unmarshalTx(writeEntries, ctx, ldr);
        }

        if (recoveryWritesBytes != null) {
            recoveryWrites = new ArrayList<>(recoveryWritesBytes.size());

            for (byte[] arr : recoveryWritesBytes)
                recoveryWrites.add(ctx.marshaller().<GridCacheTxEntry<K, V>>unmarshal(arr, ldr));

            unmarshalTx(recoveryWrites, ctx, ldr);
        }

        if (grpLockKeyBytes != null && grpLockKey == null)
            grpLockKey = ctx.marshaller().unmarshal(grpLockKeyBytes, ldr);
    }

    /** {@inheritDoc} */
    @SuppressWarnings({"CloneDoesntCallSuperClone", "CloneCallsConstructors",
        "OverriddenMethodCallDuringObjectConstruction"})
    @Override public GridTcpCommunicationMessageAdapter clone() {
        GridDistributedTxFinishRequest _clone = new GridDistributedTxFinishRequest();

        clone0(_clone);

        return _clone;
    }

    /** {@inheritDoc} */
    @Override protected void clone0(GridTcpCommunicationMessageAdapter _msg) {
        super.clone0(_msg);

        GridDistributedTxFinishRequest _clone = (GridDistributedTxFinishRequest)_msg;

        _clone.futId = futId;
        _clone.threadId = threadId;
        _clone.commitVer = commitVer;
        _clone.invalidate = invalidate;
        _clone.commit = commit;
        _clone.baseVer = baseVer;
        _clone.writeEntries = writeEntries;
        _clone.writeEntriesBytes = writeEntriesBytes;
        _clone.recoveryWrites = recoveryWrites;
        _clone.recoveryWritesBytes = recoveryWritesBytes;
        _clone.reply = reply;
        _clone.txSize = txSize;
        _clone.grpLockKey = grpLockKey;
        _clone.grpLockKeyBytes = grpLockKeyBytes;
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
            case 7:
                if (!commState.putCacheVersion(baseVer))
                    return false;

                commState.idx++;

            case 8:
                if (!commState.putBoolean(commit))
                    return false;

                commState.idx++;

            case 9:
                if (!commState.putCacheVersion(commitVer))
                    return false;

                commState.idx++;

            case 10:
                if (!commState.putGridUuid(futId))
                    return false;

                commState.idx++;

            case 11:
                if (!commState.putByteArray(grpLockKeyBytes))
                    return false;

                commState.idx++;

            case 12:
                if (!commState.putBoolean(invalidate))
                    return false;

                commState.idx++;

            case 13:
                if (recoveryWritesBytes != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(recoveryWritesBytes.size()))
                            return false;

                        commState.it = recoveryWritesBytes.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putByteArray((byte[])commState.cur))
                            return false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

                commState.idx++;

            case 14:
                if (!commState.putBoolean(reply))
                    return false;

                commState.idx++;

            case 15:
                if (!commState.putLong(threadId))
                    return false;

                commState.idx++;

            case 16:
                if (!commState.putInt(txSize))
                    return false;

                commState.idx++;

            case 17:
                if (writeEntriesBytes != null) {
                    if (commState.it == null) {
                        if (!commState.putInt(writeEntriesBytes.size()))
                            return false;

                        commState.it = writeEntriesBytes.iterator();
                    }

                    while (commState.it.hasNext() || commState.cur != NULL) {
                        if (commState.cur == NULL)
                            commState.cur = commState.it.next();

                        if (!commState.putByteArray((byte[])commState.cur))
                            return false;

                        commState.cur = NULL;
                    }

                    commState.it = null;
                } else {
                    if (!commState.putInt(-1))
                        return false;
                }

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
            case 7:
                GridCacheVersion baseVer0 = commState.getCacheVersion();

                if (baseVer0 == CACHE_VER_NOT_READ)
                    return false;

                baseVer = baseVer0;

                commState.idx++;

            case 8:
                if (buf.remaining() < 1)
                    return false;

                commit = commState.getBoolean();

                commState.idx++;

            case 9:
                GridCacheVersion commitVer0 = commState.getCacheVersion();

                if (commitVer0 == CACHE_VER_NOT_READ)
                    return false;

                commitVer = commitVer0;

                commState.idx++;

            case 10:
                GridUuid futId0 = commState.getGridUuid();

                if (futId0 == GRID_UUID_NOT_READ)
                    return false;

                futId = futId0;

                commState.idx++;

            case 11:
                byte[] grpLockKeyBytes0 = commState.getByteArray();

                if (grpLockKeyBytes0 == BYTE_ARR_NOT_READ)
                    return false;

                grpLockKeyBytes = grpLockKeyBytes0;

                commState.idx++;

            case 12:
                if (buf.remaining() < 1)
                    return false;

                invalidate = commState.getBoolean();

                commState.idx++;

            case 13:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (recoveryWritesBytes == null)
                        recoveryWritesBytes = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        byte[] _val = commState.getByteArray();

                        if (_val == BYTE_ARR_NOT_READ)
                            return false;

                        recoveryWritesBytes.add((byte[])_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

            case 14:
                if (buf.remaining() < 1)
                    return false;

                reply = commState.getBoolean();

                commState.idx++;

            case 15:
                if (buf.remaining() < 8)
                    return false;

                threadId = commState.getLong();

                commState.idx++;

            case 16:
                if (buf.remaining() < 4)
                    return false;

                txSize = commState.getInt();

                commState.idx++;

            case 17:
                if (commState.readSize == -1) {
                    if (buf.remaining() < 4)
                        return false;

                    commState.readSize = commState.getInt();
                }

                if (commState.readSize >= 0) {
                    if (writeEntriesBytes == null)
                        writeEntriesBytes = new ArrayList<>(commState.readSize);

                    for (int i = commState.readItems; i < commState.readSize; i++) {
                        byte[] _val = commState.getByteArray();

                        if (_val == BYTE_ARR_NOT_READ)
                            return false;

                        writeEntriesBytes.add((byte[])_val);

                        commState.readItems++;
                    }
                }

                commState.readSize = -1;
                commState.readItems = 0;

                commState.idx++;

        }

        return true;
    }

    /** {@inheritDoc} */
    @Override public byte directType() {
        return 24;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return GridToStringBuilder.toString(GridDistributedTxFinishRequest.class, this,
            "super", super.toString());
    }
}
