package compiler.abstr;

import compiler.abstr.tree.*;

/**
 * @author toni
 */
public interface ASTVisitor {
	public void visit(AbsListType    acceptor);
	public void visit(AbsClassDef   acceptor);
	public void visit(AbsAtomConst   acceptor);
	public void visit(AbsAtomType    acceptor);
	public void visit(AbsBinExpr     acceptor);
	public void visit(AbsDefs        acceptor);
    public void visit(AbsExprs       acceptor);
    public void visit(AbsFor         acceptor);
	public void visit(AbsFunCall     acceptor);
	public void visit(AbsFunDef      acceptor);
	public void visit(AbsIfExpr      accpetor);
	public void visit(AbsPar         acceptor);
	public void visit(AbsTypeName    acceptor);
	public void visit(AbsUnExpr      acceptor);
	public void visit(AbsVarDef      acceptor);
	public void visit(AbsVarName     acceptor);
	public void visit(AbsWhile       acceptor);
	public void visit(AbsImportDef   acceptor);
	public void visit(AbsStmts 		 acceptor);
	public void visit(AbsReturnExpr  acceptor);
	public void visit(AbsListExpr 	 acceptor);
	public void visit(AbsFunType     acceptor);
	public void visit(AbsControlTransferExpr acceptor);
}
