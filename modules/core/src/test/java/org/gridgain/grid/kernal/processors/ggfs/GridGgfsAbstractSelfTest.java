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
import org.gridgain.grid.ggfs.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.spi.discovery.tcp.*;
import org.gridgain.grid.spi.discovery.tcp.ipfinder.vm.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.testframework.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

import static org.gridgain.grid.cache.GridCacheAtomicityMode.*;
import static org.gridgain.grid.cache.GridCacheMemoryMode.*;
import static org.gridgain.grid.cache.GridCacheMode.*;
import static org.gridgain.grid.ggfs.GridGgfs.*;
import static org.gridgain.grid.ggfs.GridGgfsMode.*;

/**
 * Test fo regular GGFs operations.
 */
public abstract class GridGgfsAbstractSelfTest extends GridGgfsCommonAbstractTest {
    /** GGFS block size. */
    protected static final int GGFS_BLOCK_SIZE = 512 * 1024;

    /** Default block size (32Mb). */
    protected static final long BLOCK_SIZE = 32 * 1024 * 1024;

    /** Default repeat count. */
    protected static final int REPEAT_CNT = 5;

    /** Concurrent operations count. */
    protected static final int OPS_CNT = 16;

    /** Amount of blocks to prefetch. */
    protected static final int PREFETCH_BLOCKS = 1;

    /** Amount of sequential block reads before prefetch is triggered. */
    protected static final int SEQ_READS_BEFORE_PREFETCH = 2;

    /** Primary file system REST endpoint configuration string. */
    protected static final String PRIMARY_REST_CFG = "{type:'tcp', port:10500}";

    /** Secondary file system REST endpoint configuration string. */
    protected static final String SECONDARY_REST_CFG = "{type:'tcp', port:11500}";

    /** Directory. */
    protected static final GridGgfsPath DIR = new GridGgfsPath("/dir");

    /** Sub-directory. */
    protected static final GridGgfsPath SUBDIR = new GridGgfsPath(DIR, "subdir");

    /** Another sub-directory in the same directory. */
    protected static final GridGgfsPath SUBDIR2 = new GridGgfsPath(DIR, "subdir2");

    /** Sub-directory of the sub-directory. */
    protected static final GridGgfsPath SUBSUBDIR = new GridGgfsPath(SUBDIR, "subsubdir");

    /** File. */
    protected static final GridGgfsPath FILE = new GridGgfsPath(SUBDIR, "file");

    /** Another file in the same directory. */
    protected static final GridGgfsPath FILE2 = new GridGgfsPath(SUBDIR, "file2");

    /** Other directory. */
    protected static final GridGgfsPath DIR_NEW = new GridGgfsPath("/dirNew");

    /** Other subdirectory. */
    protected static final GridGgfsPath SUBDIR_NEW = new GridGgfsPath(DIR_NEW, "subdirNew");

    /** Other sub-directory of the sub-directory. */
    protected static final GridGgfsPath SUBSUBDIR_NEW = new GridGgfsPath(SUBDIR_NEW, "subsubdirNew");

    /** Other file. */
    protected static final GridGgfsPath FILE_NEW = new GridGgfsPath(SUBDIR_NEW, "fileNew");

    /** Default data chunk (128 bytes). */
    protected static byte[] chunk;

    /** Primary GGFS. */
    protected static GridGgfsImpl ggfs;

    /** Secondary GGFS. */
    protected static GridGgfsImpl ggfsSecondary;

    /** GGFS mode. */
    protected final GridGgfsMode mode;

    /** Dual mode flag. */
    protected final boolean dual;

    /** Memory mode. */
    protected final GridCacheMemoryMode memoryMode;

    /**
     * Constructor.
     *
     * @param mode GGFS mode.
     */
    protected GridGgfsAbstractSelfTest(GridGgfsMode mode) {
        this(mode, ONHEAP_TIERED);
    }

    protected GridGgfsAbstractSelfTest(GridGgfsMode mode, GridCacheMemoryMode memoryMode) {
        assert mode != null && mode != PROXY;

        this.mode = mode;
        this.memoryMode = memoryMode;

        dual = mode != PRIMARY;
    }

    /** {@inheritDoc} */
    @Override protected void beforeTestsStarted() throws Exception {
        chunk = new byte[128];

        for (int i = 0; i < chunk.length; i++)
            chunk[i] = (byte)i;

        Grid gridSecondary = startGridWithGgfs("grid-secondary", "ggfs-secondary", PRIMARY, null, SECONDARY_REST_CFG);

        ggfsSecondary = (GridGgfsImpl)gridSecondary.ggfs("ggfs-secondary");

        Grid grid = startGridWithGgfs("grid", "ggfs", mode, ggfsSecondary, PRIMARY_REST_CFG);

        ggfs = (GridGgfsImpl)grid.ggfs("ggfs");
    }

    /** {@inheritDoc} */
    @Override protected void afterTest() throws Exception {
        clear(ggfs, ggfsSecondary);
    }

    /** {@inheritDoc} */
    @Override protected void afterTestsStopped() throws Exception {
        G.stopAll(true);
    }

    /**
     * Start grid with GGFS.
     *
     * @param gridName Grid name.
     * @param ggfsName GGFS name
     * @param mode GGFS mode.
     * @param secondaryFs Secondary file system (optional).
     * @param restCfg Rest configuration string (optional).
     * @return Started grid instance.
     * @throws Exception If failed.
     */
    protected Grid startGridWithGgfs(String gridName, String ggfsName, GridGgfsMode mode,
        @Nullable GridGgfsFileSystem secondaryFs, @Nullable String restCfg) throws Exception {
        GridGgfsConfiguration ggfsCfg = new GridGgfsConfiguration();

        ggfsCfg.setDataCacheName("dataCache");
        ggfsCfg.setMetaCacheName("metaCache");
        ggfsCfg.setName(ggfsName);
        ggfsCfg.setBlockSize(GGFS_BLOCK_SIZE);
        ggfsCfg.setDefaultMode(mode);
        ggfsCfg.setIpcEndpointConfiguration(GridGgfsTestUtils.jsonToMap(restCfg));
        ggfsCfg.setSecondaryFileSystem(secondaryFs);
        ggfsCfg.setPrefetchBlocks(PREFETCH_BLOCKS);
        ggfsCfg.setSequentialReadsBeforePrefetch(SEQ_READS_BEFORE_PREFETCH);

        GridCacheConfiguration dataCacheCfg = defaultCacheConfiguration();

        dataCacheCfg.setName("dataCache");
        dataCacheCfg.setCacheMode(PARTITIONED);
        dataCacheCfg.setDistributionMode(GridCacheDistributionMode.PARTITIONED_ONLY);
        dataCacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        dataCacheCfg.setAffinityMapper(new GridGgfsGroupDataBlocksKeyMapper(2));
        dataCacheCfg.setBackups(0);
        dataCacheCfg.setQueryIndexEnabled(false);
        dataCacheCfg.setAtomicityMode(TRANSACTIONAL);
        dataCacheCfg.setMemoryMode(memoryMode);
        dataCacheCfg.setOffHeapMaxMemory(0);

        GridCacheConfiguration metaCacheCfg = defaultCacheConfiguration();

        metaCacheCfg.setName("metaCache");
        metaCacheCfg.setCacheMode(REPLICATED);
        metaCacheCfg.setWriteSynchronizationMode(GridCacheWriteSynchronizationMode.FULL_SYNC);
        metaCacheCfg.setQueryIndexEnabled(false);
        metaCacheCfg.setAtomicityMode(TRANSACTIONAL);

        GridConfiguration cfg = new GridConfiguration();

        cfg.setGridName(gridName);

        GridTcpDiscoverySpi discoSpi = new GridTcpDiscoverySpi();

        discoSpi.setIpFinder(new GridTcpDiscoveryVmIpFinder(true));

        cfg.setDiscoverySpi(discoSpi);
        cfg.setCacheConfiguration(dataCacheCfg, metaCacheCfg);
        cfg.setGgfsConfiguration(ggfsCfg);

        cfg.setLocalHost("127.0.0.1");
        cfg.setRestEnabled(false);

        return G.start(cfg);
    }

