package ca.l5.expandingdev.trie.handler;

import java.util.ArrayList;
import java.util.List;
import ca.l5.expandingdev.trie.Emit;

public class DefaultEmitHandler implements StatefulEmitHandler {

    private final List<Emit> emits = new ArrayList<>();

    @Override
    public boolean emit(final Emit emit) {
        this.emits.add(emit);
        return true;
    }

    @Override
    public List<Emit> getEmits() {
        return this.emits;
    }
}
