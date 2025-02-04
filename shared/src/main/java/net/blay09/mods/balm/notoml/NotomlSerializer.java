package net.blay09.mods.balm.notoml;

import java.io.IOException;
import java.io.Writer;
import java.util.List;
import java.util.Map;

public class NotomlSerializer {

    public static void serialize(Writer writer, Notoml notoml) throws IOException {
        writer.write(serializeToString(notoml));
    }

    private static String serializeToString(Notoml notoml) {
        StringBuilder sb = new StringBuilder();

        var data = notoml.getProperties();
        var comments = notoml.getComments();
        var sortedCategoryKeys = data.rowKeySet().stream().sorted().toList();
        for (String category : sortedCategoryKeys) {
            String categoryComment = comments.get(category, "");
            if (categoryComment != null) {
                sb.append("\n").append("# ").append(categoryComment).append("\n");
            }

            if (!category.isEmpty()) {
                sb.append("[").append(category).append("]").append("\n");
            }

            Map<String, Object> categoryProperties = data.row(category);
            var sortedPropertyKeys = categoryProperties.keySet().stream().sorted().toList();
            for (String property : sortedPropertyKeys) {
                String propertyComment = comments.get(category, property);
                if (propertyComment != null) {
                    sb.append("\n").append("# ").append(propertyComment).append("\n");
                }

                sb.append(property).append(" = ");

                Object value = categoryProperties.get(property);
                if (value instanceof String stringValue) {
                    if (stringValue.contains("\n")) {
                        sb.append("\"\"\"\n").append(value).append("\n\"\"\"");
                    } else {
                        sb.append("\"").append(stringValue.replace("\"", "\\\"")).append("\"");
                    }
                } else if (value instanceof List<?> listValue) {
                    serializeList(listValue, sb);
                } else if (value instanceof Enum<?> enumValue) {
                    sb.append("\"").append(enumValue.name()).append("\"");
                } else {
                    sb.append(value);
                }

                sb.append("\n");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private static String serializeList(List<?> list, StringBuilder sb) {
        sb.append("[ ");
        boolean newLines = list.size() > 3;
        if (newLines) {
            sb.append("\n");
        }
        for (int i = 0; i < list.size(); i++) {
            if (newLines) {
                sb.append("    ");
            }
            if (list.get(i) instanceof String) {
                sb.append("\"").append(((String) list.get(i)).replace("\"", "\\\"")).append("\"");
            } else if (list.get(i) instanceof Enum<?>) {
                sb.append("\"").append(((Enum<?>) list.get(i)).name()).append("\"");
            } else {
                sb.append(list.get(i));
            }
            if (i != list.size() - 1) {
                sb.append(", ");
            }
            if (newLines) {
                sb.append("\n");
            }
        }
        sb.append(" ]");
        return sb.toString();
    }

}
