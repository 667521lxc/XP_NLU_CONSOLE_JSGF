package ca.l5.expandingdev.trie;

/**包含匹配的关键字和一些有效载荷数据
 * Contains the matched keyword and some payload data.
 * 
 * @author Daniel Beck
 * @param <T> The type of the wrapped payload data.
 */
public class Payload<T> implements Comparable<Payload<T>> {

    private final String keyword;
    private final T data;

    public Payload(final String keyword, final T data) {
        super();
        this.keyword = keyword;
        this.data = data;
    }

    public String getKeyword() {
    	//System.out.println(keyword);
        return keyword;
    }

    public T getData() {
        return data;
    }

    @Override
    public int compareTo(Payload<T> other) {
        return keyword.compareTo(other.getKeyword());
    }
}
