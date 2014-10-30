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

package org.gridgain.grid.hadoop;

import org.gridgain.grid.*;
import org.gridgain.grid.kernal.processors.hadoop.*;
import org.gridgain.grid.kernal.processors.hadoop.v2.*;
import org.gridgain.grid.logger.*;
import org.gridgain.grid.util.typedef.internal.*;
import org.jetbrains.annotations.*;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;

/**
 * Hadoop job info based on default Hadoop configuration.
 */
public class GridHadoopDefaultJobInfo implements GridHadoopJobInfo, Externalizable {
    /** */
    private static final long serialVersionUID = 5489900236464999951L;

    /** {@code true} If job has combiner. */
    private boolean hasCombiner;

    /** Number of reducers configured for job. */
    private int numReduces;

    /** Configuration. */
    private Map<String,String> props = new HashMap<>();

    /** Job name. */
    private String jobName;

    /** User name. */
    private String user;

    /** */
    private static volatile Class<?> jobCls;

    /**
     * Default constructor required by {@link Externalizable}.
     */
    public GridHadoopDefaultJobInfo() {
        // No-op.
    }

    /**
     * Constructor.
     *
     * @param jobName Job name.
     * @param user User name.
     * @param hasCombiner {@code true} If job has combiner.
     * @param numReduces Number of reducers configured for job.
     * @param props All other properties of the job.
     */
    public GridHadoopDefaultJobInfo(String jobName, String user, boolean hasCombiner, int numReduces,
        Map<String, String> props) {
        this.jobName = jobName;
        this.user = user;
        this.hasCombiner = hasCombiner;
        this.numReduces = numReduces;
        this.props = props;
    }

    /** {@inheritDoc} */
    @Nullable @Override public String property(String name) {
        return props.get(name);
    }

    /** {@inheritDoc} */
    @Override public GridHadoopJob createJob(GridHadoopJobId jobId, GridLogger log) throws GridException {
        try {
            Class<?> jobCls0 = jobCls;

            if (jobCls0 == null) { // It is enough to have only one class loader with only Hadoop classes.
                synchronized (GridHadoopDefaultJobInfo.class) {
                    if ((jobCls0 = jobCls) == null) {
                        GridHadoopClassLoader ldr = new GridHadoopClassLoader(null);

                        jobCls = jobCls0 = ldr.loadClass(GridHadoopV2Job.class.getName());
                    }
                }
            }

            Constructor<?> constructor = jobCls0.getConstructor(GridHadoopJobId.class, GridHadoopDefaultJobInfo.class,
                GridLogger.class);

            return (GridHadoopJob)constructor.newInstance(jobId, this, log);
        }
        catch (Exception e) {
            throw new GridException(e);
        }
    }

    /** {@inheritDoc} */
    @Override public boolean hasCombiner() {
        return hasCombiner;
    }

    /** {@inheritDoc} */
    @Override public boolean hasReducer() {
        return reducers() > 0;
    }

    /** {@inheritDoc} */
    @Override public int reducers() {
        return numReduces;
    }

    /** {@inheritDoc} */
    @Override public String jobName() {
        return jobName;
    }

    /** {@inheritDoc} */
    @Override public String user() {
        return user;
    }

    /** {@inheritDoc} */
    @Override public void writeExternal(ObjectOutput out) throws IOException {
        U.writeString(out, jobName);
        U.writeString(out, user);

        out.writeBoolean(hasCombiner);
        out.writeInt(numReduces);

        U.writeStringMap(out, props);
    }

    /** {@inheritDoc} */
    @Override public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
        jobName = U.readString(in);
        user = U.readString(in);

        hasCombiner = in.readBoolean();
        numReduces = in.readInt();

        props = U.readStringMap(in);
    }

    /**
     * @return Properties of the job.
     */
    public Map<String, String> properties() {
        return props;
    }
}
