/**
 * Copyright 2016 Toni Kocjan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package compiler.seman;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsTupleDef;
import compiler.abstr.tree.def.AbsVarDef;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsExpr;
import compiler.abstr.tree.expr.AbsForceValueExpr;
import compiler.abstr.tree.expr.AbsFunCall;
import compiler.abstr.tree.expr.AbsLabeledExpr;
import compiler.abstr.tree.expr.AbsListExpr;
import compiler.abstr.tree.expr.AbsOptionalEvaluationExpr;
import compiler.abstr.tree.expr.AbsReturnExpr;
import compiler.abstr.tree.expr.AbsTupleExpr;
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
import compiler.abstr.tree.type.AbsOptionalType;
import compiler.abstr.tree.type.AbsTypeName;
import compiler.seman.type.*;

/**
 * Semanticni analizator.
 * 
 * @author sliva
 */
public class SemAn implements ASTVisitor {

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/**
	 * Semanticni analizator.
	 * 
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public SemAn(boolean dump) {
		this.dump = dump;
	}
	
	/**
	 * Izpise abstraktno sintaksno drevo na datoteko vmesnih rezultatov.
	 * 
	 * @param tree
	 *            Abstraktno sintaksno drevo.
	 */
	public void dump(AbsTree tree) {
		if (! dump) return;
		if (Report.dumpFile() == null) return;
		indent = 0;
		tree.accept(this);
	}
	
	// Kot Visitor izpise abstraktno sintaksno drevo:
	
	/** Trenutni zamik. */
	private int indent;
	
