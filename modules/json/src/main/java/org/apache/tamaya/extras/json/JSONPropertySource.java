/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.tamaya.extras.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.tamaya.ConfigException;
import org.apache.tamaya.spi.PropertySource;

import java.io.File;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static java.lang.String.format;

public class JSONPropertySource
    implements PropertySource {
    private int priority = 0;
    private InputResource source;
    private HashMap<String, String> values;

    public JSONPropertySource(File file, int priority) {
        this.priority = priority;
        source = new FileBasedResource(file);
    }

    @Override
    public int getOrdinal() {
        return priority;
    }

    @Override
    public String getName() {
        // @todo Implement me
        throw new RuntimeException("Not implemented yet.");
    }

    @Override
    public String get(String key) {
        Objects.requireNonNull(key, "Key must not be null");

        return getProperties().get(key);
    }

    @Override
    public Map<String, String> getProperties() {
        synchronized (this) {
            if (values == null) {
                readSource();
            }
        }

        return Collections.unmodifiableMap(values);
    }

    protected void readSource() {
        try (InputStream is = source.getInputStream()) {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(is);

            // @todo Add test for this. Oliver B. Fischer, 5. Jan. 2015
            if (!(root instanceof ObjectNode)) {
                throw new ConfigException("Currently only JSON objects are supported");
            }

            HashMap<String, String> values = new HashMap<>();
            JSONVisitor visitor = new JSONVisitor((ObjectNode) root, values);
            visitor.run();

            this.values = values;
        }
        catch (Throwable t) {
            throw new ConfigException(format("Failed to read properties from %s", source.getDescription()), t);
        }

    }

    @Override
    public boolean isScannable() {
        // @todo Implement me
        throw new RuntimeException("Not implemented yet.");
    }
}
