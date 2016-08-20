package compiler.abstr;

import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsVarDef;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsFunCall;
import compiler.abstr.tree.expr.AbsListExpr;
import compiler.abstr.tree.expr.AbsReturnExpr;
import compiler.abstr.tree.expr.AbsUnExpr;
import compiler.abstr.tree.expr.AbsVarNameExpr;
import compiler.abstr.tree.stmt.AbsCaseStmt;
import compiler.abstr.tree.stmt.AbsControlTransferStmt;
import compiler.abstr.tree.stmt.AbsForStmt;
import compiler.abstr.tree.stmt.AbsIfStmt;
import compiler.abstr.tree.stmt.AbsSwitchStmt;
import compiler.abstr.tree.stmt.AbsWhileStmt;
import compiler.abstr.tree.type.AbsAtomType;
import compiler.abstr.tree.type.AbsFunType;
import compiler.abstr.tree.type.AbsListType;
import compiler.abstr.tree.type.AbsTypeName;

/**
 * @author toni
 */
public interface ASTVisitor {
	public void visit(AbsListType acceptor);
	public void visit(AbsClassDef acceptor);
	public void visit(AbsAtomConstExpr acceptor);
	public void visit(AbsAtomType acceptor);
	public void visit(AbsBinExpr acceptor);
	public void visit(AbsDefs acceptor);
    public void visit(AbsExprs acceptor);
    public void visit(AbsForStmt acceptor);
	public void visit(AbsFunCall acceptor);
	public void visit(AbsFunDef acceptor);
	public void visit(AbsIfStmt accpetor);
	public void visit(AbsParDef acceptor);
	public void visit(AbsTypeName acceptor);
	public void visit(AbsUnExpr acceptor);
	public void visit(AbsVarDef acceptor);
	public void visit(AbsVarNameExpr acceptor);
	public void visit(AbsWhileStmt acceptor);
	public void visit(AbsImportDef acceptor);
	public void visit(AbsStmts acceptor);
	public void visit(AbsReturnExpr acceptor);
	public void visit(AbsListExpr acceptor);
	public void visit(AbsFunType acceptor);
	public void visit(AbsControlTransferStmt acceptor);
	public void visit(AbsSwitchStmt acceptor);
	public void visit(AbsCaseStmt acceptor);
}
