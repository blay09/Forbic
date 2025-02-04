package net.blay09.mods.balm.notoml;

public class NotomlError {

    private final String message;
    private Throwable cause;
    private int line = -1;

    public NotomlError(String message) {
        this.message = message;
    }

    public NotomlError(String message, Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }

    public NotomlError at(int line) {
        this.line = line;
        return this;
    }

    public boolean hasLine() {
        return line != -1;
    }

    public int getLine() {
        return line;
    }
}
