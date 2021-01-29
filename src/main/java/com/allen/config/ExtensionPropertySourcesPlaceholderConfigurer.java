package com.allen.config;

import com.allen.config.source.ExtensionPropertiesPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySourcesPropertyResolver;
import org.springframework.core.env.StandardEnvironment;

import java.util.List;

public class ExtensionPropertySourcesPlaceholderConfigurer extends PropertySourcesPlaceholderConfigurer implements
        EnvironmentAware {

    private final Logger LOGGER = LoggerFactory.getLogger(ExtensionPropertySourcesPlaceholderConfigurer.class);

    private List<ExtensionPropertiesPropertySource> propertySourceList;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        super.postProcessBeanFactory(beanFactory);

        if (null == propertySourceList || propertySourceList.isEmpty())
            return;

        MutablePropertySources propertySources = new MutablePropertySources();
        for (ExtensionPropertiesPropertySource propertySource : propertySourceList) {
            propertySource.loadProperties();
            propertySources.addLast(propertySource);

            if (null != environment && environment instanceof StandardEnvironment) {
                ((StandardEnvironment) environment).getPropertySources().addLast(propertySource);
            }
        }
        processProperties(beanFactory, new PropertySourcesPropertyResolver(propertySources));
    }

    public List<ExtensionPropertiesPropertySource> getPropertySourceList() {
        return propertySourceList;
    }

    public void setPropertySourceList(List<ExtensionPropertiesPropertySource> propertySourceList) {
        this.propertySourceList = propertySourceList;
    }
}
