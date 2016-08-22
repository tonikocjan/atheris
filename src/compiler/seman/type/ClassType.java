package compiler.seman.type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import compiler.Report;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;

public class ClassType extends PointerType {
	
	/**
	 * Definition.
	 */
	public final AbsClassDef definition;

	/**
	 * Map containing members types.
	 */
	private final LinkedHashMap<String, Type> members = new LinkedHashMap<>();
	
	/**
	 * Sum of sizes for all members.
	 */
	private final int size;

	/**
	 * Create new class type.
	 * 
	 * @param name name of structure
	 * @param names list of names for each definition
	 * @param types list of types for each definition
	 */
	public ClassType(AbsClassDef definition, 
			ArrayList<String> names, ArrayList<Type> types) {
		if (names.size() != types.size())
			Report.error("Internal error :: compiler.seman.type.SemClassType: "
					+ "names count not equal types count");

		int size = 0;
		for (int i = 0; i < names.size(); i++) {
			members.put(names.get(i), types.get(i));
			size += types.get(i).size();
		}
		this.size = size;
		this.definition = definition;
	}
	
	public LinkedHashMap<String, Type> getMembers() {
		return members;
	}
	
	public boolean containsMember(String member) {
		return members.containsKey(member);
	}
	
	/**
	 * Calculate offset for member.
	 * @param name member name
	 * @return offset of that member
	 */
	public int offsetOf(String name) {
		int offset = 0;
		
		for (Map.Entry<String, Type> entry : members.entrySet()) {
			if (name.equals(entry.getKey())) break;
			offset += entry.getValue().size();
		}
		
		return offset;
	}
	
	public String getName() {
		return definition.name;
	}

	@Override
	public boolean sameStructureAs(Type type) {
		if (!(type instanceof ClassType))
			return false;
		
		ClassType type_ = (ClassType) type;
		
		if (members.size() != type_.members.size())
			return false;
		
		List<Type> this_ = new ArrayList<>(members.values());
		List<Type> o_ = new ArrayList<>(type_.members.values());
		
		for (int i = 0; i < this_.size(); i++) {
			if (!this_.get(i).sameStructureAs(o_.get(i)))
				return false;
		}
		return true;
	}
	
	public AbsDef findMemberForName(String name) {
		return definition.statements.findDefinitionForName(name);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		sb.append("Class: ");
		sb.append(definition.name + "(");
		for (Map.Entry<String, Type> entry : members.entrySet()) {
			sb.append(entry.getKey() + ":" + entry.getValue().toString());
			if (++i < members.size()) sb.append(";");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

}
