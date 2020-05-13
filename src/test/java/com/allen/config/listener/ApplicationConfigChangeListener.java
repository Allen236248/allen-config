package com.allen.config.listener;

import org.springframework.stereotype.Component;

@Component
public class ApplicationConfigChangeListener implements ChangeListener {

    @Override
    public void onChange(ChangeEvent changeEvent) {
        for (String key : changeEvent.getChanges().keySet()) {
            System.out.println("changed:" + key + ":" + changeEvent.getChanges().get(key));
        }
    }
}
