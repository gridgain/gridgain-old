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

package org.gridgain.grid.portables;

import org.gridgain.grid.util.typedef.internal.*;

import java.sql.*;

/**
 * Defines configuration properties for a specific portable type. Providing per-type
 * configuration is optional, as it is generally enough to provide global
 * portable configuration in {@link GridPortableConfiguration} instance. However,
 * this class allows you to change configuration properties for a specific
 * portable type without affecting configuration for other portable types.
 * <p>
 * Per-type portable configuration can be specified in
 * {@link GridPortableConfiguration#getTypeConfigurations()} method.
 */
public class GridPortableTypeConfiguration {
    /** Class name. */
    private String clsName;

    /** ID mapper. */
    private GridPortableIdMapper idMapper;

    /** Serializer. */
    private GridPortableSerializer serializer;

    /** Use timestamp flag. */
    private Boolean useTs;

    /** Meta data enabled flag. */
    private Boolean metaDataEnabled;

    /** Keep deserialized flag. */
    private Boolean keepDeserialized;

    /** Affinity key field name. */
    private String affKeyFieldName;

    /**
     */
    public GridPortableTypeConfiguration() {
        // No-op.
    }

    /**
     * @param clsName Class name.
     */
    public GridPortableTypeConfiguration(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Gets type name.
     *
     * @return Type name.
     */
    public String getClassName() {
        return clsName;
    }

    /**
     * Sets type name.
     *
     * @param clsName Type name.
     */
    public void setClassName(String clsName) {
        this.clsName = clsName;
    }

    /**
     * Gets ID mapper.
     *
     * @return ID mapper.
     */
    public GridPortableIdMapper getIdMapper() {
        return idMapper;
    }

    /**
     * Sets ID mapper.
     *
     * @param idMapper ID mapper.
     */
    public void setIdMapper(GridPortableIdMapper idMapper) {
        this.idMapper = idMapper;
    }

    /**
     * Gets serializer.
     *
     * @return Serializer.
     */
    public GridPortableSerializer getSerializer() {
        return serializer;
    }

    /**
     * Sets serializer.
     *
     * @param serializer Serializer.
     */
    public void setSerializer(GridPortableSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * If {@code true} then date values converted to {@link Timestamp} during unmarshalling.
     *
     * @return Flag indicating whether date values converted to {@link Timestamp} during unmarshalling.
     */
    public Boolean isUseTimestamp() {
        return useTs;
    }

    /**
     * @param useTs Flag indicating whether date values converted to {@link Timestamp} during unmarshalling.
     */
    public void setUseTimestamp(Boolean useTs) {
        this.useTs = useTs;
    }

    /**
     * Defines whether meta data is collected for this type. If provided, this value will override
     * {@link GridPortableConfiguration#isMetaDataEnabled()} property.
     *
     * @return Whether meta data is collected.
     */
    public Boolean isMetaDataEnabled() {
        return metaDataEnabled;
    }

    /**
     * @param metaDataEnabled Whether meta data is collected.
     */
    public void setMetaDataEnabled(Boolean metaDataEnabled) {
        this.metaDataEnabled = metaDataEnabled;
    }

    /**
     * Defines whether {@link GridPortableObject} should cache deserialized instance. If provided,
     * this value will override {@link GridPortableConfiguration#isKeepDeserialized()} property.
     *
     * @return Whether deserialized value is kept.
     */
    public Boolean isKeepDeserialized() {
        return keepDeserialized;
    }

    /**
     * @param keepDeserialized Whether deserialized value is kept.
     */
    public void setKeepDeserialized(Boolean keepDeserialized) {
        this.keepDeserialized = keepDeserialized;
    }

    /**
     * Gets affinity key field name.
     *
     * @return Affinity key field name.
     */
    public String getAffinityKeyFieldName() {
        return affKeyFieldName;
    }

    /**
     * Sets affinity key field name.
     *
     * @param affKeyFieldName Affinity key field name.
     */
    public void setAffinityKeyFieldName(String affKeyFieldName) {
        this.affKeyFieldName = affKeyFieldName;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridPortableTypeConfiguration.class, this, super.toString());
    }
}
