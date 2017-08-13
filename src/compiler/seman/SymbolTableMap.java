package compiler.seman;

import compiler.Logger;
import compiler.ast.tree.def.AbsDef;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by Toni Kocjan on 13/08/2017.
 * Triglav Partner BE
 */

public interface SymbolTableMap {

    void newScope();
    void oldScope();
    void insertDefinitionOnCurrentScope(String name, AbsDef definition) throws SemIllegalInsertException;
    void removeDefinitionFromCurrentScope(String name) throws SemIllegalDeleteException;
    AbsDef findDefinitionForName(String name);
}
