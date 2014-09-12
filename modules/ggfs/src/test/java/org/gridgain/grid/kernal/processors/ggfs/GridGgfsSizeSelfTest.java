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

package org.gridgain.grid.kernal.processors.ggfs;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.events.*;
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.jdk8.backport.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheDistributionMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.cache.GridCachePreloadMode.*;
import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.events.GridEventType.*;
import static org.gridgain.grid.kernal.processors.ggfs.GridGgfsFileInfo.*;

/**
 * {@link org.gridgain.grid.kernal.processors.ggfs.GridGgfsAttributes} test case.
 */
public class GridGgfsSizeSelfTest extends GridGgfsCommonAbstractTest {
    /** How many grids to start. */
    private static final int GRID_CNT = 3;

    /** How many files to save. */
    private static final int FILES_CNT = 10;

    /** Maximum amount of bytes that could be written to particular file. */
    private static final int MAX_FILE_SIZE = 1024 * 10;

    /** Block size. */
    private static final int BLOCK_SIZE = 384;

    /** Cache name. */
    private static final String DATA_CACHE_NAME = "dataCache";

    /** Cache name. */
    private static final String META_CACHE_NAME = "metaCache";

    /** GGFS name. */
    private static final String GGFS_NAME = "ggfs";

    /** IP finder. */
    private static final GridTcpDiscoveryIpFinder IP_FINDER = new GridTcpDiscoveryVmIpFinder(true);

    /** GGFS management port */
    private static int mgmtPort;

    /** Data cache mode. */
    private GridCacheMode cacheMode;

    /** Whether near cache is enabled (applicable for PARTITIONED cache only). */
    private boolean nearEnabled;

    /** GGFS maximum space. */
    private long ggfsMaxData;

    /** Trash purge timeout. */
    private long trashPurgeTimeout;

    /** {@inheritDoc} */
    @Override protected void beforeTest() throws Exception {
        cacheMode = null;
        nearEnabled = false;
        ggfsMaxData = 0;
        trashPurgeTimeout = 0;

        mgmtPort = 11400;
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        G.stopAll(true);
    }

    /** {@inheritDoc} */
    @Override protected GridConfiguration getConfiguration(String gridName) throws Exception {
        GridConfiguration cfg = super.getConfiguration(gridName);

        GridGgfsConfiguration ggfsCfg = new GridGgfsConfiguration();

        ggfsCfg.setDataCacheName(DATA_CACHE_NAME);
        ggfsCfg.setMetaCacheName(META_CACHE_NAME);
        ggfsCfg.setName(GGFS_NAME);
        ggfsCfg.setBlockSize(BLOCK_SIZE);
        ggfsCfg.setFragmentizerEnabled(false);
        ggfsCfg.setMaxSpaceSize(ggfsMaxData);
        ggfsCfg.setTrashPurgeTimeout(trashPurgeTimeout);
        ggfsCfg.setManagementPort(++mgmtPort);

        GridCacheConfiguration dataCfg = defaultCacheConfiguration();

        dataCfg.setName(DATA_CACHE_NAME);
        dataCfg.setCacheMode(cacheMode);

        if (cacheMode == PARTITIONED) {
            dataCfg.setDistributionMode(nearEnabled ? NEAR_PARTITIONED : PARTITIONED_ONLY);
            dataCfg.setBackups(0);
        }

        dataCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        dataCfg.setPreloadMode(SYNC);
        dataCfg.setAffinityMapper(new GridGgfsGroupDataBlocksKeyMapper(128));
        dataCfg.setQueryIndexEnabled(false);
        dataCfg.setAtomicityMode(TRANSACTIONAL);

        GridCacheConfiguration metaCfg = defaultCacheConfiguration();

        metaCfg.setName(META_CACHE_NAME);
        metaCfg.setCacheMode(REPLICATED);

        metaCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        metaCfg.setPreloadMode(SYNC);
        metaCfg.setQueryIndexEnabled(false);
        metaCfg.setAtomicityMode(TRANSACTIONAL);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(IP_FINDER);

        cfg.setDiscoverySpi(discoSpi);
        cfg.setCacheConfiguration(metaCfg, dataCfg);
        cfg.setGgfsConfiguration(ggfsCfg);

        return cfg;
    }

    /**
     * Perform initial startup.
     *
     * @throws Exception If failed.
     */
    private void startUp() throws Exception {
        startGrids(GRID_CNT);
    }

    /**
     * Ensure that PARTITIONED cache is correctly initialized.
     *
     * @throws Exception If failed.
     */
    public void testPartitioned() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = true;

