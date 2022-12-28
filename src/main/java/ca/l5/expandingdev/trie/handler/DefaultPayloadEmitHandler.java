package ca.l5.expandingdev.trie.handler;

import java.util.ArrayList;
import java.util.List;
import ca.l5.expandingdev.trie.PayloadEmit;

public class DefaultPayloadEmitHandler<T> implements StatefulPayloadEmitHandler<T> {

    private final List<PayloadEmit<T>> emits = new ArrayList<>();

    @Override
    public boolean emit(final PayloadEmit<T> emit) {
        this.emits.add(emit);
        return true;
    }

    @Override
    public List<PayloadEmit<T>> getEmits() {
        return this.emits;
    }
}
