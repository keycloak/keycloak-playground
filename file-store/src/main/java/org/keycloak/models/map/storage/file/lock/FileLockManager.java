/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.models.map.storage.file.lock;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;
import org.keycloak.common.util.Time;

/**
 * A locking mechanism based on temporary files. A thread owns the lock if it successfully creates the lock file. If the lock
 * file already exists, the thread retries the operation until it either obtains the lock or the time limit is reached.
 * Upon completing its task, the thread releases the lock by deleting the corresponding lock file.
 */
public class FileLockManager {

    private static final Logger LOG = Logger.getLogger(FileLockManager.class);

    private static final Path locksDir = Path.of("/tmp/.locks/");

    private static final ThreadLocal<List<String>> lockedFiles = new ThreadLocal<>();

    static {
        if (!Files.exists(locksDir)) {
            try {
                Files.createDirectory(locksDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public static void acquireLockForFile(String fileName, Duration timeout) {
        long maximumTime = Time.currentTimeMillis() + timeout.toMillis();

        List<String> threadLockedFiles = lockedFiles.get();
        if (threadLockedFiles != null && threadLockedFiles.contains(fileName)) {
            LOG.debugf("%s already holds lock for file %s", Thread.currentThread().getName(), fileName);
            return;
        }

        int iteration = 0;
        while (true) {
            if (Time.currentTimeMillis() >= maximumTime) {
                throw new RuntimeException("Unable to acquire lock for file " + fileName);
            }
            try {
                Path filePath = Files.createFile(locksDir.resolve(fileName + ".lock"));
                LOG.debugf("%s successfully acquired lock for file %s", Thread.currentThread().getName(), fileName);
                if (lockedFiles.get() == null)
                    lockedFiles.set(new ArrayList<>());
                lockedFiles.get().add(fileName);
                break;
            } catch (IOException e) {
                iteration++;
                try {
                    int delay = computeBackoffInterval(50, iteration);
                    LOG.debugf("%s failed to create lock file in attempt %d, sleeping for %d millis", Thread.currentThread().getName(), iteration, delay);
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public static void acquireLockForFiles(List<String> files, Duration timeout) {
        // remove from the list the files for which the thread already has a lock
        List<String> threadLockedFiles = lockedFiles.get();
        List<String> toBeLocked = files.stream()
                .filter(file -> threadLockedFiles == null || !threadLockedFiles.contains(file))
                .collect(Collectors.toList());

        if (toBeLocked.isEmpty()) {
            LOG.debugf("%s already holds locks for all the files");
            return;
        }

        long maximumTime = Time.currentTimeMillis() + timeout.toMillis();
        List<String> successfullyLocked = new ArrayList<>();
        int iteration = 0;
        while (true) {
            if (Time.currentTimeMillis() >= maximumTime) {
                throw new RuntimeException("Unable to acquire lock for all files");
            }
            try {
                for (String file : toBeLocked) {
                    Path filePath = Files.createFile(locksDir.resolve(file + ".lock"));
                    successfullyLocked.add(file);
                }
                // no exceptions thrown - all locks were successfully acquired
                if (lockedFiles.get() == null)
                    lockedFiles.set(new ArrayList<>());
                lockedFiles.get().addAll(successfullyLocked);
                LOG.debugf("%s successfully acquired lock for files %s", Thread.currentThread().getName(), successfullyLocked);

                break;
            } catch (IOException e) {
                // clear any locks that were acquired in this attempt.
                successfullyLocked.forEach(file -> {
                    try {
                        Files.delete(locksDir.resolve(file + ".lock"));
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                });
                successfullyLocked.clear();
                iteration++;
                try {
                    int delay = computeBackoffInterval(50, iteration);
                    LOG.debugf("%s failed to create lock files in attempt %d, sleeping for %d millis", Thread.currentThread().getName(), iteration, delay);
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }


    private static int computeBackoffInterval(int base, int iteration) {
        int bound = base * (1 << iteration);
        return new Random().nextInt(bound);
    }

    public static void releaseLockForFile(String fileName) {
        if (lockedFiles.get() == null || !lockedFiles.get().contains(fileName)) {
            throw new RuntimeException(Thread.currentThread().getName() + " is not current holder of lock for file " + fileName);
        }
        try {
            Path filePath = locksDir.resolve(fileName + ".lock");
            if (Files.exists(filePath))
                Files.delete(filePath);
            lockedFiles.get().remove(fileName);
            LOG.debugf("%s successfully released lock for file %s", Thread.currentThread().getName(), filePath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void releaseLockForFiles(List<String> files) {
        for (String file : files) {
            if (lockedFiles.get() == null || !lockedFiles.get().contains(file)) {
                throw new RuntimeException(Thread.currentThread().getName() + " is not current holder of lock for file " + file);
            }
        }
        // now we know the thread holds all locks, proceed to remove them.
        try {
            for (String fileName : files) {
                Path filePath = locksDir.resolve(fileName + ".lock");
                if (Files.exists(filePath))
                    Files.delete(filePath);
                lockedFiles.get().remove(fileName);
            }
            LOG.debugf("%s successfully released lock for files %s", Thread.currentThread().getName(), files);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void releaseAllLocks() {
        try {
            // if we have locks structured in sub-directories (e.g. by realm), we can use Files.walk() to visit them all.
            Files.list(locksDir).filter(file -> !Files.isDirectory(file)).forEach(file -> {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
