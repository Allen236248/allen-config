package com.allen.config.listener;

public interface ChangeEventPublisher {

    void fireChangeEvent(ChangeEvent changeEvent);

}
