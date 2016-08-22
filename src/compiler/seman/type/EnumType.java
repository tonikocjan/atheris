package compiler.seman.type;

import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;

public class EnumType extends PointerType {

	/**
	 * Definition.
	 */
	public final AbsEnumDef definition;	

	/**
	 * Create new enumeration.
	 */
	public EnumType(AbsEnumDef definition) {
		this.definition = definition;
	}
	
	public AbsEnumMemberDef findMemberDefinitionForName(String name) {
		for (AbsEnumMemberDef def : definition.definitions)
			if (def.name.name.equals(name))
				return def;
		return null;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (!(type instanceof EnumType))
			return false;
		
		EnumType otherEnumType = (EnumType) type;
		return otherEnumType.definition.name.equals(definition.name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Enum: ");
		sb.append(definition.name + "(");
		for (AbsEnumMemberDef def : definition.definitions) {
			sb.append(def.name.name);
			if (def.value != null)
				sb.append(" - Raw value: " + def.value.value);
			
			if (def != definition.definitions.getLast())
				sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int size() {
		return 4;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

}
