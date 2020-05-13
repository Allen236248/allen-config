package com.allen.config.source;

import com.allen.config.listener.ChangeEvent;
import com.allen.config.listener.ChangeEventPublisher;
import com.allen.config.listener.ChangeListener;
import com.allen.config.utils.SpringContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public abstract class AbstractConfigSourcePropertySource extends MapPropertySource implements ChangeEventPublisher, PriorityOrdered {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigSourcePropertySource.class);

    public AbstractConfigSourcePropertySource(String name, Map<String, Object> source) {
        super(name, source);
    }

    @Override
    public void publishEvent(ChangeEvent event) {
        Map<String, ChangeListener> listeners = SpringContextHolder.getBeans(ChangeListener.class);
        if (CollectionUtils.isEmpty(listeners)) {
            LOGGER.info("未检查到配置源属性变更监听器");
            return;
        }

        Map<String, ChangeEvent.Change> filteredChanges = new HashMap<>();

        StandardEnvironment environment = SpringContextHolder.getBean(StandardEnvironment.class);
        // 判断是否有有必要发布变更
        String sourceName = event.getSourceName();
        Map<String, ChangeEvent.Change> changes = event.getChanges();
        if (CollectionUtils.isEmpty(changes)) {
            return;
        }
        Iterator<String> keySet = changes.keySet().iterator();
        while (keySet.hasNext()) {
            String key = keySet.next();
            // 判断key出现的配置源是否为最高优先级的配置源
            if (isPrioritySource(environment, sourceName, key)) {
                filteredChanges.put(key, changes.get(key));
            }
        }
        if (CollectionUtils.isEmpty(filteredChanges)) {
            LOGGER.info("没有要发布的配置变更。配置源：{}", sourceName);
            return;
        }
        event.setChanges(filteredChanges);
        for (ChangeListener listener : listeners.values()) {
            listener.onChange(event);
        }
    }

    /**
     * 获取key第一次出现的配置源，为优先级最高的配置源
     *
     * @param key
     * @return
     */
    private boolean isPrioritySource(StandardEnvironment environment, String sourceName, String key) {
        Iterator<PropertySource<?>> iterator = environment.getPropertySources().iterator();
        while (iterator.hasNext()) {
            PropertySource<?> source = iterator.next();
            if (source.containsProperty(key) && source.getName().equals(sourceName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 加载配置源数据
     */
    public abstract void loadProperties();

}
