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
#include "gridgain/impl/utils/gridclientdebug.hpp"

#include <sstream>
#include <iterator>

#include <boost/uuid/uuid_generators.hpp>
#include <boost/uuid/uuid.hpp>
#include <boost/uuid/uuid_io.hpp>
#include <boost/thread.hpp>

#include "gridgain/gridclientuuid.hpp"

using namespace std;

static boost::uuids::random_generator uuidGen;
static boost::mutex uuidGenMux;

GridClientUuid::GridClientUuid() {
    memset(pimpl.uuid_.data, 0, pimpl.uuid_.size());
}

GridClientUuid::GridClientUuid(const char* str)  {
    if (str != NULL) {
        std::stringstream ss;

        ss << str;

        ss >> pimpl.uuid_;
    }
    else
        memset(pimpl.uuid_.data, 0, pimpl.uuid_.size());
}

GridClientUuid::GridClientUuid(const string& str)  {
    std::stringstream ss;

    ss << str;

    ss >> pimpl.uuid_;
}

GridClientUuid::GridClientUuid(const GridClientUuid& other) {
    pimpl.uuid_ = other.pimpl.uuid_;
}

GridClientUuid GridClientUuid::randomUuid() {
    GridClientUuid ret;

    {
        boost::lock_guard<boost::mutex> g(uuidGenMux);

        ret.pimpl.uuid_ = uuidGen();
    }

    return ret;
}

GridClientUuid& GridClientUuid::operator=(const GridClientUuid& rhs){
    if (this != &rhs)
        pimpl.uuid_ = rhs.pimpl.uuid_;

    return *this;
}

GridClientUuid::~GridClientUuid(){
}

std::string const GridClientUuid::uuid() const {
    std::stringstream ss;

    ss << pimpl.uuid_;

    return ss.str();
}

int64_t GridClientUuid::leastSignificantBits() const {
    int64_t lsb = pimpl.uuid_.data[8];

    for (int i = 9 ; i < 16 ; i++) {
        lsb <<= 8 ;
        lsb |= pimpl.uuid_.data[i];
    }

    return lsb;
}

int64_t GridClientUuid::mostSignificantBits() const {
    int64_t msb = pimpl.uuid_.data[0];

    for (int i = 1 ; i < 8 ; i++) {
        msb <<= 8 ;
        msb |= pimpl.uuid_.data[i];
    }

    return msb;
}

int32_t GridClientUuid::hashCode() const {
    int32_t hashCode = (int32_t)((mostSignificantBits() >> 32) ^
            mostSignificantBits() ^ (leastSignificantBits() >> 32) ^ leastSignificantBits());

    return hashCode;
}

void GridClientUuid::convertToBytes(vector<int8_t>& bytes) const {
    back_insert_iterator<vector<int8_t>> inserter=back_inserter(bytes);

    reverse_copy(&(pimpl.uuid_.data[0]), &(pimpl.uuid_.data[pimpl.uuid_.size()/2]), inserter);

    reverse_copy(&(pimpl.uuid_.data[pimpl.uuid_.size()/2]), &(pimpl.uuid_.data[pimpl.uuid_.size()]), inserter);
}

void GridClientUuid::rawBytes(vector<int8_t>& bytes) const {
    bytes.resize(pimpl.uuid_.size());

    copy(pimpl.uuid_.data, pimpl.uuid_.data + pimpl.uuid_.size(), bytes.begin());
}

GridClientUuid GridClientUuid::fromBytes(const string& bytes) {
    GridClientUuid ret;

    ::memcpy(ret.pimpl.uuid_.data, bytes.c_str(), ret.pimpl.uuid_.size());

    return ret;
}

bool GridClientUuid::operator < (const GridClientUuid& other) const {
    if (mostSignificantBits() < other.mostSignificantBits())
        return true;

    if (mostSignificantBits() > other.mostSignificantBits())
        return false;

    return leastSignificantBits() < other.leastSignificantBits();
}

bool GridClientUuid::operator == (const GridClientUuid& other) const {
    return pimpl.uuid_ == other.pimpl.uuid_;
}
