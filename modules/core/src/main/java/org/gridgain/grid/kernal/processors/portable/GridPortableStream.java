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

package org.gridgain.grid.kernal.processors.portable;

/**
 * Portable stream.
 */
public interface GridPortableStream {
    /**
     * @return Position.
     */
    public int position();

    /**
     * @param pos Position.
     */
    public void position(int pos);

    /**
     * @return Underlying array.
     */
    public byte[] array();

    /**
     * @return Copy of data in the stream.
     */
    public byte[] arrayCopy();

    /**
     * @return Offheap pointer if stream is offheap based, otherwise {@code 0}.
     */
    public long offheapPointer();

    /**
     * @return {@code True} is stream is array based.
     */
    public boolean hasArray();
}
