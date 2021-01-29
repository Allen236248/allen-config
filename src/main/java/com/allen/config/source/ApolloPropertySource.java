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

/**
 * Apollo配置加载
 */
public final class ApolloPropertySource extends MutableExtensionPropertiesPropertySource {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApolloPropertySource.class);

    // 配置有Apollo app.id及app.namespaces的配置文件
    private final String APOLLO_FILE_LOCATION = "classpath*:/META-INF/app.properties";

    // Apollo命名空间配置KEY
    private final String APP_NAMESPACES = "app.namespaces";

    private Map<String, NamespacePropertiesHolder> namespacePropertiesHolderMap = new HashMap<>();

    public ApolloPropertySource() {
        super("APOLLO");
    }

    @Override
    public final void loadProperties() {
        Set<NamespacePropertiesHolder> namespacePropertiesHolders = new HashSet<>();
        namespacePropertiesHolders.add(new NamespacePropertiesHolder(ConfigConsts.NAMESPACE_APPLICATION));
        namespacePropertiesHolders.addAll(loadRelativeNamespaces());

        loadProperties(namespacePropertiesHolders);

        for (NamespacePropertiesHolder namespacePropertiesHolder : namespacePropertiesHolders) {
            Properties properties = namespacePropertiesHolder.getProperties();
            for (Object key : properties.keySet()) {
                String _key = (String) key;
                if (this.source.containsKey(_key))
                    continue;

                this.source.put(_key, properties.getProperty(_key));
            }
        }
    }

    /**
     * 读取项目配置的Apollo关联的namespace
     *
     * @return
     */
    protected Set<NamespacePropertiesHolder> loadRelativeNamespaces() {
        Set<NamespacePropertiesHolder> namespaces = new HashSet<>();

        List<Properties> propertiesList = PropertiesFileReader.readPropertiesList(APOLLO_FILE_LOCATION);
        if (CollectionUtils.isEmpty(propertiesList))
            return namespaces;

        for (Properties properties : propertiesList) {
            String value = properties.getProperty(APP_NAMESPACES);
            if (!StringUtils.hasText(value))
                continue;

            String[] namespaceArray = StringUtils.commaDelimitedListToStringArray(value);
            for (String namespace : namespaceArray) {
                if (StringUtils.hasText(value)) {
                    namespaces.add(new NamespacePropertiesHolder(namespace));
                }
            }
        }
        return namespaces;
    }

    /**
     * 优先级原则：
     * <ul>
     * <li>1、application命名空间具有最高优先级</li>
     * <li>2、应用关联配置的命名空间按照在apollo.properties配置文件配置的顺序优先级依次降低</li>
     * </ul>
     *
     * @param namespacePropertiesHolders
     * @return
     */
    private void loadProperties(Set<NamespacePropertiesHolder> namespacePropertiesHolders) {
        boolean isDefaultNamespaceLoaded = false;
        for (NamespacePropertiesHolder namespacePropertiesHolder : namespacePropertiesHolders) {
            String namespace = namespacePropertiesHolder.getNamespace();
            Config config = null;
            if (ConfigConsts.NAMESPACE_APPLICATION.equals(namespace)) {
                if (isDefaultNamespaceLoaded) {
                    throw new RuntimeException("The reserved name '" + namespace + "' can not be used in relative " +
                            "namespaces");
                }
                isDefaultNamespaceLoaded = true;
                config = ConfigService.getAppConfig();
            } else {
                config = ConfigService.getConfig(namespace);
            }

            if (config == null) {
                LOGGER.warn("Apollo服务器上不存在名称为{}的空间配置", namespace);
                continue;
            }

            Set<String> propertyNames = config.getPropertyNames();
            for (String propertyName : propertyNames) {
                String propertyValue = config.getProperty(propertyName, "");
                namespacePropertiesHolder.getProperties().put(propertyName, propertyValue);
            }

            //按照添加先后设置排序
            namespacePropertiesHolder.setOrder(namespacePropertiesHolderMap.size());
            namespacePropertiesHolderMap.put(namespace, namespacePropertiesHolder);
            // 配置改变监听
            config.addChangeListener(new ApolloConfigChangeListener());
        }
    }

    @Override
    protected void doRefreshProperties(PropertiesHolder propertiesHolder, Set<String> changedKeys) {
        if (propertiesHolder instanceof NamespacePropertiesHolder && null != changedKeys && changedKeys.size() > 0) {
            NamespacePropertiesHolder namespacePropertiesHolder = (NamespacePropertiesHolder) propertiesHolder;
            Set<String> copied = new HashSet<>();
            copied.addAll(changedKeys);
            for (String changedKey : copied) {
                boolean isPrior = true;
                Set<String> namespaces = namespacePropertiesHolderMap.keySet();
                for (String namespace : namespaces) {
                    NamespacePropertiesHolder np = namespacePropertiesHolderMap.get(namespace);
                    if (np.getProperties().containsKey(changedKey) && np.compareTo(namespacePropertiesHolder) < 0) {
                        isPrior = false;
                        changedKeys.remove(changedKey);
                        LOGGER.info("配置在namespace:{}中的{}具有更高优先级，当前namespace:{}中的修改将被忽略", np.getNamespace(),
                                changedKey, namespacePropertiesHolder.getNamespace());
                        break;
                    }
                }

                // 按照优先级，如果当前变更的key所在的namespace是key所在的所有namespace中优先级最高的，则更新应用使用的全局属性
                if (isPrior) {
                    Properties properties = namespacePropertiesHolder.getProperties();
                    if (!properties.containsKey(changedKey)) {
                        source.remove(changedKey);
                    } else {
                        source.put(changedKey, properties.getProperty(changedKey));
                    }
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return 100;
    }

    private class ApolloConfigChangeListener implements ConfigChangeListener {

        private final Logger LOGGER = LoggerFactory.getLogger(ApolloConfigChangeListener.class);

        @Override
        public void onChange(ConfigChangeEvent configChangeEvent) {
            String namespace = configChangeEvent.getNamespace();
            Set<String> changedKeys = configChangeEvent.changedKeys();
            for (String key : changedKeys) {
                ConfigChange change = configChangeEvent.getChange(key);
                LOGGER.info("Apollo配置变更通知：{}", change);

                if (change.getChangeType() == PropertyChangeType.DELETED) {
                    namespacePropertiesHolderMap.get(namespace).getProperties().remove(key);
                } else {
                    //增加或修改
                    namespacePropertiesHolderMap.get(namespace).getProperties().put(key, change.getNewValue());
                }
            }

            refreshProperties(namespacePropertiesHolderMap.get(namespace), new ChangeEvent.EventConverter() {

                @Override
                public ChangeEvent convert() {
                    Map<String, ChangeEvent.Change> changes = new HashMap<>();
                    for (String changedKey : configChangeEvent.changedKeys()) {
                        ConfigChange configChange = configChangeEvent.getChange(changedKey);
                        PropertyChangeType changeType = configChange.getChangeType();

                        String propertyName = configChange.getPropertyName();
                        String oldValue = configChange.getOldValue();
                        String newValue = configChange.getNewValue();

                        ChangeEvent.ChangeType type = ChangeEvent.ChangeType.values()[changeType.ordinal()];
                        ChangeEvent.Change change = new ChangeEvent.Change(propertyName, oldValue, newValue, type);
                        changes.put(changedKey, change);
                    }
                    return new ChangeEvent(ApolloPropertySource.this.getName(), changes);
                }
            });
        }

    }

    private class NamespacePropertiesHolder extends PropertiesHolder implements Comparable<NamespacePropertiesHolder> {

        private String namespace;

        private int order;

        private NamespacePropertiesHolder(String namespace) {
            super();
            this.namespace = namespace;
        }

        public String getNamespace() {
            return namespace;
        }

        public int getOrder() {
            return order;
        }

        public void setOrder(int order) {
            this.order = order;
        }

        @Override
        public int compareTo(NamespacePropertiesHolder o) {
            return this.getOrder() - o.getOrder();
        }
    }

}