        check();
    }

    /**
     * Ensure that co-located cache is correctly initialized.
     *
     * @throws Exception If failed.
     */
    public void testColocated() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = false;

        check();
    }

    /**
     * Ensure that REPLICATED cache is correctly initialized.
     *
     * @throws Exception If failed.
     */
    public void testReplicated() throws Exception {
        cacheMode = REPLICATED;

        check();
    }

    /**
     * Ensure that exception is thrown in case PARTITIONED cache is oversized.
     *
     * @throws Exception If failed.
     */
    public void testPartitionedOversize() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = true;

        checkOversize();
    }

    /**
     * Ensure that exception is thrown in case co-located cache is oversized.
     *
     * @throws Exception If failed.
     */
    public void testColocatedOversize() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = false;

        check();
    }

    /**
     * Ensure that exception is thrown in case REPLICATED cache is oversized.
     *
     * @throws Exception If failed.
     */
    public void testReplicatedOversize() throws Exception {
        cacheMode = REPLICATED;

        check();
    }

    /**
     * Ensure that exception is not thrown in case PARTITIONED cache is oversized, but data is deleted concurrently.
     *
     * @throws Exception If failed.
     */
    public void testPartitionedOversizeDelay() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = true;

        checkOversizeDelay();
    }

    /**
     * Ensure that exception is not thrown in case co-located cache is oversized, but data is deleted concurrently.
     *
     * @throws Exception If failed.
     */
    public void testColocatedOversizeDelay() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = false;

        checkOversizeDelay();
    }

    /**
     * Ensure that exception is not thrown in case REPLICATED cache is oversized, but data is deleted concurrently.
     *
     * @throws Exception If failed.
     */
    public void testReplicatedOversizeDelay() throws Exception {
        cacheMode = REPLICATED;

        checkOversizeDelay();
    }

    /**
     * Ensure that GGFS size is correctly updated in case of preloading for PARTITIONED cache.
     *
     * @throws Exception If failed.
     */
    public void testPartitionedPreload() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = true;

        checkPreload();
    }

    /**
     * Ensure that GGFS size is correctly updated in case of preloading for co-located cache.
     *
     * @throws Exception If failed.
     */
    public void testColocatedPreload() throws Exception {
        cacheMode = PARTITIONED;
        nearEnabled = false;

        checkPreload();
    }

    /**
     * Ensure that GGFS cache size is calculated correctly.
     *
     * @throws Exception If failed.
     */
    private void check() throws Exception {
        startUp();

        // Ensure that cache was marked as GGFS data cache.
        for (int i = 0; i < GRID_CNT; i++) {
            GridEx g = grid(i);

            GridCacheProjectionEx cache = (GridCacheProjectionEx)g.cachex(DATA_CACHE_NAME).cache();

            assert cache.isGgfsDataCache();
        }

        // Perform writes.
        Collection<GgfsFile> files = write();

        // Check sizes.
        Map<UUID, Integer> expSizes = new HashMap<>(GRID_CNT, 1.0f);

        for (GgfsFile file : files) {
            for (GgfsBlock block : file.blocks()) {
                Collection<UUID> ids = primaryOrBackups(block.key());

                for (UUID id : ids) {
                    if (expSizes.get(id) == null)
                        expSizes.put(id, block.length());
                    else
                        expSizes.put(id, expSizes.get(id) + block.length());
                }
            }
        }

        for (int i = 0; i < GRID_CNT; i++) {
            UUID id = grid(i).localNode().id();

            GridCacheAdapter<GridGgfsBlockKey, byte[]> cache = cache(id);

            int expSize = expSizes.get(id) != null ? expSizes.get(id) : 0;

            assert expSize == cache.ggfsDataSpaceUsed();
        }

        // Perform reads which could potentially be non-local.
        byte[] buf = new byte[BLOCK_SIZE];

        for (GgfsFile file : files) {
            for (int i = 0; i < GRID_CNT; i++) {
                int total = 0;

                GridGgfsInputStream is = ggfs(i).open(file.path());

                while (true) {
                    int read = is.read(buf);

                    if (read == -1)
                        break;
                    else
                        total += read;
                }

                assert total == file.length() : "Not enough bytes read: [expected=" + file.length() + ", actual=" +
                    total + ']';

                is.close();
            }
        }

        // Check sizes after read.
        if (cacheMode == PARTITIONED) {
            // No changes since the previous check for co-located cache.
            for (int i = 0; i < GRID_CNT; i++) {
                UUID id = grid(i).localNode().id();

                GridCacheAdapter<GridGgfsBlockKey, byte[]> cache = cache(id);

                int expSize = expSizes.get(id) != null ? expSizes.get(id) : 0;

                assert expSize == cache.ggfsDataSpaceUsed();
            }
        }
        else {
            // All data must exist on each cache.
            int totalSize = 0;

            for (GgfsFile file : files)
                totalSize += file.length();

            for (int i = 0; i < GRID_CNT; i++) {
                UUID id = grid(i).localNode().id();

                GridCacheAdapter<GridGgfsBlockKey, byte[]> cache = cache(id);

                assertEquals(totalSize, cache.ggfsDataSpaceUsed());
            }
        }

        // Delete data and ensure that all counters are 0 now.
        for (GgfsFile file : files) {
            ggfs(0).delete(file.path(), false);

            // Await for actual delete to occur.
            for (GgfsBlock block : file.blocks()) {
                for (int i = 0; i < GRID_CNT; i++) {
                    while (cache(grid(i).localNode().id()).peek(block.key()) != null)
                        U.sleep(100);
                }
            }
        }

        for (int i = 0; i < GRID_CNT; i++) {
            GridCacheAdapter<GridGgfsBlockKey, byte[]> cache = cache(grid(i).localNode().id());

            assert 0 == cache.ggfsDataSpaceUsed() : "Size counter is not 0: " + cache.ggfsDataSpaceUsed();
        }
    }

    /**
     * Ensure that an exception is thrown in case of GGFS oversize.
     *
     * @throws Exception If failed.
     */
    private void checkOversize() throws Exception {
        ggfsMaxData = BLOCK_SIZE;

        startUp();

        final GridGgfsPath path = new GridGgfsPath("/file");

        // This write is expected to be successful.
        GridGgfsOutputStream os = ggfs(0).create(path, false);
        os.write(chunk(BLOCK_SIZE - 1));
        os.close();

        // This write must be successful as well.
        os = ggfs(0).append(path, false);
        os.write(chunk(1));
        os.close();

        // This write must fail w/ exception.
        GridTestUtils.assertThrows(log(), new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsOutputStream osErr = ggfs(0).append(path, false);

                try {
                    osErr.write(chunk(BLOCK_SIZE));
                    osErr.close();

                    return null;
                }
                catch (IOException e) {
                    Throwable e0 = e;

                    while (e0.getCause() != null)
                        e0 = e0.getCause();

                    throw (Exception)e0;
                }
                finally {
                    U.closeQuiet(osErr);
                }
            }
        }, GridGgfsOutOfSpaceException.class, "Failed to write data block (GGFS maximum data size exceeded) [used=" +
            ggfsMaxData + ", allowed=" + ggfsMaxData + ']');
    }

    /**
     * Ensure that exception is not thrown or thrown with some delay when there is something in trash directory.
     *
     * @throws Exception If failed.
     */
    private void checkOversizeDelay() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);

        ggfsMaxData = 256;
        trashPurgeTimeout = 2000;

        startUp();

        GridGgfsImpl ggfs = ggfs(0);

        final GridGgfsPath path = new GridGgfsPath("/file");
        final GridGgfsPath otherPath = new GridGgfsPath("/fileOther");

        // Fill cache with data up to it's limit.
        GridGgfsOutputStream os = ggfs.create(path, false);
        os.write(chunk((int)ggfsMaxData));
        os.close();

        final GridCache<GridUuid, GridGgfsFileInfo> metaCache = ggfs.context().kernalContext().cache().cache(
            ggfs.configuration().getMetaCacheName());

        // Start a transaction in a separate thread which will lock file ID.
        final GridUuid id = ggfs.context().meta().fileId(path);
        final GridGgfsFileInfo info = ggfs.context().meta().info(id);

        final AtomicReference<Throwable> err = new AtomicReference<>();

        try {
            new Thread(new Runnable() {
                @Override public void run() {
                    try {

                        try (GridCacheTx tx = metaCache.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                            metaCache.get(id);

                            latch.await();

                            U.sleep(1000); // Sleep here so that data manager could "see" oversize.

                            tx.commit();
                        }
                    }
                    catch (Throwable e) {
                        err.set(e);
                    }
                }
            }).start();

            // Now add file ID to trash listing so that delete worker could "see" it.

            try (GridCacheTx tx = metaCache.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                Map<String, GridGgfsListingEntry> listing = Collections.singletonMap(path.name(),
                    new GridGgfsListingEntry(info));

                // Clear root listing.
                metaCache.put(ROOT_ID, new GridGgfsFileInfo(ROOT_ID));

                // Add file to trash listing.
                GridGgfsFileInfo trashInfo = metaCache.get(TRASH_ID);

                if (trashInfo == null)
                    metaCache.put(TRASH_ID, new GridGgfsFileInfo(listing, new GridGgfsFileInfo(TRASH_ID)));
                else
                    metaCache.put(TRASH_ID, new GridGgfsFileInfo(listing, trashInfo));

                tx.commit();
            }

            assert metaCache.get(TRASH_ID) != null;

            // Now the file is locked and is located in trash, try adding some more data.
            os = ggfs.create(otherPath, false);
            os.write(new byte[1]);

            latch.countDown();

            os.close();

            assert err.get() == null;
        }
        finally {
            latch.countDown(); // Safety.
        }
    }

    /**
     * Ensure that GGFS size is correctly updated in case of preloading.
     *
     * @throws Exception If failed.
     */
    private void checkPreload() throws Exception {
        assert cacheMode == PARTITIONED;

        startUp();

        // Perform writes.
        Collection<GgfsFile> files = write();

        // Check sizes.
        Map<UUID, Integer> expSizes = new HashMap<>(GRID_CNT, 1.0f);

        for (GgfsFile file : files) {
            for (GgfsBlock block : file.blocks()) {
                Collection<UUID> ids = primaryOrBackups(block.key());

                for (UUID id : ids) {
                    if (expSizes.get(id) == null)
                        expSizes.put(id, block.length());
                    else
                        expSizes.put(id, expSizes.get(id) + block.length());
                }
            }
        }

        info("Size map before node start: " + expSizes);

        for (int i = 0; i < GRID_CNT; i++) {
            UUID id = grid(i).localNode().id();

            GridCacheAdapter<GridGgfsBlockKey, byte[]> cache = cache(id);

            int expSize = expSizes.get(id) != null ? expSizes.get(id) : 0;

            assertEquals(expSize, cache.ggfsDataSpaceUsed());
        }

        // Start a node.
        final CountDownLatch latch = new CountDownLatch(GRID_CNT - 1);

        for (int i = 0; i < GRID_CNT - 1; i++) {
            grid(0).events().localListen(new GridPredicate<GridEvent>() {
                @Override public boolean apply(GridEvent evt) {
                    latch.countDown();

                    return true;
                }
            }, EVT_CACHE_PRELOAD_STOPPED);
        }

        Grid g = startGrid(GRID_CNT);

        info("Started grid: " + g.localNode().id());

        U.awaitQuiet(latch);

        // Wait partitions are evicted.
        awaitPartitionMapExchange();

        // Check sizes again.
        expSizes.clear();

        for (GgfsFile file : files) {
            for (GgfsBlock block : file.blocks()) {
                Collection<UUID> ids = primaryOrBackups(block.key());

                assert !ids.isEmpty();

                for (UUID id : ids) {
                    if (expSizes.get(id) == null)
                        expSizes.put(id, block.length());
                    else
                        expSizes.put(id, expSizes.get(id) + block.length());
                }
            }
        }

        info("Size map after node start: " + expSizes);

        for (int i = 0; i < GRID_CNT - 1; i++) {
            UUID id = grid(i).localNode().id();

            GridCacheAdapter<GridGgfsBlockKey, byte[]> cache = cache(id);

            int expSize = expSizes.get(id) != null ? expSizes.get(id) : 0;

            assertEquals("For node: " + id, expSize, cache.ggfsDataSpaceUsed());
        }
    }

    /**
     * Create data chunk of the given length.
     *
     * @param len Length.
     * @return Data chunk.
     */
    private byte[] chunk(int len) {
        byte[] chunk = new byte[len];

        for (int i = 0; i < len; i++)
            chunk[i] = (byte)i;

        return chunk;
    }

    /**
     * Create block key.
     *
     * @param path Path.
     * @param blockId Block ID.
     * @return Block key.
     * @throws Exception If failed.
     */
    private GridGgfsBlockKey blockKey(GridGgfsPath path, long blockId) throws Exception {
        GridGgfsEx ggfs0 = (GridGgfsEx)grid(0).ggfs(GGFS_NAME);

        GridUuid fileId = ggfs0.context().meta().fileId(path);

        return new GridGgfsBlockKey(fileId, null, true, blockId);
    }

    /**
     * Determine primary node for the given block key.
     *
     * @param key Block key.
     * @return Node ID.
     */
    private UUID primary(GridGgfsBlockKey key) {
        GridEx grid = grid(0);

        for (GridNode node : grid.nodes()) {
            if (grid.cachex(DATA_CACHE_NAME).affinity().isPrimary(node, key))
                return node.id();
        }

        return null;
    }

    /**
     * Determine primary and backup node IDs for the given block key.
     *
     * @param key Block key.
     * @return Collection of node IDs.
     */
    private Collection<UUID> primaryOrBackups(GridGgfsBlockKey key) {
        GridEx grid = grid(0);

        Collection<UUID> ids = new HashSet<>();

        for (GridNode node : grid.nodes()) {
            if (grid.cachex(DATA_CACHE_NAME).affinity().isPrimaryOrBackup(node, key))
                ids.add(node.id());
        }

        return ids;
    }

    /**
     * Get GGfs of a node with the given index.
     *
     * @param idx Node index.
     * @return GGFS.
     * @throws Exception If failed.
     */
    private GridGgfsImpl ggfs(int idx) throws Exception {
        return (GridGgfsImpl)grid(idx).ggfs(GGFS_NAME);
    }

    /**
     * Get GGfs of the given node.
     *
     * @param grid Node;
     * @return GGFS.
     * @throws Exception If failed.
     */
    private GridGgfsImpl ggfs(Grid grid) throws Exception {
        return (GridGgfsImpl)grid.ggfs(GGFS_NAME);
    }

    /**
     * Get data cache for the given node ID.
     *
     * @param nodeId Node ID.
     * @return Data cache.
     */
    private GridCacheAdapter<GridGgfsBlockKey, byte[]> cache(UUID nodeId) {
        return (GridCacheAdapter<GridGgfsBlockKey, byte[]>)((GridEx)G.grid(nodeId)).cachex(DATA_CACHE_NAME)
            .<GridGgfsBlockKey, byte[]>cache();
    }

    /**
     * Perform write of the files.
     *
     * @return Collection of written file descriptors.
     * @throws Exception If failed.
     */
    private Collection<GgfsFile> write() throws Exception {
        Collection<GgfsFile> res = new HashSet<>(FILES_CNT, 1.0f);

        ThreadLocalRandom8 rand = ThreadLocalRandom8.current();

        for (int i = 0; i < FILES_CNT; i++) {
            // Create empty file locally.
            GridGgfsPath path = new GridGgfsPath("/file-" + i);

            ggfs(0).create(path, false).close();

            GridGgfsMetaManager meta = ggfs(0).context().meta();

            GridUuid fileId = meta.fileId(path);

            // Calculate file blocks.
            int fileSize = rand.nextInt(MAX_FILE_SIZE);

            int fullBlocks = fileSize / BLOCK_SIZE;
            int remainderSize = fileSize % BLOCK_SIZE;

            Collection<GgfsBlock> blocks = new ArrayList<>(fullBlocks + remainderSize > 0 ? 1 : 0);

            for (int j = 0; j < fullBlocks; j++)
                blocks.add(new GgfsBlock(new GridGgfsBlockKey(fileId, null, true, j), BLOCK_SIZE));

            if (remainderSize > 0)
                blocks.add(new GgfsBlock(new GridGgfsBlockKey(fileId, null, true, fullBlocks), remainderSize));

            GgfsFile file = new GgfsFile(path, fileSize, blocks);

            // Actual write.
            for (GgfsBlock block : blocks) {
                GridGgfsOutputStream os = ggfs(0).append(path, false);

                os.write(chunk(block.length()));

                os.close();
            }

            // Add written file to the result set.
            res.add(file);
        }

        return res;
    }

    /** A file written to the file system. */
    private static class GgfsFile {
        /** Path to the file, */
        private final GridGgfsPath path;

        /** File length. */
        private final int len;

        /** Blocks with their corresponding locations. */
        private final Collection<GgfsBlock> blocks;

        /**
         * Constructor.
         *
         * @param path Path.
         * @param len Length.
         * @param blocks Blocks.
         */
        private GgfsFile(GridGgfsPath path, int len, Collection<GgfsBlock> blocks) {
            this.path = path;
            this.len = len;
            this.blocks = blocks;
        }

        /** @return Path. */
        GridGgfsPath path() {
            return path;
        }

        /** @return Length. */
        int length() {
            return len;
        }

        /** @return Blocks. */
        Collection<GgfsBlock> blocks() {
            return blocks;
        }
    }

    /** Block written to the file system. */
    private static class GgfsBlock {
        /** Block key. */
        private final GridGgfsBlockKey key;

        /** Block length. */
        private final int len;

        /**
         * Constructor.
         *
         * @param key Block key.
         * @param len Block length.
         */
        private GgfsBlock(GridGgfsBlockKey key, int len) {
            this.key = key;
            this.len = len;
        }

        /** @return Block key. */
        private GridGgfsBlockKey key() {
            return key;
        }

        /** @return Block length. */
        private int length() {
            return len;
        }
    }
}
