package com.allen.config.source;

import com.allen.config.listener.ChangeEvent;
import com.allen.config.utils.PropertiesFileReader;
import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.enums.PropertyChangeType;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Apollo配置加载
 */
public class ApolloConfigSourcePropertySource extends AbstractConfigSourcePropertySource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloConfigSourcePropertySource.class);

    // 配置有Apollo app.id及app.namespaces的配置文件
    private static final String APP_FILE_LOCATION = "classpath*:/META-INF/app.properties";

    // Apollo命名空间配置KEY
    private static final String APP_NAMESPACES = "app.namespaces";

    private static final String SOURCE_NAME = "APOLLO";

    private static final String SEPARATOR_COMMA = ",";

    private ReadWriteLock lock = new ReentrantReadWriteLock();

    private Map<String, Map<String, Object>> namespaceProperties = new LinkedHashMap<>();

    public ApolloConfigSourcePropertySource() {
        super(SOURCE_NAME, new LinkedHashMap<>());
    }

    @Override
    public void loadProperties() {
        try {
            lock.writeLock().lock();

            if (!source.isEmpty()) {
                this.source.clear();
            }

            // 加载application配置
            loadApplicationNamespaceProperties();

            // 加载关联配置
            loadRelativeNamespacesProperties();

            this.source.putAll(mergeProperties());
        } catch (Exception e) {
            logger.error("Load config from apollo failure", e);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * 加载application命名空间的配置。Apollo在创建项目后此命名空间自动生成
     */
    private void loadApplicationNamespaceProperties() {
        Config config = ConfigService.getAppConfig();

        loadApolloConfig(config, ConfigConsts.NAMESPACE_APPLICATION);
    }

    /**
     * 加载项目关联的命名空间的配置
     */
    private void loadRelativeNamespacesProperties() {
        Set<String> namespaces = getNamespaces(APP_NAMESPACES);
        if (CollectionUtils.isEmpty(namespaces)) {
            LOGGER.info("项目关联的命名空间配置为空");
            return;
        }

        for (String namespace : namespaces) {
            Config config = ConfigService.getConfig(namespace);
            if (config == null) {
                LOGGER.warn("Apollo服务器上不存在名称为{}的空间配置，请检查是否已添加关联配置", namespace);
                continue;
            }

            loadApolloConfig(config, namespace);
        }
    }

    /**
     * 获取项目配置的命名空间，包括引入包的命名空间配置
     *
     * @param key
     * @return
     */
    public Set<String> getNamespaces(String key) {
        Set<String> namespaces = new LinkedHashSet<>();

        List<Properties> propertiesList = PropertiesFileReader.readPropertiesList(APP_FILE_LOCATION);
        if (CollectionUtils.isEmpty(propertiesList))
            return namespaces;

        StringBuilder namespaceStr = new StringBuilder();

        for (Properties properties : propertiesList) {
            if (properties.containsKey(key)) {
                String value = properties.getProperty(key);
                if (StringUtils.hasText(value)) {
                    namespaceStr.append(value.trim()).append(SEPARATOR_COMMA);
                }
            }
        }

        if (namespaceStr.length() == 0)
            return namespaces;

        namespaceStr.deleteCharAt(namespaceStr.length() - 1);

        String[] namespaceArray = namespaceStr.toString().split(SEPARATOR_COMMA);
        for (String namespace : namespaceArray) {
            if (StringUtils.hasText(namespace)) {
                namespaces.add(namespace);
            }
        }
        return namespaces;
    }

    /**
     * 合并多个namespace的配置，优先级低的namespace中的同名配置将被忽略。
     * 优先级原则：首先是application命名空间，其次应用配置文件按照namespace配置顺序优先级依次降低
     *
     * @return
     */
    private Map<String, Object> mergeProperties() {
        Map<String, Object> allProperties = new HashMap<>();
        for (Map<String, Object> properties : namespaceProperties.values()) {
            for (Map.Entry<String, Object> entry : properties.entrySet()) {
                String key = entry.getKey();
                if (allProperties.containsKey(key))
                    continue;

                allProperties.put(key, entry.getValue());
            }
        }
        return allProperties;
    }

    private void loadApolloConfig(Config config, String namespace) {
        Set<String> propertyNames = config.getPropertyNames();

        Map<String, Object> properties = new LinkedHashMap<>();
        for (String propertyName : propertyNames) {
            properties.put(propertyName, config.getProperty(propertyName, ""));
        }
        namespaceProperties.put(namespace, properties);

        // 配置改变刷新
        config.addChangeListener(new ApolloConfigChangeListener());
    }

    @Override
    public int getOrder() {
        return 100;
    }

    private class ApolloConfigChangeListener implements ConfigChangeListener {

        private final Logger LOGGER = LoggerFactory.getLogger(ApolloConfigChangeListener.class);

        @Override
        public void onChange(ConfigChangeEvent changeEvent) {
            String namespace = changeEvent.getNamespace();
            Set<String> changedKeys = changeEvent.changedKeys();
            Set<String> firedKey = new HashSet<>();
            for (String key : changedKeys) {
                ConfigChange change = changeEvent.getChange(key);
                if (change.getChangeType().ordinal() == PropertyChangeType.DELETED.ordinal()) {
                    LOGGER.info("收到Apollo配置删除通知，命名空间：{}，Key:{}", namespace, key);
                    namespaceProperties.get(namespace).remove(key);
                } else {
                    String oldValue = change.getOldValue();
                    String newValue = change.getNewValue();
                    LOGGER.info("收到Apollo配置修改通知，命名空间：{}，Key:{}，旧值：{}，新值：{}", namespace, key, oldValue, newValue);
                    namespaceProperties.get(namespace).put(key, newValue);
                }

                // 按照优先级，如果当前变更的key所在的namespace是key所在的所有namespace中优先级最高的，则更新应用使用的全局属性
                if (isPriorityNamespace(namespace, key)) {
                    firedKey.add(key);
                    if (change.getChangeType().ordinal() == PropertyChangeType.DELETED.ordinal()) {
                        source.remove(key);
                    } else {
                        source.put(key, change.getNewValue());
                    }
                }
            }

            if (firedKey.size() > 0) {
                ChangeEvent event = convertToLocalEvent(changeEvent);
                publishEvent(event);
            }
        }

        /**
         * 获取key第一次出现的命名空间
         *
         * @param key
         * @return
         */
        private boolean isPriorityNamespace(String namespace, String key) {
            Set<String> namespaces = namespaceProperties.keySet();
            for (String ns : namespaces) {
                Map<String, Object> properties = namespaceProperties.get(ns);
                if (properties.containsKey(key) && ns.equals(namespace)) {
                    return true;
                }
            }
            return false;
        }

        private ChangeEvent convertToLocalEvent(ConfigChangeEvent changeEvent) {
            Map<String, ChangeEvent.Change> changes = new HashMap<>();
            for (String key : changeEvent.changedKeys()) {
                ConfigChange configChange = changeEvent.getChange(key);
                PropertyChangeType changeType = configChange.getChangeType();

                String propertyName = configChange.getPropertyName();
                String oldValue = configChange.getOldValue();
                String newValue = configChange.getNewValue();

                ChangeEvent.ChangeType type = ChangeEvent.ChangeType.values()[changeType.ordinal()];
                ChangeEvent.Change change = new ChangeEvent.Change(propertyName, oldValue, newValue, type);
                changes.put(key, change);
            }
            return new ChangeEvent(changes, SOURCE_NAME);
        }

    }
}
