package com.allen.config.source;

import com.allen.config.listener.ChangeEvent;
import com.allen.config.listener.ChangeEventPublisher;
import com.allen.config.listener.ChangeListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

import java.util.*;

public abstract class MutableExtensionPropertiesPropertySource extends ExtensionPropertiesPropertySource implements
        ApplicationContextAware, ChangeEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(MutableExtensionPropertiesPropertySource.class);

    private int order = Ordered.LOWEST_PRECEDENCE;

    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    public MutableExtensionPropertiesPropertySource(String name) {
        super(name);
    }

    public void refreshProperties(PropertiesHolder propertiesHolder, ChangeEvent.EventConverter converter) {
        ChangeEvent event = converter.convert();
        Set<String> changedKeys = event.getChanges().keySet();
        doRefreshProperties(propertiesHolder, changedKeys);

        fireChangeEvent(event);
    }

    protected abstract void doRefreshProperties(PropertiesHolder propertiesHolder, Set<String> changedKeys);

    @Override
    public void fireChangeEvent(ChangeEvent event) {
        Map<String, ChangeListener> listeners = applicationContext.getBeansOfType(ChangeListener.class);
        if (CollectionUtils.isEmpty(listeners)) {
            LOGGER.info("未检查到配置源属性变更监听器");
            return;
        }

        // 判断是否有有必要发布变更
        Map<String, ChangeEvent.Change> changes = event.getChanges();
        if (CollectionUtils.isEmpty(changes)) {
            return;
        }
        LOGGER.info("准备发布配置修改事件：{}", event);

        Set<String> changedKeys = new HashSet<>();
        changedKeys.addAll(changes.keySet());
        for (String changedKey : changedKeys) {
            // 判断key出现的配置源是否为最高优先级的配置源
            if (!isPrioritySource(changedKey)) {
                changes.remove(changedKey);
            }
        }

        if (!CollectionUtils.isEmpty(changes)) {
            LOGGER.info("发布配置修改事件：{}", event);
            for (ChangeListener listener : listeners.values()) {
                listener.onChange(event);
            }
        }
    }

    /**
     * 当前自定义的扩展配置源是否具有更高优先级
     *
     * @param key
     * @return
     */
    private boolean isPrioritySource(String key) {
        StandardEnvironment environment = (StandardEnvironment) applicationContext.getEnvironment();
        Iterator<PropertySource<?>> iterator = environment.getPropertySources().iterator();
        while (iterator.hasNext()) {
            PropertySource<?> source = iterator.next();
            if (source.containsProperty(key)) {
                if (!(source instanceof ExtensionPropertiesPropertySource)) {
                    //非自定义的扩展配置源具有更高优先级
                    LOGGER.info("配置在{}中的{}具有更高优先级，当前{}中的修改将被忽略", source.getName(), key, this.getName());
                    return false;
                }

                //自定义的扩展配置源order越小，优先级越高
                ExtensionPropertiesPropertySource e_source = (ExtensionPropertiesPropertySource) source;
                if (e_source.compareTo(this) < 0) {
                    LOGGER.info("配置在{}中的{}具有更高优先级，当前{}中的修改将被忽略", source.getName(), key, this.getName());
                    return false;
                }
            }
        }
        return true;
    }

    public static class PropertiesHolder {

        private Properties properties;

        public PropertiesHolder() {
            this.properties = new Properties();
        }

        public Properties getProperties() {
            return properties;
        }

        public void setProperties(Properties properties) {
            this.properties = properties;
        }
    }

}
