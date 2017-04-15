package compiler.seman.type;

import compiler.abstr.tree.def.AbsClassDef;

import java.util.LinkedList;

/**
 * Created by toni on 15/04/2017.
 */
public class StructType extends ObjectType implements ValueType {

    public StructType(AbsClassDef definition, LinkedList<String> names, LinkedList<Type> types) {
        super(definition, names, types, null);
    }
}
