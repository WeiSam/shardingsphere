/*
 * Copyright 2016-2018 shardingsphere.io.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * </p>
 */

package io.shardingsphere.jdbc.orchestration.reg.newzk.client.cache;

import io.shardingsphere.core.parsing.lexer.LexerEngine;
import io.shardingsphere.core.parsing.lexer.token.Assist;
import io.shardingsphere.core.parsing.lexer.token.Symbol;
import io.shardingsphere.jdbc.orchestration.reg.newzk.client.utility.PathUtil;
import io.shardingsphere.jdbc.orchestration.reg.newzk.client.utility.ZookeeperConstants;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/*
 * Zookeeper node cache.
 *
 * @author lidongbo
 */
@Slf4j
public final class PathNode {
    
    private final Map<String, PathNode> children = new ConcurrentHashMap<>();

    private final String nodeKey;
    
    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    private String path;

    @Getter(value = AccessLevel.PACKAGE)
    @Setter(value = AccessLevel.PACKAGE)
    private byte[] value;
    
    PathNode(final String key) {
        this(key, ZookeeperConstants.RELEASE_VALUE);
    }
    
    PathNode(final String key, final byte[] value) {
        this.nodeKey = key;
        this.value = value;
        this.path = key;
    }
    
    /**
     * Get children.
     *
     * @return children
     */
    public Map<String, PathNode> getChildren() {
        return children;
    }
    
    /**
     * Get key.
     *
     * @return node key
     */
    public String getKey() {
        return this.nodeKey;
    }
    
    /**
     * Attach child node.
     *
     * @param node node
     */
    public void attachChild(final PathNode node) {
        this.children.put(node.nodeKey, node);
        node.setPath(PathUtil.getRealPath(path, node.getKey()));
    }
    
    PathNode set(final PathResolve pathResolve, final String value) {
        if (pathResolve.isEnd()) {
            setValue(value.getBytes(ZookeeperConstants.UTF_8));
            return this;
        }
        pathResolve.next();
        log.debug("PathNode set:{},value:{}", pathResolve.getCurrent(), value);
        if (children.containsKey(pathResolve.getCurrent())) {
            if (pathResolve.isEnd()) {
                PathNode result = children.get(pathResolve.getCurrent());
                result.setValue(value.getBytes(ZookeeperConstants.UTF_8));
                return result;
            } else {
                set(pathResolve, value);
            }
        }
        PathNode result;
        PathNode current = new PathNode(pathResolve.getCurrent());
        this.attachChild(current);
        do {
            pathResolve.next();
            result = new PathNode(pathResolve.getCurrent());
            current.attachChild(result);
            current = result;
        }
        while (!pathResolve.isEnd());
        return result;
    }
    
    PathNode get(final PathResolve pathResolve) {
        pathResolve.next();
        if (children.containsKey(pathResolve.getCurrent())) {
            if (pathResolve.isEnd()) {
                return children.get(pathResolve.getCurrent());
            }
            return children.get(pathResolve.getCurrent()).get(pathResolve);
        }
        return null;
    }
    
    void delete(final PathResolve pathResolve) {
        pathResolve.next();
        if (children.containsKey(pathResolve.getCurrent())) {
            if (pathResolve.isEnd()) {
                children.remove(pathResolve.getCurrent());
            } else {
                children.get(pathResolve.getCurrent()).delete(pathResolve);
            }
        }
    }
}
