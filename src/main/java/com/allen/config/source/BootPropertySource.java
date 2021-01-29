package com.allen.config.source;

import com.allen.config.utils.PropertiesFileReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Map;
import java.util.Properties;

/**
 * 本地Boot文件配置加载
 */
public final class BootPropertySource extends ExtensionPropertiesPropertySource {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootPropertySource.class);

    public BootPropertySource() {
        super("BOOT_FILE");
    }

    @Override
    public void loadProperties() {
        try {
            // 本地boot文件路径
            String fileName = "file:" + System.getProperty("user.home") + File.separatorChar +
                    ".yunnex" + File.separatorChar + "boot";
            Properties properties = PropertiesFileReader.readProperties(fileName);
            this.source.putAll((Map) properties);
        } catch (Exception e) {
            logger.error("加载Boot文件配置失败", e);
        }
    }

    @Override
    public int getOrder() {
        return 50;
    }

}
