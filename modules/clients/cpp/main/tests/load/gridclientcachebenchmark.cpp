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

#include <vector>

#include <gridgain/gridgain.hpp>
#include <boost/lexical_cast.hpp>
#include <boost/thread.hpp>
#include <boost/foreach.hpp>
#include <boost/program_options.hpp>
#include <boost/timer.hpp>

#include "gridtestcommon.hpp"
#include "gridgain/impl/gridclientpartitionedaffinity.hpp"

/** Global atomic used to calculate statistics. */
TGridAtomicInt gIters;

/** Maximum number of distinct keys. */
const int KEY_COUNT = 1000000;

/** Map of command line arguments */
boost::program_options::variables_map vm;

/** GridGain client interface. */
TGridClientPtr client;

/** Used to stop worker threads. */
TGridAtomicBool gExit;

/** Used to stop only collect status after warmup. */
TGridAtomicBool gWarmupDone;

/** Test types enumerator. */
enum GridClientCacheTestType {
    PUT = 0,
    GET,
    PUT_TX,
    GET_TX,
    NUM_TEST_TYPES
};

/**
 * Threadproc that prints out performance statistics every second.
 *
 */
void StatsPrinterThreadProc() {
	int LastIters = gIters.load(); // Save global iterations count so while

    while (true) {
    	boost::this_thread::sleep(boost::posix_time::seconds(1));

    	int CurIters = gIters.load();

        std::cout << "Operations for last second: " << CurIters - LastIters << std::endl;

        LastIters = CurIters;
    }
}

/**
 * Converts string int test type enum.
 *
 * @param typeName String representation of test type.
 * @param Enum representation of test type.
 */
GridClientCacheTestType testTypeFromString(std::string typeName) {
    if (!strcmp(typeName.c_str(), "PUT"))
        return PUT;
    else if (!strcmp(typeName.c_str(), "PUT_TX"))
        return PUT_TX;
    else if (!strcmp(typeName.c_str(), "GET_TX"))
        return GET_TX;
    else if (!strcmp(typeName.c_str(), "GET"))
        return GET;
    return NUM_TEST_TYPES;
}

/**
 * Returns a random int between 0 and max, thread safe.
 *
 * @param max A maximum value of a random integer.
 * @param seed A seed to use. Modifiable. Needs to be passed each time.
 */
int randomInt(int max, unsigned int* seed) {
    return rand_r(seed) % (max + 1);
}

/**
 * Class representing one thread working with the client.
 */
class TestThread: private boost::noncopyable {
public:
    /**
     * Constructs the test thread.
     *
     * @param iterationCnt How many iterations to perform.
     */
    TestThread(GridClientCacheTestType op) {
        iters = 1;

        seed = time(NULL);

        thread = boost::thread(boost::bind(&TestThread::run, this, op));
    }

    /**
     * Thread proc for running specific type of test.
     *
     * @param opType Type of test to run
     */
    void run(GridClientCacheTestType opType) {
        try {
            TGridClientDataPtr data = client->data(vm["cachename"].as<string>());

            switch (opType) {
                case PUT: { // block of code to avoid "jump to the case label" compilation error
                        TGridClientVariantMap theMap;

                        theMap[randomInt(KEY_COUNT - 1, &seed)] = 42;

                        while (!gExit) {
                            data->putAll(theMap);
                            ++gIters;
                        }
                    }

                    break;

                case GET:
                    while (!gExit && ++iters) {
                        data->get((int16_t) randomInt(KEY_COUNT - 1, &seed));
                        ++gIters;
                    }

                    break;

                case PUT_TX:
                case GET_TX:
                    std::cerr << "Unsupported test operation.\n";

                    break;

                default:
                    std::cerr << "Invalid test operation.\n";

                    break;
            }
        }
        catch (GridClientException& e) {
            std::cerr << "GridClientException: " << e.what() << "\n";
        }
        catch (...) {
            std::cerr << "Unknown exception.\n";
        }
    }

    /** Joins the test thread. */
    void join() {
        thread.join();
    }

    /** Returns number of iterations completed. */
    int getIters() {
        return iters;
    }

private:
    /** Thread implementation. */
    boost::thread thread;

    /** Number of completed iterations. */
    int iters;

    /** A random seed used as a state for thread-safe random functions. */
    unsigned int seed;
};

typedef std::shared_ptr<TestThread> TestThreadPtr;

int main(int argc, const char** argv) {
	gIters = 0;
    gExit = false;
    gWarmupDone = false;

    using namespace std;
    using namespace boost::program_options;

    // initialize random seed
    srand(time(NULL));

    // Declare the supported options.
    options_description desc("Allowed options");
    desc.add_options()
    		("help",	"produce help message")
    		("host",	value<string>()->required(),	"Host to connect to")
    		("port",	value<int>()->required(),	"Port to connect to")
    		("threads",	value<int>()->required(),	"Number of threads")
    		("testtype",	value<string>()->required(),	"Type of operations to run")
    		("cachename",	value<string>()->required(),	"Cache name")
    		("warmupseconds",	value<int>()->required(),	"Seconds to warm up")
    		("runseconds",	value<int>()->required(),	"Seconds to run")
    		("usetransactions",	boost::program_options::value<bool>()->required(),	"Use transactions (bool)");

    try {
        store(parse_command_line(argc, argv, desc), vm);
        notify(vm);
    }
    catch (exception &e) {
    	cerr << "Error parsing arguments: " << e.what() << endl;
    	cerr << desc << endl;
    	return 1;
    }

    if (vm.count("help")) {
    	cout << desc << endl;
    	return 0;
    }

    GridClientConfiguration cfg = clientConfig();

    std::vector<GridClientSocketAddress> servers;
    servers.push_back(GridClientSocketAddress(vm["host"].as<string>(), vm["port"].as<int>()));

    cfg.servers(servers);

    GridClientDataConfiguration cacheCfg;

    // Set remote cache name.
    cacheCfg.name(vm["cachename"].as<string>());

    std::shared_ptr<GridClientDataAffinity> ptrAffinity(new GridClientPartitionAffinity());

    // Set client partitioned affinity for this cache.
    cacheCfg.affinity(ptrAffinity);

    std::vector<GridClientDataConfiguration> dataConfigurations;
    dataConfigurations.push_back(cacheCfg);

    cfg.dataConfiguration(dataConfigurations);

    client = GridClientFactory::start(cfg);
    std::vector<TestThreadPtr> workers;

    boost::thread printerThread(StatsPrinterThreadProc);

    int numThreads = vm["threads"].as<int>();

    for (int i = 0; i < numThreads; i++) {
        workers.push_back(TestThreadPtr(new TestThread(testTypeFromString(vm["testtype"].as<string>()))));
    }

    // let it warm up for requested amount of time and start gathering stats
    boost::this_thread::sleep(boost::posix_time::seconds(vm["warmupseconds"].as<int>()));
    gWarmupDone = true;

    int itersBeforeTest = gIters.load(); // Save Iterations before final benchmark

    // Let tests run for requested amount of time and then signal the exit
    boost::this_thread::sleep(boost::posix_time::seconds(vm["runseconds"].as<int>()));
    gExit = true;

    int itersAfterTest = gIters.load(); // Save iterations after final benchmark

    //join all threads
    for (std::vector<TestThreadPtr>::iterator i = workers.begin(); i != workers.end(); i++) {
        (*i)->join();
    }

    workers.clear();
    client.reset();

    GridClientFactory::stopAll();

    cout << "Average operations/s :" << (itersAfterTest - itersBeforeTest) / vm["runseconds"].as<int>() << endl;

    return EXIT_SUCCESS;
}
