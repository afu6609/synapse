package com.synapse.embedding.event;

import org.springframework.context.ApplicationEvent;
import java.util.List;
import java.util.Map;

public class ConfigChangedEvent extends ApplicationEvent {
    private final List<String> changedFields;
    private final Map<String, Object> configSnapshot;

    public ConfigChangedEvent(Object source, List<String> changedFields, Map<String, Object> configSnapshot) {
        super(source);
        this.changedFields = changedFields;
        this.configSnapshot = configSnapshot;
    }

    public List<String> getChangedFields() { return changedFields; }
    public Map<String, Object> getConfigSnapshot() { return configSnapshot; }
}