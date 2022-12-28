package ca.l5.expandingdev.jsgf;

public class Token implements Expansion {
    private short textIdx;

    public Token(short idx) {
        textIdx = idx;
    }

    public short getText() {
        return textIdx;
    }

    @Override
    public boolean hasChildren() {
        return false;
    }

    @Override
    public boolean hasUnparsedChildren() {
        return false;
    }

    @Override
    public String getString() {
        return Grammar.dict2.get(textIdx);
    }
}
