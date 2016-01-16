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
package org.apache.tamaya.mutableconfig;

import org.apache.tamaya.ConfigException;
import org.junit.Test;

import java.io.File;
import java.net.URI;

import static org.junit.Assert.*;

/**
 * Created by atsticks on 16.01.16.
 */
public class ConfigChangeManagerTest {

    @Test
    public void testCreateChangeRequest() throws Exception {
        File f = File.createTempFile("ConfigChangeRequest",".properties");
        ConfigChangeRequest req = ConfigChangeManager.createChangeRequest(f.toURI());
        assertNotNull(req);
        f = File.createTempFile("ConfigChangeRequest",".xml");
        req = ConfigChangeManager.createChangeRequest(f.toURI());
        assertNotNull(req);
    }

    @Test(expected=ConfigException.class)
    public void testInvalidCreateChangeRequest() throws Exception {
        ConfigChangeRequest req = ConfigChangeManager.createChangeRequest(new URI("foo:bar"));
    }

    @Test(expected=NullPointerException.class)
    public void testNullCreateChangeRequest() throws Exception {
        ConfigChangeRequest req = ConfigChangeManager.createChangeRequest(null);
    }
}