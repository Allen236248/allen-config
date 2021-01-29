package com.allen.config.source;

import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.core.env.PropertiesPropertySource;

import java.util.Properties;

public abstract class ExtensionPropertiesPropertySource extends PropertiesPropertySource implements PriorityOrdered,
        Comparable<ExtensionPropertiesPropertySource> {

    private int order = Ordered.LOWEST_PRECEDENCE;

    public ExtensionPropertiesPropertySource(String name) {
        super(name, new Properties());
    }

    @Override
    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public abstract void loadProperties();

    @Override
    public int compareTo(ExtensionPropertiesPropertySource o) {
        return this.getOrder() - o.getOrder();
    }

}
