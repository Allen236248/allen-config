package com.allen.config.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class PropertiesFileReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesFileReader.class);

    /**
     * 读取classpath中匹配的配置文件，支持classpath*
     *
     * 如果从classpath中匹配到多个文件,只返回第一个
     *
     * @param fileName 文件名称，可以是classpath下面的文件
     * @return
     */
    public static Properties readProperties(String fileName) {
        List<Properties> propertiesList = readPropertiesList(fileName);
        if (CollectionUtils.isEmpty(propertiesList)) {
            return new Properties();
        }
        return propertiesList.get(0);
    }

    /**
     * 读取classpath中匹配的配置文件，支持classpath*
     *
     * @param fileName
     * @return
     */
    public static List<Properties> readPropertiesList(String fileName) {
        List<Properties> propertiesList = new ArrayList<>();
        InputStream is = null;
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(fileName);
            if (resources == null || resources.length == 0) {
                return propertiesList;
            }

            for (Resource resource : resources) {
                try {
                    Properties properties = new Properties();
                    is = resource.getInputStream();
                    properties.load(is);
                    propertiesList.add(properties);
                } catch (IOException e) {
                    LOGGER.error("读取配置文件" + fileName + "失败", e);
                }
            }
        } catch (IOException e) {
            LOGGER.error("读取配置文件" + fileName + "失败", e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    LOGGER.warn("关闭文件输入流失败", e);
                }
            }
        }
        return propertiesList;
    }
}
