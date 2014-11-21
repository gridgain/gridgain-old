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
# Parses command line parameters into GridGain variables that are common for the launcher scripts:
# CONFIG
# INTERACTIVE
# QUIET
# JVM_XOPTS
#
# Script setups reasonable defaults (see below) for omitted arguments.
#
# Scripts accepts following incoming variables:
# DEFAULT_CONFIG
#
# Can be used like:
#       . "${GRIDGAIN_HOME}"/bin/include/parseargs.sh
# in other scripts to parse common command lines parameters.
#

CONFIG=${DEFAULT_CONFIG}
INTERACTIVE="0"
QUIET="-DGRIDGAIN_QUIET=true"
JVM_XOPTS=""

while [ $# -gt 0 ]
do
    case "$1" in
        -i) INTERACTIVE="1";;
        -v) QUIET="-DGRIDGAIN_QUIET=false";;
        -J*) JVM_XOPTS="$JVM_XOPTS ${1:2}";;
        *) CONFIG="$1";;
    esac
    shift
done
