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

#ifndef GRID_SOCKET_ADDRESS_HPP_INCLUDED
#define GRID_SOCKET_ADDRESS_HPP_INCLUDED

#include <string>

/**
 * Grid host + port address holder.
 */
class GridClientSocketAddress {
public:
    /**
     * Public constructor.
     *
     * @param host Host address.
     * @param port Host port.
     */
    GridClientSocketAddress(const std::string& host, int port) : host_(host), port_(port) {}

    /**
     * Copy constructor.
     *
     * @param peer Address to copy data from.
     */
    GridClientSocketAddress(const GridClientSocketAddress& peer) : host_(peer.host_), port_(peer.port_) {}

    /**
     * Getter method for the host.
     *
     * @return Host set in the constructor.
     */
    std::string host() const { return host_; }

    /**
     * Getter method for the port.
     *
     * @return Port set in constructor.
     */
    int port() const { return port_; }

private:
    /** Host. */
    std::string host_;

    /** Port. */
    int port_;
};

#endif
