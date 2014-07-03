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

package org.gridgain.grid.util;

import org.gridgain.grid.*;
import org.gridgain.grid.spi.*;
import org.gridgain.grid.util.lang.*;

/**
 * Wrapper used to covert {@link GridSpiCloseableIterator} to {@link GridCloseableIterator}.
 */
public class GridSpiCloseableIteratorWrapper<T> extends GridCloseableIteratorAdapter<T> {
    /** */
    private static final long serialVersionUID = 0L;

    /** */
    private final GridSpiCloseableIterator<T> iter;

    /**
     * @param iter Spi iterator.
     */
    public GridSpiCloseableIteratorWrapper(GridSpiCloseableIterator<T> iter) {
        assert iter != null;

        this.iter = iter;
    }

    /** {@inheritDoc} */
    @Override protected T onNext() throws GridException {
        return iter.next();
    }

    /** {@inheritDoc} */
    @Override protected boolean onHasNext() throws GridException {
        return iter.hasNext();
    }

    /** {@inheritDoc} */
    @Override protected void onClose() throws GridException {
        iter.close();
    }
}
