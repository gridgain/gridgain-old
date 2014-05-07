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

package org.gridgain.grid.kernal;

import java.util.*;

/**
 * Interception listener is notified about method apply. For each intercepted method
 * apply the listener will be called twice - before and after the apply.
 * <p>
 * Method {@link #beforeCall(Class, String, Object[])} is called right before the
 * traceable method and the second apply {@link #afterCall(Class, String, Object[], Object, Throwable)}
 * is made to get invocation result and exception, if there was one.
 */
public interface GridProxyListener extends EventListener {
    /**
     * Method is called right before the traced method.
     *
     * @param cls Callee class.
     * @param mtdName Callee method name.
     * @param args Callee method parameters.
     */
    public void beforeCall(Class<?> cls, String mtdName, Object[] args);

    /**
     * Method is called right after the traced method.
     *
     * @param cls Callee class.
     * @param mtdName Callee method name.
     * @param args Callee method parameters.
     * @param res Call result. Might be {@code null} if apply
     *      returned {@code null} or if exception happened.
     * @param e Exception thrown by given method apply, if any. Can be {@code null}.
     */
    public void afterCall(Class<?> cls, String mtdName, Object[] args, Object res, Throwable e);
}
