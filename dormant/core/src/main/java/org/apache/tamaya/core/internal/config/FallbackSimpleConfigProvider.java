package org.apache.tamaya.core.internal.config;

import org.apache.tamaya.Configuration;
import org.apache.tamaya.core.properties.AggregationPolicy;
import org.apache.tamaya.core.properties.PropertySourceBuilder;
import old.ConfigurationProviderSpi;
import org.apache.tamaya.core.properties.PropertySourcesBuilder;

/**
 * Implementation of a default config provider used as fallback, if no {@link old.ConfigurationProviderSpi}
 * instance is registered for providing the {@code default} {@link org.apache.tamaya.Configuration}. The providers loads the follwing
 * config format:
 * <ul>
 *     <li>Classpath: META-INF/cfg/default/&#42;&#42;/&#42;.xml, META-INF/cfg/default/&#42;&#42;/&#42;.properties, META-INF/cfg/default/&#42;&#42;/&#42;.ini</li>
 *     <li>Classpath: META-INF/cfg/config/#42;#42;/#42;.xml, META-INF/cfg/config/#42;#42;/#42;.properties, META-INF/cfg/config/#42;#42;/#42;.ini</li>
 *     <li>Files: defined by the system property -Dconfig.dir</li>
 *     <li>system properties</li>
 * </ul>
 */
public class FallbackSimpleConfigProvider implements ConfigurationProviderSpi {

    private static final String DEFAULT_CONFIG_NAME = "default";

    /**
     * The loaded configuration instance.
     */
    private volatile Configuration configuration;

    @Override
    public String getConfigName() {
        return DEFAULT_CONFIG_NAME;
    }

    @Override
    public Configuration getConfiguration() {
        Configuration cfg = configuration;
        if (cfg == null) {
            reload();
            cfg = configuration;
        }
        return cfg;
    }


    @Override
    public void reload() {
        this.configuration = Configuration.from(
                PropertySourcesBuilder.of()
                                .addPaths("META-INF/cfg/default/**/*.xml", "META-INF/cfg/default/**/*.properties", "META-INF/cfg/default/**/*.ini")
                                .build())
                        .addProviders(PropertySourcesBuilder.of("CL default")
                                .withAggregationPolicy(AggregationPolicy.LOG_ERROR)
                                .addPaths("META-INF/cfg/config/**/*.xml", "META-INF/cfg/config/**/*.properties", "META-INF/cfg/config/**/*.ini")
                                .build())
                        .addSystemProperties()
                        .addEnvironmentProperties()
                        .build());
    }
}