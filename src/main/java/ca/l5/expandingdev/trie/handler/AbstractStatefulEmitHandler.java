package ca.l5.expandingdev.trie.handler;

import java.util.ArrayList;
import java.util.List;
import ca.l5.expandingdev.trie.Emit;

public abstract class AbstractStatefulEmitHandler implements StatefulEmitHandler {

    private final List<Emit> emits = new ArrayList<>();

    public void addEmit(final Emit emit) {
        this.emits.add(emit);
    }

    @Override
    public List<Emit> getEmits() {
        return this.emits;
    }

}
