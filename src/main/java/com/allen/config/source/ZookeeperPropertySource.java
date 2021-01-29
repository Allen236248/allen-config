package com.allen.config.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * zookeeper配置加载
 */
public class ZookeeperPropertySource extends MutableExtensionPropertiesPropertySource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperPropertySource.class);

    public ZookeeperPropertySource() {
        super("ZOOKEEPER");
    }

    @Override
    public void loadProperties() {

    }

    @Override
    public int getOrder() {
        return 200;
    }

    @Override
    protected void doRefreshProperties(PropertiesHolder propertiesHolder, Set<String> changedKeys) {

    }
}
