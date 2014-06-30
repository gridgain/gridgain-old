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

package org.gridgain.grid.kernal.managers.security;

import org.gridgain.grid.security.*;
import org.gridgain.grid.util.tostring.*;
import org.gridgain.grid.util.typedef.internal.*;

import java.net.*;
import java.util.*;

/**
 * Authenticated security subject.
 */
public class GridSecuritySubjectAdapter implements GridSecuritySubject {
    /** */
    private static final long serialVersionUID = 0L;

    /** Subject ID. */
    private UUID id;

    /** Subject type. */
    private GridSecuritySubjectType subjType;

    /** Address. */
    private InetSocketAddress addr;

    /** Permissions assigned to a subject. */
    private GridSecurityPermissionSet permissions;

    /** Login. */
    @GridToStringInclude
    private Object login;

    /**
     * @param subjType Subject type.
     * @param id Subject ID.
     */
    public GridSecuritySubjectAdapter(GridSecuritySubjectType subjType, UUID id) {
        this.subjType = subjType;
        this.id = id;
    }

    /**
     * Gets subject ID.
     *
     * @return Subject ID.
     */
    @Override public UUID id() {
        return id;
    }

    /**
     * Gets subject type.
     *
     * @return Subject type.
     */
    @Override public GridSecuritySubjectType type() {
        return subjType;
    }

    /**
     * Gets subject address.
     *
     * @return Subject address.
     */
    @Override public InetSocketAddress address() {
        return addr;
    }

    /**
     * Sets subject address.
     *
     * @param addr Subject address.
     */
    public void address(InetSocketAddress addr) {
        this.addr = addr;
    }

    /**
     * Gets security permissions.
     *
     * @return Security permissions.
     */
    @Override public GridSecurityPermissionSet permissions() {
        return permissions;
    }

    /** {@inheritDoc} */
    @Override public Object login() {
        return login;
    }

    /**
     * Sets login provided by security credentials.
     *
     * @param login Login.
     */
    public void login(Object login) {
        this.login = login;
    }

    /**
     * Sets security permissions.
     *
     * @param permissions Permissions.
     */
    public void permissions(GridSecurityPermissionSet permissions) {
        this.permissions = permissions;
    }

    /** {@inheritDoc} */
    public String toString() {
        return S.toString(GridSecuritySubjectAdapter.class, this);
    }
}
