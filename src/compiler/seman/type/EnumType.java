package compiler.seman.type;

import java.util.ArrayList;
import java.util.LinkedList;

import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsVarDef;

public class EnumType extends ClassType {

	/**
	 * Enumeration definition.
	 */
	public final AbsEnumDef enumDefinition;

	/**
	 * 
	 */
	private AbsEnumMemberDef thisDefinition;

	/**
	 * Create new enumeration.
	 */
	public EnumType(AbsEnumDef definition) {
		super(classDefinitionForEnumeration(definition), 
				namesForEnumeration(definition), typesForEnumeration(definition));
		this.enumDefinition = definition;
	}
	
	private static AbsClassDef classDefinitionForEnumeration(AbsEnumDef definition) {
		LinkedList<AbsDef> definitions = new LinkedList<>();
		LinkedList<AbsStmt> constructor = new LinkedList<>();
		
		if (definition.type != null)
			definitions.add(new AbsVarDef(definition.position, "rawValue", definition.type));
		
		return new AbsClassDef(definition.name, 
				definition.position, definitions, constructor);
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
	
	public void setDefinitionForThisType(String name) {
		thisDefinition = (AbsEnumMemberDef) findMemberForName(name);
	}
	
	public AbsEnumMemberDef getDefinitionForThisType() {
		return thisDefinition;
	}
	
	@Override
	public AbsDef findMemberForName(String name) {
		for (AbsDef def : enumDefinition.definitions) {
			String definitionsName = def.getName();
			
			if (definitionsName.equals(name))
				return def;
		}
		return super.findMemberForName(name);
	}
	
	public int offsetForDefinitionName(String name) {
		int offset = 0;
		for (AbsDef def : enumDefinition.definitions) {
			if (def.getName().equals(name)) return offset;
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
		for (AbsDef def : enumDefinition.definitions) {
			sb.append(def.getName());
			if (def instanceof AbsEnumMemberDef) {
				if (((AbsEnumMemberDef)def).value != null)
					sb.append(" - Raw value: " + ((AbsEnumMemberDef)def).value.value);
			}
			
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
