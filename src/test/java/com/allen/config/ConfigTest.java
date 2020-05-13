package com.allen.config;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

public class ConfigTest extends Launcher {

    //@Value("${app.name}")
    //private String appName;

    @Value("${config.server.url}")
    private String configServerUrl;

    @Value("${dubbo.registry.protocol}")
    private String dubboRegistryProtocol;

    @Autowired
    private ConfigLoader configLoader;

    @Autowired
    private Environment env;

    @Test
    public void testLoadPropertiesByKey() {
        //System.out.println(configLoader.getProperty("app.name")); // from application.properties
        System.out.println(configLoader.getProperty("dubbo.registry.protocol"));// from apollo file
        System.out.println(configLoader.getProperty("config.server.url"));// from boot file
        System.out.println(configLoader.getProperty("env"));
    }

    @Test
    public void testInjectProperties() {
        //org.junit.Assert.assertNotNull(appName); // from app.properties
        System.out.println(configServerUrl);// from boot file
        System.out.println(dubboRegistryProtocol);// from apollo file
    }

    @Test
    public void test测试profile激活() {
        org.junit.Assert.assertSame(1, env.getActiveProfiles().length);;
    }

    @Test
    public void testChangeListener() {
        try {
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


}
