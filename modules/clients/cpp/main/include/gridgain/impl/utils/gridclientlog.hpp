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

#ifndef GRIDCLIENTLOG_HPP_INCLUDED
#define GRIDCLIENTLOG_HPP_INCLUDED

#include <string>

#define GG_LOG_ERROR(m, ...) if(GridClientLog::isLevel(GridClientLog::LEVEL_ERROR)) \
    GridClientLog::log(GridClientLog::LEVEL_ERROR,__FILE__,__LINE__,__FUNCTION__, m, __VA_ARGS__)

#define GG_LOG_AND_THROW(ex, m, ...) {\
    if(GridClientLog::isLevel(GridClientLog::LEVEL_ERROR))\
        GG_LOG_ERROR(m, __VA_ARGS__);\
\
    throw ex(fmtstring(m, __VA_ARGS__));\
}

#define GG_LOG_ERROR0(m) GG_LOG_ERROR(m, NULL)

#define GG_LOG_WARN(m, ...) if(GridClientLog::isLevel(GridClientLog::LEVEL_WARN)) \
    GridClientLog::log(GridClientLog::LEVEL_WARN,__FILE__,__LINE__,__FUNCTION__, m, __VA_ARGS__)

#define GG_LOG_WARN0(m) GG_LOG_WARN(m, NULL)

#define GG_LOG_INFO(m, ...) if(GridClientLog::isLevel(GridClientLog::LEVEL_INFO)) \
    GridClientLog::log(GridClientLog::LEVEL_INFO,__FILE__,__LINE__,__FUNCTION__, m, __VA_ARGS__)

#define GG_LOG_INFO0(m) GG_LOG_INFO(m, NULL)

#define GG_LOG_DEBUG(m, ...) if(GridClientLog::isLevel(GridClientLog::LEVEL_DEBUG)) \
    GridClientLog::log(GridClientLog::LEVEL_DEBUG,__FILE__,__LINE__,__FUNCTION__,m, __VA_ARGS__)

#define GG_LOG_DEBUG0(m) GG_LOG_DEBUG(m, NULL)

std::string fmtstring(const char* fmt, ...);

struct GridClientLog {
public:
    enum Level {
        LEVEL_ERROR = 1, LEVEL_WARN, LEVEL_INFO, LEVEL_DEBUG
    };

    static void log(Level level, const char* file, int line, const char* funcName, const char* format, ...);

    static bool isLevel(Level level);
};

#endif /* GRIDCLIENTLOG_HPP_INCLUDED */
