package compiler.frames;

import java.util.LinkedList;
import java.util.Vector;

import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsVarDef;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsExpr;
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
import compiler.seman.SymbDesc;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;
import compiler.seman.type.FunctionType;

public class FrmEvaluator implements ASTVisitor {

	private int currentLevel = 1;
	private FrmFrame currentFrame = null;
	private ClassType classType = null;
	public FrmFrame entryPoint = null;
	
	public static final String ENTRY_POINT = "_main";
	
	public FrmEvaluator() {
		AbsFunDef _main = new AbsFunDef(null, ENTRY_POINT, new LinkedList<>(), 
				new AbsAtomType(null, AtomTypeEnum.VOID), new AbsStmts(null, new Vector<>()));

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
		ClassType tmp = classType;
		classType = (ClassType) ((CanType) SymbDesc.getType(acceptor)).childType;
		
		acceptor.definitions.accept(this);
		for (AbsFunDef c : acceptor.contrustors) {
			c.accept(this);
		}
		
		classType = tmp;
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {

	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);
		
		if (acceptor.oper == AbsBinExpr.ASSIGN) {
			if (SymbDesc.getType(acceptor.expr1) instanceof FunctionType &&
					SymbDesc.getType(acceptor.expr2) instanceof FunctionType) {
				FrmDesc.setFrame(SymbDesc.getNameDef(acceptor.expr1), 
						FrmDesc.getFrame(SymbDesc.getNameDef(acceptor.expr2)));	
			}
		}
		else if (acceptor.oper == AbsBinExpr.DOT) {
			// FIXME
			if (acceptor.expr1 instanceof AbsVarNameExpr &&
					acceptor.expr2 instanceof AbsVarNameExpr) {
				AbsVarDef parentDef = (AbsVarDef) SymbDesc.getNameDef(acceptor.expr1);
				AbsVarDef memberDef = (AbsVarDef) SymbDesc.getNameDef(acceptor.expr2);
				FrmMemberAccess memberAccess = (FrmMemberAccess) FrmDesc.getAccess(memberDef);
				
				memberAccess.parentDef = parentDef;
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
	public void visit(AbsForStmt acceptor) {
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

		for (AbsParDef par : acceptor.getParamaters())
			par.accept(this);

		currentLevel++;

		acceptor.func.accept(this);

		currentFrame = tmp;
		currentLevel--;
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
		}

		if (acceptor.elseBody != null)
			acceptor.elseBody.accept(this);
	}

	@Override
	public void visit(AbsParDef acceptor) {
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
		if (classType != null)
			// member access
			FrmDesc.setAccess(acceptor, new FrmMemberAccess(acceptor, classType));
		else if (currentFrame.label.name().equals("_" + ENTRY_POINT))
			// var access
			FrmDesc.setAccess(acceptor, new FrmVarAccess(acceptor));
		else
			// local function acces
			FrmDesc.setAccess(acceptor, new FrmLocAccess(acceptor, currentFrame));
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {

	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
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

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		for (AbsCaseStmt singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null)
			switchStmt.defaultBody.accept(this);
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		///
	}
}
