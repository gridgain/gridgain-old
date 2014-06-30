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

#include <vector>

#include "gridgain/impl/hash/gridclientvarianthasheableobject.hpp"
#include "gridgain/impl/hash/gridclientsimpletypehasheableobject.hpp"
#include "gridgain/impl/hash/gridclientstringhasheableobject.hpp"
#include "gridgain/impl/hash/gridclientbytearrayshasheableobject.hpp"
#include "gridgain/impl/hash/gridclientboolhasheableobject.hpp"
#include "gridgain/impl/hash/gridclientfloathasheableobject.hpp"
#include "gridgain/impl/hash/gridclientdoublehasheableobject.hpp"

using namespace std;

template <class T> void getHashInfo(const T& val, int& hashCode, std::vector<int8_t>& bytes) {
    TGridHasheableObjectPtr simpleHasheable = createHasheable(val);

    bytes.clear();

    hashCode = simpleHasheable->hashCode();

    simpleHasheable->convertToBytes(bytes);
}

namespace {

class GridClientVariantVisitorImpl : public GridClientVariantVisitor {
public:
    GridClientVariantVisitorImpl(int& pHashCode, std::vector<int8_t>& pBytes)
        : hashCode_(pHashCode), bytes(pBytes) {
        hashCode_ = -1;

        bytes.clear();
    }

    virtual void visit(const int16_t pShort) const {
        getHashInfo(pShort, hashCode_, bytes);
    }

    virtual void visit(const int32_t pInt) const {
        getHashInfo(pInt, hashCode_, bytes);
    }

    virtual void visit(const int64_t pLong) const {
        getHashInfo(pLong, hashCode_, bytes);
    }

    virtual void visit(const bool pVal) const {
        GridBoolHasheableObject boolHasheable(pVal);

        hashCode_ = boolHasheable.hashCode();

        boolHasheable.convertToBytes(bytes);
    }

    virtual void visit(const double pVal) const {
        GridDoubleHasheableObject doubleHasheable(pVal);

        hashCode_ = doubleHasheable.hashCode();

        doubleHasheable.convertToBytes(bytes);
    }

    virtual void visit(const float pVal) const {
        GridFloatHasheableObject floatHasheable(pVal);

        hashCode_ = floatHasheable.hashCode();

        floatHasheable.convertToBytes(bytes);
    }

    virtual void visit(const string& pText) const {
        GridStringHasheableObject strHasheable(pText);

        hashCode_ = strHasheable.hashCode();

        strHasheable.convertToBytes(bytes);
    }

    virtual void visit(const std::wstring& pText) const {
        GridWideStringHasheableObject wstrHasheable(pText);

        hashCode_ = wstrHasheable.hashCode();

        wstrHasheable.convertToBytes(bytes);
    }

    virtual void visit(const vector<int8_t>& buf) const {
        GridByteArrayHasheableObject bytesHasheable(buf);

        hashCode_ = bytesHasheable.hashCode();

        bytesHasheable.convertToBytes(bytes);
    }

    virtual void visit(const vector<GridClientVariant>& vvec) const {
        hashCode_ = 1;

        for (auto i = vvec.begin(); i != vvec.end(); i++) {
            GridClientVariantHasheableObject o(*i);

            hashCode_ = 31 * hashCode_ + o.hashCode();

            vector<int8_t> b;
            o.convertToBytes(b);

            bytes.insert(bytes.end(), b.begin(), b.end());
        }
    }

    virtual void visit(const GridClientUuid& uuid) const {
        hashCode_ = uuid.hashCode();

        uuid.convertToBytes(bytes);
    }

private:
    int32_t& hashCode_;
    std::vector<int8_t>& bytes;
};

}

void GridClientVariantHasheableObject::init(const GridClientVariant& var) {
    GridClientVariantVisitorImpl vis(hashCode_, bytes);

    var.accept(vis);
}

GridClientVariantHasheableObject::GridClientVariantHasheableObject(const GridClientVariant& var) {
    init(var);
}

GridClientVariantHasheableObject::GridClientVariantHasheableObject(const GridClientVariant& var,
        int calculatedHashCode) {
    init(var);

    hashCode_ = calculatedHashCode;
}

