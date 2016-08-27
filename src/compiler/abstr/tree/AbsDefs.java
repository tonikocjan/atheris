package compiler.abstr.tree;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsVarDef;

/**
 * List of definitions.
 * 
 * @author toni kocjan
 */
public class AbsDefs extends AbsTree {

	/** Definitions. */
	public final LinkedList<AbsDef> definitions;

	/**
	 * Create new definitions list.
	 * 
	 * @param pos
	 *            Position.
	 * @param defs
	 *            Definitions.
	 */
	public AbsDefs(Position pos, LinkedList<AbsDef> defs) {
		super(pos);
		this.definitions = defs;
	}

	/**
	 * Find definition for given name.
	 * @param name Name of the definition
	 * @return Definition if found, otherwise null
	 */
	public AbsDef findDefinitionForName(String name) {
		for (AbsDef d : definitions) {
			if (d instanceof AbsVarDef && ((AbsVarDef) d).name.equals(name))
				return d;
			if (d instanceof AbsFunDef && ((AbsFunDef) d).name.equals(name))
				return d;
		}
		return null;
	}
	
	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