    /**
     * Execute provided task in a separate thread.
     *
     * @param task Task to execute.
     * @return Result.
     */
    protected static <T> GridPlainFuture<T> execute(final Callable<T> task) {
        final GridPlainFutureAdapter<T> fut = new GridPlainFutureAdapter<>();

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    fut.onDone(task.call());
                }
                catch (Throwable e) {
                    fut.onDone(e);
                }
            }
        }).start();

        return fut;
    }

    /**
     * Test existence check when the path exists both locally and remotely.
     *
     * @throws Exception If failed.
     */
    public void testExists() throws Exception {
        create(ggfs, paths(DIR), null);

        checkExist(ggfs, ggfsSecondary, DIR);
    }

    /**
     * Test existence check when the path doesn't exist remotely.
     *
     * @throws Exception If failed.
     */
    public void testExistsPathDoesNotExist() throws Exception {
        assert !ggfs.exists(DIR);
    }

    /**
     * Test list files routine.
     *
     * @throws Exception If failed.
     */
    public void testListFiles() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR), paths(FILE));

        Collection<GridGgfsFile> paths = ggfs.listFiles(SUBDIR);

        assert paths != null;
        assert paths.size() == 2;

        Iterator<GridGgfsFile> iter = paths.iterator();

        GridGgfsFile path1 = iter.next();
        GridGgfsFile path2 = iter.next();

        assert (SUBSUBDIR.equals(path1.path()) && FILE.equals(path2.path())) ||
            (FILE.equals(path1.path()) && SUBSUBDIR.equals(path2.path()));
    }

    /**
     * Test list files routine when the path doesn't exist remotely.
     *
     * @throws Exception If failed.
     */
    public void testListFilesPathDoesNotExist() throws Exception {
        Collection<GridGgfsFile> paths = null;

        try {
            paths = ggfs.listFiles(SUBDIR);
        }
        catch (GridException ignore) {
            // No-op.
        }

        assert paths == null || paths.isEmpty();
    }

    /**
     * Test info routine when the path exists both locally and remotely.
     *
     * @throws Exception If failed.
     */
    public void testInfo() throws Exception {
        create(ggfs, paths(DIR), null);

        GridGgfsFile info = ggfs.info(DIR);

        assert info != null;

        assertEquals(DIR, info.path());
    }

    /**
     * Test info routine when the path doesn't exist remotely.
     *
     * @throws Exception If failed.
     */
    public void testInfoPathDoesNotExist() throws Exception {
        GridGgfsFile info = null;

        try {
            info = ggfs.info(DIR);
        }
        catch (GridException ignore) {
            // No-op.
        }

        assert info == null;
    }

    /**
     * Test rename in case both local and remote file systems have the same folder structure and the path being renamed
     * is a file.
     *
     * @throws Exception If failed.
     */
    public void testRenameFile() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), paths(FILE));

        ggfs.rename(FILE, FILE2);

        checkExist(ggfs, ggfsSecondary, FILE2);
        checkNotExist(ggfs, ggfsSecondary, FILE);
    }

    /**
     * Test file rename when parent folder is the root.
     *
     * @throws Exception If failed.
     */
    public void testRenameFileParentRoot() throws Exception {
        GridGgfsPath file1 = new GridGgfsPath("/file1");
        GridGgfsPath file2 = new GridGgfsPath("/file2");

        create(ggfs, null, paths(file1));

        ggfs.rename(file1, file2);

        checkExist(ggfs, ggfsSecondary, file2);
        checkNotExist(ggfs, ggfsSecondary, file1);
    }

    /**
     * Test rename in case both local and remote file systems have the same folder structure and the path being renamed
     * is a directory.
     *
     * @throws Exception If failed.
     */
    public void testRenameDirectory() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        ggfs.rename(SUBDIR, SUBDIR2);

        checkExist(ggfs, ggfsSecondary, SUBDIR2);
        checkNotExist(ggfs, ggfsSecondary, SUBDIR);
    }

    /**
     * Test directory rename when parent folder is the root.
     *
     * @throws Exception If failed.
     */
    public void testRenameDirectoryParentRoot() throws Exception {
        GridGgfsPath dir1 = new GridGgfsPath("/dir1");
        GridGgfsPath dir2 = new GridGgfsPath("/dir2");

        create(ggfs, paths(dir1), null);

        ggfs.rename(dir1, dir2);

        checkExist(ggfs, ggfsSecondary, dir2);
        checkNotExist(ggfs, ggfsSecondary, dir1);
    }

    /**
     * Test move in case both local and remote file systems have the same folder structure and the path being renamed is
     * a file.
     *
     * @throws Exception If failed.
     */
    public void testMoveFile() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, DIR_NEW, SUBDIR_NEW), paths(FILE));

        ggfs.rename(FILE, SUBDIR_NEW);

        checkExist(ggfs, ggfsSecondary, new GridGgfsPath(SUBDIR_NEW, FILE.name()));
        checkNotExist(ggfs, ggfsSecondary, FILE);
    }

    /**
     * Test file move when destination is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveFileDestinationRoot() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), paths(FILE));

        ggfs.rename(FILE, new GridGgfsPath());

        checkExist(ggfs, ggfsSecondary, new GridGgfsPath("/" + FILE.name()));
        checkNotExist(ggfs, ggfsSecondary, FILE);
    }

    /**
     * Test file move when source parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveFileSourceParentRoot() throws Exception {
        GridGgfsPath file = new GridGgfsPath("/" + FILE.name());

        create(ggfs, paths(DIR_NEW, SUBDIR_NEW), paths(file));

        ggfs.rename(file, SUBDIR_NEW);

        checkExist(ggfs, ggfsSecondary, new GridGgfsPath(SUBDIR_NEW, FILE.name()));
        checkNotExist(ggfs, ggfsSecondary, file);
    }

    /**
     * Test move and rename in case both local and remote file systems have the same folder structure and the path being
     * renamed is a file.
     *
     * @throws Exception If failed.
     */
    public void testMoveRenameFile() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, DIR_NEW, SUBDIR_NEW), paths(FILE));

        ggfs.rename(FILE, FILE_NEW);

        checkExist(ggfs, ggfsSecondary, FILE_NEW);
        checkNotExist(ggfs, ggfsSecondary, FILE);
    }

    /**
     * Test file move and rename when destination is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveRenameFileDestinationRoot() throws Exception {
        GridGgfsPath file = new GridGgfsPath("/" + FILE.name());

        create(ggfs, paths(DIR, SUBDIR), paths(FILE));

        ggfs.rename(FILE, file);

        checkExist(ggfs, ggfsSecondary, file);
        checkNotExist(ggfs, ggfsSecondary, FILE);
    }

    /**
     * Test file move and rename when source parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveRenameFileSourceParentRoot() throws Exception {
        GridGgfsPath file = new GridGgfsPath("/" + FILE_NEW.name());

        create(ggfs, paths(DIR_NEW, SUBDIR_NEW), paths(file));

        ggfs.rename(file, FILE_NEW);

        checkExist(ggfs, ggfsSecondary, FILE_NEW);
        checkNotExist(ggfs, ggfsSecondary, file);
    }

    /**
     * Test move in case both local and remote file systems have the same folder structure and the path being renamed is
     * a directory.
     *
     * @throws Exception If failed.
     */
    public void testMoveDirectory() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR, DIR_NEW, SUBDIR_NEW), null);

        ggfs.rename(SUBSUBDIR, SUBDIR_NEW);

        checkExist(ggfs, ggfsSecondary, new GridGgfsPath(SUBDIR_NEW, SUBSUBDIR.name()));
        checkNotExist(ggfs, ggfsSecondary, SUBSUBDIR);
    }

    /**
     * Test directory move when destination is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveDirectoryDestinationRoot() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR), null);

        ggfs.rename(SUBSUBDIR, new GridGgfsPath());

        checkExist(ggfs, ggfsSecondary, new GridGgfsPath("/" + SUBSUBDIR.name()));
        checkNotExist(ggfs, ggfsSecondary, SUBSUBDIR);
    }

    /**
     * Test directory move when source parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveDirectorySourceParentRoot() throws Exception {
        GridGgfsPath dir = new GridGgfsPath("/" + SUBSUBDIR.name());

        create(ggfs, paths(DIR_NEW, SUBDIR_NEW, dir), null);

        ggfs.rename(dir, SUBDIR_NEW);

        checkExist(ggfs, ggfsSecondary, new GridGgfsPath(SUBDIR_NEW, SUBSUBDIR.name()));
        checkNotExist(ggfs, ggfsSecondary, dir);
    }

    /**
     * Test move and rename  in case both local and remote file systems have the same folder structure and the path
     * being renamed is a directory.
     *
     * @throws Exception If failed.
     */
    public void testMoveRenameDirectory() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR, DIR_NEW, SUBDIR_NEW), null);

        ggfs.rename(SUBSUBDIR, SUBSUBDIR_NEW);

        checkExist(ggfs, ggfsSecondary, SUBSUBDIR_NEW);
        checkNotExist(ggfs, ggfsSecondary, SUBSUBDIR);
    }

    /**
     * Test directory move and rename when destination is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveRenameDirectoryDestinationRoot() throws Exception {
        GridGgfsPath dir = new GridGgfsPath("/" + SUBSUBDIR.name());

        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR), null);

        ggfs.rename(SUBSUBDIR, dir);

        checkExist(ggfs, ggfsSecondary, dir);
        checkNotExist(ggfs, ggfsSecondary, SUBSUBDIR);
    }

    /**
     * Test directory move and rename when source parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testMoveRenameDirectorySourceParentRoot() throws Exception {
        GridGgfsPath dir = new GridGgfsPath("/" + SUBSUBDIR_NEW.name());

        create(ggfs, paths(DIR_NEW, SUBDIR_NEW, dir), null);

        ggfs.rename(dir, SUBSUBDIR_NEW);

        checkExist(ggfs, ggfsSecondary, SUBSUBDIR_NEW);
        checkNotExist(ggfs, ggfsSecondary, dir);
    }

    /**
     * Ensure that rename doesn't occur in case source doesn't exist remotely.
     *
     * @throws Exception If failed.
     */
    public void testMoveRenameSourceDoesNotExist() throws Exception {
        create(ggfs, paths(DIR, DIR_NEW), null);

        GridTestUtils.assertThrowsInherited(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                ggfs.rename(SUBDIR, SUBDIR_NEW);

                return null;
            }
        }, GridGgfsException.class, null);

        checkNotExist(ggfs, ggfsSecondary, SUBDIR, SUBDIR_NEW);
    }

    /**
     * Test mkdirs in case both local and remote file systems have the same folder structure.
     *
     * @throws Exception If failed.
     */
    public void testMkdirs() throws Exception {
        Map<String, String> props = properties(null, null, "0555"); // mkdirs command doesn't propagate user info.

        create(ggfs, paths(DIR, SUBDIR), null);

        ggfs.mkdirs(SUBSUBDIR, props);

        // Ensure that directory was created and properties are propagated.
        checkExist(ggfs, ggfsSecondary, SUBSUBDIR);

        if (dual)
            assertEquals(props, ggfsSecondary.info(SUBSUBDIR).properties());

        // We check only permission because GGFS client adds username and group name explicitly.
        assertEquals(props.get(PROP_PERMISSION), ggfs.info(SUBSUBDIR).properties().get(PROP_PERMISSION));
    }

    /**
     * Test mkdirs in case parent is the root directory.
     *
     * @throws Exception If failed.
     */
    public void testMkdirsParentRoot() throws Exception {
        Map<String, String> props = properties(null, null, "0555"); // mkdirs command doesn't propagate user info.

        ggfs.mkdirs(DIR, props);

        checkExist(ggfs, ggfsSecondary, DIR);

        if (dual)
            assertEquals(props, ggfsSecondary.info(DIR).properties());

        // We check only permission because GGFS client adds username and group name explicitly.
        assertEquals(props.get(PROP_PERMISSION), ggfs.info(DIR).properties().get(PROP_PERMISSION));
    }

    /**
     * Test delete in case both local and remote file systems have the same folder structure.
     *
     * @throws Exception If failed.
     */
    public void testDelete() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR), paths(FILE));

        ggfs.delete(SUBDIR, true);

        checkNotExist(ggfs, ggfsSecondary, SUBDIR, SUBSUBDIR, FILE);
    }

    /**
     * Test delete when the path parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testDeleteParentRoot() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR), paths(FILE));

        ggfs.delete(DIR, true);

        checkNotExist(ggfs, ggfsSecondary, DIR, SUBDIR, SUBSUBDIR, FILE);
    }

    /**
     * Ensure that delete will not be successful in non-empty directory when recursive flag is set to {@code false}.
     *
     * @throws Exception If failed.
     */
    public void testDeleteDirectoryNotEmpty() throws Exception {
        create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR), paths(FILE));

        // We have different results for dual and non-dual modes.
        if (dual)
            GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    ggfs.delete(SUBDIR, false);

                    return null;
                }
            }, GridException.class, "Failed to delete the path due to secondary file system exception:");
        else {
            GridTestUtils.assertThrows(log, new Callable<Object>() {
                @Override public Object call() throws Exception {
                    ggfs.delete(SUBDIR, false);

                    return null;
                }
            }, GridGgfsDirectoryNotEmptyException.class, "Failed to remove directory (directory is not empty and " +
                   "recursive flag is not set)");
        }

        checkExist(ggfs, ggfsSecondary, SUBDIR, SUBSUBDIR, FILE);
    }

    /**
     * Test update in case both local and remote file systems have the same folder structure.
     *
     * @throws Exception If failed.
     */
    public void testUpdate() throws Exception {
        Map<String, String> props = properties("owner", "group", "0555");

        create(ggfs, paths(DIR, SUBDIR), paths(FILE));

        ggfs.update(FILE, props);

        if (dual)
            assertEquals(props, ggfsSecondary.info(FILE).properties());

        assertEquals(props, ggfs.info(FILE).properties());
    }

    /**
     * Test update when parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testUpdateParentRoot() throws Exception {
        Map<String, String> props = properties("owner", "group", "0555");

        create(ggfs, paths(DIR), null);

        ggfs.update(DIR, props);

        if (dual)
            assertEquals(props, ggfsSecondary.info(DIR).properties());

        assertEquals(props, ggfs.info(DIR).properties());
    }

    /**
     * Check that exception is thrown in case the path being updated doesn't exist remotely.
     *
     * @throws Exception If failed.
     */
    public void testUpdatePathDoesNotExist() throws Exception {
        final Map<String, String> props = properties("owner", "group", "0555");

        assert ggfs.update(SUBDIR, props) == null;

        checkNotExist(ggfs, ggfsSecondary, SUBDIR);
    }

    /**
     * Ensure that formatting is not propagated to the secondary file system.
     *
     * @throws Exception If failed.
     */
    // TODO Enable after GG-8578.
    @SuppressWarnings("ConstantConditions")
    public void _testFormat() throws Exception {
        GridKernal grid = (GridKernal)G.grid("grid");
        GridCache cache = grid.internalCache("dataCache");

        if (dual)
            create(ggfsSecondary, paths(DIR, SUBDIR, DIR_NEW, SUBDIR_NEW), paths(FILE, FILE_NEW));

        create(ggfs, paths(DIR, SUBDIR), paths(FILE));

        try (GridGgfsOutputStream os = ggfs.append(FILE, false)) {
            os.write(new byte[10 * 1024 * 1024]);
        }

        if (dual)
            checkExist(ggfsSecondary, DIR, SUBDIR, FILE, DIR_NEW, SUBDIR_NEW, FILE_NEW);

        checkExist(ggfs, DIR, SUBDIR, FILE);

        assert ggfs.info(FILE).length() == 10 * 1024 * 1024;

        int size = cache.size();
        int primarySize = cache.primarySize();
        int primaryKeySetSize = cache.primaryKeySet().size();

        int primaryPartSize = 0;

        for (int p : cache.affinity().primaryPartitions(grid.localNode())) {
            Set set = cache.entrySet(p);

            if (set != null)
                primaryPartSize += set.size();
        }

        assert size > 0;
        assert primarySize > 0;
        assert primarySize == primaryKeySetSize;
        assert primarySize == primaryPartSize;

        ggfs.format().get();

        // Ensure format is not propagated to the secondary file system.
        if (dual) {
            checkExist(ggfsSecondary, DIR, SUBDIR, FILE, DIR_NEW, SUBDIR_NEW, FILE_NEW);

            ggfsSecondary.format().get();
        }

        // Ensure entries deletion in the primary file system.
        checkNotExist(ggfs, DIR, SUBDIR, FILE);

        int sizeNew = cache.size();
        int primarySizeNew = cache.primarySize();
        int primaryKeySetSizeNew = cache.primaryKeySet().size();

        int primaryPartSizeNew = 0;

        for (int p : cache.affinity().primaryPartitions(grid.localNode())) {
            Set set = cache.entrySet(p);

            if (set != null) {
                for (Object entry : set)
                    System.out.println(entry);

                primaryPartSizeNew += set.size();
            }
        }

        assert sizeNew == 0;
        assert primarySizeNew == 0;
        assert primaryKeySetSizeNew == 0;
        assert primaryPartSizeNew == 0;
    }

    /**
     * Test regular file open.
     *
     * @throws Exception If failed.
     */
    public void testOpen() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, true, chunk);

        checkFileContent(ggfs, FILE, chunk);
    }

    /**
     * Test file open in case it doesn't exist both locally and remotely.
     *
     * @throws Exception If failed.
     */
    public void testOpenDoesNotExist() throws Exception {
        ggfsSecondary.delete(FILE, false);

        GridTestUtils.assertThrows(log(), new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsInputStream is = null;

                try {
                    is = ggfs.open(FILE);
                }
                finally {
                    U.closeQuiet(is);
                }

                return null;
            }
        }, GridGgfsFileNotFoundException.class, "File not found: " + FILE);
    }

    /**
     * Test regular create.
     *
     * @throws Exception If failed.
     */
    public void testCreate() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, true, chunk);

        checkFile(ggfs, ggfsSecondary, FILE, chunk);
    }

    /**
     * Test create when parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testCreateParentRoot() throws Exception {
        GridGgfsPath file = new GridGgfsPath("/" + FILE.name());

        createFile(ggfs, file, true, chunk);

        checkFile(ggfs, ggfsSecondary, file, chunk);
    }

    /**
     * Test subsequent "create" commands on the same file without closing the output streams.
     *
     * @throws Exception If failed.
     */
    public void testCreateNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        GridTestUtils.assertThrows(log(), new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsOutputStream os1 = null;
                GridGgfsOutputStream os2 = null;

                try {
                    os1 = ggfs.create(FILE, true);
                    os2 = ggfs.create(FILE, true);
                }
                finally {
                    U.closeQuiet(os1);
                    U.closeQuiet(os2);
                }

                return null;
            }
        }, GridGgfsException.class, null);
    }

    /**
     * Test rename on the file when it was opened for write(create) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testCreateRenameNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        GridGgfsOutputStream os = null;

        try {
            os = ggfs.create(FILE, true);

            ggfs.rename(FILE, FILE2);

            os.close();
        }
        finally {
            U.closeQuiet(os);
        }
    }

    /**
     * Test rename on the file parent when it was opened for write(create) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testCreateRenameParentNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        GridGgfsOutputStream os = null;

        try {
            os = ggfs.create(FILE, true);

            ggfs.rename(SUBDIR, SUBDIR2);

            os.close();
        }
        finally {
            U.closeQuiet(os);
        }
    }

    /**
     * Test delete on the file when it was opened for write(create) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testCreateDeleteNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsOutputStream os = null;

                try {
                    os = ggfs.create(FILE, true);

                    ggfs.format().get();

                    os.write(chunk);

                    os.close();
                }
                finally {
                    U.closeQuiet(os);
                }

                return null;
            }
        }, IOException.class, "File was concurrently deleted: " + FILE);
    }

    /**
     * Test delete on the file parent when it was opened for write(create) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testCreateDeleteParentNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsOutputStream os = null;

                try {
                    os = ggfs.create(FILE, true);

                    GridUuid id = ggfs.context().meta().fileId(FILE);

                    ggfs.delete(SUBDIR, true); // Since GG-4911 we allow deletes in this case.

                    while (ggfs.context().meta().exists(id))
                        U.sleep(100);

                    os.close();
                }
                finally {
                    U.closeQuiet(os);
                }

                return null;
            }
        }, IOException.class, "File was concurrently deleted: " + FILE);
    }

    /**
     * Test update on the file when it was opened for write(create) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testCreateUpdateNoClose() throws Exception {
        Map<String, String> props = properties("owner", "group", "0555");

        create(ggfs, paths(DIR, SUBDIR), null);

        GridGgfsOutputStream os = null;

        try {
            os = ggfs.create(FILE, true);

            ggfs.update(FILE, props);

            os.close();
        }
        finally {
            U.closeQuiet(os);
        }
    }

    /**
     * Ensure consistency of data during file creation.
     *
     * @throws Exception If failed.
     */
    public void testCreateConsistency() throws Exception {
        final AtomicInteger ctr = new AtomicInteger();
        final AtomicReference<Exception> err = new AtomicReference<>();

        int threadCnt = 10;

        multithreaded(new Runnable() {
            @Override public void run() {
                int idx = ctr.incrementAndGet();

                GridGgfsPath path = new GridGgfsPath("/file" + idx);

                try {
                    for (int i = 0; i < REPEAT_CNT; i++) {
                        GridGgfsOutputStream os = ggfs.create(path, 128, true, null, 0, 256, null);

                        os.write(chunk);

                        os.close();

                        assert ggfs.exists(path);
                    }

                    awaitFileClose(ggfs, path);

                    checkFileContent(ggfs, path, chunk);
                }
                catch (IOException | GridException e) {
                    err.compareAndSet(null, e); // Log the very first error.
                }
            }
        }, threadCnt);

        if (err.get() != null)
            throw err.get();
    }

    /**
     * Ensure create consistency when multiple threads writes to the same file.
     *
     * @throws Exception If failed.
     */
    public void testCreateConsistencyMultithreaded() throws Exception {
        final AtomicBoolean stop = new AtomicBoolean();

        final AtomicInteger createCtr = new AtomicInteger(); // How many times the file was re-created.
        final AtomicReference<Exception> err = new AtomicReference<>();

        ggfs.create(FILE, false).close();

        int threadCnt = 5;

        GridFuture<?> fut = multithreadedAsync(new Runnable() {
            @Override public void run() {
                while (!stop.get()) {
                    GridGgfsOutputStream os = null;

                    try {
                        os = ggfs.create(FILE, true);

                        os.write(chunk);

                        U.sleep(50);

                        os.close();

                        createCtr.incrementAndGet();
                    }
                    catch (GridException ignore) {
                        try {
                            U.sleep(10);
                        }
                        catch (GridInterruptedException ignored) {
                            // nO-op.
                        }
                    }
                    catch (IOException e) {
                        // We can ignore concurrent deletion exception since we override the file.
                        if (!e.getMessage().startsWith("File was concurrently deleted"))
                            err.compareAndSet(null, e);
                    }
                    finally {
                        U.closeQuiet(os);
                    }
                }
            }
        }, threadCnt);

        long startTime = U.currentTimeMillis();

        while (createCtr.get() < 50 && U.currentTimeMillis() - startTime < 60 * 1000)
            U.sleep(100);

        stop.set(true);

        fut.get();

        awaitFileClose(ggfs, FILE);

        if (err.get() != null)
            throw err.get();

        checkFileContent(ggfs, FILE, chunk);
    }

    /**
     * Test regular append.
     *
     * @throws Exception If failed.
     */
    public void testAppend() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, true, BLOCK_SIZE, chunk);

        appendFile(ggfs, FILE, chunk);

        checkFile(ggfs, ggfsSecondary, FILE, chunk, chunk);
    }

    /**
     * Test create when parent is the root.
     *
     * @throws Exception If failed.
     */
    public void testAppendParentRoot() throws Exception {
        GridGgfsPath file = new GridGgfsPath("/" + FILE.name());

        createFile(ggfs, file, true, BLOCK_SIZE, chunk);

        appendFile(ggfs, file, chunk);

        checkFile(ggfs, ggfsSecondary, file, chunk, chunk);
    }

    /**
     * Test subsequent "append" commands on the same file without closing the output streams.
     *
     * @throws Exception If failed.
     */
    public void testAppendNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, false);

        GridTestUtils.assertThrowsInherited(log(), new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsOutputStream os1 = null;
                GridGgfsOutputStream os2 = null;

                try {
                    os1 = ggfs.append(FILE, false);
                    os2 = ggfs.append(FILE, false);
                }
                finally {
                    U.closeQuiet(os1);
                    U.closeQuiet(os2);
                }

                return null;
            }
        }, GridException.class, null);
    }

    /**
     * Test rename on the file when it was opened for write(append) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testAppendRenameNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, false);

        GridGgfsOutputStream os = null;

        try {
            os = ggfs.append(FILE, false);

            ggfs.rename(FILE, FILE2);

            os.close();
        }
        finally {
            U.closeQuiet(os);
        }
    }

    /**
     * Test rename on the file parent when it was opened for write(append) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testAppendRenameParentNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, false);

        GridGgfsOutputStream os = null;

        try {
            os = ggfs.append(FILE, false);

            ggfs.rename(SUBDIR, SUBDIR2);

            os.close();
        }
        finally {
            U.closeQuiet(os);
        }
    }

    /**
     * Test delete on the file when it was opened for write(append) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testAppendDeleteNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, false);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsOutputStream os = null;

                try {
                    os = ggfs.append(FILE, false);

                    ggfs.format().get();

                    os.write(chunk);

                    os.close();
                }
                finally {
                    U.closeQuiet(os);
                }

                return null;
            }
        }, IOException.class, "File was concurrently deleted: " + FILE);
    }

    /**
     * Test delete on the file parent when it was opened for write(append) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testAppendDeleteParentNoClose() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, false);

        GridTestUtils.assertThrows(log, new Callable<Object>() {
            @Override public Object call() throws Exception {
                GridGgfsOutputStream os = null;

                try {
                    GridUuid id = ggfs.context().meta().fileId(FILE);

                    os = ggfs.append(FILE, false);

                    ggfs.delete(SUBDIR, true); // Since GG-4911 we allow deletes in this case.

                    for (int i = 0; i < 100 && ggfs.context().meta().exists(id); i++)
                        U.sleep(100);

                    os.write(chunk);

                    os.close();
                }
                finally {
                    U.closeQuiet(os);
                }

                return null;
            }
        }, IOException.class, "File was concurrently deleted: " + FILE);
    }

    /**
     * Test update on the file when it was opened for write(create) and is not closed yet.
     *
     * @throws Exception If failed.
     */
    public void testAppendUpdateNoClose() throws Exception {
        Map<String, String> props = properties("owner", "group", "0555");

        create(ggfs, paths(DIR, SUBDIR), null);

        createFile(ggfs, FILE, false);

        GridGgfsOutputStream os = null;

        try {
            os = ggfs.append(FILE, false);

            ggfs.update(FILE, props);

            os.close();
        }
        finally {
            U.closeQuiet(os);
        }
    }

    /**
     * Ensure consistency of data during appending to a file.
     *
     * @throws Exception If failed.
     */
    public void testAppendConsistency() throws Exception {
        final AtomicInteger ctr = new AtomicInteger();
        final AtomicReference<Exception> err = new AtomicReference<>();

        int threadCnt = 10;

        for (int i = 0; i < threadCnt; i++)
            createFile(ggfs, new GridGgfsPath("/file" + i), false);

        multithreaded(new Runnable() {
            @Override public void run() {
                int idx = ctr.getAndIncrement();

                GridGgfsPath path = new GridGgfsPath("/file" + idx);

                try {
                    byte[][] chunks = new byte[REPEAT_CNT][];

                    for (int i = 0; i < REPEAT_CNT; i++) {
                        chunks[i] = chunk;

                        GridGgfsOutputStream os = ggfs.append(path, false);

                        os.write(chunk);

                        os.close();

                        assert ggfs.exists(path);
                    }

                    awaitFileClose(ggfs, path);

                    checkFileContent(ggfs, path, chunks);
                }
                catch (IOException | GridException e) {
                    err.compareAndSet(null, e); // Log the very first error.
                }
            }
        }, threadCnt);

        if (err.get() != null)
            throw err.get();
    }

    /**
     * Ensure append consistency when multiple threads writes to the same file.
     *
     * @throws Exception If failed.
     */
    public void testAppendConsistencyMultithreaded() throws Exception {
        final AtomicBoolean stop = new AtomicBoolean();

        final AtomicInteger chunksCtr = new AtomicInteger(); // How many chunks were written.
        final AtomicReference<Exception> err = new AtomicReference<>();

        ggfs.create(FILE, false).close();

        int threadCnt = 5;

        GridFuture<?> fut = multithreadedAsync(new Runnable() {
            @Override public void run() {
                while (!stop.get()) {
                    GridGgfsOutputStream os = null;

                    try {
                        os = ggfs.append(FILE, false);

                        os.write(chunk);

                        U.sleep(50);

                        os.close();

                        chunksCtr.incrementAndGet();
                    }
                    catch (GridException ignore) {
                        try {
                            U.sleep(10);
                        }
                        catch (GridInterruptedException ignored) {
                            // nO-op.
                        }
                    }
                    catch (IOException e) {
                        err.compareAndSet(null, e);
                    }
                    finally {
                        U.closeQuiet(os);
                    }
                }
            }
        }, threadCnt);

        long startTime = U.currentTimeMillis();

        while (chunksCtr.get() < 50 && U.currentTimeMillis() - startTime < 60 * 1000)
            U.sleep(100);

        stop.set(true);

        fut.get();

        awaitFileClose(ggfs, FILE);

        if (err.get() != null)
            throw err.get();

        byte[][] data = new byte[chunksCtr.get()][];

        Arrays.fill(data, chunk);

        checkFileContent(ggfs, FILE, data);
    }

    /**
     * Ensure that GGFS is able to stop in case not closed output stream exist.
     *
     * @throws Exception If failed.
     */
    public void testStop() throws Exception {
        create(ggfs, paths(DIR, SUBDIR), null);

        GridGgfsOutputStream os = ggfs.create(FILE, true);

        os.write(chunk);

        ggfs.stop();

        // Reset test state.
        afterTestsStopped();
        beforeTestsStarted();
    }

    /**
     * Ensure that in case we create the folder A and delete its parent at the same time, resulting file system
     * structure is consistent.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentMkdirsDelete() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            final CyclicBarrier barrier = new CyclicBarrier(2);

            GridPlainFuture<Boolean> res1 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        ggfs.mkdirs(SUBSUBDIR);
                    }
                    catch (GridException ignored) {
                        return false;
                    }

                    return true;
                }
            });

            GridPlainFuture<Boolean> res2 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        return ggfs.delete(DIR, true);
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            assert res1.get(); // MKDIRS must succeed anyway.

            if (res2.get())
                checkNotExist(ggfs, ggfsSecondary, DIR, SUBDIR, SUBSUBDIR);
            else
                checkExist(ggfs, ggfsSecondary, DIR, SUBDIR, SUBSUBDIR);

            clear(ggfs, ggfsSecondary);
        }
    }

    /**
     * Ensure that in case we rename the folder A and delete it at the same time, only one of these requests succeed.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentRenameDeleteSource() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            final CyclicBarrier barrier = new CyclicBarrier(2);

            create(ggfs, paths(DIR, SUBDIR, DIR_NEW), paths());

            GridPlainFuture<Boolean> res1 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        ggfs.rename(SUBDIR, SUBDIR_NEW);

                        return true;
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            GridPlainFuture<Boolean> res2 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        return ggfs.delete(SUBDIR, true);
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            res1.get();
            res2.get();

            if (res1.get()) {
                assert !res2.get(); // Rename succeeded, so delete must fail.

                checkExist(ggfs, ggfsSecondary, DIR, DIR_NEW, SUBDIR_NEW);
                checkNotExist(ggfs, ggfsSecondary, SUBDIR);
            }
            else {
                assert res2.get(); // Rename failed because delete succeeded.

                checkExist(ggfs, DIR); // DIR_NEW should not be synchronized with he primary GGFS.

                if (dual)
                    checkExist(ggfsSecondary, DIR, DIR_NEW);

                checkNotExist(ggfs, ggfsSecondary, SUBDIR, SUBDIR_NEW);
            }

            clear(ggfs, ggfsSecondary);
        }
    }

    /**
     * Ensure that in case we rename the folder A to B and delete B at the same time, FS consistency is not
     * compromised.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentRenameDeleteDestination() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            final CyclicBarrier barrier = new CyclicBarrier(2);

            create(ggfs, paths(DIR, SUBDIR, DIR_NEW), paths());

            GridPlainFuture<Boolean> res1 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        ggfs.rename(SUBDIR, SUBDIR_NEW);

                        return true;
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            GridPlainFuture<Boolean> res2 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        return ggfs.delete(SUBDIR_NEW, true);
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            assert res1.get();

            if (res2.get()) {
                // Delete after rename.
                checkExist(ggfs, ggfsSecondary, DIR, DIR_NEW);
                checkNotExist(ggfs, ggfsSecondary, SUBDIR, SUBDIR_NEW);
            }
            else {
                // Delete before rename.
                checkExist(ggfs, ggfsSecondary, DIR, DIR_NEW, SUBDIR_NEW);
                checkNotExist(ggfs, ggfsSecondary, SUBDIR);
            }

            clear(ggfs, ggfsSecondary);
        }
    }

    /**
     * Ensure file system consistency in case two concurrent rename requests are executed: A -> B and B -> A.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentRenames() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            final CyclicBarrier barrier = new CyclicBarrier(2);

            create(ggfs, paths(DIR, SUBDIR, DIR_NEW), paths());

            GridPlainFuture<Boolean> res1 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        ggfs.rename(SUBDIR, SUBDIR_NEW);

                        return true;
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            GridPlainFuture<Boolean> res2 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        ggfs.rename(SUBDIR_NEW, SUBDIR);

                        return true;
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            res1.get();
            res2.get();

            assert res1.get(); // First rename must be successful anyway.

            if (res2.get()) {
                checkExist(ggfs, ggfsSecondary, DIR, SUBDIR, DIR_NEW);
                checkNotExist(ggfs, ggfsSecondary, SUBDIR_NEW);
            }
            else {
                checkExist(ggfs, ggfsSecondary, DIR, DIR_NEW, SUBDIR_NEW);
                checkNotExist(ggfs, ggfsSecondary, SUBDIR);
            }

            clear(ggfs, ggfsSecondary);
        }
    }

    /**
     * Ensure that in case we delete the folder A and delete its parent at the same time, resulting file system
     * structure is consistent.
     *
     * @throws Exception If failed.
     */
    public void testConcurrentDeletes() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            final CyclicBarrier barrier = new CyclicBarrier(2);

            create(ggfs, paths(DIR, SUBDIR, SUBSUBDIR), paths());

            GridPlainFuture<Boolean> res1 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        ggfs.delete(SUBDIR, true);

                        return true;
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            GridPlainFuture<Boolean> res2 = execute(new Callable<Boolean>() {
                @Override public Boolean call() throws Exception {
                    U.awaitQuiet(barrier);

                    try {
                        ggfs.delete(SUBSUBDIR, true);

                        return true;
                    }
                    catch (GridException ignored) {
                        return false;
                    }
                }
            });

            assert res1.get(); // Delete on the parent must succeed anyway.
            res2.get();

            checkExist(ggfs, ggfsSecondary, DIR);
            checkNotExist(ggfs, ggfsSecondary, SUBDIR, SUBSUBDIR);

            clear(ggfs, ggfsSecondary);
        }
    }

    /**
     * Ensure that deadlocks do not occur during concurrent rename operations.
     *
     * @throws Exception If failed.
     */
    public void testDeadlocksRename() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            try {
                checkDeadlocks(5, 2, 2, 2, OPS_CNT, 0, 0, 0, 0);
            }
            finally {
                info(">>>>>> Start deadlock test");

                clear(ggfs, ggfsSecondary);

                info(">>>>>> End deadlock test");
            }
        }
    }

    /**
     * Ensure that deadlocks do not occur during concurrent delete operations.
     *
     * @throws Exception If failed.
     */
    public void testDeadlocksDelete() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            try {
                checkDeadlocks(5, 2, 2, 2, 0, OPS_CNT, 0, 0, 0);
            }
            finally {
                clear(ggfs, ggfsSecondary);
            }
        }
    }

    /**
     * Ensure that deadlocks do not occur during concurrent update operations.
     *
     * @throws Exception If failed.
     */
    public void testDeadlocksUpdate() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            try {
                checkDeadlocks(5, 2, 2, 2, 0, 0, OPS_CNT, 0, 0);
            }
            finally {
                clear(ggfs, ggfsSecondary);
            }
        }
    }

    /**
     * Ensure that deadlocks do not occur during concurrent directory creation operations.
     *
     * @throws Exception If failed.
     */
    public void testDeadlocksMkdirs() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            try {
                checkDeadlocks(5, 2, 2, 2, 0, 0, 0, OPS_CNT, 0);
            }
            finally {
                clear(ggfs, ggfsSecondary);
            }
        }
    }

    /**
     * Ensure that deadlocks do not occur during concurrent file creation operations.
     *
     * @throws Exception If failed.
     */
    public void testDeadlocksCreate() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            try {
                checkDeadlocks(5, 2, 2, 2, 0, 0, 0, 0, OPS_CNT);
            }
            finally {
                clear(ggfs, ggfsSecondary);
            }
        }
    }

    /**
     * Ensure that deadlocks do not occur during concurrent operations of various types.
     *
     * @throws Exception If failed.
     */
    public void testDeadlocks() throws Exception {
        for (int i = 0; i < REPEAT_CNT; i++) {
            try {
                checkDeadlocks(5, 2, 2, 2, OPS_CNT, OPS_CNT, OPS_CNT, OPS_CNT, OPS_CNT);
            }
            finally {
                clear(ggfs, ggfsSecondary);
            }
        }
    }

    /**
     * Check deadlocks by creating complex directories structure and then executing chaotic operations on it. A lot of
     * exception are expected here. We are not interested in them. Instead, we want to ensure that no deadlocks occur
     * during execution.
     *
     * @param lvlCnt Total levels in folder hierarchy.
     * @param childrenDirPerLvl How many children directories to create per level.
     * @param childrenFilePerLvl How many children file to create per level.
     * @param primaryLvlCnt How many levels will exist in the primary file system before check start.
     * @param renCnt How many renames to perform.
     * @param delCnt How many deletes to perform.
     * @param updateCnt How many updates to perform.
     * @param mkdirsCnt How many directory creations to perform.
     * @param createCnt How many file creations to perform.
     * @throws Exception If failed.
     */
    public void checkDeadlocks(final int lvlCnt, final int childrenDirPerLvl, final int childrenFilePerLvl,
        int primaryLvlCnt, int renCnt, int delCnt,
        int updateCnt, int mkdirsCnt, int createCnt) throws Exception {
        assert childrenDirPerLvl > 0;

        // First define file system structure.
        final Map<Integer, List<GridGgfsPath>> dirPaths = new HashMap<>();
        final Map<Integer, List<GridGgfsPath>> filePaths = new HashMap<>();

        Queue<GridBiTuple<Integer, GridGgfsPath>> queue = new ArrayDeque<>();

        queue.add(F.t(0, new GridGgfsPath())); // Add root directory.

        while (!queue.isEmpty()) {
            GridBiTuple<Integer, GridGgfsPath> entry = queue.poll();

            int lvl = entry.getKey();

            if (lvl < lvlCnt) {
                int newLvl = lvl + 1;

                for (int i = 0; i < childrenDirPerLvl; i++) {
                    GridGgfsPath path = new GridGgfsPath(entry.getValue(), "dir-" + newLvl + "-" + i);

                    queue.add(F.t(newLvl, path));

                    if (!dirPaths.containsKey(newLvl))
                        dirPaths.put(newLvl, new ArrayList<GridGgfsPath>());

                    dirPaths.get(newLvl).add(path);
                }

                for (int i = 0; i < childrenFilePerLvl; i++) {
                    GridGgfsPath path = new GridGgfsPath(entry.getValue(), "file-" + newLvl + "-" + i);

                    if (!filePaths.containsKey(newLvl))
                        filePaths.put(newLvl, new ArrayList<GridGgfsPath>());

                    filePaths.get(newLvl).add(path);
                }
            }
        }

        // Now as we have all paths defined, plan operations on them.
        final Random rand = new Random(U.currentTimeMillis());

        int totalOpCnt = renCnt + delCnt + updateCnt + mkdirsCnt + createCnt;

        final CyclicBarrier barrier = new CyclicBarrier(totalOpCnt);

        Collection<Thread> threads = new ArrayList<>(totalOpCnt);

        // Renames.
        for (int i = 0; i < renCnt; i++) {
            Runnable r = new Runnable() {
                @Override public void run() {
                    try {
                        int fromLvl = rand.nextInt(lvlCnt) + 1;
                        int toLvl = rand.nextInt(lvlCnt) + 1;

                        List<GridGgfsPath> fromPaths;
                        List<GridGgfsPath> toPaths;

                        if (rand.nextInt(childrenDirPerLvl + childrenFilePerLvl) < childrenDirPerLvl) {
                            // Rename directories.
                            fromPaths = dirPaths.get(fromLvl);
                            toPaths = dirPaths.get(toLvl);
                        }
                        else {
                            // Rename files.
                            fromPaths = filePaths.get(fromLvl);
                            toPaths = filePaths.get(toLvl);
                        }

                        GridGgfsPath fromPath = fromPaths.get(rand.nextInt(fromPaths.size()));
                        GridGgfsPath toPath = toPaths.get(rand.nextInt(toPaths.size()));

                        U.awaitQuiet(barrier);

                        ggfs.rename(fromPath, toPath);
                    }
                    catch (GridException ignore) {
                        // No-op.
                    }
                }
            };

            threads.add(new Thread(r));
        }

        // Deletes.
        for (int i = 0; i < delCnt; i++) {
            Runnable r = new Runnable() {
                @Override public void run() {
                    try {
                        int lvl = rand.nextInt(lvlCnt) + 1;

                        GridGgfsPath path = rand.nextInt(childrenDirPerLvl + childrenFilePerLvl) < childrenDirPerLvl ?
                            dirPaths.get(lvl).get(rand.nextInt(dirPaths.get(lvl).size())) :
                            filePaths.get(lvl).get(rand.nextInt(filePaths.get(lvl).size()));

                        U.awaitQuiet(barrier);

                        ggfs.delete(path, true);
                    }
                    catch (GridException ignore) {
                        // No-op.
                    }
                }
            };

            threads.add(new Thread(r));
        }

        // Updates.
        for (int i = 0; i < updateCnt; i++) {
            Runnable r = new Runnable() {
                @Override public void run() {
                    try {
                        int lvl = rand.nextInt(lvlCnt) + 1;

                        GridGgfsPath path = rand.nextInt(childrenDirPerLvl + childrenFilePerLvl) < childrenDirPerLvl ?
                            dirPaths.get(lvl).get(rand.nextInt(dirPaths.get(lvl).size())) :
                            filePaths.get(lvl).get(rand.nextInt(filePaths.get(lvl).size()));

                        U.awaitQuiet(barrier);

                        ggfs.update(path, properties("owner", "group", null));
                    }
                    catch (GridException ignore) {
                        // No-op.
                    }
                }
            };

            threads.add(new Thread(r));
        }

        // Directory creations.
        final AtomicInteger dirCtr = new AtomicInteger();

        for (int i = 0; i < mkdirsCnt; i++) {
            Runnable r = new Runnable() {
                @Override public void run() {
                    try {
                        int lvl = rand.nextInt(lvlCnt) + 1;

                        GridGgfsPath parentPath = dirPaths.get(lvl).get(rand.nextInt(dirPaths.get(lvl).size()));

                        GridGgfsPath path = new GridGgfsPath(parentPath, "newDir-" + dirCtr.incrementAndGet());

                        U.awaitQuiet(barrier);

                        ggfs.mkdirs(path);

                    }
                    catch (GridException ignore) {
                        // No-op.
                    }
                }
            };

            threads.add(new Thread(r));
        }

        // File creations.
        final AtomicInteger fileCtr = new AtomicInteger();

        for (int i = 0; i < createCnt; i++) {
            Runnable r = new Runnable() {
                @Override public void run() {
                    try {
                        int lvl = rand.nextInt(lvlCnt) + 1;

                        GridGgfsPath parentPath = dirPaths.get(lvl).get(rand.nextInt(dirPaths.get(lvl).size()));

                        GridGgfsPath path = new GridGgfsPath(parentPath, "newFile-" + fileCtr.incrementAndGet());

                        U.awaitQuiet(barrier);

                        GridGgfsOutputStream os = null;

                        try {
                            os = ggfs.create(path, true);

                            os.write(chunk);
                        }
                        finally {
                            U.closeQuiet(os);
                        }
                    }
                    catch (IOException | GridException ignore) {
                        // No-op.
                    }
                }
            };

            threads.add(new Thread(r));
        }

        // Create folder structure.
        for (int i = 0; i < lvlCnt; i++) {
            int lvl = i + 1;

            GridGgfsImpl targetGgfs = dual ? lvl <= primaryLvlCnt ? ggfs : ggfsSecondary : ggfs;

            GridGgfsPath[] dirs = dirPaths.get(lvl).toArray(new GridGgfsPath[dirPaths.get(lvl).size()]);
            GridGgfsPath[] files = filePaths.get(lvl).toArray(new GridGgfsPath[filePaths.get(lvl).size()]);

            create(targetGgfs, dirs, files);
        }

        // Start all threads and wait for them to finish.
        for (Thread thread : threads)
            thread.start();

        U.joinThreads(threads, null);
    }

    /**
     * Create the given directories and files in the given GGFS.
     *
     * @param ggfs GGFS.
     * @param dirs Directories.
     * @param files Files.
     * @throws Exception If failed.
     */
    public static void create(GridGgfsFileSystem ggfs, @Nullable GridGgfsPath[] dirs, @Nullable GridGgfsPath[] files)
        throws Exception {
        if (dirs != null) {
            for (GridGgfsPath dir : dirs)
                ggfs.mkdirs(dir);
        }

        if (files != null) {
            for (GridGgfsPath file : files) {
                OutputStream os = ggfs.create(file, true);

                os.close();
            }
        }
    }

    /**
     * Create the file in the given GGFS and write provided data chunks to it.
     *
     * @param ggfs GGFS.
     * @param file File.
     * @param overwrite Overwrite flag.
     * @param chunks Data chunks.
     * @throws IOException In case of IO exception.
     * @throws GridException In case of Grid exception.
     */
    protected static void createFile(GridGgfsFileSystem ggfs, GridGgfsPath file, boolean overwrite,
        @Nullable byte[]... chunks) throws IOException, GridException {
        OutputStream os = null;

        try {
            os = ggfs.create(file, overwrite);

            writeFileChunks(os, chunks);
        }
        finally {
            U.closeQuiet(os);

            awaitFileClose(ggfs, file);
        }
    }

    /**
     * Create the file in the given GGFS and write provided data chunks to it.
     *
     * @param ggfs GGFS.
     * @param file File.
     * @param overwrite Overwrite flag.
     * @param blockSize Block size.
     * @param chunks Data chunks.
     * @throws Exception If failed.
     */
    protected void createFile(GridGgfs ggfs, GridGgfsPath file, boolean overwrite, long blockSize,
        @Nullable byte[]... chunks) throws Exception {
        GridGgfsOutputStream os = null;

        try {
            os = ggfs.create(file, 256, overwrite, null, 0, blockSize, null);

            writeFileChunks(os, chunks);
        }
        finally {
            U.closeQuiet(os);

            awaitFileClose(ggfs, file);
        }
    }

    /**
     * Append to the file in the given GGFS provided data chunks.
     *
     * @param ggfs GGFS.
     * @param file File.
     * @param chunks Data chunks.
     * @throws Exception If failed.
     */
    protected void appendFile(GridGgfs ggfs, GridGgfsPath file, @Nullable byte[]... chunks)
        throws Exception {
        GridGgfsOutputStream os = null;

        try {
            os = ggfs.append(file, false);

            writeFileChunks(os, chunks);
        }
        finally {
            U.closeQuiet(os);

            awaitFileClose(ggfs, file);
        }
    }

    /**
     * Write provided data chunks to the file output stream.
     *
     * @param os Output stream.
     * @param chunks Data chunks.
     * @throws IOException If failed.
     */
    protected static void writeFileChunks(OutputStream os, @Nullable byte[]... chunks) throws IOException {
        if (chunks != null && chunks.length > 0) {
            for (byte[] chunk : chunks)
                os.write(chunk);
        }
    }

    /**
     * Await for previously opened output stream to close. This is achieved by requesting dummy update on the file.
     *
     * @param ggfs GGFS.
     * @param file File.
     */
    public static void awaitFileClose(GridGgfsFileSystem ggfs, GridGgfsPath file) {
        try {
            ggfs.update(file, Collections.singletonMap("prop", "val"));
        }
        catch (GridException ignore) {
            // No-op.
        }
    }

    /**
     * Ensure that the given paths exist in the given GGFSs.
     *
     * @param ggfs First GGFS.
     * @param ggfsSecondary Second GGFS.
     * @param paths Paths.
     * @throws Exception If failed.
     */
    protected void checkExist(GridGgfsImpl ggfs, GridGgfsImpl ggfsSecondary, GridGgfsPath... paths) throws Exception {
        checkExist(ggfs, paths);

        if (dual)
            checkExist(ggfsSecondary, paths);
    }

    /**
     * Ensure that the given paths exist in the given GGFS.
     *
     * @param ggfs GGFS.
     * @param paths Paths.
     * @throws GridException If failed.
     */
    protected void checkExist(GridGgfsImpl ggfs, GridGgfsPath... paths) throws GridException {
        for (GridGgfsPath path : paths) {
            assert ggfs.context().meta().fileId(path) != null : "Path doesn't exist [ggfs=" + ggfs.name() +
                ", path=" + path + ']';
            assert ggfs.exists(path) : "Path doesn't exist [ggfs=" + ggfs.name() + ", path=" + path + ']';
        }
    }

    /**
     * Ensure that the given paths don't exist in the given GGFSs.
     *
     * @param ggfs First GGFS.
     * @param ggfsSecondary Second GGFS.
     * @param paths Paths.
     * @throws Exception If failed.
     */
    protected void checkNotExist(GridGgfsImpl ggfs, GridGgfsImpl ggfsSecondary, GridGgfsPath... paths)
        throws Exception {
        checkNotExist(ggfs, paths);

        if (dual)
            checkNotExist(ggfsSecondary, paths);
    }

    /**
     * Ensure that the given paths don't exist in the given GGFS.
     *
     * @param ggfs GGFS.
     * @param paths Paths.
     * @throws Exception If failed.
     */
    protected void checkNotExist(GridGgfsImpl ggfs, GridGgfsPath... paths) throws Exception {
        for (GridGgfsPath path : paths) {
            assert ggfs.context().meta().fileId(path) == null : "Path exists [ggfs=" + ggfs.name() + ", path=" +
                path + ']';
            assert !ggfs.exists(path) : "Path exists [ggfs=" + ggfs.name() + ", path=" + path + ']';
        }
    }

    /**
     * Ensure that the given file exists in the given GGFSs and that it has exactly the same content as provided in the
     * "data" parameter.
     *
     * @param ggfs First GGFS.
     * @param ggfsSecondary Second GGFS.
     * @param file File.
     * @param chunks Expected data.
     * @throws Exception If failed.
     */
    protected void checkFile(GridGgfsImpl ggfs, GridGgfsImpl ggfsSecondary, GridGgfsPath file,
        @Nullable byte[]... chunks) throws Exception {
        checkExist(ggfs, file);
        checkFileContent(ggfs, file, chunks);

        if (dual) {
            checkExist(ggfsSecondary, file);
            checkFileContent(ggfsSecondary, file, chunks);
        }
    }

    /**
     * Ensure that the given file has exactly the same content as provided in the "data" parameter.
     *
     * @param ggfs GGFS.
     * @param file File.
     * @param chunks Expected data.
     * @throws IOException In case of IO exception.
     * @throws GridException In case of Grid exception.
     */
    protected void checkFileContent(GridGgfsImpl ggfs, GridGgfsPath file, @Nullable byte[]... chunks)
        throws IOException, GridException {
        if (chunks != null && chunks.length > 0) {
            GridGgfsInputStream is = null;

            try {
                is = ggfs.open(file);

                int chunkIdx = 0;

                for (byte[] chunk : chunks) {
                    byte[] buf = new byte[chunk.length];

                    is.readFully(0, buf);

                    assert Arrays.equals(chunk, buf) : "Bad chunk [ggfs=" + ggfs.name() + ", chunkIdx=" + chunkIdx +
                        ", expected=" + Arrays.toString(chunk) + ", actual=" + Arrays.toString(buf) + ']';

                    chunkIdx++;
                }

                is.close();
            }
            finally {
                U.closeQuiet(is);
            }
        }
    }

    /**
     * Create map with properties.
     *
     * @param username User name.
     * @param grpName Group name.
     * @param perm Permission.
     * @return Map with properties.
     */
    protected Map<String, String> properties(@Nullable String username, @Nullable String grpName,
        @Nullable String perm) {
        Map<String, String> props = new HashMap<>();

        if (username != null)
            props.put(PROP_USER_NAME, username);

        if (grpName != null)
            props.put(PROP_GROUP_NAME, grpName);

        if (perm != null)
            props.put(PROP_PERMISSION, perm);

        return props;
    }

    /**
     * Convenient method to group paths.
     *
     * @param paths Paths to group.
     * @return Paths as array.
     */
    protected GridGgfsPath[] paths(GridGgfsPath... paths) {
        return paths;
    }

    /**
     * Safely clear GGFSs.
     *
     * @param ggfs First GGFS.
     * @param ggfsSecondary Second GGFS.
     * @throws Exception If failed.
     */
    protected void clear(GridGgfs ggfs, GridGgfs ggfsSecondary) throws Exception {
        clear(ggfs);

        if (dual)
            clear(ggfsSecondary);
    }

    /**
     * Clear particular GGFS.
     *
     * @param ggfs GGFS.
     * @throws Exception If failed.
     */
    public static void clear(GridGgfs ggfs) throws Exception {
        Field workerMapFld = GridGgfsImpl.class.getDeclaredField("workerMap");

        workerMapFld.setAccessible(true);

        // Wait for all workers to finish.
        Map<GridGgfsPath, GridGgfsFileWorker> workerMap =
            (Map<GridGgfsPath, GridGgfsFileWorker>)workerMapFld.get(ggfs);

        for (Map.Entry<GridGgfsPath, GridGgfsFileWorker> entry : workerMap.entrySet()) {
            entry.getValue().cancel();

            U.join(entry.getValue());
        }

        // Clear ggfs.
        ggfs.format().get();
    }
}
