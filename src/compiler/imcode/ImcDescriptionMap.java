package compiler.imcode;

import compiler.ast.tree.AstNode;

public interface ImcDescriptionMap {

    void setImcCode(AstNode node, ImcCode imc);
    ImcCode getImcCode(AstNode node);
}
