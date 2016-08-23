package compiler.seman.type;

import java.util.ArrayList;
import java.util.Vector;

import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsVarDef;

public class EnumType extends ClassType {

	/**
	 * Enumeration definition.
	 */
	public final AbsEnumDef enumDefinition;


	/**
	 * Create new enumeration.
	 */
	public EnumType(AbsEnumDef definition) {
		super(classDefinitionForEnumeration(definition), 
				namesForEnumeration(definition), typesForEnumeration(definition));
		this.enumDefinition = definition;
	}
	
	private static AbsClassDef classDefinitionForEnumeration(AbsEnumDef definition) {
		Vector<AbsStmt> statements = new Vector<>();
		if (definition.type != null)
			statements.add(new AbsVarDef(definition.position, "rawValue", definition.type));
		
		return new AbsClassDef(definition.name, 
				definition.position, statements);
	}
	
	private static ArrayList<String> namesForEnumeration(AbsEnumDef definition) {
		ArrayList<String> names = new ArrayList<>();
		if (definition.type != null)
			names.add("rawValue");
		return names;
	}
	
	private static ArrayList<Type> typesForEnumeration(AbsEnumDef definition) {
		ArrayList<Type> types = new ArrayList<>();
		if (definition.type != null)
			types.add(new AtomType(definition.type.type));
		return types;
	}
	
	public AbsEnumMemberDef findMemberDefinitionForName(String name) {
		for (AbsEnumMemberDef def : enumDefinition.definitions)
			if (def.name.name.equals(name))
				return def;
		return null;
	}
	
	public int offsetForDefinitionName(String name) {
		int offset = 0;
		for (AbsEnumMemberDef def : enumDefinition.definitions) {
			if (def.name.name.equals(name)) return offset;
			offset++;
		}
		return -1;
	}

	@Override
	public String getName() {
		return enumDefinition.name;
	}
	
	@Override
	public boolean sameStructureAs(Type type) {
		if (!(type instanceof EnumType))
			return false;
		
		EnumType otherEnumType = (EnumType) type;
		return otherEnumType.enumDefinition.name.equals(enumDefinition.name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Enum: ");
		sb.append(enumDefinition.name + "(");
		for (AbsEnumMemberDef def : enumDefinition.definitions) {
			sb.append(def.name.name);
			if (def.value != null)
				sb.append(" - Raw value: " + def.value.value);
			
			if (def != enumDefinition.definitions.getLast())
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
