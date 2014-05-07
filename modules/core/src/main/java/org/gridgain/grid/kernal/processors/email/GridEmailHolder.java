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

package org.gridgain.grid.kernal.processors.email;

import org.gridgain.grid.util.typedef.internal.*;
import org.gridgain.grid.util.future.*;

import java.util.*;

/**
 * Email holder for email processor.
 */
class GridEmailHolder {
    /** */
    private String subj;

    /** */
    private String body;

    /** */
    private boolean html;

    /** */
    private Collection<String> addrs;

    /** */
    private GridFutureAdapter<Boolean> fut;

    /**
     *
     * @param fut Associated future.
     * @param subj Email subject.
     * @param body Email body.
     * @param html Whether email body is html.
     * @param addrs Addresses to send email to.
     */
    GridEmailHolder(GridFutureAdapter<Boolean> fut, String subj, String body, boolean html, Collection<String> addrs) {
        assert fut != null;
        assert subj != null;
        assert body != null;
        assert addrs != null;
        assert !addrs.isEmpty();

        this.fut = fut;
        this.subj = subj;
        this.body = body;
        this.html = html;
        this.addrs = addrs;
    }


    /**
     *
     * @return Holder's future.
     */
    GridFutureAdapter<Boolean> future() {
        return fut;
    }

    /**
     *
     * @return Email subject.
     */
    String subject() {
        return subj;
    }

    /**
     *
     * @return Email body.
     */
    String body() {
        return body;
    }

    /**
     *
     * @return Html flag.
     */
    boolean html() {
        return html;
    }

    /**
     *
     * @return Destinations.
     */
    Collection<String> addresses() {
        return addrs;
    }

    /** {@inheritDoc} */
    @Override public String toString() {
        return S.toString(GridEmailHolder.class, this);
    }
}
