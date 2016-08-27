package compiler.abstr.tree.def;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.type.AbsType;

/**
 * Function definition.
 * 
 * @author toni kocjan
 */
public class AbsFunDef extends AbsDef {
	
	/** Parameters. */
	private final LinkedList<AbsParDef> pars;

	/** Return type. */
	public final AbsType type;
	
	/** Function code. */
	public final AbsStmts func;

	/**
	 * Create new function definition.
	 * 
	 * @param pos
	 *            Position.
	 * @param name
	 *            Function name.
	 * @param pars
	 *            Parameter list.
	 * @param type
	 *            Return type.
	 * @param stmts
	 *            Function code.
	 */
	public AbsFunDef(Position pos, String name, 
			LinkedList<AbsParDef> pars, AbsType type, AbsStmts stmts) {
		super(pos, name);
		this.pars = pars;
		this.type = type;
		this.func = stmts;
	}

	/**
	 * Get parameters.
	 * @return Parameters list.
	 */
	public LinkedList<AbsParDef> getParamaters() {
		return pars;
	}
	
	/**
	 * Get parameter at given index.
	 * @param index Index.
	 * @return Parameter at index.
	 */
	public AbsParDef getParameterForIndex(int index) {
		return pars.get(index);
	}
	
	/**
	 * Add new parameter to this function.
	 * @param newPar Parameter to be added.
	 */
	public void addParamater(AbsParDef newPar) {
		pars.addFirst(newPar);
	}

	/**
	 * Get parameter count.
	 * @return Parameter count.
	 */
	public int numPars() {
		return pars.size();
	}

	@Override public void accept(ASTVisitor aSTVisitor) { aSTVisitor.visit(this); }
	
}
