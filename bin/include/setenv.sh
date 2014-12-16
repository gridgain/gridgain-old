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
# Exports GRIDGAIN_LIBS variable containing classpath for GridGain.
# Expects GRIDGAIN_HOME to be set.
# Can be used like:
#       . "${GRIDGAIN_HOME}"/bin/include/setenv.sh
# in other scripts to set classpath using exported GRIDGAIN_LIBS variable.
#

#
# Check GRIDGAIN_HOME.
#
if [ "${GRIDGAIN_HOME}" = "" ]; then
    echo $0", ERROR: GridGain installation folder is not found."
    echo "Please create GRIDGAIN_HOME variable pointing to location of"
    echo "GridGain installation folder."

    exit 1
fi

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

#
# Libraries included in classpath.
#
GRIDGAIN_LIBS="${GRIDGAIN_HOME}/libs/*"

for file in ${GRIDGAIN_HOME}/libs/*
do
    if [ -d ${file} ] && [ "${file}" != "${GRIDGAIN_HOME}"/libs/optional ]; then
        GRIDGAIN_LIBS=${GRIDGAIN_LIBS}${SEP}${file}/*
    fi

    if [ -d ${file} ] && [ "${file}" == "${GRIDGAIN_HOME}"/libs/gridgain-hadoop ]; then
        HADOOP_EDITION=1
    fi
done

if [ "${USER_LIBS}" != "" ]; then
    GRIDGAIN_LIBS=${USER_LIBS}${SEP}${GRIDGAIN_LIBS}
fi

if [ "${HADOOP_EDITION}" == "1" ]; then
    . "${SCRIPTS_HOME}"/include/hadoop-classpath.sh

    if [ "${GRIDGAIN_HADOOP_CLASSPATH}" != "" ]; then
        GRIDGAIN_LIBS=${GRIDGAIN_LIBS}${SEP}$GRIDGAIN_HADOOP_CLASSPATH
    fi
fi
