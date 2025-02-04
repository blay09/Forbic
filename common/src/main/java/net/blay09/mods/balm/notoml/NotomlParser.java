package net.blay09.mods.balm.notoml;

public class NotomlParser {

    public static Notoml parse(String input) {
        Notoml result = new Notoml();
        var stateMachine = new NotomlStateMachine();
        var parseBuffer = new NotomlParseBuffer(input);
        var tokenConsumer = new NotomlTokenConsumer() {
            @Override
            public void onPropertyParsed(String category, String property, Object value) {
                result.setProperty(category, property, value);
            }

            @Override
            public void onCommentParsed(String category, String property, String comment) {
                result.setComment(category, property, comment);
            }

            @Override
            public void onError(NotomlError error) {
                result.addError(error);
            }
        };
        while (parseBuffer.canRead()) {
            if (!stateMachine.next(parseBuffer, tokenConsumer)) {
                break;
            }
        }
        return result;
    }

}
