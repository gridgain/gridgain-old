/* @java.file.header */

/*  _________        _____ __________________        _____
 *  __  ____/___________(_)______  /__  ____/______ ____(_)_______
 *  _  / __  __  ___/__  / _  __  / _  / __  _  __ `/__  / __  __ \
 *  / /_/ /  _  /    _  /  / /_/ /  / /_/ /  / /_/ / _  /  _  / / /
 *  \____/   /_/     /_/   \_,__/   \____/   \__,_/  /_/   /_/ /_/
 */

package org.gridgain.examples.test;

import org.gridgain.grid.*;

/**
 *
 */
public class CacheNodeStartupLoop {
    /**
     * Start up an empty node with specified cache configuration.
     *
     * @param args Command line arguments, none required.
     * @throws Exception If example execution failed.
     */
    public static void main(String[] args) throws Exception {
        int i = 1;

        while (true) {
            System.out.println("Start node attempt " + i);

            try (Grid g = GridGain.start("examples/config/example-cache.xml")) {
                Thread.sleep(5000);
            }

            i++;
        }
    }
}