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

#ifndef GRIDCLIENTDATACONFIGURATION_HPP_
#define GRIDCLIENTDATACONFIGURATION_HPP_

#include <gridgain/gridconf.hpp>
#include <gridgain/gridclientdataaffinity.hpp>

#include <string>
#include <memory>

/**
 * Client cache configuration class.
 */
class GRIDGAIN_API GridClientDataConfiguration {
public:
    /**
     * Default constructor.
     */
    GridClientDataConfiguration();

    /**
     * Copy constructor.
     */
    GridClientDataConfiguration(const GridClientDataConfiguration& cfg);

    /**
     * Assignment operator.
     *
     * @param right Right hand side object to copy data from.
     * @return This object for chaining.
     */
    GridClientDataConfiguration& operator=(const GridClientDataConfiguration& right);

    /**
     * Destructor.
     */
    ~GridClientDataConfiguration();

    /**
     * Gets cache name.
     *
     * @return Cache name.
     */
    std::string name() const;

    /**
     * Sets cache name.
     *
     * @param name Cache name.
     */
    void name(const std::string& name);

    /**
     * Gets cache affinity.
     *
     * @return Cache affinity.
     */
    std::shared_ptr<GridClientDataAffinity> affinity();

    /**
     * Sets cache affinity.
     *
     * @param aff Cache affinity.
     */
    void affinity(const std::shared_ptr<GridClientDataAffinity>& aff);

private:
    /** Implementation. */
    class Impl;
    Impl* pimpl;
};

#endif /* GRIDCLIENTDATACONFIGURATION_HPP_ */
