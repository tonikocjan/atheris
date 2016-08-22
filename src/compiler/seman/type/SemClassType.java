package compiler.seman.type;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import compiler.Report;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;

public class SemClassType extends SemPtrType {
	
	/**
	 * Definition.
	 */
	public final AbsClassDef definition;

	/**
	 * Map containing members types.
	 */
	private final LinkedHashMap<String, SemType> members = new LinkedHashMap<>();
	
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
	public SemClassType(AbsClassDef definition, 
			ArrayList<String> names, ArrayList<SemType> types) {
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
	
	public LinkedHashMap<String, SemType> getMembers() {
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
		
		for (Map.Entry<String, SemType> entry : members.entrySet()) {
			if (name.equals(entry.getKey())) break;
			offset += entry.getValue().size();
		}
		
		return offset;
	}
	
	public String getName() {
		return definition.name;
	}

	@Override
	public boolean sameStructureAs(SemType type) {
		if (!(type instanceof SemClassType))
			return false;
		
		SemClassType type_ = (SemClassType) type;
		
		if (members.size() != type_.members.size())
			return false;
		
		List<SemType> this_ = new ArrayList<>(members.values());
		List<SemType> o_ = new ArrayList<>(type_.members.values());
		
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
		sb.append(definition.name + "(");
		for (Map.Entry<String, SemType> entry : members.entrySet()) {
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
	public boolean canCastTo(SemType t) {
		return false;
	}

}
