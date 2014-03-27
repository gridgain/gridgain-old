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

/**
 * \mainpage GridGain C++ Client Library
 *
 * \section intro Introduction.
 *
 * C++ Client is a lightweight gateway to GridGain nodes. Client communicates with grid nodes via REST or TCP binary
 * protocol and provides reduced but powerful subset of GridGain API. C++ Client allows to use GridGain features from
 * devices and environments where fully-functional GridGain node could not (or should not) be started.
 * <p>
 * To get started please take a look at {@link GridClientFactory}.
 * <p>
 * The examples are available in the examples/cpp directory.
 */
#ifndef GRIDGAIN_HPP__INCLUDED
#define GRIDGAIN_HPP__INCLUDED

#include <gridgain/gridconf.hpp>
#include <gridgain/gridclientprotocolconfiguration.hpp>
#include <gridgain/gridclientcompute.hpp>
#include <gridgain/gridclientdata.hpp>
#include <gridgain/gridclientnode.hpp>
#include <gridgain/gridclient.hpp>
#include <gridgain/loadbalancer/gridclientrandombalancer.hpp>
#include <gridgain/loadbalancer/gridclientroundrobinbalancer.hpp>
#include <gridgain/gridclientpredicate.hpp>
#include <gridgain/gridclientexception.hpp>
#include <gridgain/gridclientfactory.hpp>
#include <gridgain/gridclientvariant.hpp>
#include <gridgain/gridclientmodel.hpp>
#include <gridgain/gridclientdatametrics.hpp>
#include <gridgain/gridclientconfiguration.hpp>
#include <gridgain/gridfuture.hpp>

#endif // GRIDGAIN_HPP__INCLUDED
