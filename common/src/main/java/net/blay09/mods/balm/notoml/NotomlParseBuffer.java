package net.blay09.mods.balm.notoml;

public class NotomlParseBuffer {

    private final String input;
    private int i = 0;
    private int line = 1;

    public NotomlParseBuffer(String input) {
        this.input = input;
    }

    public void consumeWhitespace() {
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == ' ' || c == '\t' || c == '\r') {
                i++;
            } else if (c == '\n') {
                i++;
                line++;
            } else {
                break;
            }
        }
    }

    public String peek(int i) {
        return input.substring(this.i, Math.min(this.i + i, input.length()));
    }

    public boolean next(String... find) {
        for (String s : find) {
            if (input.startsWith(s, i)) {
                return true;
            }
        }
        return false;
    }

    public boolean nextConsume(String... find) {
        for (String s : find) {
            if (input.startsWith(s, i)) {
                i += s.length();
                return true;
            }
        }
        return false;
    }

    public String readUntil(String... find) {
        int start = i;
        while (i < input.length()) {
            if (input.charAt(i) == '\n') {
                line++;
            }
            for (String s : find) {
                if (input.startsWith(s, i)) {
                    return input.substring(start, i);
                }
            }
            i++;
        }
        return input.substring(start);
    }

    public String readUntilConsume(String... find) {
        int start = i;
        while (i < input.length()) {
            if (input.charAt(i) == '\n') {
                line++;
            }
            for (String s : find) {
                if (input.startsWith(s, i)) {
                    String read = input.substring(start, i);
                    i += s.length();
                    return read;
                }
            }
            i++;
        }
        return input.substring(start);
    }

    public String readQuoted() {
        char quote = input.charAt(i);
        if (quote != '"' && quote != '\'') {
            throw new IllegalStateException("Expected a quote character at the start of the string");
        }
        i++; // consume the starting quote

        StringBuilder sb = new StringBuilder();
        while (i < input.length()) {
            char current = input.charAt(i);
            if (current == '\\') { // it is an escape character
                if (i + 1 < input.length()) {
                    char nextChar = input.charAt(i + 1);
                    if (nextChar == quote) { // it is an escaped quote
                        sb.append(nextChar);
                        i += 2; // consume both escape character and quote
                        continue;
                    }
                }
            } else if (current == quote) { // closing quote
                i++; // consume closing quote
                return sb.toString();
            } else if (current == '\n') {
                // Technically we could throw, but we may as well be lenient
                // throw new IllegalArgumentException("Expected closing quote before end of line");
                return sb.toString();
            }
            sb.append(current);
            i++;
        }
        // Technically we could throw, but we may as well be lenient
        // throw new IllegalArgumentException("Expected closing quote before end of file");
        return sb.toString();
    }

    public int getLine() {
        return line;
    }

    public boolean canRead() {
        return i < input.length();
    }

    public int getIndex() {
        return i;
    }

    public void revertTo(int index) {
        while (i > index) {
            i--;
            if (input.charAt(i) == '\n') {
                line--;
            }
        }
    }
}
