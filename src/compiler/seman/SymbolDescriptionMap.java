package compiler.seman;

import compiler.ast.tree.AbsTree;
import compiler.ast.tree.def.AbsDef;
import compiler.seman.type.Type;

public interface SymbolDescriptionMap {
    void setScope(AbsTree node, int nodeScope);
    Integer getScope(AbsTree node);
    void setDefinitionForAstNode(AbsTree node, AbsDef def);
    AbsDef getDefinitionForAstNode(AbsTree node);
    void setTypeForAstNode(AbsTree node, Type type);
    Type getTypeForAstNode(AbsTree node);
}
