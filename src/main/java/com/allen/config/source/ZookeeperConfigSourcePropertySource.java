package com.allen.config.source;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

/**
 * zookeeper配置加载
 */
public class ZookeeperConfigSourcePropertySource extends AbstractConfigSourcePropertySource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZookeeperConfigSourcePropertySource.class);

    private static final String SOURCE_NAME = "ZOOKEEPER";

    public ZookeeperConfigSourcePropertySource() {
        super(SOURCE_NAME, new LinkedHashMap<>());
    }

    @Override
    public void loadProperties() {

    }

    @Override
    public int getOrder() {
        return 200;
    }
}
