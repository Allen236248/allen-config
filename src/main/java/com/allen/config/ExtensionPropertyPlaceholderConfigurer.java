package com.allen.config;

import com.allen.config.source.ExtensionPropertiesPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.core.env.StandardEnvironment;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class ExtensionPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements EnvironmentAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionPropertyPlaceholderConfigurer.class);

    private List<ExtensionPropertiesPropertySource> propertySourceList;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Properties mergeProperties() throws IOException {
        Properties properties = super.mergeProperties();
        if (null == properties) {
            properties = new Properties();
        }

        if (null == propertySourceList || propertySourceList.isEmpty())
            return properties;

        Collections.sort(propertySourceList);
        for (ExtensionPropertiesPropertySource propertySource : propertySourceList) {
            propertySource.loadProperties();

            for (String key : propertySource.getSource().keySet()) {
                if (properties.containsKey(key))
                    continue;

                properties.put(key, propertySource.getSource().get(key));
            }

            if (null != environment && environment instanceof StandardEnvironment) {
                ((StandardEnvironment) environment).getPropertySources().addLast(propertySource);
            }
        }
        return properties;
    }

    public List<ExtensionPropertiesPropertySource> getPropertySourceList() {
        return propertySourceList;
    }

    public void setPropertySourceList(List<ExtensionPropertiesPropertySource> propertySourceList) {
        this.propertySourceList = propertySourceList;
    }
}
