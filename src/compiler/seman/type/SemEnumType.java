package compiler.seman.type;

import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;

public class SemEnumType extends SemType {

	/**
	 * Definition.
	 */
	public final AbsEnumDef definition;	

	/**
	 * Create new enumeration.
	 */
	public SemEnumType(AbsEnumDef definition) {
		this.definition = definition;
	}
	
	public AbsEnumMemberDef findMemberDefinitionForName(String name) {
		for (AbsEnumMemberDef def : definition.definitions)
			if (def.name.name.equals(name))
				return def;
		return null;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (!(type instanceof SemEnumType))
			return false;
		
		SemEnumType otherEnumType = (SemEnumType) type;
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
	public boolean canCastTo(SemType t) {
		return false;
	}

}
