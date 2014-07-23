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

import org.gridgain.client.marshaller.*;
import org.gridgain.grid.kernal.processors.*;
import org.gridgain.portable.*;
import org.jetbrains.annotations.*;

/**
 * Portable processor.
 */
public interface GridPortableProcessor extends GridProcessor {
    /**
     * @param typeName Type name.
     * @return Type ID.
     */
    public int typeId(String typeName);

    /**
     * @param obj Object to marshal.
     * @return Portable object.
     * @throws GridPortableException In case of error.
     */
    public Object marshalToPortable(@Nullable Object obj) throws GridPortableException;

    /**
     * @param obj Object (portable or not).
     * @return Detached portable object or original object.
     */
    public Object detachPortable(@Nullable Object obj);

    /**
     * @return Portable marshaller for client connectivity or {@code null} if it's not
     *      supported (in case of OS edition).
     */
    @Nullable public GridClientMarshaller portableMarshaller();

    /**
     * @param marsh Client marshaller.
     * @return Whether marshaller is portable.
     */
    public boolean isPortable(GridClientMarshaller marsh);

    /**
     * @return Builder.
     */
    public <T> GridPortableBuilder<T> builder();
}
