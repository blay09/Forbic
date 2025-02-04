package net.blay09.mods.balm.notoml;

import java.util.regex.Pattern;

public enum NotomlParserState {
    None {
        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            buffer.consumeWhitespace();
            if (buffer.nextConsume("[")) {
                state.transition(Category);
            } else if (buffer.nextConsume("#")) {
                state.transition(Comment);
            } else {
                state.transition(Property);
            }
        }
    },
    Category {
        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            String categoryName = buffer.readUntilConsume("]").trim();
            if (categoryName.isEmpty()) {
                throw new IllegalArgumentException("Expected a category name");
            }
            consumer.emitCategory(categoryName);
            state.transition(None);
        }
    },
    Comment {
        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            String comment = buffer.readUntilConsume("\n", "\r\n", "\r").trim();
            consumer.emitComment(comment);
            state.transition(None);
        }
    },
    Property {
        private static final Pattern PROPERTY_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9_\\-]+");

        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            String property = buffer.readUntilConsume("=", "\n", "\r\n", "\r").trim();
            if (!PROPERTY_KEY_PATTERN.matcher(property).matches()) {
                throw new IllegalStateException("Invalid property key '" + property + "' (properties may only contain letters, numbers, dashes and underscores)");
            }
            consumer.emitPropertyKey(property);
            state.transition(PropertyValue);
        }
    },
    PropertyValue {
        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            buffer.consumeWhitespace();
            if (buffer.nextConsume("\"\"\"")) {
                consumer.emitMultiLineStringStart();
                state.transition(MultiLineString);
            } else if (buffer.next("\"", "'")) {
                String value = buffer.readQuoted();
                consumer.emitPropertyValue(value);
                state.transition(None);
            } else if (buffer.nextConsume("[")) {
                consumer.emitListStart();
                state.transition(List);
            } else {
                String value = buffer.readUntilConsume("\n", "\r\n", "\r");
                consumer.emitPropertyValue(value);
                state.transition(None);
            }
        }
    },
    MultiLineString {
        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            if (buffer.nextConsume("\"\"\"")) {
                consumer.emitMultiLineStringEnd();
                state.transition(None);
            } else {
                int start = buffer.getIndex();
                int line = buffer.getLine();
                consumer.emitPropertyValue(buffer.readUntil("\"\"\""));
                if (!buffer.next("\"\"\"")) {
                    consumer.emitError(new NotomlError("Expected \"\"\" to end multi-line string").at(line));
                    buffer.revertTo(start);
                    buffer.readUntil("\n", "\r\n");
                    state.transition(None);
                }
            }
        }
    },
    List {
        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            buffer.consumeWhitespace();
            if (buffer.nextConsume("]")) {
                consumer.emitListEnd();
                state.transition(None);
            } else if (buffer.nextConsume(",")) {
                state.transition(ListItem);
            } else {
                state.transition(ListItem);
            }
        }
    },
    ListItem {
        @Override
        void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
            buffer.consumeWhitespace();
            if (buffer.next("\"", "'")) {
                String value = buffer.readQuoted();
                consumer.emitPropertyValue(value);
                state.transition(List);
            } else {
                String value = buffer.readUntil(",", "]").trim();
                if (!value.isBlank()) {
                    consumer.emitPropertyValue(value);
                }
                state.transition(List);
            }
        }
    };

    abstract void next(NotomlStateMachine state, NotomlParseBuffer buffer, NotomlTokenConsumer consumer);
}
