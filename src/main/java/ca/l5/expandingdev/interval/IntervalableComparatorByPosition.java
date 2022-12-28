package ca.l5.expandingdev.interval;

import java.util.Comparator;

public class IntervalableComparatorByPosition implements Comparator<Intervalable> {//比较器

    @Override
    public int compare(final Intervalable intervalable, final Intervalable intervalable2) {
        return intervalable.getStart() - intervalable2.getStart();
    }

}
