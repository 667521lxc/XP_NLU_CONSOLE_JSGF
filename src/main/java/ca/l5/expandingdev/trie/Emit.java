package ca.l5.expandingdev.trie;

import ca.l5.expandingdev.interval.Interval;
import ca.l5.expandingdev.interval.Intervalable;

/**
 * Responsible for tracking the bounds of matched terms.负责跟踪匹配条款的边界。
 */
public class Emit extends Interval implements Intervalable {
    private final String keyword;

    public Emit(final int start, final int end, final String keyword) {
        super(start, end);
        this.keyword = keyword;
    }

    public String getKeyword() {
        return this.keyword;
    }

    @Override
    public String toString() {
        return super.toString() + "=" + this.keyword;
    }

}
