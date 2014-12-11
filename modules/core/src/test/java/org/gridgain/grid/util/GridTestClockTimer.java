package org.gridgain.grid.util;

/**
 * Clock timer for tests.
 */
public class GridTestClockTimer implements Runnable {
    /** {@inheritDoc} */
    @Override public void run() {
        while (true) {
            GridUtils.curTimeMillis = System.currentTimeMillis();

            try {
                Thread.sleep(10);
            }
            catch (InterruptedException ignored) {
                GridUtils.log(null, "Timer thread has been interrupted.");

                break;
            }
        }
    }
}
