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

package org.gridgain.jvmtest;

import junit.framework.*;

import javax.swing.*;
import java.io.*;
import java.nio.channels.*;

/**
 * Java file locks test.
 */
public class FileLocksTest extends TestCase {
    /** File path (on Windows file will be created under the root directory of the current drive). */
    private static final String LOCK_FILE_PATH = "/test-java-file-lock-tmp.bin";

    /**
     * @throws Exception If failed.
     */
    public void testWriteLocks() throws Exception {
        final File file = new File(LOCK_FILE_PATH);

        file.createNewFile();

        RandomAccessFile raf = new RandomAccessFile(file, "rw");

        System.out.println("Getting lock...");

        FileLock lock = raf.getChannel().lock();

        System.out.println("Obtained lock: " + lock);

        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    RandomAccessFile raf = new RandomAccessFile(file, "rw");

                    System.out.println("Getting lock (parallel thread)...");

                    FileLock lock = raf.getChannel().lock();

                    System.out.println("Obtained lock (parallel tread): " + lock);

                    lock.release();
                }
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        JOptionPane.showMessageDialog(null, "Press OK to release lock.");

        lock.release();

        thread.join();
    }

    /**
     * @throws Exception If failed.
     */
    public void testReadLocks() throws Exception {
        final File file = new File(LOCK_FILE_PATH);

        file.createNewFile();

        RandomAccessFile raf = new RandomAccessFile(file, "r");

        System.out.println("Getting lock...");

        FileLock lock = raf.getChannel().lock(0, Long.MAX_VALUE, true);

        System.out.println("Obtained lock: " + lock);

        Thread thread = new Thread(new Runnable() {
            @Override public void run() {
                try {
                    RandomAccessFile raf = new RandomAccessFile(file, "r");

                    System.out.println("Getting lock (parallel thread)...");

                    FileLock lock = raf.getChannel().lock(0, Long.MAX_VALUE, true);

                    System.out.println("Obtained lock (parallel thread): " + lock);

                    lock.release();

                }
                catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        });

        thread.start();

        JOptionPane.showMessageDialog(null, "Press OK to release lock.");

        lock.release();

        thread.join();
    }
}
