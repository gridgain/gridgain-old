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

package org.gridgain.grid.kernal.processors.rest.handlers.cache;

import org.gridgain.grid.*;
import org.gridgain.grid.cache.*;
import org.gridgain.grid.cache.datastructures.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.cache.*;
import org.gridgain.grid.kernal.processors.license.*;
import org.gridgain.grid.kernal.processors.rest.*;
import org.gridgain.grid.kernal.processors.rest.handlers.*;
import org.gridgain.grid.kernal.processors.rest.request.*;
import org.gridgain.grid.kernal.processors.task.*;
import org.gridgain.grid.lang.*;
import org.gridgain.grid.resources.*;
import org.gridgain.grid.util.future.*;
import org.gridgain.grid.util.lang.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

import static org.gridgain.grid.cache.GridCacheTxConcurrency.*;
import static org.gridgain.grid.cache.GridCacheTxIsolation.*;
import static org.gridgain.grid.kernal.processors.rest.GridRestCommand.*;
import static org.gridgain.grid.kernal.processors.license.GridLicenseSubsystem.*;

/**
 * Command handler for API requests.
 */
public class GridCacheCommandHandler extends GridRestCommandHandlerAdapter {
    /** Supported commands. */
    private static final Collection<GridRestCommand> SUPPORTED_COMMANDS = U.sealList(
        CACHE_GET,
        CACHE_GET_ALL,
        CACHE_PUT,
        CACHE_ADD,
        CACHE_PUT_ALL,
        CACHE_REMOVE,
        CACHE_REMOVE_ALL,
        CACHE_REPLACE,
        CACHE_INCREMENT,
        CACHE_DECREMENT,
        CACHE_CAS,
        CACHE_APPEND,
        CACHE_PREPEND,
        CACHE_METRICS
    );

    /** Requests with required parameter {@code key}. */
    private static final EnumSet<GridRestCommand> KEY_REQUIRED_REQUESTS = EnumSet.of(
        CACHE_GET,
        CACHE_PUT,
        CACHE_ADD,
        CACHE_REMOVE,
        CACHE_REPLACE,
        CACHE_INCREMENT,
        CACHE_DECREMENT,
        CACHE_CAS,
        CACHE_APPEND,
        CACHE_PREPEND
    );

    /** */
    private static final GridCacheFlag[] EMPTY_FLAGS = new GridCacheFlag[0];

    /**
     * @param ctx Context.
     */
    public GridCacheCommandHandler(GridKernalContext ctx) {
        super(ctx);
    }

    /** {@inheritDoc} */
    @Override public Collection<GridRestCommand> supportedCommands() {
        return SUPPORTED_COMMANDS;
    }

    /**
     * Retrieves cache flags from corresponding bits.
     *
     * @param cacheFlagsBits Integer representation of cache flags bit set.
     * @return Array of cache flags.
     */
    public static GridCacheFlag[] parseCacheFlags(int cacheFlagsBits) {
        if (cacheFlagsBits == 0)
            return EMPTY_FLAGS;

        EnumSet<GridCacheFlag> flagSet = EnumSet.noneOf(GridCacheFlag.class);

        if ((cacheFlagsBits & 1) != 0)
            flagSet.add(GridCacheFlag.SKIP_STORE);

        if ((cacheFlagsBits & (1 << 1)) != 0)
            flagSet.add(GridCacheFlag.SKIP_SWAP);

        if ((cacheFlagsBits & (1 << 2)) != 0)
            flagSet.add(GridCacheFlag.SYNC_COMMIT);

        if ((cacheFlagsBits & (1 << 4)) != 0)
            flagSet.add(GridCacheFlag.INVALIDATE);

        return flagSet.toArray(new GridCacheFlag[flagSet.size()]);
    }

