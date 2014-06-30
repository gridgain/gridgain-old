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

package org.gridgain.grid.cache.store.hibernate;

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.junits.cache.*;
import org.hibernate.*;

import java.io.*;
import java.net.*;

/**
 * Cache store test.
 */
public class GridCacheHibernateBlobStoreSelfTest extends
    GridAbstractCacheStoreSelfTest<GridCacheHibernateBlobStore<Object, Object>> {
    /**
     * @throws Exception If failed.
     */
    public GridCacheHibernateBlobStoreSelfTest() throws Exception {
        // No-op.
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        super.afterTest();

        Session s = store.session(null);

        if (s == null)
            return;

        try {
            s.createQuery("delete from " + GridCacheHibernateBlobStoreEntry.class.getSimpleName())
                    .setFlushMode(FlushMode.ALWAYS).executeUpdate();

            Transaction hTx = s.getTransaction();

            if (hTx != null && hTx.isActive())
                hTx.commit();
        }
        finally {
            s.close();
        }
    }

    /** {@inheritDoc} */
    @Override protected GridCacheHibernateBlobStore<Object, Object> store() {
        return new GridCacheHibernateBlobStore<>();
    }

    /**
     * @throws Exception If failed.
     */
    public void testConfigurationByUrl() throws Exception {
        URL url = U.resolveGridGainUrl(
            "modules/hibernate/src/test/java/org/gridgain/grid/cache/store/hibernate/hibernate.cfg.xml");

        assert url != null;

        store.setHibernateConfigurationPath(url.toString());

        // Store will be implicitly initialized.
        store.load(null, "key");
    }

    /**
     * @throws Exception If failed.
     */
    public void testConfigurationByFile() throws Exception {
        URL url = U.resolveGridGainUrl(
            "modules/hibernate/src/test/java/org/gridgain/grid/cache/store/hibernate/hibernate.cfg.xml");

        assert url != null;

        File file = new File(url.toURI());

        store.setHibernateConfigurationPath(file.getAbsolutePath());

        // Store will be implicitly initialized.
        store.load(null, "key");
    }

    /**
     * @throws Exception If failed.
     */
    public void testConfigurationByResource() throws Exception {
        store.setHibernateConfigurationPath("/org/gridgain/grid/cache/store/hibernate/hibernate.cfg.xml");

        // Store will be implicitly initialized.
        store.load(null, "key");
    }
}
