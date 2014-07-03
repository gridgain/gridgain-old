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

#ifndef GRID_CLIENT_FACTORY_FIXTURE_INCLUDED
#define GRID_CLIENT_FACTORY_FIXTURE_INCLUDED

#include "gridgain/gridclientfactory.hpp"

/**
 * Pointer to a function that returns an instance of GidClientConfiguration class.
 */
typedef GridClientConfiguration (*ClientConfigFunc)();

/**
 * This fixture is called before an after execution of a test case. 
 * It provides setUp/tearDown lifecycle of the GidGain client.
 */
template <ClientConfigFunc func>
class GridClientFactoryFixture1 {
public:
    GridClientFactoryFixture1() : client(GridClientFactory::start(func())) {
    }

    ~GridClientFactoryFixture1(){
        GridClientFactory::stopAll();
    }

    TGridClientPtr client;
};

/**
 * This fixture is called before an after execution of a test case.
 * It provides setUp/tearDown lifecycle of the GidGain client.
 */
class GridClientFactoryFixture2 {
public:
    /**
     * Stops the client.
     */
    ~GridClientFactoryFixture2() {
        GridClientFactory::stopAll();
    }

    /**
     * Instantiates the client from test configuration.
     *
     * @param cfg Test configuration.
     * @return New client instance.
     */
    template <class CfgT> TGridClientPtr client(const CfgT& cfg) {
        return GridClientFactory::start(cfg());
    }
};

#endif // GRID_CLIENT_FACTORY_FIXTURE_INCLUDED
