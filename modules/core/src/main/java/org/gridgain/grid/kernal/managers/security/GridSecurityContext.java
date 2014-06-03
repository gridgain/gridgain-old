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

import java.io.*;
import java.util.*;

/**
 * Security context.
 *
 * @author @java.author
 * @version @java.version
 */
public class GridSecurityContext implements Externalizable {
    /** */
    private static final long serialVersionUID = 0L;

    /** Security subject. */
    private GridSecuritySubject subj;

    /** String task permissions. */
    private Map<String, Collection<GridSecurityPermission>> strictTaskPermissions = new LinkedHashMap<>();

    /** String task permissions. */
    private Map<String, Collection<GridSecurityPermission>> wildcardTaskPermissions = new LinkedHashMap<>();

    /** String task permissions. */
    private Map<String, Collection<GridSecurityPermission>> strictCachePermissions = new LinkedHashMap<>();

    /** String task permissions. */
    private Map<String, Collection<GridSecurityPermission>> wildcardCachePermissions = new LinkedHashMap<>();

    /**
     * Empty constructor required by {@link Externalizable}.
     */
    public GridSecurityContext() {
        // No-op.
    }

    /**
     * @param subj Subject.
     */
    public GridSecurityContext(GridSecuritySubject subj) {
        this.subj = subj;

        initRules();
    }

    /**
     * @return Security subject.
     */
    public GridSecuritySubject subject() {
        return subj;
    }

    /**
     * Checks whether task operation is allowed.
     *
     * @param taskClsName Task class name.
     * @param perm Permission to check.
     * @return {@code True} if task operation is allowed.
     */
    public boolean taskOperationAllowed(String taskClsName, GridSecurityPermission perm) {
        assert perm == GridSecurityPermission.TASK_EXECUTE || perm == GridSecurityPermission.TASK_CANCEL;

        Collection<GridSecurityPermission> p = strictTaskPermissions.get(taskClsName);

        if (p != null)
            return p.contains(perm);

        for (Map.Entry<String, Collection<GridSecurityPermission>> entry : wildcardTaskPermissions.entrySet()) {
            if (taskClsName.startsWith(entry.getKey()))
                return entry.getValue().contains(perm);
        }

        return subj.permissions().defaultAllowAll();
    }

    /**
     * Checks whether cache operation is allowed.
     *
     * @param cacheName Cache name.
     * @param perm Permission to check.
     * @return {@code True} if cache operation is allowed.
     */
    public boolean cacheOperationAllowed(String cacheName, GridSecurityPermission perm) {
        assert perm == GridSecurityPermission.CACHE_PUT || perm == GridSecurityPermission.CACHE_READ ||
            perm == GridSecurityPermission.CACHE_REMOVE;

        Collection<GridSecurityPermission> p = strictCachePermissions.get(cacheName);

        if (p != null)
            return p.contains(perm);

        for (Map.Entry<String, Collection<GridSecurityPermission>> entry : wildcardCachePermissions.entrySet()) {
            if (cacheName != null) {
                if (cacheName.startsWith(entry.getKey()))
                    return entry.getValue().contains(perm);
            }
            else {
                // Match null cache to '*'
                if (entry.getKey().isEmpty())
                    return entry.getValue().contains(perm);
            }
        }

        return subj.permissions().defaultAllowAll();
    }

    /**
     * Init rules.
     */
    private void initRules() {
        GridSecurityPermissionSet permSet = subj.permissions();

        for (Map.Entry<String, Collection<GridSecurityPermission>> entry : permSet.taskPermissions().entrySet()) {
            String ptrn = entry.getKey();

            Collection<GridSecurityPermission> vals = Collections.unmodifiableCollection(entry.getValue());

            if (ptrn.endsWith("*")) {
                String noWildcard = ptrn.substring(0, ptrn.length() - 1);

                wildcardTaskPermissions.put(noWildcard, vals);
            }
            else
                strictTaskPermissions.put(ptrn, vals);
        }

        for (Map.Entry<String, Collection<GridSecurityPermission>> entry : permSet.cachePermissions().entrySet()) {
            String ptrn = entry.getKey();

            Collection<GridSecurityPermission> vals = Collections.unmodifiableCollection(entry.getValue());

            if (ptrn != null && ptrn.endsWith("*")) {
                String noWildcard = ptrn.substring(0, ptrn.length() - 1);

                wildcardCachePermissions.put(noWildcard, vals);
            }
            else
                strictCachePermissions.put(ptrn, vals);
        }
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        out.writeObject(subj);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        subj = (GridSecuritySubject)in.readObject();

        initRules();
    }
}
