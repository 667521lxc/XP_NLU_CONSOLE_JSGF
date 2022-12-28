package ca.l5.expandingdev.trie.handler;

import ca.l5.expandingdev.trie.PayloadEmit;

public interface PayloadEmitHandler<T> {
    boolean emit(PayloadEmit<T> emit);
}
