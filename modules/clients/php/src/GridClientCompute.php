#!/bin/php

<?php

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

/**
 * PHP client API.
 */
interface GridClientCompute {
    /**
     * @abstract
     * @param GridClientNode $node
     * @return GridClientCompute
     */
    public function projectionByNode(GridClientNode $node);

    /**
     * @abstract
     * @param array $nodes
     * @param GridClientComputeBalancer|null $balancer
     * @return GridClientCompute
     */
    public function projectionByNodes(array $nodes, GridClientComputeBalancer $balancer = null);

    /**
     * @abstract
     * @param GridClientNodeFilter $filter
     * @param GridClientComputeBalancer|null $balancer
     * @return GridClientCompute
     */
    public function projectionByFilter(GridClientNodeFilter $filter, GridClientComputeBalancer $balancer = null);

    /**
     * @abstract
     * @param string $taskName
     * @param array $params
     * @param mixed|null $affKey
     * @return mixed
     */
    public function execute(string $taskName, array $params, $affKey = null);

    /**
     * @abstract
     * @param string $id
     * @return GridClientNode
     */
    public function node(string $id);

    /**
     * @abstract
     * @param array|null $ids
     * @return array
     */
    public function nodes(array $ids = null);

    /**
     * @abstract
     * @param string $id
     * @param boolean $includeAttrs
     * @return GridClientNode
     */
    public function refreshNodeById(string $id, boolean $includeAttrs);

    /**
     * @abstract
     * @param string $ip
     * @param boolean $includeAttrs
     * @return GridClientNode
     */
    public function refreshNodeByIp(string $ip, boolean $includeAttrs);

    /**
     * @abstract
     * @param boolean $includeAttrs
     * @return array
     */
    public function refreshTopology(boolean $includeAttrs);

    /**
     * @abstract
     * @param string|null $path
     * @return array
     */
    public function log(string $path = null);
}

?>
