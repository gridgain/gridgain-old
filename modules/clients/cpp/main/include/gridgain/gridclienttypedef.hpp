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

#ifndef GRID_CLIENT_TYPEDEF_HPP_INCLUDED
#define GRID_CLIENT_TYPEDEF_HPP_INCLUDED

#include <map>
#include <vector>
#include <memory>

class GridClient;
class GridClientNode;
class GridClientData;
class GridClientSharedData;
class GridClientCompute;
class GridClientTopologyListener;
class GridClientLoadBalancer;
class GridClientVariant;
class GridClientDataMetrics;
class GridClientSocketAddress;
class GridClientBoolFuture;
class GridClientRouterBalancer;
template <class T> class GridClientFuture;
template<class T> class GridClientPredicate;

typedef GridClientPredicate<GridClientNode> TGridClientNodePredicate;

typedef std::shared_ptr<GridClientBoolFuture> TGridBoolFuturePtr;
typedef std::shared_ptr<GridClient> TGridClientPtr;
typedef std::shared_ptr<GridClientNode> TGridClientNodePtr;
typedef std::shared_ptr<GridClientData> TGridClientDataPtr;
typedef std::shared_ptr<GridClientSharedData> TGridClientSharedDataPtr;
typedef std::shared_ptr<GridClientCompute> TGridClientComputePtr;
typedef std::shared_ptr<GridClientLoadBalancer> TGridClientLoadBalancerPtr;
typedef std::shared_ptr<GridClientTopologyListener> TGridClientTopologyListenerPtr;
typedef std::shared_ptr<TGridClientNodePredicate> TGridClientNodePredicatePtr;
typedef std::shared_ptr<GridClientRouterBalancer> TGridClientRouterBalancerPtr;

typedef std::map<GridClientVariant, GridClientVariant> TGridClientVariantMap;
typedef std::vector<GridClientVariant> TGridClientVariantSet;
typedef std::vector<TGridClientNodePtr> TGridClientNodeList;
typedef std::vector<GridClientSocketAddress> TGridClientSocketAddressList;

typedef std::shared_ptr<GridClientFuture<TGridClientVariantMap> > TGridClientFutureVariantMap;
typedef std::shared_ptr<GridClientFuture<GridClientDataMetrics> > TGridClientFutureDataMetrics;
typedef std::shared_ptr<GridClientFuture<GridClientVariant> > TGridClientFutureVariant;
typedef std::shared_ptr<GridClientFuture<TGridClientNodePtr> > TGridClientNodeFuturePtr;
typedef std::shared_ptr<GridClientFuture<TGridClientNodeList> > TGridClientNodeFutureList;
typedef std::shared_ptr<GridClientFuture<std::vector<std::string> > > TGridFutureStringList;

typedef std::vector<TGridClientTopologyListenerPtr> TGridClientTopologyListenerList;

typedef std::shared_ptr<TGridClientTopologyListenerList> TGridClientTopologyListenerListPtr;

#ifdef _MSC_VER
#include <boost/atomic.hpp>

typedef boost::atomic_bool TGridAtomicBool;
typedef boost::atomic_int TGridAtomicInt;
#else
#include <atomic>

typedef std::atomic_bool TGridAtomicBool;
typedef std::atomic_int TGridAtomicInt;
#endif

#endif // end of header define
