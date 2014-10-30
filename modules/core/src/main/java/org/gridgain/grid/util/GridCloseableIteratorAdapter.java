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
import org.gridgain.grid.util.lang.*;

import java.util.*;

/**
 * Convenient adapter for closeable iterator.
 */
public abstract class GridCloseableIteratorAdapter<T> extends GridIteratorAdapter<T> implements
    GridCloseableIterator<T> {
    /** */
    private static final long serialVersionUID = 0L;

    /** Closed flag. */
    private boolean closed;

    /** {@inheritDoc} */
    @Override public final T nextX() throws GridException {
        if (!hasNextX())
            throw new NoSuchElementException();

        return onNext();
    }

    /**
     * @return Next element.
     * @throws GridException If failed.
     * @throws NoSuchElementException If no element found.
     */
    protected abstract T onNext() throws GridException;

    /** {@inheritDoc} */
    @Override public final boolean hasNextX() throws GridException {
        return !closed && onHasNext();
    }

    /**
     * @return {@code True} if iterator has next element.
     * @throws GridException If failed.
     */
    protected abstract boolean onHasNext() throws GridException;

    /** {@inheritDoc} */
    @Override public final void removeX() throws GridException {
        checkClosed();

        onRemove();
    }

    /**
     * Called on remove from iterator.
     *
     * @throws GridException If failed.
     */
    protected void onRemove() throws GridException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public final void close() throws GridException {
        if (!closed) {
            onClose();

            closed = true;
        }
    }

    /** {@inheritDoc} */
    @Override public boolean isClosed() {
        return closed;
    }

    /**
     * Invoked on iterator close.
     *
     * @throws GridException If closing failed.
     */
    protected void onClose() throws GridException {
        // No-op.
    }

    /**
     * Throws {@link NoSuchElementException} if iterator has been closed.
     *
     * @throws NoSuchElementException If iterator has already been closed.
     */
    protected final void checkClosed() throws NoSuchElementException {
        if (closed)
            throw new NoSuchElementException("Iterator has been closed.");
    }
}
