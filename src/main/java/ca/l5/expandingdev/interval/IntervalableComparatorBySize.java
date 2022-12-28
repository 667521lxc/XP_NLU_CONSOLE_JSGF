package ca.l5.expandingdev.interval;

import java.util.Comparator;

public class IntervalableComparatorBySize implements Comparator<Intervalable> {//另一种形式的比较器

    @Override
    public int compare(final Intervalable intervalable, final Intervalable intervalable2) {
        int comparison = intervalable2.size() - intervalable.size();

        if (comparison == 0) {
            comparison = intervalable.getStart() - intervalable2.getStart();
        }

        return comparison;
    }

}
