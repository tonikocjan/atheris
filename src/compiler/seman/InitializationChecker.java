package compiler.seman;

import java.util.HashSet;

import compiler.Report;
import compiler.abstr.Visitor;
import compiler.abstr.tree.AbsAtomConst;
import compiler.abstr.tree.AbsAtomType;
import compiler.abstr.tree.AbsBinExpr;
import compiler.abstr.tree.AbsClassDef;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExpr;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsFor;
import compiler.abstr.tree.AbsFunCall;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsFunType;
import compiler.abstr.tree.AbsIfThen;
import compiler.abstr.tree.AbsIfThenElse;
import compiler.abstr.tree.AbsImportDef;
import compiler.abstr.tree.AbsListExpr;
import compiler.abstr.tree.AbsListType;
import compiler.abstr.tree.AbsPar;
import compiler.abstr.tree.AbsReturnExpr;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AbsTypeName;
import compiler.abstr.tree.AbsUnExpr;
import compiler.abstr.tree.AbsVarDef;
import compiler.abstr.tree.AbsVarName;
import compiler.abstr.tree.AbsWhile;

/**
 * Initialization checking phase of the compiler.
 * @author toni
 *
 */
public class InitializationChecker implements Visitor {
	
	/**
	 * List of already initialized variables
	 */
	private HashSet<AbsVarDef> initializedVariables = new HashSet<>();
	
	/**
	 * 
	 * @param def
	 */
	private void initializeVariable(AbsVarDef def) {
		initializedVariables.add(def);
	}
	
	/**
	 * 
	 * @param def
	 * @return
	 */
	private boolean isInitialized(AbsVarDef def) {
		return initializedVariables.contains(def);
	}
	
	private boolean shouldCheckIfInitialized = true;
	
	///

	@Override
	public void visit(AbsListType acceptor) {

	}

	@Override
	public void visit(AbsClassDef acceptor) {
		for (AbsFunDef c : acceptor.contrustors)
			c.accept(this);
	}

	@Override
	public void visit(AbsAtomConst acceptor) {
		
	}

	@Override
	public void visit(AbsAtomType acceptor) {
		
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		if (acceptor.oper == AbsBinExpr.ASSIGN) {
			if (acceptor.expr1 instanceof AbsBinExpr) {
				shouldCheckIfInitialized = false;
				acceptor.expr1.accept(this);
				shouldCheckIfInitialized = true;
			}
			else {
				acceptor.expr2.accept(this);
				shouldCheckIfInitialized = false;
				acceptor.expr1.accept(this);
				shouldCheckIfInitialized = true;
			}
		}
		else if (acceptor.oper == AbsBinExpr.DOT) {
			if (!shouldCheckIfInitialized) {
				shouldCheckIfInitialized = true;
				acceptor.expr1.accept(this);
				shouldCheckIfInitialized = false;
				acceptor.expr2.accept(this);
			}
			else {
				acceptor.expr1.accept(this);
				acceptor.expr2.accept(this);
			}
		}
		else {
			acceptor.expr1.accept(this);
			acceptor.expr2.accept(this);
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
		
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (AbsExpr e : acceptor.exprs) {
			e.accept(this);
		}
	}

	@Override
	public void visit(AbsFor acceptor) {
		initializedVariables.add((AbsVarDef) SymbDesc.getNameDef(acceptor.iterator));
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		for (AbsExpr e: acceptor.args)
			e.accept(this);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		acceptor.func.accept(this);
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
		acceptor.elseBody.accept(this);
	}

	@Override
	public void visit(AbsPar acceptor) {
		
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		
	}

	@Override
	public void visit(AbsVarName acceptor) {
		if (SymbDesc.getNameDef(acceptor) instanceof AbsPar || 
				SymbDesc.getNameDef(acceptor) instanceof AbsFunDef)
			return;
		
		AbsVarDef def = (AbsVarDef) SymbDesc.getNameDef(acceptor);

		if (shouldCheckIfInitialized) {
			if (!isInitialized(def)) {
				String s = def.isConstant ? "Constant '" : "Variable '";
				Report.error(acceptor.position, s + def.name + "' used before being initialized");
			}
		}
		else {
			if (isInitialized(def) && def.isConstant) 
				Report.error(acceptor.position, "Cannot assign value to a constant '" + def.name + "'");
			initializeVariable(def);
		}
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsImportDef acceptor) {

	}

	@Override
	public void visit(AbsStmts acceptor) {
		for (AbsStmt s : acceptor.stmts) {
			s.accept(this);
		}
	}

	@Override
	public void visit(AbsReturnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsListExpr acceptor) {
		for (AbsExpr e : acceptor.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AbsFunType acceptor) {
		
	}

}
