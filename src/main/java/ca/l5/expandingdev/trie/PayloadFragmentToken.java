package ca.l5.expandingdev.trie;

/***可以发出一种有效载荷的令牌（“片段”）的容器。
 * Container for a token ("the fragment") that can emit a type of payload.
 * <p>此标记表示未找到匹配的搜索项，因此*｛@link#isMatch（）｝始终返回｛@code false｝。
 * This token indicates a matching search term was not found, so
 * {@link #isMatch()} always returns {@code false}.
 * </p>
 * 
 * @author Daniel Beck
 *
 * @param <T> The Type of the emitted payloads.
 */
public class PayloadFragmentToken<T> extends PayloadToken<T> {

    public PayloadFragmentToken(String fragment) {
        super(fragment);
    }

    @Override
    public boolean isMatch() {
        return false;
    }

    /**
     * Returns null.
     */
    @Override
    public PayloadEmit<T> getEmit() {
        return null;
    }
}
