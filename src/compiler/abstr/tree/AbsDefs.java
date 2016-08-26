package compiler.abstr.tree;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsVarDef;

/**
 * Seznam definicij.
 * 
 * @author sliva
 */
public class AbsDefs extends AbsTree {

	/** Definitions. */
	public final LinkedList<AbsDef> definitions;

	/**
	 * Ustvari nov seznam definicij.
	 * 
	 * @param pos
	 *            Polozaj stavcne oblike tega drevesa.
	 * @param defs
	 *            Definicije.
	 */
	public AbsDefs(Position pos, LinkedList<AbsDef> defs) {
		super(pos);
		this.definitions = defs;
	}

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
