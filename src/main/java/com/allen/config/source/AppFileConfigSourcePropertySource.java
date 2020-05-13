package com.allen.config.source;

import com.allen.config.utils.PropertiesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * app.properties文件配置加载
 */
public class AppFileConfigSourcePropertySource extends AbstractConfigSourcePropertySource {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppFileConfigSourcePropertySource.class);

    // boot文件路径
    private static final String APP_FILE_LOCATION = "classpath*:/META-INF/app.properties";

    private static final String SOURCE_NAME = "APP_FILE";

    private Map<String, Map<String, Object>> namespaceProperties = new LinkedHashMap<>();

    public AppFileConfigSourcePropertySource() {
        super(SOURCE_NAME, new LinkedHashMap<>());
    }

    @Override
    public void loadProperties() {
        try {
            if (!source.isEmpty()) {
                this.source.clear();
            }
            Properties properties = PropertiesFileReader.readProperties(APP_FILE_LOCATION);
            Enumeration<?> enumeration = properties.propertyNames();
            while(enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                this.source.put(key, properties.getProperty(key));
            }
        } catch (Exception e) {
            logger.error("加载app.properties文件配置失败", e);
        }
    }

    @Override
    public int getOrder() {
        return 10;
    }
}
