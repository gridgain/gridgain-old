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

package org.gridgain.grid.kernal.processors.streamer;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.grid.kernal.processors.license.*;
import org.gridgain.grid.streamer.*;
import org.gridgain.grid.streamer.index.*;
import org.gridgain.grid.streamer.window.*;
import org.gridgain.grid.util.typedef.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import javax.management.*;
import java.util.*;

import static org.gridgain.grid.product.GridProductEdition.*;
import static org.gridgain.grid.kernal.GridNodeAttributes.*;

/**
 *
 */
public class GridStreamProcessor extends GridProcessorAdapter {
    /** Streamers map. */
    private Map<String, GridStreamerImpl> map;

    /** Registered MBeans */
    private Collection<ObjectName> mBeans;

    /** MBean server. */
    private final MBeanServer mBeanSrv;

    /**
     * @param ctx Kernal context.
     */
    public GridStreamProcessor(GridKernalContext ctx) {
        super(ctx);

        mBeanSrv = ctx.config().getMBeanServer();
    }

    /** {@inheritDoc} */
    @Override public void onKernalStart() throws GridException {
        if (ctx.config().isDaemon())
            return;

        super.onKernalStart();

        for (GridNode n : ctx.discovery().remoteNodes())
            checkStreamer(n);

        for (GridStreamerImpl s : map.values()) {
            try {
                mBeans.add(U.registerMBean(mBeanSrv, ctx.gridName(), U.maskName(s.name()), "Streamer",
                    new GridStreamerMBeanAdapter(s), GridStreamerMBean.class));

                if (log.isDebugEnabled())
                    log.debug("Registered MBean for streamer: " + s.name());
            }
            catch (JMException e) {
                U.error(log, "Failed to register streamer MBean: " + s.name(), e);
            }

            // Add mbeans for stages.
            for (GridStreamerStage stage : s.configuration().getStages()) {
                try {
                    mBeans.add(U.registerMBean(mBeanSrv, ctx.gridName(), U.maskName(s.name()), "Stage-" + stage.name(),
                        new GridStreamerStageMBeanAdapter(stage.name(), stage.getClass().getName(), s),
                        GridStreamerStageMBean.class));

                    if (log.isDebugEnabled())
                        log.debug("Registered MBean for streamer stage [streamer=" + s.name() +
                            ", stage=" + stage.name() + ']');
                }
                catch (JMException e) {
                    U.error(log, "Failed to register streamer stage MBean [streamer=" + s.name() +
                        ", stage=" + stage.name() + ']', e);
                }
            }

            // Add mbeans for windows.
            for (GridStreamerWindow win : s.configuration().getWindows()) {
                try {
                    if (hasInterface(win.getClass(), GridStreamerWindowMBean.class)) {
                        mBeans.add(U.registerMBean(mBeanSrv, ctx.gridName(), U.maskName(s.name()),
                            "Window-" + win.name(),
                            (GridStreamerWindowMBean)win,
                            GridStreamerWindowMBean.class));

                        if (log.isDebugEnabled())
                            log.debug("Registered MBean for streamer window [streamer=" + s.name() +
                                ", window=" + win.name() + ']');
                    }
                }
                catch (JMException e) {
                    U.error(log, "Failed to register streamer window MBean [streamer=" + s.name() +
                        ", window=" + win.name() + ']', e);
                }

                if (win instanceof GridStreamerWindowAdapter) {
                    GridStreamerIndexProvider[] idxs = ((GridStreamerWindowAdapter)win).indexProviders();

                    if (idxs != null && idxs.length > 0) {
                        for (GridStreamerIndexProvider idx : idxs) {
                            try {
                                mBeans.add(U.registerMBean(mBeanSrv, ctx.gridName(), U.maskName(s.name()),
                                    "Window-" + win.name() + "-index-" + idx.name(), idx,
                                    GridStreamerIndexProviderMBean.class));

                                if (log.isDebugEnabled())
                                    log.debug("Registered MBean for streamer window index [streamer=" + s.name() +
                                        ", window=" + win.name() + ", index=" + idx.name() + ']');
                            }
                            catch (JMException e) {
                                U.error(log, "Failed to register streamer index MBean [streamer=" + s.name() +
                                    ", window=" + win.name() + ", index=" + idx.name() + ']', e);
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Check configuration identity on local and remote nodes.
     *
     * @param rmtNode Remote node to check.
     * @throws GridException If configuration mismatch detected.
     */
    private void checkStreamer(GridNode rmtNode) throws GridException {
        GridStreamerAttributes[] rmtAttrs = rmtNode.attribute(ATTR_STREAMER);
        GridStreamerAttributes[] locAttrs = ctx.discovery().localNode().attribute(ATTR_STREAMER);

        // If local or remote streamer is not configured, nothing to validate.
        if (F.isEmpty(locAttrs) || F.isEmpty(rmtAttrs))
            return;

        for (GridStreamerAttributes rmtAttr : rmtAttrs) {
            for (GridStreamerAttributes locAttr : locAttrs) {
                if (!F.eq(rmtAttr.name(), locAttr.name()))
                    continue;

                if (rmtAttr.atLeastOnce() != locAttr.atLeastOnce())
                    throw new GridException("Streamer atLeastOnce configuration flag mismatch (fix atLeastOnce flag " +
                        "in streamer configuration and restart) [streamer=" + locAttr.name() +
                        ", locAtLeastOnce=" + locAttr.atLeastOnce() +
                        ", rmtAtLeastOnce=" + rmtAttr.atLeastOnce() +
                        ", rmtNodeId=" + rmtNode.id() + ']');

                if (!rmtAttr.stages().equals(locAttr.stages()))
                    throw new GridException("Streamer stages configuration mismatch (fix streamer stages " +
                        "configuration and restart) [streamer=" + locAttr.name() +
                        ", locStages=" + locAttr.stages() +
                        ", rmtStages=" + rmtAttr.stages() +
                        ", rmtNodeId=" + rmtNode.id() + ']');

                if (rmtAttr.atLeastOnce()) {
                    if (rmtAttr.maxFailoverAttempts() != locAttr.maxFailoverAttempts())
                        U.warn(log, "Streamer maxFailoverAttempts configuration property differs on local and remote " +
                            "nodes (ignore this message if it is done on purpose) [streamer=" + locAttr.name() +
                            ", locMaxFailoverAttempts=" + locAttr.maxFailoverAttempts() +
                            ", rmtMaxFailoverAttempts=" + rmtAttr.maxFailoverAttempts() +
                            ", rmtNodeId=" + rmtNode.id() + ']');

                    if (rmtAttr.maxConcurrentSessions() != locAttr.maxConcurrentSessions())
                        U.warn(log, "Streamer maxConcurrentSessions configuration property differs on local and " +
                            "remote nodes (ignore this message if it is done on purpose) [streamer=" + locAttr.name() +
                            ", locMaxConcurrentSessions=" + locAttr.maxConcurrentSessions() +
                            ", rmtMaxConcurrentSessions=" + rmtAttr.maxConcurrentSessions() +
                            ", rmtNodeId=" + rmtNode.id() + ']');
                }
            }
        }
    }

    /**
     * Traverses class hierarchy and checks if class implements given interface.
     *
     * @param cls Class to check.
     * @param iface Interface to search for.
     * @return {@code True} if at least one parent implements given interface.
     */
    private boolean hasInterface(Class<?> cls, Class<?> iface) {
        while (cls != null) {
            Class<?>[] interfaces = cls.getInterfaces();

            for (Class<?> iface0 : interfaces) {
                if (iface0.equals(iface))
                    return true;
            }

            cls = cls.getSuperclass();
        }

        return false;
    }

    /** {@inheritDoc} */
    @Override public void start() throws GridException {
        if (ctx.config().isDaemon())
            return;

        super.start();

        GridStreamerConfiguration[] cfg = ctx.config().getStreamerConfiguration();

        if (F.isEmpty(cfg)) {
            map = Collections.emptyMap();

            return;
        }
        else {
            int len = cfg.length;

            map = new HashMap<>(len, 1.0f);

            mBeans = new ArrayList<>(len);
        }

        for (GridStreamerConfiguration c : cfg) {
            // Register streaming usage with license manager.
            GridLicenseUseRegistry.onUsage(STREAMING, getClass());

            GridStreamerImpl s = new GridStreamerImpl(ctx, c);

            s.start();

            GridStreamerImpl old = map.put(c.getName(), s);

            if (old != null) {
                old.stop(true);

                throw new GridException("Duplicate streamer name found (check configuration and " +
                    "assign unique name to each streamer): " + c.getName());
            }
        }
    }

    /** {@inheritDoc} */
    @Override public void onKernalStop(boolean cancel) {
        if (ctx.config().isDaemon())
            return;

        super.onKernalStop(cancel);

        if (!F.isEmpty(mBeans)) {
            for (ObjectName name : mBeans) {
                try {
                    mBeanSrv.unregisterMBean(name);
                }
                catch (JMException e) {
                    U.error(log, "Failed to unregister streamer MBean.", e);
                }
            }
        }

        for (GridStreamerImpl streamer : map.values())
            streamer.onKernalStop(cancel);
    }

    /** {@inheritDoc} */
    @Override public void stop(boolean cancel) throws GridException {
        if (ctx.config().isDaemon())
            return;

        super.stop(cancel);

        for (GridStreamerImpl s : map.values())
            s.stop(cancel);
    }

    /** {@inheritDoc} */
    @Override public void addAttributes(Map<String, Object> attrs) throws GridException {
        super.addAttributes(attrs);

        GridStreamerConfiguration[] cfg = ctx.config().getStreamerConfiguration();

        if (F.isEmpty(cfg))
            return;

        GridStreamerAttributes[] arr = new GridStreamerAttributes[cfg.length];

        int i = 0;

        for (GridStreamerConfiguration c : cfg)
            arr[i++] = new GridStreamerAttributes(c);

        attrs.put(ATTR_STREAMER, arr);
    }

    /**
     * @return Default no-name streamer.
     */
    public GridStreamer streamer() {
        return streamer(null);
    }

    /**
     * @param name Streamer name.
     * @return Streamer for given name.
     */
    public GridStreamer streamer(@Nullable String name) {
        GridStreamer streamer = map.get(name);

        if (streamer == null)
            throw new IllegalArgumentException("Streamer is not configured: " + name);

        return streamer;
    }

    /**
     * @return All configured streamers.
     */
    public Collection<GridStreamer> streamers() {
        Collection<GridStreamerImpl> streamers = map.values();

        Collection<GridStreamer> res = new ArrayList<>(streamers.size());

        streamers.addAll(map.values());

        return res;
    }

    /**
     * Callback for undeployed class loaders.
     *
     * @param leftNodeId Left node ID.
     * @param ldr Class loader.
     */
    public void onUndeployed(UUID leftNodeId, ClassLoader ldr) {
        for (GridStreamerEx streamer : map.values())
            streamer.onUndeploy(leftNodeId, ldr);
    }
}
