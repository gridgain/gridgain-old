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

package org.gridgain.grid.kernal.ggfs.common;

import org.gridgain.grid.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.processors.ggfs.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.tostring.*;

import java.io.*;
import java.util.*;

import static org.gridgain.grid.kernal.ggfs.common.GridGgfsIpcCommand.*;

/**
 * GGFS path command response.
 */
public class GridGgfsControlResponse extends GridGgfsMessage {
    /** Generic error (not GGFS) while performing operations. */
    private static final int ERR_GENERIC = 0;

    /** Generic GGFS error while performing operations. */
    private static final int ERR_GGFS_GENERIC = 1;

    /** Target file not found. */
    private static final int ERR_FILE_NOT_FOUND = 2;

    /** Target path already exists. */
    private static final int ERR_PATH_ALREADY_EXISTS = 3;

    /** Directory is not empty with */
    private static final int ERR_DIRECTORY_NOT_EMPTY = 4;

    /** Target parent is not a directory. */
    private static final int ERR_PARENT_NOT_DIRECTORY = 5;

    /** Secondary HDFS version differs from classpath version. */
    private static final int ERR_INVALID_HDFS_VERSION = 6;

    /** Failed to retrieve file's data block. */
    private static final int ERR_CORRUPTED_FILE = 7;

    /** Response is boolean. */
    public static final int RES_TYPE_BOOLEAN = 0;

    /** Response is Long. */
    public static final int RES_TYPE_LONG = 1;

    /** Response is GridGgfsFile. */
    public static final int RES_TYPE_GGFS_FILE = 2;

    /** Response is GridGgfsFileInfo. */
    public static final int RES_TYPE_GGFS_STREAM_DESCRIPTOR = 3;

    /** Response is GridGgfsPath. */
    public static final int RES_TYPE_GGFS_PATH = 4;

    /** Response is collection of GridGgfsFile. */
    public static final int RES_TYPE_COL_GGFS_FILE = 5;

    /** Response is collection of GridGgfsPath. */
    public static final int RES_TYPE_COL_GGFS_PATH = 6;

    /** Response is collection of GridGgfsBlockLocation. */
    public static final int RES_TYPE_COL_GGFS_BLOCK_LOCATION = 7;

    /** Response is collection of GridGgfsBlockLocation. */
    public static final int RES_TYPE_BYTE_ARRAY = 8;

    /** Response is an error containing stream ID and error message. */
    public static final int RES_TYPE_ERR_STREAM_ID = 9;

    /** Response is a handshake  */
    public static final int RES_TYPE_HANDSHAKE = 10;

    /** Response is a handshake  */
    public static final int RES_TYPE_STATUS = 11;

    /** Response is a path summary. */
    public static final int RES_TYPE_GGFS_PATH_SUMMARY = 12;

    /** Message header size. */
    public static final int RES_HEADER_SIZE = 9;

    /** We have limited number of object response types. */
    private int resType = -1;

    /** Response. */
    @GridToStringInclude
    private Object res;

    /** Bytes length to avoid iteration and summing. */
    private int len;

    /** Error (if any). */
    private String err;

    /** Error code. */
    private int errCode = -1;

    /**
     *
     */
    public GridGgfsControlResponse() {
        command(CONTROL_RESPONSE);
    }

    /**
     * @return Response.
     */
    public Object response() {
        return res;
    }

