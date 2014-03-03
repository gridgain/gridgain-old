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

#include "gridgain/impl/utils/gridthreadpool.hpp"


GridThreadPoolIsShutdownException::GridThreadPoolIsShutdownException():
    std::logic_error("Thread pool is shut down.") {
}

GridThreadPool::GridThreadPool(unsigned int nthreads) {
    isShutdown = false;

    if (nthreads == 0)
        throw std::invalid_argument("GridThreadPool: number of threads cannot be 0.");

    for (unsigned int i = 0; i < nthreads; i++) {
        boost::thread t(boost::bind(&GridThreadPool::worker, this)); // Starts the thread.

        threads.push_back(boost::move(t));
    }
}

GridThreadPool::~GridThreadPool() {
    this->shutdown();
}

void GridThreadPool::execute(TGridThreadPoolTaskPtr& task) {
    if (isShutdown)
        return;

    tasks.offer(task);
}

void GridThreadPool::waitForEmptyQueue() {
    tasks.waitUntilEmpty();
}

void GridThreadPool::shutdown() {
    if (isShutdown)
        return;

    isShutdown = true;

    // Interrupt all threads.
    for (auto i = threads.begin(); i != threads.end(); i++)
        i->interrupt();

    // Wait for all threads to finish.
    for (auto i = threads.begin(); i != threads.end(); i++)
        i->join();

    threads.clear();

    tasks.forEach([] (TGridThreadPoolTaskPtr task) {
        task->cancel();
    });
}

void GridThreadPool::worker() {
    while(!boost::this_thread::interruption_requested() && !isShutdown) {
        // Get the task from queue, block if empty.
        TGridThreadPoolTaskPtr task = tasks.poll();

        try {
            task->run();
        }
        catch (boost::thread_interrupted&) {
            return;
        }
        catch (...) {
            // We can do nothing if the task fails.
            // User can only rely on handling exceptions inside the task.
        }
    }
}

size_t GridThreadPool::queueSize() const {
    return tasks.size();
}
