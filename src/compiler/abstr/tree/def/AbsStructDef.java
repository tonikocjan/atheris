package compiler.abstr.tree.def;

import compiler.Position;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.type.AbsType;

import java.util.LinkedList;

/**
 * Created by toni on 15/04/2017.
 */
public class AbsStructDef extends AbsClassDef {

    public AbsStructDef(String name, Position pos, AbsType baseClass, LinkedList<AbsDef> definitions, LinkedList<AbsStmt> defaultConstructor, LinkedList<AbsFunDef> constructors) {
        super(name, pos, baseClass, definitions, defaultConstructor, constructors);
    }

    @Override
    public String toString() {
        return "AbsStructDef " + position.toString() + ": " + getName();
    }
}