    /**
     * @param res Response.
     */
    public void response(boolean res) {
        resType = RES_TYPE_BOOLEAN;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void response(long res) {
        resType = RES_TYPE_LONG;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void response(byte[][] res) {
        resType = RES_TYPE_BYTE_ARRAY;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void response(GridGgfsInputStreamDescriptor res) {
        resType = RES_TYPE_GGFS_STREAM_DESCRIPTOR;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void response(GridGgfsFile res) {
        resType = RES_TYPE_GGFS_FILE;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void response(GridGgfsPath res) {
        resType = RES_TYPE_GGFS_PATH;

        this.res = res;
    }

    /**
     * @param res Path summary response.
     */
    public void response(GridGgfsPathSummary res) {
        resType = RES_TYPE_GGFS_PATH_SUMMARY;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void files(Collection<GridGgfsFile> res) {
        resType = RES_TYPE_COL_GGFS_FILE;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void paths(Collection<GridGgfsPath> res) {
        resType = RES_TYPE_COL_GGFS_PATH;

        this.res = res;
    }

    /**
     * @param res Response.
     */
    public void locations(Collection<GridGgfsBlockLocation> res) {
        resType = RES_TYPE_COL_GGFS_BLOCK_LOCATION;

        this.res = res;
    }

    /**
     * @param res Handshake message.
     */
    public void handshake(GridGgfsHandshakeResponse res) {
        resType = RES_TYPE_HANDSHAKE;

        this.res = res;
    }

    /**
     * @param res Status response.
     */
    public void status(GridGgfsStatus res) {
        resType = RES_TYPE_STATUS;

        this.res = res;
    }

    /**
     * @param len Response length.
     */
    public void length(int len) {
        this.len = len;
    }

    /**
     * @return Error message if occurred.
     */
    public boolean hasError() {
        return errCode != -1;
    }

    /**
     * @param errCode Error code.
     * @param err Error.
     * @throws GridException Based on error code.
     */
    public static void throwError(Integer errCode, String err) throws GridException {
        assert err != null;
        assert errCode != -1;

        if (errCode == ERR_FILE_NOT_FOUND)
            throw new GridGgfsFileNotFoundException(err);
        else if (errCode == ERR_PATH_ALREADY_EXISTS)
            throw new GridGgfsPathAlreadyExistsException(err);
        else if (errCode == ERR_DIRECTORY_NOT_EMPTY)
            throw new GridGgfsDirectoryNotEmptyException(err);
        else if (errCode == ERR_PARENT_NOT_DIRECTORY)
            throw new GridGgfsParentNotDirectoryException(err);
        else if (errCode == ERR_INVALID_HDFS_VERSION)
            throw new GridGgfsInvalidHdfsVersionException(err);
        else if (errCode == ERR_CORRUPTED_FILE)
            throw new GridGgfsCorruptedFileException(err);
        else if (errCode == ERR_GGFS_GENERIC)
            throw new GridGgfsException(err);

        throw new GridException(err);
    }

    /**
     * @throws GridException Based on error code.
     */
    public void throwError() throws GridException {
        throwError(errCode, err);
    }

    /**
     * @return Error code.
     */
    public int errorCode() {
        return errCode;
    }

    /**
     * @param e Error if occurred.
     */
    public void error(GridException e) {
        err = e.getMessage();
        errCode = errorCode(e);
    }

    /**
     * @param streamId Stream ID.
     * @param err Error message if occurred.
     */
    public void error(long streamId, String err) {
        resType = RES_TYPE_ERR_STREAM_ID;

        res = streamId;
        errCode = ERR_GENERIC;

        this.err = err;
    }

    /**
     * Gets error code based on exception class.
     *
     * @param e Exception to analyze.
     * @return Error code.
     */
    private int errorCode(GridException e) {
        return errorCode(e, true);
    }

    /**
     * Gets error code based on exception class.
     *
     * @param e Exception to analyze.
     * @param checkIo Whether to check for IO exception.
     * @return Error code.
     */
    @SuppressWarnings("unchecked")
    private int errorCode(GridException e, boolean checkIo) {
        if (X.hasCause(e, GridGgfsFileNotFoundException.class))
            return ERR_FILE_NOT_FOUND;
        else if (GridGgfsPathAlreadyExistsException.class.isInstance(e))
            return ERR_PATH_ALREADY_EXISTS;
        else if (GridGgfsDirectoryNotEmptyException.class.isInstance(e))
            return ERR_DIRECTORY_NOT_EMPTY;
        else if (GridGgfsParentNotDirectoryException.class.isInstance(e))
            return ERR_PARENT_NOT_DIRECTORY;
        else if (GridGgfsInvalidHdfsVersionException.class.isInstance(e))
            return ERR_INVALID_HDFS_VERSION;
        else if (X.hasCause(e, GridGgfsCorruptedFileException.class))
            return ERR_CORRUPTED_FILE;
            // This check should be the last.
        else if (GridGgfsException.class.isInstance(e))
            return ERR_GGFS_GENERIC;

        return ERR_GENERIC;
    }

    /**
     * Writes object to data output. Do not use externalizable interface to avoid marshaller.
     *
     * @param out Data output.
     * @throws IOException If error occurred.
     */
    @SuppressWarnings("unchecked")
    public void writeExternal(ObjectOutput out) throws IOException {
        byte[] hdr = new byte[RES_HEADER_SIZE];

        U.intToBytes(resType, hdr, 0);

        int off = 4;

        hdr[off++] = err != null ? (byte)1 : (byte)0;

        if (resType == RES_TYPE_BYTE_ARRAY)
            U.intToBytes(len, hdr, off);

        out.write(hdr);

        if (err != null) {
            out.writeUTF(err);
            out.writeInt(errCode);

            if (resType == RES_TYPE_ERR_STREAM_ID)
                out.writeLong((Long)res);

            return;
        }

        switch (resType) {
            case RES_TYPE_BOOLEAN:
                out.writeBoolean((Boolean)res);

                break;

            case RES_TYPE_LONG:
                out.writeLong((Long)res);

                break;

            case RES_TYPE_BYTE_ARRAY:
                byte[][] buf = (byte[][])res;

                for (byte[] bytes : buf)
                    out.write(bytes);

                break;

            case RES_TYPE_GGFS_PATH:
            case RES_TYPE_GGFS_PATH_SUMMARY:
            case RES_TYPE_GGFS_FILE:
            case RES_TYPE_GGFS_STREAM_DESCRIPTOR:
            case RES_TYPE_HANDSHAKE:
            case RES_TYPE_STATUS: {
                out.writeBoolean(res != null);

                if (res != null)
                    ((Externalizable)res).writeExternal(out);

                break;
            }

            case RES_TYPE_COL_GGFS_FILE:
            case RES_TYPE_COL_GGFS_PATH:
            case RES_TYPE_COL_GGFS_BLOCK_LOCATION: {
                Collection<Externalizable> items = (Collection<Externalizable>)res;

                if (items != null) {
                    out.writeInt(items.size());

                    for (Externalizable item : items)
                        item.writeExternal(out);
                }
                else
                    out.writeInt(-1);

                break;
            }
        }
    }

    /**
     * Reads object from data input.
     *
     * @param in Data input.
     * @throws IOException If read failed.
     * @throws ClassNotFoundException If could not find class.
     */
    public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        byte[] hdr = new byte[RES_HEADER_SIZE];

        in.readFully(hdr);

        resType = U.bytesToInt(hdr, 0);

        boolean hasErr = hdr[4] != 0;

        if (hasErr) {
            err = in.readUTF();
            errCode = in.readInt();

            if (resType == RES_TYPE_ERR_STREAM_ID)
                res = in.readLong();

            return;
        }

        switch (resType) {
            case RES_TYPE_BOOLEAN:
                res = in.readBoolean();

                break;

            case RES_TYPE_LONG:
                res = in.readLong();

                break;

            case RES_TYPE_GGFS_PATH: {
                boolean hasVal = in.readBoolean();

                if (hasVal) {
                    GridGgfsPath path = new GridGgfsPath();

                    path.readExternal(in);

                    res = path;
                }

                break;
            }

            case RES_TYPE_GGFS_PATH_SUMMARY: {
                boolean hasVal = in.readBoolean();

                if (hasVal) {
                    GridGgfsPathSummary sum = new GridGgfsPathSummary();

                    sum.readExternal(in);

                    res = sum;
                }

                break;
            }

            case RES_TYPE_GGFS_FILE: {
                boolean hasVal = in.readBoolean();

                if (hasVal) {
                    GridGgfsFileImpl file = new GridGgfsFileImpl();

                    file.readExternal(in);

                    res = file;
                }

                break;
            }

            case RES_TYPE_GGFS_STREAM_DESCRIPTOR: {
                boolean hasVal = in.readBoolean();

                if (hasVal) {
                    GridGgfsInputStreamDescriptor desc = new GridGgfsInputStreamDescriptor();

                    desc.readExternal(in);

                    res = desc;
                }

                break;
            }

            case RES_TYPE_HANDSHAKE: {
                boolean hasVal = in.readBoolean();

                if (hasVal) {
                    GridGgfsHandshakeResponse msg = new GridGgfsHandshakeResponse();

                    msg.readExternal(in);

                    res = msg;
                }

                break;
            }

            case RES_TYPE_STATUS: {
                boolean hasVal = in.readBoolean();

                if (hasVal) {
                    GridGgfsStatus msg = new GridGgfsStatus();

                    msg.readExternal(in);

                    res = msg;
                }

                break;
            }

            case RES_TYPE_COL_GGFS_FILE: {
                Collection<GridGgfsFile> files = null;

                int size = in.readInt();

                if (size >= 0) {
                    files = new ArrayList<>(size);

                    for (int i = 0; i < size; i++) {
                        GridGgfsFileImpl file = new GridGgfsFileImpl();

                        file.readExternal(in);

                        files.add(file);
                    }
                }

                res = files;

                break;
            }

            case RES_TYPE_COL_GGFS_PATH: {
                Collection<GridGgfsPath> paths = null;

                int size = in.readInt();

                if (size >= 0) {
                    paths = new ArrayList<>(size);

                    for (int i = 0; i < size; i++) {
                        GridGgfsPath path = new GridGgfsPath();

                        path.readExternal(in);

                        paths.add(path);
                    }
                }

                res = paths;

                break;
            }

            case RES_TYPE_COL_GGFS_BLOCK_LOCATION: {
                Collection<GridGgfsBlockLocation> locations = null;

                int size = in.readInt();

                if (size >= 0) {
                    locations = new ArrayList<>(size);

                    for (int i = 0; i < size; i++) {
                        GridGgfsBlockLocationImpl location = new GridGgfsBlockLocationImpl();

                        location.readExternal(in);

                        locations.add(location);
                    }
                }

                res = locations;

                break;
            }

            case RES_TYPE_BYTE_ARRAY:
                assert false : "Response type of byte array should never be processed by marshaller.";
        }
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridGgfsControlResponse.class, this);
    }
}
