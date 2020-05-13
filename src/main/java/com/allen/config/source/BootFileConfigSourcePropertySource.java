package com.allen.config.source;

import com.allen.config.utils.PropertiesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Properties;

/**
 * Boot文件配置加载
 */
public class BootFileConfigSourcePropertySource extends AbstractConfigSourcePropertySource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootFileConfigSourcePropertySource.class);

    // boot文件路径
    private static final String BOOT_FILE_LOCATION = "file:" + System.getProperty("user.home") + File.separatorChar + ".yunnex" + File.separatorChar + "boot";

    private static final String SOURCE_NAME = "BOOT_FILE";

    public BootFileConfigSourcePropertySource() {
        super(SOURCE_NAME, new LinkedHashMap<>());
    }

    @Override
    public void loadProperties() {
        try {
            if (!source.isEmpty()) {
                this.source.clear();
            }
            Properties properties = PropertiesFileReader.readProperties(BOOT_FILE_LOCATION);
            Enumeration<?> enumeration = properties.propertyNames();
            while(enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                this.source.put(key, properties.getProperty(key));
            }
        } catch (Exception e) {
            logger.error("加载Boot文件配置失败", e);
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
