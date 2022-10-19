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
package org.keycloak.models.map.storage.file.entity;

import org.keycloak.models.map.storage.file.RealmParser;
import java.io.FileNotFoundException;
import org.junit.jupiter.api.Test;

/**
 *
 * @author hmlnarik
 */
public class RealmParserTest {

    @Test
    public void testSomeMethod() throws FileNotFoundException {
        RealmParser p = new RealmParser();
        p.parse(getClass().getResourceAsStream("/testdir/realm1/realm.yaml"));
    }

    @Test
    public void testEventProcessing() throws FileNotFoundException {
        RealmParser p = new RealmParser();
        FileRealmEntity v = p.parseLowLevel(getClass().getResourceAsStream("/testdir/realm1/realm.yaml"));
        System.out.println(v);
    }

}