    /** {@inheritDoc} */
    @Override public GridFuture<GridRestResponse> handleAsync(final GridRestRequest req) {
        assert req instanceof GridRestCacheRequest : "Invalid command for topology handler: " + req;

        assert SUPPORTED_COMMANDS.contains(req.command());

        GridLicenseUseRegistry.onUsage(DATA_GRID, getClass());

        if (log.isDebugEnabled())
            log.debug("Handling cache REST request: " + req);

        GridRestCacheRequest req0 = (GridRestCacheRequest)req;

        final String cacheName = req0.cacheName();

        final Object key = req0.key();

        final GridCacheFlag[] flags = parseCacheFlags(req0.cacheFlags());

        try {
            GridRestCommand cmd = req0.command();

            if (key == null && KEY_REQUIRED_REQUESTS.contains(cmd))
                throw new GridException(GridRestCommandHandlerAdapter.missingParameter("key"));

            final Long ttl = req0.ttl();

            GridFuture<GridRestResponse> fut;

            switch (cmd) {
                case CACHE_GET: {
                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new GetCommand(key), req.portableMode());

                    break;
                }

                case CACHE_GET_ALL: {
                    Set<Object> keys = req0.values().keySet();

                    if (F.isEmpty(keys))
                        throw new GridException(GridRestCommandHandlerAdapter.missingParameter("keys"));

                    // HashSet wrapping for correct serialization
                    keys = new HashSet<>(keys);

                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new GetAllCommand(keys), req.portableMode());

                    break;
                }

                case CACHE_PUT: {
                    final Object val = req0.value();

                    if (val == null)
                        throw new GridException(GridRestCommandHandlerAdapter.missingParameter("val"));

                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key, new
                        PutCommand(key, ttl, val), req.portableMode());

                    break;
                }

                case CACHE_ADD: {
                    final Object val = req0.value();

                    if (val == null)
                        throw new GridException(GridRestCommandHandlerAdapter.missingParameter("val"));

                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new AddCommand(key, ttl, val), req.portableMode());

