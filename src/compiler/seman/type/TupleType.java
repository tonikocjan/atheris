package compiler.seman.type;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import compiler.abstr.tree.def.AbsTupleDef;

public class TupleType extends PointerType {
	
	/**
	 * Definition for this type.
	 */
	public final AbsTupleDef definition;

	/**
	 * Tuple member types.
	 */
	private final LinkedHashMap<String, Type> members = new LinkedHashMap<>();

	/**
	 * Sum of sizes of all members.
	 */
	private final int size;
	
	/**
	 * Create new tuple type.
	 * @param types
	 * @param names
	 */
	public TupleType(LinkedList<Type> types, LinkedList<String> names) {
		int size = 0;
		
		for (int i = 0; i < names.size(); i++) {
			members.put(names.get(i), types.get(i));
			size += types.get(i).size();
		}
		
		this.definition = null;
		this.size = size;
	}
	
	/**
	 * Create new tuple type.
	 * @param definition
	 * @param types
	 * @param names
	 */
	public TupleType(AbsTupleDef definition, LinkedList<Type> types, LinkedList<String> names) {
		int size = 0;
		
		for (int i = 0; i < names.size(); i++) {
			members.put(names.get(i), types.get(i));
			size += types.get(i).size();
		}
		
		this.definition = definition;
		this.size = size;
	}

	/**
	 * Get type for given name.
	 * @param name
	 * @return
	 */
	public Type typeForName(String name) {
		return members.get(name);
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

	@Override
	public boolean sameStructureAs(Type type) {
		return false;
	}

	@Override
	public boolean canCastTo(Type t) {
		return false;
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		int i = 0;
		sb.append("Tuple: (");
		for (Map.Entry<String, Type> entry : members.entrySet()) {
			sb.append(entry.getKey());
			sb.append(":");
			sb.append(entry.getValue().toString());
			if (++i < members.size()) sb.append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

	@Override
	public boolean containsMember(String name) {
		return members.containsKey(name);
	}

}