	public void visit(AbsListType listType) {
		Report.dump(indent, "AbsListType " + listType.position.toString() + ": " + "[" + listType.count + "]");
		{
			Type typ = SymbDesc.getType(listType);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; listType.type.accept(this); indent -= 2;
	}		
	
	@Override
	public void visit(AbsClassDef classDef) {
		Report.dump(indent, "AbsClassDef " + classDef.position.toString() + ": " + classDef.getName());		
		{
			Type typ = SymbDesc.getType(classDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		Report.dump(indent, "Member definitions:");
		indent += 2; classDef.definitions.accept(this); indent -= 2;
		Report.dump(indent, "Constructors:");
		indent += 2;
		for (AbsFunDef c : classDef.contrustors) {
			c.accept(this);
		}
		indent -= 4;
	}
	
	public void visit(AbsAtomConstExpr atomConst) {
		switch (atomConst.type) {
		case LOG:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString() + ": LOGICAL(" + atomConst.value + ")");
			break;
		case INT:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString() + ": INTEGER(" + atomConst.value + ")");
			break;
		case STR:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString() + ": STRING(" + atomConst.value + ")");
			break;
		case DOB:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString() + ": DOUBLE(" + atomConst.value + ")");
			break;
		case CHR:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString() + ": CHAR(" + atomConst.value + ")");
			break;
		case VOID:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString() + ": VOID(" + atomConst.value + ")");
			break;
		case NIL:
			Report.dump(indent, "AbsAtomConst " + atomConst.position.toString() + " (NIL)");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Seman.visit(AbsAtomConst)");
		}
		{
			Type typ = SymbDesc.getType(atomConst);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
	}
	
	public void visit(AbsAtomType atomType) {
		switch (atomType.type) {
		case LOG:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString() + ": LOGICAL");
			break;
		case INT:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString() + ": INTEGER");
			break;
		case STR:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString() + ": STRING");
			break;
		case DOB:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString() + ": DOUBLE");
			break;
		case CHR:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString() + ": CHAR");
			break;
		case VOID:
			Report.dump(indent, "AbsAtomType " + atomType.position.toString() + ": VOID");
			break;
        case NIL:
            Report.dump(indent, "AbsAtomType " + atomType.position.toString() + ": NIL");
            break;
		default:
			Report.error("Internal error :: compiler.abstr.Seman.visit(AbsAtomType)");
		}
		{
			Type typ = SymbDesc.getType(atomType);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
	}
	
	public void visit(AbsBinExpr binExpr) {
		switch (binExpr.oper) {
		case AbsBinExpr.IOR:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": IOR");
			break;
		case AbsBinExpr.AND:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": AND");
			break;
		case AbsBinExpr.EQU:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": EQU");
			break;
		case AbsBinExpr.NEQ:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": NEQ");
			break;
		case AbsBinExpr.LEQ:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": LEQ");
			break;
		case AbsBinExpr.GEQ:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": GEQ");
			break;
		case AbsBinExpr.LTH:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": LTH");
			break;
		case AbsBinExpr.GTH:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": GTH");
			break;
		case AbsBinExpr.ADD:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": ADD");
			break;
		case AbsBinExpr.SUB:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": SUB");
			break;
		case AbsBinExpr.MUL:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": MUL");
			break;
		case AbsBinExpr.DIV:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": DIV");
			break;
		case AbsBinExpr.MOD:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": MOD");
			break;
		case AbsBinExpr.ARR:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": ARR");
			break;
		case AbsBinExpr.ASSIGN:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": ASSIGN");
			break;
		case AbsBinExpr.DOT:
			Report.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": DOT");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Abstr.visit(AbsBinExpr)");
		}
		{
			Type typ = SymbDesc.getType(binExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; binExpr.expr1.accept(this); indent -= 2;
		indent += 2; binExpr.expr2.accept(this); indent -= 2;
	}
	
	public void visit(AbsDefs defs) {
		Report.dump(indent, "AbsDefs " + defs.position.toString() + ":");
		{
			Type typ = SymbDesc.getType(defs);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (AbsDef def : defs.definitions) {
			indent += 2; def.accept(this); indent -= 2;
		}
	}
	
	public void visit(AbsExprs exprs) {
		Report.dump(indent, "AbsExprs " + exprs.position.toString() + ":");
		{
			Type typ = SymbDesc.getType(exprs);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (AbsExpr e : exprs.expressions) {
			indent += 2; e.accept(this); indent -= 2;
		}
	}
	
	public void visit(AbsForStmt forStmt) {
		Report.dump(indent, "AbsFor " + forStmt.position.toString() + ":");
		{
			Type typ = SymbDesc.getType(forStmt);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; forStmt.iterator.accept(this); indent -= 2;
		indent += 2; forStmt.collection.accept(this); indent -= 2;
		indent += 2; forStmt.body.accept(this); indent -= 2;
	}
	
	public void visit(AbsFunCall funCall) {
		Report.dump(indent, "AbsFunCall " + funCall.position.toString() + ": " + funCall.name);
		{
			AbsDef def = SymbDesc.getNameDef(funCall);
			if (def != null && def.position != null)
				Report.dump(indent + 2, "#defined at " + def.position.toString());
		}
		{
			Type typ = SymbDesc.getType(funCall);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (int arg = 0; arg < funCall.numArgs(); arg++) {
			indent += 2; funCall.arg(arg).accept(this); indent -= 2;
		}
	}
	
	public void visit(AbsFunDef funDef) {
		Report.dump(indent, "AbsFunDef " + funDef.position.toString() + ": " + funDef.name);
		{
			Type typ = SymbDesc.getType(funDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (AbsParDef par : funDef.getParamaters()) {
			indent += 2; par.accept(this); indent -= 2;
		}
		indent += 2; funDef.type.accept(this); indent -= 2;
		indent += 2; funDef.func.accept(this); indent -= 2;
	}
	
	public void visit(AbsIfStmt ifExpr) {
		Report.dump(indent, "AbsIfExpr " + ifExpr.position.toString() + ":");
		Type typ = SymbDesc.getType(ifExpr);
		if (typ != null)
			Report.dump(indent + 2, "#typed as " + typ.toString());
		
		indent += 2;
		for (Condition c : ifExpr.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
		}
		if (ifExpr.elseBody != null)
			ifExpr.elseBody.accept(this);
		indent -= 2;
	}
	
	public void visit(AbsParDef par) {
		Report.dump(indent, "AbsPar " + par.position.toString() + ": " + par.name);
		{
			Type typ = SymbDesc.getType(par);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; par.type.accept(this); indent -= 2;
	}
	
	public void visit(AbsTypeName typeName) {
		Report.dump(indent, "AbsTypeName " + typeName.position.toString() + ": " + typeName.name);
		{
			AbsDef def = SymbDesc.getNameDef(typeName);
			if (def != null)
				Report.dump(indent + 2, "#defined at " + def.position.toString());
		}
		{
			Type typ = SymbDesc.getType(typeName);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
	}
	
	public void visit(AbsUnExpr unExpr) {
		switch (unExpr.oper) {
		case AbsUnExpr.ADD:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString() + ": ADD");
			break;
		case AbsUnExpr.SUB:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString() + ": SUB");
			break;
		case AbsUnExpr.NOT:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString() + ": NOT");
			break;
		case AbsUnExpr.MEM:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString() + ": MEM");
			break;
		case AbsUnExpr.VAL:
			Report.dump(indent, "AbsUnExpr " + unExpr.position.toString() + ": VAL");
			break;
		default:
			Report.error("Internal error :: compiler.abstr.Abstr.visit(AbsBinExpr)");
		}
		{
			Type typ = SymbDesc.getType(unExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; unExpr.expr.accept(this); indent -= 2;
	}
	
	public void visit(AbsVarDef varDef) {
		Report.dump(indent, "AbsVarDef " + varDef.position.toString() + ": " + varDef.name);
		if (!varDef.isMutable)
			Report.dump(indent + 2, "#CONSTANT");
		{
			Type typ = SymbDesc.getType(varDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; if (varDef.type != null) varDef.type.accept(this); indent -= 2;
	}
	
	public void visit(AbsVarNameExpr varName) {
		Report.dump(indent, "AbsVarName " + varName.position.toString() + ": " + varName.name);
		{
			AbsDef def = SymbDesc.getNameDef(varName);
			if (def != null)
				Report.dump(indent + 2, "#defined at " + def.position.toString());
		}
		{
			Type typ = SymbDesc.getType(varName);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
	}
	
	public void visit(AbsWhileStmt whileStmt) {
		Report.dump(indent, "AbsWhileName " + whileStmt.position.toString() + ":");
		{
			Type typ = SymbDesc.getType(whileStmt);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; whileStmt.cond.accept(this); indent -= 2;
		indent += 2; whileStmt.body.accept(this); indent -= 2;
	}

	@Override
	public void visit(AbsImportDef importDef) {
		Report.dump(indent, "AbsImportDef " + importDef.position + ":");
		{
			Type typ = SymbDesc.getType(importDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; importDef.imports.accept(this); indent -= 2;
	}

	@Override
	public void visit(AbsStmts stmts) {
		Report.dump(indent, "AbsStmts " + stmts.position.toString() + ":");
		{
			Type typ = SymbDesc.getType(stmts);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());	
		}
		
		for (AbsStmt stmt : stmts.statements) {
			indent += 2; stmt.accept(this); indent -= 2;
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		Report.dump(indent, "AbsReturnExpr " + returnExpr.position.toString());
		{
			Type typ = SymbDesc.getType(returnExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; if (returnExpr.expr != null) returnExpr.expr.accept(this); indent -= 2;
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Report.dump(indent, "AbsListExpr " + absListExpr.position.toString());
		{
			Type typ = SymbDesc.getType(absListExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; 
		for (AbsExpr e : absListExpr.expressions)
			e.accept(this);
		indent -= 2;		
	}

	@Override
	public void visit(AbsFunType funType) {
		Report.dump(indent, "AbsFunType " + funType.position.toString() + ":");
		Report.dump(indent + 2, funType.toString());
		{
			Type typ = SymbDesc.getType(funType);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		Report.dump(indent, "AbsControlTransferStmt: " + acceptor.control);		
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		Report.dump(indent, "AbsSwitchStmt " + switchStmt.position.toString() + ":");
		indent += 2;
		{
			Type typ = SymbDesc.getType(switchStmt);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		
		Report.dump(indent, "Subject expr:");
		indent += 2;
		switchStmt.subjectExpr.accept(this);
		{
			Type typ = SymbDesc.getType(switchStmt.subjectExpr);
			if (typ != null)
				Report.dump(indent, "#typed as " + typ.toString());
		}
		indent -= 2;
		
		for (AbsCaseStmt singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null) {
			Report.dump(indent, "Default:");
			indent += 2;
			switchStmt.defaultBody.accept(this);
			{
				Type typ = SymbDesc.getType(switchStmt.defaultBody);
				if (typ != null)
					Report.dump(indent + 2, "#typed as " + typ.toString());
			}
			indent -= 2;
		}
		
		indent -= 2;		
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		Report.dump(indent, "Case:");
		indent += 2; 
		for (AbsExpr e : acceptor.exprs) {
			e.accept(this);

			Type typ = SymbDesc.getType(e);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent -= 2;
		Report.dump(indent, "Body:");
		indent += 2; acceptor.body.accept(this); indent -= 2;		
	}

	@Override
	public void visit(AbsEnumDef enumDef) {
		Report.dump(indent, "AbsEnumDef " + enumDef.position.toString() + ": " + enumDef.name);		
		{
			Type typ = SymbDesc.getType(enumDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}

		indent += 2;
		if (enumDef.type != null)
			enumDef.type.accept(this);		
		indent -= 2;
		
		indent += 2;
		for (AbsDef def : enumDef.definitions)
			def.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		acceptor.name.accept(this);
		{
			Type typ = SymbDesc.getType(acceptor);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		if (acceptor.value != null)
			acceptor.value.accept(this);
	}

	@Override
	public void visit(AbsTupleDef tupleDef) {
		Report.dump(indent, "AbsTupleDef " + tupleDef.position.toString());		
		{
			Type typ = SymbDesc.getType(tupleDef);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}		
	}

	@Override
	public void visit(AbsLabeledExpr labeledExpr) {
		Report.dump(indent, "AbsLabeledExpr " + labeledExpr.position.toString());		
		{
			Type typ = SymbDesc.getType(labeledExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}	
		indent += 2;
		Report.dump(indent, "Label: " + labeledExpr.name);
		labeledExpr.expr.accept(this);
		{
			Type typ = SymbDesc.getType(labeledExpr);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent -= 2;
	}

	@Override
	public void visit(AbsTupleExpr tupleExpr) {
		Report.dump(indent, "AbsTupleExpr " + tupleExpr.position.toString());
		indent += 2;
		tupleExpr.expressions.accept(this);
		indent -= 2;
	}
	
	@Override
	public void visit(AbsOptionalType acceptor) {
		Report.dump(indent, "AbsOptionalType " + acceptor.position.toString());
		{
			Type typ = SymbDesc.getType(acceptor);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		acceptor.childType.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr acceptor) {
		Report.dump(indent, "AbsOptionalEvaluationExpr " + acceptor.position.toString());
		{
			Type typ = SymbDesc.getType(acceptor);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		acceptor.subExpr.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		Report.dump(indent, "AbsForceValueExpr " + acceptor.position.toString());
		{
			Type typ = SymbDesc.getType(acceptor);
			if (typ != null)
				Report.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		acceptor.subExpr.accept(this);
		indent -= 2;
	}
}