                    break;
                }

                case CACHE_PUT_ALL: {
                    Map<Object, Object> map = req0.values();

                    if (F.isEmpty(map))
                        throw new GridException(GridRestCommandHandlerAdapter.missingParameter("values"));

                    for (Map.Entry<Object, Object> e : map.entrySet()) {
                        if (e.getKey() == null)
                            throw new GridException("Failing putAll operation (null keys are not allowed).");

                        if (e.getValue() == null)
                            throw new GridException("Failing putAll operation (null values are not allowed).");
                    }

                    // HashMap wrapping for correct serialization
                    map = new HashMap<>(map);

                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new PutAllCommand(map), req.portableMode());

                    break;
                }

                case CACHE_REMOVE: {
                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new RemoveCommand(key), req.portableMode());

                    break;
                }

                case CACHE_REMOVE_ALL: {
                    Map<Object, Object> map = req0.values();

                    // HashSet wrapping for correct serialization
                    Set<Object> keys = map == null ? null : new HashSet<>(map.keySet());

                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new RemoveAllCommand(keys), req.portableMode());

                    break;
                }

                case CACHE_REPLACE: {
                    final Object val = req0.value();

                    if (val == null)
                        throw new GridException(GridRestCommandHandlerAdapter.missingParameter("val"));

                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new ReplaceCommand(key, ttl, val), req.portableMode());

                    break;
                }

                case CACHE_INCREMENT: {
                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, key,
                        new IncrementCommand(key, req0));

                    break;
                }

                case CACHE_DECREMENT: {
                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, key,
                        new DecrementCommand(key, req0));

                    break;
                }

                case CACHE_CAS: {
                    final Object val1 = req0.value();
                    final Object val2 = req0.value2();

                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new CasCommand(val2, val1, key), req.portableMode());

                    break;
                }

                case CACHE_APPEND: {
                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new AppendCommand(key, req0), req.portableMode());

                    break;
                }

                case CACHE_PREPEND: {
                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, flags, key,
                        new PrependCommand(key, req0), req.portableMode());

                    break;
                }

                case CACHE_METRICS: {
                    fut = executeCommand(req.destinationId(), req.clientId(), cacheName, key, new MetricsCommand());

                    break;
                }

                default:
                    throw new IllegalArgumentException("Invalid command for cache handler: " + req);
            }

            return fut;
        }
        catch (GridRuntimeException e) {
            U.error(log, "Failed to execute cache command: " + req, e);

            return new GridFinishedFuture<>(ctx, e);
        }
        catch (GridException e) {
            U.error(log, "Failed to execute cache command: " + req, e);

            return new GridFinishedFuture<>(ctx, e);
        }
        finally {
            if (log.isDebugEnabled())
                log.debug("Handled cache REST request: " + req);
        }
    }

    /**
     * Executes command on flagged cache projection. Checks {@code destId} to find
     * if command could be performed locally or routed to a remote node.
     *
     * @param destId Target node Id for the operation.
     *      If {@code null} - operation could be executed anywhere.
     * @param clientId Client ID.
     * @param cacheName Cache name.
     * @param flags Cache flags.
     * @param key Key to set affinity mapping in the response.
     * @param op Operation to perform.
     * @return Operation result in future.
     * @throws GridException If failed
     */
    private GridFuture<GridRestResponse> executeCommand(
        @Nullable UUID destId,
        UUID clientId,
        final String cacheName,
        final GridCacheFlag[] flags,
        final Object key,
        final CacheProjectionCommand op,
        final boolean portable) throws GridException {

        final boolean locExec =
            destId == null || destId.equals(ctx.localNodeId()) || replicatedCacheAvailable(cacheName);

        if (locExec) {
            GridCacheProjection<?,?> prj = localCache(cacheName).forSubjectId(clientId).flagsOn(flags);

            if (portable)
                prj = prj.keepPortable();

            return op.apply((GridCacheProjection<Object, Object>)prj, ctx).
                chain(resultWrapper((GridCacheProjection<Object, Object>)prj, key));
        }
        else {
            return ctx.grid().forPredicate(F.nodeForNodeId(destId)).compute().withNoFailover().
                call(new FlaggedCacheOperationCallable(clientId, cacheName, flags, op, key, portable));
        }
    }

    /**
     * Executes command on cache. Checks {@code destId} to find
     * if command could be performed locally or routed to a remote node.
     *
     * @param destId Target node Id for the operation.
     *      If {@code null} - operation could be executed anywhere.
     * @param clientId Client ID.
     * @param cacheName Cache name.
     * @param key Key to set affinity mapping in the response.
     * @param op Operation to perform.
     * @return Operation result in future.
     * @throws GridException If failed
     */
    private GridFuture<GridRestResponse> executeCommand(
        @Nullable UUID destId,
        UUID clientId,
        final String cacheName,
        final Object key,
        final CacheCommand op) throws GridException {
        final boolean locExec = destId == null || destId.equals(ctx.localNodeId()) ||
            ctx.cache().cache(cacheName) != null;

        if (locExec) {
            final GridCacheProjection<Object, Object> cache = localCache(cacheName).forSubjectId(clientId);

            return op.apply(cache, ctx).chain(resultWrapper(cache, key));
        }
        else {
            return ctx.grid().forPredicate(F.nodeForNodeId(destId)).compute().withNoFailover().
                call(new CacheOperationCallable(clientId, cacheName, op, key));
        }
    }

    /**
     * Handles increment and decrement commands.
     *
     * @param cache Cache.
     * @param key Key.
     * @param req Request.
     * @param decr Whether to decrement (increment otherwise).
     * @return Future of operation result.
     * @throws GridException In case of error.
     */
    private static GridFuture<?> incrementOrDecrement(GridCacheProjection<Object, Object> cache, String key,
        GridRestCacheRequest req, final boolean decr) throws GridException {
        assert cache != null;
        assert key != null;
        assert req != null;

        Long init = req.initial();
        Long delta = req.delta();

        if (delta == null)
            throw new GridException(GridRestCommandHandlerAdapter.missingParameter("delta"));

        final GridCacheAtomicLong l = cache.cache().dataStructures().atomicLong(key, init != null ? init : 0, true);

        final Long d = delta;

        return ((GridKernal)cache.gridProjection().grid()).context().closure().callLocalSafe(new Callable<Object>() {
            @Override public Object call() throws Exception {
                return l.addAndGet(decr ? -d : d);
            }
        }, false);
    }

    /**
     * Handles append and prepend commands.
     *
     * @param ctx Kernal context.
     * @param cache Cache.
     * @param key Key.
     * @param req Request.
     * @param prepend Whether to prepend.
     * @return Future of operation result.
     * @throws GridException In case of any exception.
     */
    private static GridFuture<?> appendOrPrepend(
        final GridKernalContext ctx,
        final GridCacheProjection<Object, Object> cache,
        final Object key, GridRestCacheRequest req, final boolean prepend) throws GridException {
        assert cache != null;
        assert key != null;
        assert req != null;

        final Object val = req.value();

        if (val == null)
            throw new GridException(GridRestCommandHandlerAdapter.missingParameter("val"));

        return ctx.closure().callLocalSafe(new Callable<Object>() {
            @Override public Object call() throws Exception {
                try (GridCacheTx tx = cache.txStart(PESSIMISTIC, REPEATABLE_READ)) {
                    Object curVal = cache.get(key);

                    if (curVal == null)
                        return false;

                    // Modify current value with appendix one.
                    Object newVal = appendOrPrepend(curVal, val, !prepend);

                    // Put new value asynchronously.
                    cache.putx(key, newVal);

                    tx.commit();
                }

                return true;
            }
        }, false);
    }

    /**
     * Append or prepend new value to the current one.
     *
     * @param origVal Original value.
     * @param appendVal Appendix value to add to the original one.
     * @param appendPlc Append or prepend policy flag.
     * @return Resulting value.
     * @throws GridException In case of grid exceptions.
     */
    private static Object appendOrPrepend(Object origVal, Object appendVal, boolean appendPlc) throws GridException {
        // Strings.
        if (appendVal instanceof String && origVal instanceof String)
            return appendPlc ? origVal + (String)appendVal : (String)appendVal + origVal;

        // Maps.
        if (appendVal instanceof Map && origVal instanceof Map) {
            Map<Object, Object> origMap = (Map<Object, Object>)origVal;
            Map<Object, Object> appendMap = (Map<Object, Object>)appendVal;

            Map<Object, Object> map = X.cloneObject(origMap, false, true);

            if (appendPlc)
                map.putAll(appendMap); // Append.
            else {
                map.clear();
                map.putAll(appendMap); // Prepend.
                map.putAll(origMap);
            }

            for (Map.Entry<Object, Object> e : appendMap.entrySet()) // Remove zero-valued entries.
                if (e.getValue() == null && map.get(e.getKey()) == null)
                    map.remove(e.getKey());

            return map;
        }

        // Generic collection.
        if (appendVal instanceof Collection<?> && origVal instanceof Collection<?>) {
            Collection<Object> origCol = (Collection<Object>)origVal;
            Collection<Object> appendCol = (Collection<Object>)appendVal;

            Collection<Object> col = X.cloneObject(origCol, false, true);

            if (appendPlc)
                col.addAll(appendCol); // Append.
            else {
                col.clear();
                col.addAll(appendCol); // Prepend.
                col.addAll(origCol);
            }

            return col;
        }

        throw new GridException("Incompatible types [appendVal=" + appendVal + ", old=" + origVal + ']');
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridCacheCommandHandler.class, this);
    }

    /**
     * Creates a transformation function from {@link CacheCommand}'s results into {@link GridRestResponse}.
     *
     * @param c Cache instance to obtain affinity data.
     * @param key Affinity key for previous operation.
     * @return Rest response.
     */
    private static GridClosure<GridFuture<?>, GridRestResponse> resultWrapper(
        final GridCacheProjection<Object, Object> c, @Nullable final Object key) {
        return new CX1<GridFuture<?>, GridRestResponse>() {
            @Override public GridRestResponse applyx(GridFuture<?> f) throws GridException {
                GridCacheRestResponse resp = new GridCacheRestResponse();

                resp.setResponse(f.get());

                if (key != null)
                    resp.setAffinityNodeId(c.cache().affinity().mapKeyToNode(key).id().toString());

                return resp;
            }
        };
    }

    /**
     * @param cacheName Cache name.
     * @return If replicated cache with given name is locally available.
     */
    private boolean replicatedCacheAvailable(String cacheName) {
        GridCacheAdapter<Object,Object> cache = ctx.cache().internalCache(cacheName);

        return cache != null && cache.configuration().getCacheMode() == GridCacheMode.REPLICATED;
    }

    /**
     * Used for test purposes.
     *
     * @param cacheName Name of the cache.
     * @return Instance on the named cache.
     * @throws GridException If cache not found.
     */
    protected GridCacheProjectionEx<Object, Object> localCache(String cacheName) throws GridException {
        GridCacheProjectionEx<Object, Object> cache = (GridCacheProjectionEx<Object, Object>)ctx.cache().cache(cacheName);

        if (cache == null)
            throw new GridException(
                "Failed to find cache for given cache name (null for default cache): " + cacheName);

        return cache;
    }

    /**
     * @param grid Grid instance.
     * @param cacheName Name of the cache.
     * @return Instance on the named cache.
     * @throws GridException If cache not found.
     */
    private static GridCacheProjectionEx<Object, Object> cache(Grid grid, String cacheName) throws GridException {
        GridCache<Object, Object> cache = grid.cache(cacheName);

        if (cache == null)
            throw new GridException(
                "Failed to find cache for given cache name (null for default cache): " + cacheName);

        return (GridCacheProjectionEx<Object, Object>)cache;
    }

    /**
     * Fixed result closure.
     */
    private static final class FixedResult extends CX1<GridFuture<?>, Object> {
        /** */
        private static final long serialVersionUID = 0L;

        /** Closure result. */
        private final Object res;

        /**
         * @param res Closure result.
         */
        private FixedResult(Object res) {
            this.res = res;
        }

        /** {@inheritDoc} */
        @Override public Object applyx(GridFuture<?> f) throws GridException {
            f.get();

            return res;
        }
    }

    /**
     * Type alias.
     */
    private abstract static class CacheCommand
        extends GridClosure2X<GridCacheProjection<Object, Object>, GridKernalContext, GridFuture<?>> {
        /** */
        private static final long serialVersionUID = 0L;

        // No-op.
    }

    /**
     * Type alias.
     */
    private abstract static class CacheProjectionCommand
        extends GridClosure2X<GridCacheProjection<Object, Object>, GridKernalContext, GridFuture<?>> {
        /** */
        private static final long serialVersionUID = 0L;

        // No-op.
    }

    /**
     * Class for flagged cache operations.
     */
    @GridInternal
    private static class FlaggedCacheOperationCallable implements Callable<GridRestResponse>, Serializable {
        /** */
        private static final long serialVersionUID = 0L;

        /** Client ID. */
        private UUID clientId;

        /** */
        private final String cacheName;

        /** */
        private final GridCacheFlag[] flags;

        /** */
        private final CacheProjectionCommand op;

        /** */
        private final Object key;

        /** */
        private final boolean portable;

        /** */
        @GridInstanceResource
        private Grid g;

        /**
         * @param cacheName Cache name.
         * @param flags Flags.
         * @param op Operation.
         * @param key Key.
         */
        private FlaggedCacheOperationCallable(UUID clientId, String cacheName, GridCacheFlag[] flags,
            CacheProjectionCommand op, Object key, boolean portable) {
            this.clientId = clientId;
            this.cacheName = cacheName;
            this.flags = flags;
            this.op = op;
            this.key = key;
            this.portable = portable;
        }

        /** {@inheritDoc} */
        @Override public GridRestResponse call() throws Exception {
            GridCacheProjection<?, ?> prj = cache(g, cacheName).forSubjectId(clientId).flagsOn(flags);

            if (portable)
                prj = prj.keepPortable();

            // Need to apply both operation and response transformation remotely
            // as cache could be inaccessible on local node and
            // exception processing should be consistent with local execution.
            return op.apply((GridCacheProjection<Object, Object>)prj, ((GridKernal)g).context()).
                chain(resultWrapper((GridCacheProjection<Object, Object>)prj, key)).get();
        }
    }

    /**
     * Class for cache operations.
     */
    @GridInternal
    private static class CacheOperationCallable implements Callable<GridRestResponse>, Serializable {
        /** */
        private static final long serialVersionUID = 0L;

        /** Client ID. */
        private UUID clientId;

        /** */
        private final String cacheName;

        /** */
        private final CacheCommand op;

        /** */
        private final Object key;

        /** */
        @GridInstanceResource
        private Grid g;

        /**
         * @param cacheName Cache name.
         * @param op Operation.
         * @param key Key.
         */
        private CacheOperationCallable(UUID clientId, String cacheName, CacheCommand op, Object key) {
            this.clientId = clientId;
            this.cacheName = cacheName;
            this.op = op;
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public GridRestResponse call() throws Exception {
            final GridCacheProjection<Object, Object> cache = cache(g, cacheName).forSubjectId(clientId);

            // Need to apply both operation and response transformation remotely
            // as cache could be inaccessible on local node and
            // exception processing should be consistent with local execution.
            return op.apply(cache, ((GridKernal)g).context()).chain(resultWrapper(cache, key)).get();
        }
    }

    /** */
    private static class GetCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /**
         * @param key Key.
         */
        GetCommand(Object key) {
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            return c.getAsync(key);
        }
    }

    /** */
    private static class GetAllCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Collection<Object> keys;

        /**
         * @param keys Keys.
         */
        GetAllCommand(Collection<Object> keys) {
            this.keys = keys;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            return c.getAllAsync(keys);
        }
    }

    /** */
    private static class PutAllCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Map<Object, Object> map;

        /**
         * @param map Objects to put.
         */
        PutAllCommand(Map<Object, Object> map) {
            this.map = map;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            return c.putAllAsync(map).chain(new FixedResult(true));
        }
    }

    /** */
    private static class RemoveCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /**
         * @param key Key.
         */
        RemoveCommand(Object key) {
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            return c.removexAsync(key);
        }
    }

    /** */
    private static class RemoveAllCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Collection<Object> keys;

        /**
         * @param keys Keys to remove.
         */
        RemoveAllCommand(Collection<Object> keys) {
            this.keys = keys;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            return (F.isEmpty(keys) ? c.removeAllAsync() : c.removeAllAsync(keys))
                .chain(new FixedResult(true));
        }
    }

    /** */
    private static class CasCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object exp;

        /** */
        private final Object val;

        /** */
        private final Object key;

        /**
         * @param exp Expected previous value.
         * @param val New value.
         * @param key Key.
         */
        CasCommand(Object exp, Object val, Object key) {
            this.val = val;
            this.exp = exp;
            this.key = key;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            return exp == null && val == null ? c.removexAsync(key) :
                exp == null ? c.putxIfAbsentAsync(key, val) :
                    val == null ? c.removeAsync(key, exp) :
                        c.replaceAsync(key, exp, val);
        }
    }

    /** */
    private static class PutCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /** */
        private final Long ttl;

        /** */
        private final Object val;

        /**
         * @param key Key.
         * @param ttl TTL.
         * @param val Value.
         */
        PutCommand(Object key, Long ttl, Object val) {
            this.key = key;
            this.ttl = ttl;
            this.val = val;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            if (ttl != null) {
                GridCacheEntry<Object, Object> entry = c.entry(key);

                if (entry != null) {
                    entry.timeToLive(ttl);

                    return entry.setxAsync(val);
                }
                else
                    return new GridFinishedFuture<Object>(ctx, false);
            }
            else
                return c.putxAsync(key, val);
        }
    }

    /** */
    private static class AddCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /** */
        private final Long ttl;

        /** */
        private final Object val;

        /**
         * @param key Key.
         * @param ttl TTL.
         * @param val Value.
         */
        AddCommand(Object key, Long ttl, Object val) {
            this.key = key;
            this.ttl = ttl;
            this.val = val;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            GridCacheEntry<Object, Object> entry = c.entry(key);

            if (entry != null) {
                if (ttl != null)
                    entry.timeToLive(ttl);

                return entry.setxIfAbsentAsync(val);
            }
            else
                return new GridFinishedFuture<Object>(ctx, false);
        }
    }

    /** */
    private static class ReplaceCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /** */
        private final Long ttl;

        /** */
        private final Object val;

        /**
         * @param key Key.
         * @param ttl TTL.
         * @param val Value.
         */
        ReplaceCommand(Object key, Long ttl, Object val) {
            this.key = key;
            this.ttl = ttl;
            this.val = val;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            GridCacheEntry<Object, Object> entry = c.entry(key);

            if (entry != null) {
                if (ttl != null)
                    entry.timeToLive(ttl);

                return entry.replacexAsync(val);
            }
            else
                return new GridFinishedFuture<Object>(ctx, false);
        }
    }

    /** */
    private static class IncrementCommand extends CacheCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /** */
        private final GridRestCacheRequest req;

        /**
         * @param key Key.
         * @param req Operation request.
         */
        IncrementCommand(Object key, GridRestCacheRequest req) {
            this.key = key;
            this.req = req;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx)
            throws GridException {
            return incrementOrDecrement(c, (String)key, req, false);
        }
    }

    /** */
    private static class DecrementCommand extends CacheCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /** */
        private final GridRestCacheRequest req;

        /**
         * @param key Key.
         * @param req Operation request.
         */
        DecrementCommand(Object key, GridRestCacheRequest req) {
            this.key = key;
            this.req = req;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) throws GridException {
            return incrementOrDecrement(c, (String)key, req, true);
        }
    }

    /** */
    private static class AppendCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /** */
        private final GridRestCacheRequest req;

        /**
         * @param key Key.
         * @param req Operation request.
         */
        AppendCommand(Object key, GridRestCacheRequest req) {
            this.key = key;
            this.req = req;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx)
            throws GridException {
            return appendOrPrepend(ctx, c, key, req, false);
        }
    }

    /** */
    private static class PrependCommand extends CacheProjectionCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** */
        private final Object key;

        /** */
        private final GridRestCacheRequest req;

        /**
         * @param key Key.
         * @param req Operation request.
         */
        PrependCommand(Object key, GridRestCacheRequest req) {
            this.key = key;
            this.req = req;
        }

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx)
            throws GridException {
            return appendOrPrepend(ctx, c, key, req, true);
        }
    }

    /** */
    private static class MetricsCommand extends CacheCommand {
        /** */
        private static final long serialVersionUID = 0L;

        /** {@inheritDoc} */
        @Override public GridFuture<?> applyx(GridCacheProjection<Object, Object> c, GridKernalContext ctx) {
            GridCacheMetrics metrics = c.cache().metrics();

            assert metrics != null;

            return new GridFinishedFuture<Object>(ctx, new GridCacheRestMetrics(
                metrics.createTime(), metrics.readTime(), metrics.writeTime(),
                metrics.reads(), metrics.writes(), metrics.hits(), metrics.misses()));
        }
    }
}
