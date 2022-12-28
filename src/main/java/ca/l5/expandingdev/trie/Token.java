package ca.l5.expandingdev.trie;

public abstract class Token {
    private String fragment;

    public Token(String fragment) {
        this.fragment = fragment;//碎片
    }

    public String getFragment() {
        return this.fragment;
    }

    public abstract boolean isMatch();

    public abstract Emit getEmit();
}
