package com.allen.config;

import com.allen.config.source.AbstractConfigSourcePropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.OrderComparator;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class ConfigSourcePropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer implements InitializingBean, ApplicationContextAware {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigSourcePropertyPlaceholderConfigurer.class);

    private ApplicationContext applicationContext;

    private List<AbstractConfigSourcePropertySource> configSources;

    /**
     * 管理所有配置源配置的属性
     */
    private Properties properties = new Properties();

    private void loadConfigSourceProperties(Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (properties.containsKey(key))
                continue;

            Object value = entry.getValue();
            if (value instanceof Map) {
                //嵌套加载配置信息
                loadConfigSourceProperties((Map<String, Object>) value);
            }

            this.properties.put(key, value);
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (CollectionUtils.isEmpty(configSources)) {
            LOGGER.warn("配置源为空");
            return;
        }

        OrderComparator.sort(configSources);
        for (AbstractConfigSourcePropertySource configSource : configSources) {
            // 加载配置源的属性
            configSource.loadProperties();

            // 按照配置源的顺序将其加入Spring环境中。相同属性配置信息，在优先级高的配置源中生效
            StandardEnvironment environment = (StandardEnvironment) applicationContext.getEnvironment();
            environment.getPropertySources().addLast(configSource);

            Map<String, Object> source = configSource.getSource();
            // 将配置源配置的属性加载到当前属性管理器中
            loadConfigSourceProperties(source);
        }

        // 将自定义加载的properties加入spring管理中
        this.setProperties(properties);

        // 调用合并方法，合并配置
        this.mergeProperties();

        LOGGER.info("加载所有配置源信息完成");
    }

    public void setConfigSources(List<AbstractConfigSourcePropertySource> configSources) {
        this.configSources = configSources;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}
