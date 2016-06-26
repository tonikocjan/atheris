package compiler.abstr;

import compiler.abstr.tree.*;

/**
 * @author toni
 */
public interface Visitor {
	public void visit(AbsPtrType     acceptor);
	public void visit(AbsListType    acceptor);
	public void visit(AbsStructDef   acceptor);
	public void visit(AbsAtomConst   acceptor);
	public void visit(AbsAtomType    acceptor);
	public void visit(AbsBinExpr     acceptor);
	public void visit(AbsDefs        acceptor);
    public void visit(AbsExprs       acceptor);
    public void visit(AbsFor         acceptor);
	public void visit(AbsFunCall     acceptor);
	public void visit(AbsFunDef      acceptor);
	public void visit(AbsIfThen      accpetor);
	public void visit(AbsIfThenElse  accpetor);
	public void visit(AbsPar         acceptor);
	public void visit(AbsTypeName    acceptor);
	public void visit(AbsUnExpr      acceptor);
	public void visit(AbsVarDef      acceptor);
	public void visit(AbsVarName     acceptor);
	public void visit(AbsWhile       acceptor);
	public void visit(AbsImportDef   acceptor);
	public void visit(AbsStmts 		 acceptor);
	public void visit(AbsConstDef    acceptor);
	public void visit(AbsReturnExpr  acceptor);
}
