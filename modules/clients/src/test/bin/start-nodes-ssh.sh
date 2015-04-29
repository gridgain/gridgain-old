#!/bin/bash
#
# Copyright (C) GridGain Systems. All Rights Reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at

#    http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

#  _________        _____ __________________        _____
#  __  ____/___________(_)______  /__  ____/______ ____(_)_______
#  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
#  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
#  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
#
# Version: @sh.file.version
#

#
# This script starts nodes in SSH session.
#
# It resolves MacOS X problem on TeamCity to start nodes with direct script call:
# nohup: can't detach from console: Inappropriate ioctl for device
#

# Define environment paths.
SCRIPT_DIR=$(cd $(dirname "$0"); pwd)
GG_HOME=$(cd $SCRIPT_DIR/../../..; pwd)

SSH_COMMAND="ssh localhost /bin/bash $SCRIPT_DIR/start-nodes.sh"

echo Execute command in ssh session:
echo $SSH_COMMAND
echo

# Require ~/.ssh/authorized_keys to be defined for current user.
$SSH_COMMAND

echo
echo SSH session closed.
