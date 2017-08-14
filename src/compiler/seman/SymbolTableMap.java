package compiler.seman;

import compiler.ast.tree.def.AstDefinition;

/**
 * Created by Toni Kocjan on 13/08/2017.
 * Triglav Partner BE
 */

public interface SymbolTableMap {

    void newScope();
    void oldScope();
    void insertDefinitionOnCurrentScope(String name, AstDefinition definition) throws SemIllegalInsertException;
    void removeDefinitionFromCurrentScope(String name) throws SemIllegalDeleteException;
    AstDefinition findDefinitionForName(String name);
}
