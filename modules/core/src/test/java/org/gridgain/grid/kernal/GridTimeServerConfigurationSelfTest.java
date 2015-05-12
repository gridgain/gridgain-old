/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.grid.kernal;

import org.gridgain.grid.*;
import org.gridgain.testframework.junits.common.*;

/**
 * Test that the configuration for the "time-server" is propagated it (GG-10090).
 */
@GridCommonTest(group = "Kernal Self")
public class GridTimeServerConfigurationSelfTest extends GridCommonAbstractTest {
    /**
     * @throws Exception If failed.
     */
    public void testTimeServerConfiguration() throws Exception {
        GridConfiguration cfg0 = new GridConfiguration();
        
        cfg0.setTimeServerPortBase(10999); // Change default.
        cfg0.setTimeServerPortRange(99); // Change default.
        
        try(Grid grid = GridGain.start(cfg0)) {
            GridConfiguration cfg1 = grid.configuration();
            
            assertEquals(cfg0.getTimeServerPortBase(), cfg1.getTimeServerPortBase());
            assertEquals(cfg0.getTimeServerPortRange(), cfg1.getTimeServerPortRange());
        }
    }
}
