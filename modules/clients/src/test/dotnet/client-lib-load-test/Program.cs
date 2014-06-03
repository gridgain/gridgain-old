﻿// 
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


/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

namespace GridGain {
    using System;
    using System.Linq;
    using System.Text;
    using System.Threading;
    using System.Collections.Generic;
    using GridGain.Client;
    using System.Diagnostics;

    using G = GridGain.Client.GridClientFactory;
    using X = System.Console;

    /** <summary>Client load tests.</summary> */
    class GridClientLoadTest {
        static void Main(string[] args) {
            var cfg = CreateConfig(GridClientProtocol.Tcp, "127.0.0.1:10080");

            IGridClient client = G.Start(cfg);

            IGridClientData cache = client.Data("atomic");

            X.WriteLine("Client is started.");

            X.WriteLine(">>>");
            X.WriteLine(">>> Running test: PUT_GET");
            X.WriteLine(">>>");

            var threadsCount = 128;

            var warmup = 20;

            var duration = 40;

            long adder = 0;

            long operationPerSecond = 0;

            var closed = false;

            var timer = new Thread(() => {
                int i = 0;

                while (!closed) {
                    Thread.Sleep(TimeSpan.FromSeconds(1));


                    X.WriteLine("Operations/second: " + Interlocked.Exchange(ref adder, 0));
                }                
            });

            timer.IsBackground = true;
            timer.Start();

            long ops = 0;
            long time = 0;
                
            var threads = new List<Thread>();

            //Stopwatch testStart2 = new Stopwatch();

            //testStart2.Start();

            while (threads.Count < threadsCount) {
                var t = new Thread(() => {
                    Random rnd = new Random();
 
                    int i = 0;

                    //Stopwatch start = new Stopwatch();

                    while(true) {
                        //start.Restart();

                        // Operation begin.
                        Object key = rnd.Next(1000000);

                        //Object o = cache.GetItem<Object, Object>(key);

                        //if (o != null)
                        //    key = rnd.Next(1000000);

                        cache.Put(key, key);
                        // Operation end.

                        //start.Stop();

                        //Interlocked.Add(ref time, (long)(start.Elapsed.TotalMilliseconds));

                        Interlocked.Increment(ref adder);

                        //testStart2.Stop();

                        //if (testStart2.Elapsed.TotalSeconds > (duration + warmup))
                        //    break;
                        //else 
                        //    testStart2.Start();

                        i++;
                    }

                    Interlocked.Add(ref ops, i);                  
                });

                t.Name = "comparison-worker-" + threads.Count;

                threads.Add(t);
            }

            foreach (var t in threads)
                t.Start();

            // Wait threads finish their job.
            foreach (var t in threads)
                t.Join();

            closed = true;

            timer.Join();

            X.WriteLine("Average operations/second: " + operationPerSecond);

            double latency = 1d * time / ops;

            X.WriteLine("Average latency for PUT_GET test: " + latency + "ms");

            G.StopAll();
        }

        private static IGridClientConfiguration CreateConfig(GridClientProtocol proto, String srv) {
            var atomic = new GridClientDataConfiguration();
            atomic.Name = "atomic";
            atomic.Affinity = new GridClientPartitionAffinity();

            GridClientConfiguration cfg = new GridClientConfiguration();

            cfg.DataConfigurations.Add(atomic);
            cfg.Servers.Add(srv);

            return cfg;
        }
    }
}
