package compiler.frames;

import compiler.ast.tree.AstNode;
import compiler.ast.tree.def.AstDefinition;
import compiler.seman.type.ClassType;

public interface FrameDescriptionMap {
    void setFrame(AstDefinition fun, FrmFrame frame);
    FrmFrame getFrame(AstNode fun);

    void setAccess(AstDefinition var, FrmAccess access);
    FrmAccess getAccess(AstDefinition var);

    void setVirtualTable(ClassType classType, FrmVirtualTableAccess virtualTable);
    FrmVirtualTableAccess getVirtualTable(ClassType classType);
}
