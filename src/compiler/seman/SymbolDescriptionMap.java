package compiler.seman;

import compiler.ast.tree.AstNode;
import compiler.ast.tree.def.AstDefinition;
import compiler.seman.type.Type;

public interface SymbolDescriptionMap {
    void setScope(AstNode node, int nodeScope);
    Integer getScope(AstNode node);
    void setDefinitionForAstNode(AstNode node, AstDefinition def);
    AstDefinition getDefinitionForAstNode(AstNode node);
    void setTypeForAstNode(AstNode node, Type type);
    Type getTypeForAstNode(AstNode node);
}
