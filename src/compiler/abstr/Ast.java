package compiler.abstr;

import compiler.*;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
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

/**
 * @author sliva
 */
public class Ast implements ASTVisitor {

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	public Ast(boolean dump) {
		this.dump = dump;
	}

	public void dump(AbsTree tree) {
		if (!dump)
			return;
		if (Report.dumpFile() == null)
			return;
		indent = 0;
		tree.accept(this);
	}

	// Kot Visitor izpise abstraktno sintaksno drevo:

	/** Trenutni zamik. */
	private int indent;

	public void visit(AbsListType arrType) {
		Report.dump(indent, "AbsArrType " + arrType.position.toString() + ":");
		Report.dump(indent + 2, "[" + arrType.count + "]");
		indent += 2;
		arrType.type.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsClassDef classDef) {
		Report.dump(indent, "AbsClassDef " + classDef.position.toString()
				+ ":");
		indent += 2;
		Report.dump(indent, "Constructors: ");
		indent += 2;
		for (AbsFunDef c : classDef.contrustors) {
			c.accept(this);
		}
		indent -= 2;
		for (int i = 0; i < classDef.statements.numStmts(); i++)
			classDef.statements.stmt(i).accept(this);
		indent -= 2;
	}

	public void visit(AbsAtomConstExpr atomConst) {
		switch (atomConst.type) {
		case LOG:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": LOGICAL(" + atomConst.value + ")");
			break;
		case INT:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": INTEGER(" + atomConst.value + ")");
			break;
		case STR:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": STRING(" + atomConst.value + ")");
			break;
		case CHR:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": CHAR(" + atomConst.value + ")");
			break;
		case DOB:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": DOUBLE(" + atomConst.value + ")");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Abstr.visit(AbsAtomConst)");
		}
	}

	public void visit(AbsAtomType atomType) {
		switch (atomType.type) {
		case LOG:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": LOGICAL");
			break;
		case INT:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": INTEGER");
			break;
		case STR:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": STRING");
			break;
		case DOB:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": DOUBLE");
			break;
		case CHR:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": CHAR");
			break;
		case VOID:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": VOID");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Abstr.visit(AbsAtomType)");
		}
	}

	public void visit(AbsBinExpr binExpr) {
		switch (binExpr.oper) {
		case AbsBinExpr.IOR:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": IOR");
			break;
		case AbsBinExpr.AND:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": AND");
			break;
		case AbsBinExpr.EQU:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": EQU");
			break;
		case AbsBinExpr.NEQ:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": NEQ");
			break;
		case AbsBinExpr.LEQ:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": LEQ");
			break;
		case AbsBinExpr.GEQ:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": GEQ");
			break;
		case AbsBinExpr.LTH:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": LTH");
			break;
		case AbsBinExpr.GTH:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": GTH");
			break;
		case AbsBinExpr.ADD:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": ADD");
			break;
		case AbsBinExpr.SUB:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": SUB");
			break;
		case AbsBinExpr.MUL:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": MUL");
			break;
		case AbsBinExpr.DIV:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": DIV");
			break;
		case AbsBinExpr.MOD:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": MOD");
			break;
		case AbsBinExpr.ARR:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": ARR");
			break;
		case AbsBinExpr.ASSIGN:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": ASSIGN");
			break;
		case AbsBinExpr.DOT:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": DOT");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Abstr.visit(AbsBinExpr)");
		}
		indent += 2;
		binExpr.expr1.accept(this);
		indent -= 2;
		indent += 2;
		binExpr.expr2.accept(this);
		indent -= 2;
	}

	public void visit(AbsDefs defs) {
		Report.dump(indent, "AbsDefs " + defs.position.toString() + ":");
		for (int def = 0; def < defs.numDefs(); def++) {
			indent += 2;
			defs.def(def).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsExprs exprs) {
		Report.dump(indent, "AbsExprs " + exprs.position.toString() + ":");
		for (int expr = 0; expr < exprs.numExprs(); expr++) {
			indent += 2;
			exprs.expr(expr).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsForStmt forStmt) {
		Report.dump(indent, "AbsFor " + forStmt.position.toString() + ":");
		indent += 2;
		forStmt.iterator.accept(this);
		indent -= 2;
		indent += 2;
		forStmt.collection.accept(this);
		indent -= 2;
		indent += 2;
		forStmt.body.accept(this);
		indent -= 2;
	}

	public void visit(AbsFunCall funCall) {
		Report.dump(indent, "AbsFunCall " + funCall.position.toString() + ": "
				+ funCall.name);
		for (int arg = 0; arg < funCall.numArgs(); arg++) {
			indent += 2;
			funCall.arg(arg).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsFunDef funDef) {
		Report.dump(indent, "AbsFunDef " + funDef.position.toString() + ": "
				+ funDef.name);
		for (int par = 0; par < funDef.numPars(); par++) {
			indent += 2;
			funDef.par(par).accept(this);
			indent -= 2;
		}
		indent += 2;
		funDef.type.accept(this);
		indent -= 2;
		indent += 2;
		funDef.func.accept(this);
		indent -= 2;
	}

	public void visit(AbsIfStmt ifExpr) {
		Report.dump(indent, "AbsIfExpr " + ifExpr.position.toString() + ":");
		indent += 2;
		for (Condition c : ifExpr.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
		}
		ifExpr.elseBody.accept(this);
		indent -= 2;
	}

	public void visit(AbsParDef par) {
		Report.dump(indent, "AbsPar " + par.position.toString() + ": "
				+ par.name);
		indent += 2;
		par.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsTypeName typeName) {
		Report.dump(indent, "AbsTypeName " + typeName.position.toString()
				+ ": " + typeName.name);
	}

	public void visit(AbsUnExpr unExpr) {
		switch (unExpr.oper) {
		case AbsUnExpr.ADD:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": ADD");
			break;
		case AbsUnExpr.SUB:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": SUB");
			break;
		case AbsUnExpr.NOT:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": NOT");
			break;
		case AbsUnExpr.MEM:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": MEM");
			break;
		case AbsUnExpr.VAL:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": VAL");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Abstr.visit(AbsUnExpr)");
		}
		indent += 2;
		unExpr.expr.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarDef varDef) {
		Report.dump(indent, "AbsVarDef " + varDef.position.toString() + ": "
				+ varDef.name);
		indent += 2;
		if (varDef.isConstant)
			Report.dump(indent, "#CONSTANT");
		if (varDef.type == null)
			Report.dump(indent, "?");
		else
			varDef.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarNameExpr varName) {
		Report.dump(indent, "AbsVarName " + varName.position.toString() + ": "
				+ varName.name);
	}

	public void visit(AbsWhileStmt where) {
		Report.dump(indent, "AbsWhileName " + where.position.toString() + ":");
		indent += 2;
		where.cond.accept(this);
		indent -= 2;
		indent += 2;
		where.body.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsImportDef imp) {
		Report.dump(indent, "AbsImportDef " + imp.position.toString() + ":");
		Report.dump(indent + 2, "Filename: " + imp.fileName);
	}

	@Override
	public void visit(AbsStmts stmts) {
		Report.dump(indent, "AbsStmts " + stmts.position.toString() + ":");
		for (int def = 0; def < stmts.numStmts(); def++) {
			indent += 2;
			stmts.stmt(def).accept(this);
			indent -= 2;
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		Report.dump(indent, "AbsConstDef " + returnExpr.position.toString());
		indent += 2;
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Report.dump(indent, "AbsListExpr");
		indent += 2;
		for (AbsExpr e: absListExpr.expressions)
			e.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsFunType funType) {
		Report.dump(indent, "AbsFunType " + funType.position.toString() + ":");
		Report.dump(indent + 2, funType.toString());
	}

	@Override
	public void visit(AbsControlTransferStmt controlTransfer) {
		Report.dump(indent, "AbsControlTransferStmt: " + controlTransfer.control);
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		Report.dump(indent, "AbsSwitchStmt " + switchStmt.position.toString() + ":");
		indent += 2;
		
		Report.dump(indent, "Subject expr:");
		indent += 2;
		switchStmt.subjectExpr.accept(this);
		indent -= 2;
		
		for (AbsCaseStmt singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null) {
			Report.dump(indent, "Default:");
			indent += 2;
			switchStmt.defaultBody.accept(this);
			indent -= 2;
		}
		
		indent -= 2;
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		Report.dump(indent, "Case:");
		indent += 2; 
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		indent -= 2;
		Report.dump(indent, "Body:");
		indent += 2; acceptor.body.accept(this); indent -= 2;
	}

	@Override
	public void visit(AbsEnumDef enumDef) {
		Report.dump(indent, "AbsEnumDef " + enumDef.position.toString() + ":");
		if (enumDef.type != null) {
			indent += 2;
			enumDef.type.accept(this);
			indent -= 2;
		}
		indent += 2;
		for (AbsEnumMemberDef def : enumDef.definitions)
			def.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsEnumMemberDef def) {
		Report.dump(indent, "Name:");
		indent += 2;
		def.name.accept(this);
		indent -= 2;
		if (def.value != null) {
			Report.dump(indent, "Raw value:");
			indent += 2;
			def.value.accept(this);
			indent -= 2;
		}
	}
}
