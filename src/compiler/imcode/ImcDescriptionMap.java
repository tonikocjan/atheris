package compiler.imcode;

import compiler.ast.tree.AbsTree;

public interface ImcDescriptionMap {

    void setImcCode(AbsTree node, ImcCode imc);
    ImcCode getImcCode(AbsTree node);
}
