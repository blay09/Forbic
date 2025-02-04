package net.blay09.mods.balm.notoml;

public class NotomlStateMachine {
    private NotomlParserState state = NotomlParserState.None;

    public void transition(NotomlParserState state) {
        this.state = state;
    }

    public void end() {
        state = null;
    }

    public boolean next(NotomlParseBuffer buffer, NotomlTokenConsumer consumer) {
        try {
            state.next(this, buffer, consumer);
        } catch (Exception e) {
            consumer.emitError(new NotomlError(e.getMessage()).at(buffer.getLine()));
            buffer.readUntil("\n");
            state = NotomlParserState.None;
        }
        return state != null;
    }
}
