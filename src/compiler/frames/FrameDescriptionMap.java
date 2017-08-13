package compiler.frames;

import compiler.ast.tree.AbsTree;
import compiler.ast.tree.def.AbsDef;
import compiler.seman.type.ClassType;

public interface FrameDescriptionMap {

    void setFrame(AbsDef fun, FrmFrame frame);
    FrmFrame getFrame(AbsTree fun);

    void setAccess(AbsDef var, FrmAccess access);
    FrmAccess getAccess(AbsDef var);

    void setVirtualTable(ClassType classType, FrmVirtualTableAccess virtualTable);
    FrmVirtualTableAccess getVirtualTable(ClassType classType);
}
