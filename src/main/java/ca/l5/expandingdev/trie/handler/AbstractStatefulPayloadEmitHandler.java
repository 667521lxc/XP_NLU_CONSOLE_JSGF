package ca.l5.expandingdev.trie.handler;

import java.util.ArrayList;
import java.util.List;
import ca.l5.expandingdev.trie.PayloadEmit;

public abstract class AbstractStatefulPayloadEmitHandler<T> implements StatefulPayloadEmitHandler<T> {

    private final List<PayloadEmit<T>> emits = new ArrayList<>();

    public void addEmit(final PayloadEmit<T> emit) {
        this.emits.add(emit);
    }

    @Override
    public List<PayloadEmit<T>> getEmits() {
        return this.emits;
    }

}
