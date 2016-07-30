/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package tamaya.config.spi;

import tamaya.config.Config;

/**
 * <p>A filter which can be added to the
 * {@link Config}.
 * The filter can be used to decrypt config values or prepare
 * values for logging.</p>
 *
 * <p>Registering a {@code ConfigFilter} can be done via the
 * {@code java.util.ServiceLoader} pattern.</p>
 *
 * @author <a href="mailto:struberg@apache.org">Mark Struberg</a>
 */
public interface ConfigFilter
{
    /**
     * Filter the given configuration value
     * @return the filtered value or the original input String if no filter shall be applied
     */
    String filterValue(String key, String value);

    /**
     * Filter the given configuration value for usage in logs.
     * This might be used to mask out passwords, etc.
     * @return the filtered value or the original input String if no filter shall be applied
     */
    String filterValueForLog(String key, String value);
}
