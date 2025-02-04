package net.blay09.mods.balm.notoml;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;

import java.util.ArrayList;
import java.util.List;

public class Notoml {
    private final List<NotomlError> errors = new ArrayList<>();
    private final Table<String, String, Object> properties = HashBasedTable.create();
    private final Table<String, String, String> comments = HashBasedTable.create();

    public Notoml() {
    }

    public Notoml(Table<String, String, Object> properties, Table<String, String, String> comments) {
        this.properties.putAll(properties);
        this.comments.putAll(comments);
    }

    public List<NotomlError> getErrors() {
        return errors;
    }

    public Table<String, String, Object> getProperties() {
        return properties;
    }

    public Table<String, String, String> getComments() {
        return comments;
    }

    public void setProperty(String category, String property, Object value) {
        properties.put(category, property, value);
    }

    public void setComment(String category, String property, String comment) {
        comments.put(category, property, comment);
    }

    public void addError(NotomlError error) {
        errors.add(error);
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public boolean containsProperties(Notoml other) {
        for (Table.Cell<String, String, Object> cell : other.properties.cellSet()) {
            if (!properties.contains(cell.getRowKey(), cell.getColumnKey())) {
                return false;
            }
        }
        return true;
    }
}
