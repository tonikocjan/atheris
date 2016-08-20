package compiler.seman;


import compiler.Report;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsAtomConst;
import compiler.abstr.tree.AbsAtomType;
import compiler.abstr.tree.AbsBinExpr;
import compiler.abstr.tree.AbsClassDef;
import compiler.abstr.tree.AbsControlTransferStmt;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExpr;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsFor;
import compiler.abstr.tree.AbsFunCall;
import compiler.abstr.tree.AbsFunDef;
import compiler.abstr.tree.AbsFunType;
import compiler.abstr.tree.AbsIfExpr;
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
import compiler.abstr.tree.Condition;

/**
 * Initialization checking phase of the compiler.
 * @author toni
 *
 */
public class InitializationChecker implements ASTVisitor {
	
	private boolean shouldCheckIfInitialized = true;
	
	///

	@Override
	public void visit(AbsListType acceptor) {
		///
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
		InitTable.newScope();
		InitTable.initialize((AbsVarDef) SymbDesc.getNameDef(acceptor.iterator));
		acceptor.body.accept(this);
		InitTable.oldScope();
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
	public void visit(AbsIfExpr acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);

			InitTable.newScope();
			c.body.accept(this);
			InitTable.oldScope();
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);
		}
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
			if (!InitTable.isInitialized(def)) {
				String errorMsg = def.isConstant ? "Constant '" : "Variable '";
				Report.error(acceptor.position, errorMsg + def.name + "' used before being initialized");
			}
		}
		else {
			if (InitTable.isInitialized(def) && def.isConstant) 
				Report.error(acceptor.position, "Cannot assign value to a constant '" + def.name + "'");
			InitTable.initialize(def);
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
		///
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

}
