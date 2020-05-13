package com.allen.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public final class ConfigLoader implements ApplicationContextAware {

    private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);

    private static final String SEPARATOR_COMMA = ",";

    protected static ApplicationContext applicationContext;

    public int getIntProperty(String key, int defaultValue) {
        try {
            String value = getProperty(key);
            return value == null ? defaultValue : Integer.parseInt(value);
        } catch (Exception e) {
            logger.error("Get int property failed with key :{}", key, e);
            return defaultValue;
        }
    }

    public long getLongProperty(String key, int defaultValue) {
        try {
            String value = getProperty(key);
            return value == null ? defaultValue : Long.parseLong(value);
        } catch (Exception e) {
            logger.error("Get int property failed with key :{}", key, e);
            return defaultValue;
        }
    }

    public double getDoubleProperty(String key, int defaultValue) {
        try {
            String value = getProperty(key);
            return value == null ? defaultValue : Double.parseDouble(value);
        } catch (Exception e) {
            logger.error("Get int property failed with key :{}", key, e);
            return defaultValue;
        }
    }

    public boolean getBooleanProperty(String key, boolean defaultValue) {
        try {
            String value = getProperty(key);
            return value == null ? defaultValue : "true".equals(value);
        } catch (Exception e) {
            logger.error("Get boolean property failed with key :{}", key, e);
            return defaultValue;
        }
    }

    public String[] getArrayProperty(String key, String[] defaultValue) {
        return getArrayProperty(key, defaultValue, SEPARATOR_COMMA);
    }

    public String[] getArrayProperty(String key, String[] defaultValue, String separator) {
        try {
            String value = getProperty(key);
            return StringUtils.isEmpty(value) ? defaultValue : value.split(separator);
        } catch (Exception e) {
            logger.error("Get array property failed with key :{}", key, e);
            return defaultValue;
        }
    }

    public String getProperty(String key) {
        return getEnvironment().getProperty(key);
    }

    public String getProperty(String key, String defaultValue) {
        try {
            return getEnvironment().getProperty(key, defaultValue);
        } catch (Exception e) {
            logger.error("Get value failed with key :{}", key, e);
            return defaultValue;
        }
    }

    public boolean containsProperty(String key) {
        return getEnvironment().containsProperty(key);
    }

    public StandardEnvironment getEnvironment() {
        return (StandardEnvironment) applicationContext.getEnvironment();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
