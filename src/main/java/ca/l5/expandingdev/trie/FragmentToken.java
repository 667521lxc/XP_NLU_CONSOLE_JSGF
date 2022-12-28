package ca.l5.expandingdev.trie;
//这是干什么的类？
public class FragmentToken extends Token {

    public FragmentToken(String fragment) {
        super(fragment);
    }

    @Override
    public boolean isMatch() {
        return false;
    }

    @Override
    public Emit getEmit() {
        return null;
    }

}
