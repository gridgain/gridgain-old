// 
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

namespace GridGain.Client.Balancer {
    using System;
    using System.Collections.Generic;
    using System.Linq;
    using System.Text;
    using NUnit.Framework;

    [TestFixture]
    public class GridClientRandomBalancerTest {
            [Test]
            public void TestBalancedNode() {
                GridClientRandomBalancer balancer = new GridClientRandomBalancer();
                ICollection<IGridClientNode> nodes = new List<IGridClientNode>();

                Assert.AreEqual(0, nodes.Count);
                try {
                    balancer.BalancedNode(nodes);

                    Assert.Fail("Expected empty list exception.");
                }
                catch (ArgumentException) {
                    /* Noop - normal behaviour */
                }

                // Fill nodes collection.
                for (int i = 0; i < 100; i++)
                    nodes.Add(null);

                for (int i = 0; i < 100; i++) {
                    IGridClientNode node = balancer.BalancedNode(nodes);

                    Assert.IsTrue(nodes.Contains(node));
                }

                nodes = new HashSet<IGridClientNode>(nodes);

                for (int i = 0; i < 100; i++) {
                    IGridClientNode node = balancer.BalancedNode(nodes);

                    Assert.IsTrue(nodes.Contains(node));
                }

        }
    }
}
