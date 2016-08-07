package compiler.frames;

import java.util.Vector;

import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.*;
import compiler.seman.SymbDesc;
import compiler.seman.type.SemFunType;

public class FrmEvaluator implements ASTVisitor {

	private int currentLevel = 1;
	private FrmFrame currentFrame = null;
	public FrmFrame entryPoint = null;
	
	public static final String ENTRY_POINT = "_main";
	
	public FrmEvaluator() {
		AbsFunDef _main = new AbsFunDef(null, ENTRY_POINT, new Vector<>(), 
				new AbsAtomType(null, AtomType.VOID), new AbsStmts(null, new Vector<>()));

		entryPoint = new FrmFrame(_main, 0);
		entryPoint.label = FrmLabel.newLabel(ENTRY_POINT);
		entryPoint.sizePars = 0;
		entryPoint.numPars = 0;
		
		currentFrame = entryPoint;
	}

	@Override
	public void visit(AbsListType acceptor) {

	}

	@Override
	public void visit(AbsClassDef acceptor) {
		acceptor.statements.accept(this);
		for (AbsFunDef c : acceptor.contrustors) {
			c.accept(this);
		}
	}

	@Override
	public void visit(AbsAtomConst acceptor) {

	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);
		
		if (acceptor.oper == AbsBinExpr.ASSIGN) {
			if (SymbDesc.getType(acceptor.expr1) instanceof SemFunType &&
					SymbDesc.getType(acceptor.expr2) instanceof SemFunType) {
				FrmDesc.setFrame(SymbDesc.getNameDef(acceptor.expr1), 
						FrmDesc.getFrame(SymbDesc.getNameDef(acceptor.expr2)));	
			}
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int def = 0; def < acceptor.numDefs(); def++)
			acceptor.def(def).accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int i = 0; i < acceptor.numExprs(); i++)
			acceptor.expr(i).accept(this);
	}

	@Override
	public void visit(AbsFor acceptor) {
		SymbDesc.getNameDef(acceptor.iterator).accept(this);
		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		int parSize = 4;
		for (int i = 0; i < acceptor.numArgs(); i++)
			parSize += SymbDesc.getType(acceptor.arg(i)).size();

		currentFrame.sizeArgs = Math.max(currentFrame.sizeArgs, parSize);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		FrmFrame frame = new FrmFrame(acceptor, currentLevel);
		FrmDesc.setFrame(acceptor, frame);
		FrmDesc.setAccess(acceptor, new FrmFunAccess(acceptor));

		FrmFrame tmp = currentFrame;
		currentFrame = frame;

		for (int i = 0; i < acceptor.numPars(); i++)
			acceptor.par(i).accept(this);

		currentLevel++;

		acceptor.func.accept(this);

		currentFrame = tmp;
		currentLevel--;
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
		FrmDesc.setAccess(acceptor, new FrmParAccess(acceptor, currentFrame));
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
		if (currentFrame.label.name().equals("_" + ENTRY_POINT))
			FrmDesc.setAccess(acceptor, new FrmVarAccess(acceptor));
		else
			FrmDesc.setAccess(acceptor,
					new FrmLocAccess(acceptor, currentFrame));
	}

	@Override
	public void visit(AbsVarName acceptor) {

	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsImportDef importDef) {
		importDef.imports.accept(this);
	}

	@Override
	public void visit(AbsStmts stmts) {
		for (int stmt = 0; stmt < stmts.numStmts(); stmt++) {
			stmts.stmt(stmt).accept(this);
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		for (AbsExpr e : absListExpr.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AbsFunType acceptor) {
		
	}

}
