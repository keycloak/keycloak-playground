/*
 * Copyright 2023 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.models.map.storage.file;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author hmlnarik
 */
public class RunOnlyOnce implements Runnable {

    private AtomicBoolean ran = new AtomicBoolean(false);
    private final Runnable preTask;
    private final Runnable postTask;

    public RunOnlyOnce(Runnable preTask, Runnable postTask) {
        this.preTask = preTask;
        this.postTask = postTask;
    }

    @Override
    public void run() {
        if (ran.compareAndSet(false, true) && preTask != null) {
            preTask.run();
        }
    }

    public void runPostTask() {
        if (hasRun() && postTask != null) {
            postTask.run();
        }
    }

    public boolean hasRun() {
        return ran.get();
    }

    @Override
    public String toString() {
        return "RunOnlyOnce" 
          + (hasRun() ? " - ran already" : "")
          + " " + preTask;
    }

}
