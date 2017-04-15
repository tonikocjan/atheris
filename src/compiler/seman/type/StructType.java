package compiler.seman.type;

import compiler.abstr.tree.def.AbsClassDef;

import java.util.Iterator;
import java.util.LinkedList;

/**
 * Created by toni on 15/04/2017.
 */
public class StructType extends ObjectType implements ValueType {

    public StructType(AbsClassDef definition, LinkedList<String> names, LinkedList<Type> types) {
        super(definition, names, types, null, 4);
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
