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

namespace GridGain.Client {
    /** <summary>Future for asynchronous operations.</summary> */
    public interface IGridClientFuture {
        /**
         * <summary>
         * Synchronously waits for task completion.</summary>
         *
         * <exception cref="GridClientException">If task execution fails with exception.</exception>
         */
        void WaitDone();

        /**
         * <summary>
         * Synchronously waits for task completion.</summary>
         *
         * <param name="timeout">Timeout interval to wait future completes.</param>
         * <returns><c>true</c> if this future completes, <c>false</c> - otherwize.</returns>
         * <exception cref="GridClientException">If task execution fails with exception.</exception>
         */
        bool WaitDone(System.TimeSpan timeout);

        /** <summary>Future is done flag.</summary> */
        bool IsDone {
            get;
        }
    }

    /** <summary>Generic future with result for asynchronous operations.</summary> */
    public interface IGridClientFuture<T> : IGridClientFuture {
        /**
         * <summary>
         * Synchronously waits for task completion and returns execution result.</summary>
         *
         * <exception cref="GridClientException">If task execution fails with exception.</exception>
         */
        T Result {
            get;
        }
    }
}
