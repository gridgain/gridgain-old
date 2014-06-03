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


#include "gridgain/gridclientdataconfiguration.hpp"
#include "gridgain/impl/gridclientpartitionedaffinity.hpp"

/**
 * Implementation class.
 */
class GridClientDataConfiguration::Impl {
public:
    Impl(): aff(new GridClientPartitionAffinity()) {}

    Impl(const Impl& from): name(from.name), aff(from.aff) {}

    std::string name;

    std::shared_ptr<GridClientDataAffinity> aff;
};

GridClientDataConfiguration::GridClientDataConfiguration(): pimpl(new Impl()) {}

GridClientDataConfiguration::GridClientDataConfiguration(const GridClientDataConfiguration& cfg):
    pimpl(new Impl(*cfg.pimpl)) {}

GridClientDataConfiguration& GridClientDataConfiguration::operator=(const GridClientDataConfiguration& right) {
    if (this != &right) {
        delete pimpl;

        pimpl = new Impl(*right.pimpl);
    }

    return *this;
}

GridClientDataConfiguration::~GridClientDataConfiguration() {
    delete pimpl;
}

std::string GridClientDataConfiguration::name() const {
    return pimpl->name;
}

void GridClientDataConfiguration::name(const std::string& name) {
    pimpl->name = name;
}

std::shared_ptr<GridClientDataAffinity> GridClientDataConfiguration::affinity() {
    return pimpl->aff;
}

void GridClientDataConfiguration::affinity(const std::shared_ptr<GridClientDataAffinity>& aff) {
    pimpl->aff = aff;
}
