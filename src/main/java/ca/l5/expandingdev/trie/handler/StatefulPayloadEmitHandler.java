package ca.l5.expandingdev.trie.handler;

import java.util.List;
import ca.l5.expandingdev.trie.PayloadEmit;

public interface StatefulPayloadEmitHandler<T> extends PayloadEmitHandler<T>{
    List<PayloadEmit<T>> getEmits();
}
