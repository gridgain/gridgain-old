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

#ifndef GRIDFUTURE_HPP_
#define GRIDFUTURE_HPP_

#include <gridgain/gridconf.hpp>
#include <boost/date_time.hpp>

/** Future for operations that return boolean flag.
 */
class GRIDGAIN_API GridClientBoolFuture {
public:
    /** Destructor. */
    virtual ~GridClientBoolFuture() {}

    /**
     * Waits for future to finish and returns the result of operation.
     *
     * @return <tt>true</tt> if the operation succeeded, <tt>false</tt> otherwise.
     * @throws GridClientException In case an operation has thrown exception (it is rethrown).
     */
    virtual bool get() const =0;

    /**
     * Waits for future to finish and returns the result of operation.
     *
     * @param timeout Time to wait for result.
     * @return <tt>true</tt> if the operation succeeded, <tt>false</tt> otherwise.
     * @throws GridClientException In case an operation has thrown exception (it is rethrown).
     * @throws GridClientFutureTimeoutException If timed out before future finishes.
     */
    virtual bool get(const boost::posix_time::time_duration& timeout) const = 0;
};

/**
 * Future for operations that return complex values like GridClientVariant, etc.
 */
template<class T>
class GridClientFuture {
public:
    /** Destructor. */
    virtual ~GridClientFuture(){}

    /**
     * Was the operation successful or not. If it was not successful, result is not valid.
     *
     * @return Flag indicating the result of the future.
     */
    virtual bool success() const = 0;

    /**
     * Result of operation. If the operation was not successful, it is not valid.
     *
     * @return Result value.
     */
    virtual T result() const = 0;

    /**
     * Waits for future to complete and fills success and result values.
     *
     * @return Operation result.
     */
    virtual T get() = 0;

    /**
     * Waits for future to finish and returns the result of operation.
     *
     * @param timeout Time to wait for result.
     * @return Operation result.
     * @throws GridClientException In case an operation has thrown exception (it is rethrown).
     * @throws GridClientFutureTimeoutException If timed out before future finishes.
     */
    virtual T get(const boost::posix_time::time_duration& timeout) = 0;
};

#endif /* GRIDFUTURE_HPP_ */
