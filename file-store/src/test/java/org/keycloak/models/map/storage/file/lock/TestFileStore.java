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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Duration;
import java.util.UUID;

import org.jboss.logging.Logger;

/**
 * A simple file-based store that relies on the file locking mechanism to safely read/write entities from/to files.
 */
public class TestFileStore {

    private static final Logger LOG = Logger.getLogger(TestFileStore.class);

    private final Path storageDir = Path.of("/tmp/storage/");

    public TestFileStore() {
        if (!Files.exists(storageDir)) {
            try {
                Files.createDirectory(storageDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Convenience method that performs all steps involved in reading an entity.
     *
     * @param id the id of the entity to be read.
     * @return the {@link TestEntity} that was read, or {@code null} if no entity with the given id is found.
     */
    public TestEntity readEntity(String id) {
        ReadResult result = getInputStreamAndModifiedTime(id);
        return readEntity(result);
    }

    /**
     * Obtains the object input stream for the file containing the entity identified by the given id, along with the file's
     * last modified time. This operation is controlled by a file lock, so reading operations acquire the lock and release
     * it as soon as it obtains the stream it needs to read the object from. The actual reading is not controlled by the lock.
     *
     * This is done to minimize the amount of time the lock is employed. As soon as a stream is obtained, the thread can
     * read the file's contents even if in the meantime another thread writes to the file.
     *
     * @param entityId the id of the entity being read.
     * @return a {@link ReadResult} containing the input stream and last modified time.
     */
    public ReadResult getInputStreamAndModifiedTime(String entityId) {
        String fileName = entityId + ".kcs";
        Path filePath = storageDir.resolve(fileName);
        if (!Files.exists(filePath)) {
            LOG.debugf("Entity with id %s not found in file storage", entityId);
            return null;
        }

        // acquire lock for file representing the entity
        FileLockManager.acquireLockForFile(fileName, Duration.ofSeconds(5));
        try {
            // obtain file metadata to extract the last modified time (used as version in the entity)
            BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
            long lastModifiedTime = attr.lastModifiedTime().toMillis();
            // obtain an input stream to read the object
            ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(filePath.toString()));
            return new ReadResult(lastModifiedTime, inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            FileLockManager.releaseLockForFile(fileName);
        }
    }

    /**
     * Reads the entity using the data contained in the specified {@link ReadResult}. This operation is not controlled
     * by any locks.
     *
     * @param readResult the object containing the input stream and last modified time used to read the entity.
     * @return the {@link TestEntity} that was read.
     */
    public TestEntity readEntity(final ReadResult readResult) {
        // simply read the object from the stream and set the last modified time as its version.
        try {
            TestEntity entity = (TestEntity) readResult.getInputStream().readObject();
            entity.setVersion(readResult.getLastModifiedTime());
            readResult.getInputStream().close();
            return entity;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Convenience method that performs all steps involved in persisting an entity.
     *
     * @param entity the entity to be written.
     */
    public void writeEntity(TestEntity entity) {
        Path tempFilePath = writeEntityToTempFile(entity);
        moveTempFileToEntityFile(tempFilePath, entity);
    }

    /**
     * Writes the specified entity to a temporary file. This operation is not controlled by any locks.
     *
     * @param entity the entity to be written.
     * @return the {@link Path} to the temporary file.
     */
    public Path writeEntityToTempFile(final TestEntity entity) {
        Path tempFilePath = storageDir.resolve(UUID.randomUUID() + ".tmp");
        try {
            // check if the entity is in a valid state. If not, do not waste resources serializing an entity that
            // cannot be written to the store.
            checkIsSafeToWrite(entity);
            // write the entity to a temporary file.
            ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(tempFilePath.toString()));
            stream.writeObject(entity);
            stream.close();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
        return tempFilePath;
    }

    /**
     * Moves the temporary file into the actual file that represents the entity. This operation is controlled by a file lock
     * to prevent concurrent access to the file that being overridden.
     *
     * @param tempFilePath the {@link Path} to the temp file.
     * @param entity the entity being written.
     */
    public void moveTempFileToEntityFile(Path tempFilePath, TestEntity entity) {
        String entityFileName = entity.getId() + ".kcs";
        // obtain lock for the file representing the entity.
        FileLockManager.acquireLockForFile(entityFileName, Duration.ofSeconds(5));
        try {
            // check again if entity can be written - this can have changed since the entity was written to the temp file.
            checkIsSafeToWrite(entity);
            // move the temporary file into the actual file, then release the lock.
            Files.move(tempFilePath, storageDir.resolve(entityFileName), StandardCopyOption.ATOMIC_MOVE);
        } catch (RuntimeException re) {
            throw re;
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            // release the lock.
            FileLockManager.releaseLockForFile(entityFileName);
        }
    }

    public void deleteEntity(TestEntity entity) {
        String fileName = entity.getId() + ".kcs";
        // acquire lock for file representing the entity
        FileLockManager.acquireLockForFile(fileName, Duration.ofSeconds(5));
        // delete the file from the storage
        try {
            if (Files.exists(storageDir.resolve(fileName)))
                Files.delete(storageDir.resolve(fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            // release the lock
            FileLockManager.releaseLockForFile(fileName);
        }
    }

    private void checkIsSafeToWrite(TestEntity entity) {
        try {
            Path filePath = storageDir.resolve(entity.getId() + ".kcs");
            // check the entity's version - if zero, this is a new entity.
            if (entity.getVersion() == 0) {
                // if a file already exists with the same id, we have an attempt to create a duplicate entity.
                if (Files.exists(filePath)) {
                    throw new RuntimeException("Entity with id " + entity.getId() + " already exists");
                }
            } else {
                // if the file representing the entity doesn't exist anymore, we have a stale entity.
                if (!Files.exists(filePath)) {
                    throw new RuntimeException("Entity with id " + entity.getId() + " was removed");
                }
                // if the file exists but has a newer modified date, we have a stale entity.
                BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
                long lastModifiedTime = attr.lastModifiedTime().toMillis();
                if (entity.getVersion() < lastModifiedTime) {
                    throw new RuntimeException("Entity with id " + entity.getId() + " was changed");
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes all files from the storage.
     */
    public void clearStorage() {
        try {
            // if we have locks structured in sub-directories (e.g. by realm), we can use Files.walk() to visit them all.
            Files.list(storageDir).filter(file -> !Files.isDirectory(file)).forEach(file -> {
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

class ReadResult {

    private long lastModifiedTime;
    private ObjectInputStream inputStream;

    public ReadResult(long lastModifiedTime, ObjectInputStream stream) {
        this.lastModifiedTime = lastModifiedTime;
        this.inputStream = stream;
    }

    public long getLastModifiedTime() {
        return this.lastModifiedTime;
    }

    public ObjectInputStream getInputStream() {
        return this.inputStream;
    }
}
