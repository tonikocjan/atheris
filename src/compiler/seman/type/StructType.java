package compiler.seman.type;

import compiler.ast.tree.def.AbsClassDef;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

public class StructType extends ObjectType implements ValueType {

    public StructType(AbsClassDef definition, ArrayList<String> names, ArrayList<Type> types) {
        super(definition, names, types, null, 0);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("Struct: ");
        sb.append(classDefinition.name + "(");

        Iterator<String> namesIterator = memberNames.iterator();
        Iterator<Type> typesIterator = memberTypes.iterator();

        while (namesIterator.hasNext()) {
            sb.append(namesIterator.next() + ":" + typesIterator.next().toString());
            if (namesIterator.hasNext()) sb.append(";");
        }

        sb.append(")");
        return sb.toString();
    }
}
