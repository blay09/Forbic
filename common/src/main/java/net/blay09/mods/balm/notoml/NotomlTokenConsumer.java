package net.blay09.mods.balm.notoml;

import java.util.ArrayList;
import java.util.List;

public abstract class NotomlTokenConsumer {
    private String currentCategory = "";
    private String currentProperty = "";
    private List<String> currentList;
    private StringBuilder currentMultiLineString;
    private StringBuilder currentComment;

    public void emitComment(String comment) {
        if (currentComment == null) {
            currentComment = new StringBuilder();
        }
        currentComment.append(comment).append("\n");
    }

    public void emitCategory(String categoryName) {
        currentCategory = categoryName;
        if (currentComment != null) {
            onCommentParsed(currentCategory, currentProperty, currentComment.toString());
            currentComment = null;
        }
    }

    public void emitPropertyKey(String property) {
        currentProperty = property;
        if (currentComment != null) {
            onCommentParsed(currentCategory, currentProperty, currentComment.toString());
            currentComment = null;
        }
    }

    public void emitPropertyValue(String value) {
        if (currentList != null) {
            currentList.add(value);
        } else if(currentMultiLineString != null) {
            currentMultiLineString.append(value);
        } else {
            onPropertyParsed(currentCategory, currentProperty, value);
        }
    }

    public void emitListStart() {
        currentList = new ArrayList<>();
    }

    public void emitListEnd() {
        onPropertyParsed(currentCategory, currentProperty, currentList);
        currentList = null;
    }

    public void emitMultiLineStringStart() {
        currentMultiLineString = new StringBuilder();
    }

    public void emitMultiLineStringEnd() {
        onPropertyParsed(currentCategory, currentProperty, currentMultiLineString.toString().trim());
        currentMultiLineString = null;
    }

    public void emitError(NotomlError error) {
        onError(error);
    }

    protected abstract void onPropertyParsed(String category, String property, Object value);

    protected abstract void onCommentParsed(String category, String property, String comment);

    protected abstract void onError(NotomlError error);
}
