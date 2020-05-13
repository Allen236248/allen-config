package com.allen.config.listener;

import java.util.Map;

public class ChangeEvent {

    private Map<String, Change> changes;

    private String sourceName;

    public ChangeEvent(Map<String, Change> changes, String sourceName) {
        this.changes = changes;
        this.sourceName = sourceName;
    }

    public Map<String, Change> getChanges() {
        return changes;
    }

    public void setChanges(Map<String, Change> changes) {
        this.changes = changes;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public static class Change {

        private String key;

        private String oldValue;

        private String newValue;

        private ChangeType changeType;

        public Change(String key, String oldValue, String newValue, ChangeType changeType) {
            this.key = key;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.changeType = changeType;
        }

        public String getKey() {
            return key;
        }

        public void setKey(String key) {
            this.key = key;
        }

        public String getOldValue() {
            return oldValue;
        }

        public void setOldValue(String oldValue) {
            this.oldValue = oldValue;
        }

        public String getNewValue() {
            return newValue;
        }

        public void setNewValue(String newValue) {
            this.newValue = newValue;
        }

        public ChangeType getChangeType() {
            return changeType;
        }

        public void setChangeType(ChangeType changeType) {
            this.changeType = changeType;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder("Change{");
            sb.append("key:").append(key);
            sb.append(", oldValue:").append(oldValue);
            sb.append(", newValue:").append(newValue);
            sb.append(", changeType:").append(changeType);
            sb.append('}');
            return sb.toString();
        }
    }

    public enum ChangeType {
        ADDED, MODIFIED, DELETED
    }
}
