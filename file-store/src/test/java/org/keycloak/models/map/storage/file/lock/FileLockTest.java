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

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class FileLockTest {

    private TestEntity entity;
    private static TestFileStore store = new TestFileStore();

    @AfterAll()
    public static void afterAll() {
        store.clearStorage();
    }

    @BeforeEach
    public void beforeEach() {
        FileLockManager.releaseAllLocks();
        // create a test entity in the store
        this.entity = new TestEntity("jduke");
        store.writeEntity(this.entity);
    }

    // tests for the file locking mechanism

    @Test
    public void concurrentLockingTest() {
        final String FILE_NAME = "testFileName";

        AtomicInteger counter = new AtomicInteger();
        int numIterations = 50;
        Random rand = new Random();
        List<Integer> resultingList = new LinkedList<>();

        IntStream.range(0, numIterations).parallel().forEach(index -> {

                FileLockManager.acquireLockForFile(FILE_NAME, Duration.ofSeconds(30));

                // Locked block
                int c = counter.getAndIncrement();

                try {
                    Thread.sleep(rand.nextInt(100));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }

                resultingList.add(c);

                FileLockManager.releaseLockForFile(FILE_NAME);
            });

        assertThat(resultingList, hasSize(numIterations));
        assertThat(resultingList, equalTo(IntStream.range(0, 50).boxed().collect(Collectors.toList())));
    }

    @Test
    public void lockTimeoutExceptionTest() {
        final String LOCK_NAME = "lockTimeoutExceptionTestLock";
        AtomicInteger counter = new AtomicInteger();
        CountDownLatch waitForTheOtherThreadToFail = new CountDownLatch(1);

        IntStream.range(0, 2).parallel().forEach(index -> {

            try {
                    FileLockManager.acquireLockForFile(LOCK_NAME, Duration.ofSeconds(2));

                    int c = counter.incrementAndGet();
                    if (c == 1) {
                        try {
                            waitForTheOtherThreadToFail.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        throw new RuntimeException("Lock acquired by more than one thread.");
                    }

                    FileLockManager.releaseLockForFile(LOCK_NAME);
            } catch (RuntimeException e) {
                int c = counter.incrementAndGet();
                if (c != 2) {
                    throw new RuntimeException("Acquiring lock failed by different thread than second.");
                }

                assertThat(e.getMessage(), containsString("Unable to acquire lock for file " + LOCK_NAME));
                waitForTheOtherThreadToFail.countDown();
            }
        });
    }

    @Test
    public void testReleaseAllLocksMethod() throws InterruptedException {
        final int NUMBER_OF_THREADS = 4;
        ExecutorService executor = Executors.newFixedThreadPool(NUMBER_OF_THREADS);

        CountDownLatch locksAcquired = new CountDownLatch(NUMBER_OF_THREADS);
        CountDownLatch testFinished = new CountDownLatch(1);

        try {
            // Acquire locks and let the threads wait until the end of this test method
            executor.submit(() -> {
                IntStream.range(0, NUMBER_OF_THREADS).parallel().forEach(i -> {

                    FileLockManager.acquireLockForFile("FILE_" + i, Duration.ofSeconds(1));

                    locksAcquired.countDown();
                    try {
                        testFinished.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }

                    FileLockManager.releaseLockForFile("FILE_" + i);
                });
            });

            locksAcquired.await();

            // Test no lock can be acquired because all are still hold by the executor above
            AtomicInteger counter = new AtomicInteger();
            IntStream.range(0, NUMBER_OF_THREADS).parallel().forEach(i -> {
                        try {
                            FileLockManager.acquireLockForFile("FILE_" + i, Duration.ofSeconds(1));
                            throw new RuntimeException("Acquiring lock should not succeed as it was acquired in the first transaction");
                        } catch (RuntimeException e) {
                            counter.incrementAndGet();
                        }
                    });
            assertThat(counter.get(), Matchers.equalTo(NUMBER_OF_THREADS));

            FileLockManager.releaseAllLocks();

            // Test all locks can be acquired again
            counter.set(0);
            IntStream.range(0, NUMBER_OF_THREADS).parallel().forEach(i -> {
                        FileLockManager.acquireLockForFile("FILE_" + i, Duration.ofSeconds(1));
                        counter.incrementAndGet();
                        FileLockManager.releaseLockForFile("FILE_" + i);
                    });
            assertThat(counter.get(), Matchers.equalTo(NUMBER_OF_THREADS));
        } finally {
            testFinished.countDown();
            executor.shutdown();
        }
    }

    @Test
    public void concurrentLockingMultipleFilesTest() {
        final List<String> filenames = IntStream.range(0, 10).mapToObj(i -> "FILE_" + i).collect(Collectors.toList());

        AtomicInteger counter = new AtomicInteger();
        int numIterations = 50;
        Random rand = new Random();
        List<Integer> resultingList = new LinkedList<>();

        IntStream.range(0, numIterations).parallel().forEach(index -> {

            FileLockManager.acquireLockForFiles(filenames, Duration.ofSeconds(30));

            // Locked block
            int c = counter.getAndIncrement();

            try {
                Thread.sleep(rand.nextInt(100));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }

            resultingList.add(c);

            FileLockManager.releaseLockForFiles(filenames);
        });

        assertThat(resultingList, hasSize(numIterations));
        assertThat(resultingList, equalTo(IntStream.range(0, 50).boxed().collect(Collectors.toList())));
    }

    @Test
    public void lockTimeoutExceptionMultipleFilesTest() throws Exception {
        final List<String> filenames = IntStream.range(0, 10).mapToObj(i -> "FILE_" + i).collect(Collectors.toList());
        CountDownLatch latch = new CountDownLatch(1);

        // main thread acquires lock for one of the files
        FileLockManager.acquireLockForFile("FILE_1", Duration.ofSeconds(1));

        // spawn a different thread that will attempt to acquire locks for all files
        new Thread(() -> {
            try {
                FileLockManager.acquireLockForFiles(filenames, Duration.ofSeconds(2));
                throw new RuntimeException("Acquiring lock should not succeed as one file was locked by main transaction");
            } catch(RuntimeException re) {
                latch.countDown();
                assertThat(re.getMessage(), containsString("Unable to acquire lock for all files"));
            }
        }).start();

        latch.await(5, TimeUnit.SECONDS);
        FileLockManager.releaseLockForFile("FILE_1");
    }

    // tests showcasing the file lock mechanism in the context of a store that reads/writes entities from/to files.

    @Test
    public void testWriteBeforeReadingStream() throws Exception {
        CountDownLatch inputStreamReady = new CountDownLatch(1);
        CountDownLatch writeConcluded = new CountDownLatch(1);

        new Thread(() -> {
            ReadResult result = store.getInputStreamAndModifiedTime(entity.getId());
            inputStreamReady.countDown();
            try {
                // wait until writing is finished.
                writeConcluded.await();

                // finish reading the entity with the previously obtained stream and check that the name still matches the old name.
                TestEntity testEntity = store.readEntity(result);
                assertThat(testEntity.getName(), equalTo("jduke"));

                // reading entity again (i.e. with a new input stream) should yield the updated entity name.
                testEntity = store.readEntity(entity.getId());
                assertThat(testEntity.getName(), equalTo("theduke"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);            }
        }).start();

        // wait until input stream for file representing the entity is obtained.
        inputStreamReady.await();

        // read the entity again to refresh the state (i.e. load version) and then change the name and write it back.
        TestEntity testEntity = store.readEntity(entity.getId());
        testEntity.setName("theduke");
        store.writeEntity(testEntity);
        writeConcluded.countDown();
    }

    @Test
    public void testWriteOutdatedEntity() throws Exception {
        CountDownLatch readPerformed = new CountDownLatch(1);
        CountDownLatch mainLatch = new CountDownLatch(1);

        new Thread(() -> {
            TestEntity testEntity = store.readEntity(entity.getId());
            readPerformed.countDown();
            try {
                // wait until main finishes.
                mainLatch.await();

                // change the entity we've read and attempt to write it.
                testEntity.setName("anotherduke");
                store.writeEntity(testEntity);
            } catch (RuntimeException re) {
                assertThat(re.getMessage(), equalTo("Entity with id " + testEntity.getId() + " was changed"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }).start();

        // wait until the other thread reads the entity.
        readPerformed.await();

        // read the entity again to refresh the state (i.e. load version) and then change the name and write it back.
        TestEntity testEntity = store.readEntity(entity.getId());
        testEntity.setName("theduke");
        store.writeEntity(testEntity);
        mainLatch.countDown();
    }

    @Test
    public void testWriteDeletedEntity() throws Exception {
        CountDownLatch readPerformed = new CountDownLatch(1);
        CountDownLatch mainLatch = new CountDownLatch(1);

        new Thread(() -> {
            TestEntity testEntity = store.readEntity(entity.getId());
            readPerformed.countDown();
            try {
                // wait until main finishes.
                mainLatch.await();

                // change the entity we've read and attempt to write it.
                testEntity.setName("anotherduke");
                store.writeEntity(testEntity);
            } catch (RuntimeException re) {
                assertThat(re.getMessage(), equalTo("Entity with id " + testEntity.getId() + " was removed"));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            }
        }).start();

        // wait until the other thread reads the entity.
        readPerformed.await();

        // delete the entity.
        store.deleteEntity(entity);
    }

    @Test
    public void testWriteDuplicateEntity() {
        // attempt to create an entity with the same id as the test entity
        TestEntity anotherEntity = new TestEntity("anotherduke", UUID.fromString(entity.getId()));
        try {
            store.writeEntity(anotherEntity);
        } catch(RuntimeException re) {
            assertThat(re.getMessage(), equalTo("Entity with id " + anotherEntity.getId() + " already exists"));
        }
    }
}

