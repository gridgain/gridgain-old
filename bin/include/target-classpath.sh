#!/bin/sh
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

# Target class path resolver.
#
# Can be used like:
#       . "${GRIDGAIN_HOME}"/os/bin/include/target-classpath.sh
# in other scripts to set classpath using libs from target folder.
#
# Will be excluded in release.


#
# OS specific support.
#
SEP=":";

case "`uname`" in
    MINGW*)
        SEP=";";
        export GRIDGAIN_HOME=`echo $GRIDGAIN_HOME | sed -e 's/^\/\([a-zA-Z]\)/\1:/'`
        ;;
    CYGWIN*)
        SEP=";";
        export GRIDGAIN_HOME=`echo $GRIDGAIN_HOME | sed -e 's/^\/\([a-zA-Z]\)/\1:/'`
        ;;
esac

includeToClassPath() {
    for file in $1/*
    do
        if [ -d ${file} ] && [ -d "${file}/target" ]; then
            if [ -d "${file}/target/classes" ]; then
                GRIDGAIN_LIBS=${GRIDGAIN_LIBS}${SEP}${file}/target/classes
            fi

            if [ -d "${file}/target/libs" ]; then
                GRIDGAIN_LIBS=${GRIDGAIN_LIBS}${SEP}${file}/target/libs/*
            fi
        fi
    done
}

#
# Include target libraries for opensourse modules to classpath.
#
includeToClassPath ${GRIDGAIN_HOME}/os/modules

#
# Include target libraries for enterprise modules to classpath.
#
includeToClassPath ${GRIDGAIN_HOME}/modules
