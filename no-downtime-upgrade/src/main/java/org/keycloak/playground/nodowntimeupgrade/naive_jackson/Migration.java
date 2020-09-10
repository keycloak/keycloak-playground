/*
 * Copyright 2020 Red Hat, Inc. and/or its affiliates
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
package org.keycloak.playground.nodowntimeupgrade.naive_jackson;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 *
 * @author hmlnarik
 */
public class Migration {

    private static final List<Function<ObjectNode, ObjectNode>> MIGRATORS = Arrays.asList(
      o -> o,
      Migration::migrateTreeFrom1To2,
      Migration::migrateTreeFrom2To3,
      Migration::migrateTreeFrom3To4
    );

    public static ObjectNode migrateTreeFrom1To2(ObjectNode node) {
        JsonNode clientTemplateIdNode = node.path("clientTemplateId");
        if (clientTemplateIdNode != null && clientTemplateIdNode.isTextual()) {
            node.put("clientScopeId", "template-" + clientTemplateIdNode.asText());
            node.remove("clientTemplateId");
        }
        return node;
    }

    public static ObjectNode migrateTreeFrom2To3(ObjectNode node) {
        JsonNode node2Node = node.path("node2");
        if (node2Node != null && node2Node.isTextual()) {
            node.remove("node2");
        }
        return node;
    }

    public static ObjectNode migrateTreeFrom3To4(ObjectNode node) {
        JsonNode clientTimeoutNode = node.path("timeout");
        if (clientTimeoutNode != null && clientTimeoutNode.isInt()) {
            node.put("timeout1", clientTimeoutNode.asInt());
            node.put("timeout2", clientTimeoutNode.asInt());
            node.remove("timeout");
        }
        return node;
    }

    public static ObjectNode migrateTreeTo(int currentVersion, int targetVersion, ObjectNode node) {
        while (currentVersion < targetVersion) {
            Function<ObjectNode, ObjectNode> migrator = MIGRATORS.get(currentVersion);
            if (migrator != null) {
                node = migrator.apply(node);
            }
            currentVersion++;
        }
        return node;
    }

}
