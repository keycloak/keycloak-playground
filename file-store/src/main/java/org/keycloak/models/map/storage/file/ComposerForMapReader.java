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
package org.keycloak.models.map.storage.file;

import java.util.List;
import java.util.Stack;
import org.jboss.logging.Logger;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.composer.Composer;
import org.yaml.snakeyaml.nodes.MappingNode;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.parser.Parser;
import org.yaml.snakeyaml.resolver.Resolver;

/**
 *
 * @author hmlnarik
 */
public class ComposerForMapReader extends Composer {

    private final Stack<String> context;

    private final Logger LOG = Logger.getLogger(ComposerForMapReader.class);

    public ComposerForMapReader(Parser parser, Resolver resolver, Stack<String> context) {
        super(parser, resolver);
        this.context = context;
    }

    public ComposerForMapReader(Parser parser, Resolver resolver, LoaderOptions loadingConfig, Stack<String> context) {
        super(parser, resolver, loadingConfig);
        this.context = context;
    }

    @Override
    protected Node composeValueNode(MappingNode node) {
        LOG.infof("composeValueNode(%s)", node);
        return super.composeValueNode(node);
    }

    @Override
    protected Node composeKeyNode(MappingNode node) {
        LOG.infof("composeKeyNode(%s)", node);
        return super.composeKeyNode(node);
    }

    @Override
    protected void composeMappingChildren(List<NodeTuple> children, MappingNode node) {
        LOG.infof("composeMappingChildren(%s)", node);
        super.composeMappingChildren(children, node);
    }

    @Override
    protected Node composeMappingNode(String anchor) {
        LOG.infof("composeMappingNode(%s)", anchor);
        return super.composeMappingNode(anchor);
    }

    @Override
    protected Node composeSequenceNode(String anchor) {
        LOG.infof("composeSequenceNode(%s)", anchor);
        return super.composeSequenceNode(anchor);
    }


}
