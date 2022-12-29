package ca.l5.expandingdev.jsgf;

public class ReTags {
    public String tag;
    public int wp;
    public int wpe;

    public ReTags(String tagn, int wpn, int wpen) {
        tag = tagn;
        wp = wpn;
        wpe = wpen;
    }

    public String getString() {
        String s = "";
        s += tag;
        return s;
    }
}
