package ca.l5.expandingdev.trie.handler;

import java.util.List;
import ca.l5.expandingdev.trie.Emit;

public interface StatefulEmitHandler extends EmitHandler {
    List<Emit> getEmits();
}
