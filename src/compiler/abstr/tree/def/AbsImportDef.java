package compiler.abstr.tree.def;

import java.util.HashSet;

import compiler.Position;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;

/**
 * Import definition.
 * @author toni kocjan
 *
 */
public class AbsImportDef extends AbsDef {
	
	/** Imported definitions.  */
	public AbsDefs imports;
	
	/** Set of definition names which to be imported. */
	public final HashSet<String> definitions = new HashSet<>();

	/**
	 * Create new import definition.
	 * @param pos Position.
	 * @param fileName Name of the file to import.
	 */
	public AbsImportDef(Position pos, String fileName) {
		super(pos, fileName);
		imports = null;
	}

	@Override
	public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }

}
