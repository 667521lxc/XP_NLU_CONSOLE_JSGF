package ca.l5.expandingdev.trie;

/***PayloadToken保存一个文本（“片段”）并发出一些输出。如果｛@link#isMatch（）｝返回｛@code true｝，则标记与搜索词匹配。
 * PayloadToken holds a text ("the fragment") an emits some output. If
 * {@link #isMatch()} returns {@code true}, the token matched a search term.
 *
 * @author Daniel Beck
 *
 * @param <T> The Type of the emitted payloads.
 */
public abstract class PayloadToken<T> {
    private String fragment;

    public PayloadToken(String fragment) {
        this.fragment = fragment;
    }

    public String getFragment() {
        return this.fragment;
    }

    /**
     * Return {@code true} if a search term matched.
     * @return {@code true} if this is a match
     */
    public abstract boolean isMatch();

    /**
     * @return the payload
     */
    public abstract PayloadEmit<T> getEmit();
}
