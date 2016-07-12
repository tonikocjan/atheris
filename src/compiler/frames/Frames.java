package compiler.frames;

import java.util.Vector;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.*;
import compiler.seman.type.*;

/**
 * Izracun klicnih zapisov.
 * 
 * @author sliva
 */
public class Frames implements Visitor {

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/**
	 * Izracun klicnih zapisov.
	 * 
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public Frames(boolean dump) {
		this.dump = dump;
	}

	/**
	 * Izpise abstraktno sintaksno drevo na datoteko vmesnih rezultatov.
	 * 
	 * @param tree
	 *            Abstraktno sintaksno drevo.
	 */
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
		Report.dump(indent, "AbsArrType " + arrType.position.toString() + ": "
				+ "[" + arrType.count + "]");
		{
			SemType typ = SymbDesc.getType(arrType);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		arrType.type.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsClassDef classDef) {
		Report.dump(indent, "AbsStructType " + classDef.position.toString()
				+ ": " + classDef.getName());
		{
			SemType typ = SymbDesc.getType(classDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		for (AbsFunDef c : classDef.contrustors) {
			c.accept(this);
		}
		indent += 2;
		classDef.getDefinitions().accept(this);
		indent -= 2;
	}

	public void visit(AbsAtomConst atomConst) {
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
		case DOB:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": DOUBLE(" + atomConst.value + ")");
			break;
		case CHR:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": CHAR(" + atomConst.value + ")");
		case VOID:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": VOID(" + atomConst.value + ")");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Frames.visit(AbsAtomConst)");
		}
		{
			SemType typ = SymbDesc.getType(atomConst);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
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
			Report.error("Internal error :: compiler.abstr.Frames.visit(AbsAtomType)");
		}
		{
			SemType typ = SymbDesc.getType(atomType);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
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
		{
			SemType typ = SymbDesc.getType(binExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
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
		{
			SemType typ = SymbDesc.getType(defs);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (int def = 0; def < defs.numDefs(); def++) {
			indent += 2;
			defs.def(def).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsExprs exprs) {
		Report.dump(indent, "AbsExprs " + exprs.position.toString() + ":");
		{
			SemType typ = SymbDesc.getType(exprs);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (int expr = 0; expr < exprs.numExprs(); expr++) {
			indent += 2;
			exprs.expr(expr).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsFor forStmt) {
		Report.dump(indent, "AbsFor " + forStmt.position.toString() + ":");
		{
			SemType typ = SymbDesc.getType(forStmt);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
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
		{
			AbsDef def = SymbDesc.getNameDef(funCall);
			if (def != null && def.position != null)
				Report.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			SemType typ = SymbDesc.getType(funCall);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (int arg = 0; arg < funCall.numArgs(); arg++) {
			indent += 2;
			funCall.arg(arg).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsFunDef funDef) {
		Report.dump(indent, "AbsFunDef " + funDef.position.toString() + ": "
				+ funDef.name);
		{
			SemType typ = SymbDesc.getType(funDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmFrame frame = FrmDesc.getFrame(funDef);
			if (frame != null)
				Report.dump(indent + 2, "#framed as " + frame.toString());
		}
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

	public void visit(AbsIfThen ifThen) {
		Report.dump(indent, "AbsIfThen " + ifThen.position.toString() + ":");
		{
			SemType typ = SymbDesc.getType(ifThen);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		ifThen.cond.accept(this);
		indent -= 2;
		indent += 2;
		ifThen.thenBody.accept(this);
		indent -= 2;
	}

	public void visit(AbsIfThenElse ifThenElse) {
		Report.dump(indent, "AbsIfThenElse " + ifThenElse.position.toString()
				+ ":");
		{
			SemType typ = SymbDesc.getType(ifThenElse);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		ifThenElse.cond.accept(this);
		indent -= 2;
		indent += 2;
		ifThenElse.thenBody.accept(this);
		indent -= 2;
		indent += 2;
		ifThenElse.elseBody.accept(this);
		indent -= 2;
	}

	public void visit(AbsPar par) {
		Report.dump(indent, "AbsPar " + par.position.toString() + ": "
				+ par.name);
		{
			SemType typ = SymbDesc.getType(par);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmAccess access = FrmDesc.getAccess(par);
			if (access != null)
				Report.dump(indent + 2, "#accesed as " + access.toString());
		}
		indent += 2;
		par.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsTypeDef typeDef) {
//		Report.dump(indent, "AbsTypeDef " + typeDef.position.toString() + ": "
//				+ typeDef.name);
//		{
//			SemType typ = SymbDesc.getType(typeDef);
//			if (typ != null)
//				Report.dump(indent + 2, "#typed as " + typ.toString());
//		}
//		indent += 2;
//		typeDef.type.accept(this);
//		indent -= 2;
	}

	public void visit(AbsTypeName typeName) {
		Report.dump(indent, "AbsTypeName " + typeName.position.toString()
				+ ": " + typeName.name);
		{
			AbsDef def = SymbDesc.getNameDef(typeName);
			if (def != null)
				Report.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			SemType typ = SymbDesc.getType(typeName);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
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
			Report.error("Internal error :: compiler.abstr.Abstr.visit(AbsBinExpr)");
		}
		{
			SemType typ = SymbDesc.getType(unExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		unExpr.expr.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarDef varDef) {
		Report.dump(indent, "AbsVarDef " + varDef.position.toString() + ": "
				+ varDef.name);
		{
			SemType typ = SymbDesc.getType(varDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmAccess access = FrmDesc.getAccess(varDef);
			if (access != null)
				Report.dump(indent + 2, "#accesed as " + access.toString());
		}
		indent += 2;
		varDef.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarName varName) {
		Report.dump(indent, "AbsVarName " + varName.position.toString() + ": "
				+ varName.name);
		{
			AbsDef def = SymbDesc.getNameDef(varName);
			if (def != null)
				Report.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			SemType typ = SymbDesc.getType(varName);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AbsWhile whileStmt) {
		Report.dump(indent, "AbsWhileName " + whileStmt.position.toString()
				+ ":");
		{
			SemType typ = SymbDesc.getType(whileStmt);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		whileStmt.cond.accept(this);
		indent -= 2;
		indent += 2;
		whileStmt.body.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsImportDef acceptor) {

	}

	@Override
	public void visit(AbsStmts stmts) {
		Report.dump(indent, "AbsStmts " + stmts.position.toString() + ":");
		{
			SemType typ = SymbDesc.getType(stmts);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (int stmt = 0; stmt < stmts.numStmts(); stmt++) {
			indent += 2;
			stmts.stmt(stmt).accept(this);
			indent -= 2;
		}
	}

	@Override
	public void visit(AbsConstDef constDef) {
		Report.dump(indent, "AbsConstDef " + constDef.position.toString() + ": "
				+ constDef.name);
		{
			SemType typ = SymbDesc.getType(constDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmAccess access = FrmDesc.getAccess(constDef);
			if (access != null)
				Report.dump(indent + 2, "#accesed as " + access.toString());
		}
		indent += 2;
		constDef.type.accept(this);
		indent -= 2;
	}
	

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		Report.dump(indent, "AbsReturnExpr " + returnExpr.position.toString());
		{
			SemType typ = SymbDesc.getType(returnExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Report.dump(indent, "AbsListExpr " + absListExpr.position.toString());
		{
			SemType typ = SymbDesc.getType(absListExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; 
		for (AbsExpr e : absListExpr.expressions)
			e.accept(this);
		indent -= 2;
	}

}
