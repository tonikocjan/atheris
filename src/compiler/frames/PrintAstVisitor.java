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

package compiler.frames;

import compiler.*;
import compiler.ast.*;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AbsAtomConstExpr;
import compiler.ast.tree.expr.AbsBinExpr;
import compiler.ast.tree.expr.AbsExpr;
import compiler.ast.tree.expr.AbsForceValueExpr;
import compiler.ast.tree.expr.AbsFunCall;
import compiler.ast.tree.expr.AbsLabeledExpr;
import compiler.ast.tree.expr.AbsListExpr;
import compiler.ast.tree.expr.AbsOptionalEvaluationExpr;
import compiler.ast.tree.expr.AbsReturnExpr;
import compiler.ast.tree.expr.AbsTupleExpr;
import compiler.ast.tree.expr.AbsUnExpr;
import compiler.ast.tree.expr.AbsVarNameExpr;
import compiler.ast.tree.stmt.AbsCaseStmt;
import compiler.ast.tree.stmt.AbsControlTransferStmt;
import compiler.ast.tree.stmt.AbsForStmt;
import compiler.ast.tree.stmt.AbsIfStmt;
import compiler.ast.tree.stmt.AbsSwitchStmt;
import compiler.ast.tree.stmt.AbsWhileStmt;
import compiler.ast.tree.type.AbsAtomType;
import compiler.ast.tree.type.AbsFunType;
import compiler.ast.tree.type.AbsListType;
import compiler.ast.tree.type.AbsOptionalType;
import compiler.ast.tree.type.AbsTypeName;
import compiler.seman.*;
import compiler.seman.type.*;

public class PrintAstVisitor implements ASTVisitor {

	private boolean dump;
    private int indent;
    private SymbolTableMap symbolTable;
    private SymbolDescriptionMap symbolDescription;
    private FrameDescriptionMap frameDescription;

    public PrintAstVisitor(boolean dump, SymbolTableMap symbolTable, SymbolDescriptionMap symbolDescription, FrameDescriptionMap frameDescription) {
        this.dump = dump;
        this.symbolTable = symbolTable;
        this.symbolDescription = symbolDescription;
        this.frameDescription = frameDescription;
    }

	public void dump(AbsTree tree) {
		if (!dump)
			return;
		if (Logger.dumpFile() == null)
			return;
		indent = 0;
		tree.accept(this);
	}

	public void visit(AbsListType arrType) {
		Logger.dump(indent, "AbsArrType " + arrType.position.toString() + ": "
				+ "[" + arrType.count + "]");
		{
			Type typ = symbolDescription.getTypeForAstNode(arrType);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		arrType.type.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsClassDef classDef) {
        Logger.dump(indent, classDef.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(classDef);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		Logger.dump(indent, "Member definitions:");
		indent += 2; classDef.definitions.accept(this); indent -= 2;
		Logger.dump(indent, "Constructors:");
		indent += 2;
		for (AbsFunDef c : classDef.construstors) {
			c.accept(this);
		}
		indent -= 4;
	}

	public void visit(AbsAtomConstExpr atomConst) {
		switch (atomConst.type) {
		case LOG:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": LOGICAL(" + atomConst.value + ")");
			break;
		case INT:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": INTEGER(" + atomConst.value + ")");
			break;
		case STR:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": STRING(" + atomConst.value + ")");
			break;
		case DOB:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": DOUBLE(" + atomConst.value + ")");
			break;
		case CHR:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": CHAR(" + atomConst.value + ")");
		case VOID:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": VOID(" + atomConst.value + ")");
			break;
        case NIL:
            Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
                    + ": NULL");
            break;
		default:
			Logger.error("Internal error :: compiler.ast.PrintAstVisitor.visit(AbsAtomConst)");
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(atomConst);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AbsAtomType atomType) {
		switch (atomType.type) {
		case LOG:
			Logger.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": LOGICAL");
			break;
		case INT:
			Logger.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": INTEGER");
			break;
		case STR:
			Logger.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": STRING");
			break;
		case DOB:
			Logger.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": DOUBLE");
			break;
		case CHR:
			Logger.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": CHAR");
			break;
		case VOID:
			Logger.dump(indent, "AbsAtomType " + atomType.position.toString()
					+ ": VOID");
			break;
        case NIL:
            Logger.dump(indent, "AbsAtomType " + atomType.position.toString()
                    + ": NIL");
            break;
		default:
			Logger.error("Internal error :: compiler.ast.PrintAstVisitor.visit(AbsAtomType)");
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(atomType);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AbsBinExpr binExpr) {
        switch (binExpr.oper) {
            case AbsBinExpr.IOR:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": IOR");
                break;
            case AbsBinExpr.AND:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": AND");
                break;
            case AbsBinExpr.EQU:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": EQU");
                break;
            case AbsBinExpr.NEQ:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": NEQ");
                break;
            case AbsBinExpr.LEQ:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": LEQ");
                break;
            case AbsBinExpr.GEQ:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": GEQ");
                break;
            case AbsBinExpr.LTH:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": LTH");
                break;
            case AbsBinExpr.GTH:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": GTH");
                break;
            case AbsBinExpr.ADD:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": ADD");
                break;
            case AbsBinExpr.SUB:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": SUB");
                break;
            case AbsBinExpr.MUL:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": MUL");
                break;
            case AbsBinExpr.DIV:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": DIV");
                break;
            case AbsBinExpr.MOD:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": MOD");
                break;
            case AbsBinExpr.ARR:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": ARR");
                break;
            case AbsBinExpr.ASSIGN:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": ASSIGN");
                break;
            case AbsBinExpr.DOT:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": DOT");
                break;
            case AbsBinExpr.IS:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": IS");
                break;
            case AbsBinExpr.AS:
                Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString() + ": As");
                break;
            default:
                Logger.error("Internal error :: compiler.ast.Abstr.visit(AbsBinExpr)");
        }
        {
            Type typ = symbolDescription.getTypeForAstNode(binExpr);
            if (typ != null)
                Logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        indent += 2; binExpr.expr1.accept(this); indent -= 2;
        indent += 2; binExpr.expr2.accept(this); indent -= 2;
	}

	public void visit(AbsDefs defs) {
		Logger.dump(indent, "AbsDefs " + defs.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(defs);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (AbsDef def : defs.definitions) {
			indent += 2;
			def.accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsExprs exprs) {
		Logger.dump(indent, "AbsExprs " + exprs.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(exprs);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		for (AbsExpr e : exprs.expressions)
			e.accept(this);
		indent -= 2;
	}

	public void visit(AbsForStmt forStmt) {
		Logger.dump(indent, "AbsFor " + forStmt.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(forStmt);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
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
		Logger.dump(indent, "AbsFunCall " + funCall.position.toString() + ": "
				+ funCall.name);
		{
			AbsDef def = symbolDescription.getDefinitionForAstNode(funCall);
			if (def != null && def.position != null)
				Logger.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(funCall);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (int arg = 0; arg < funCall.numArgs(); arg++) {
			indent += 2;
			funCall.arg(arg).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsFunDef funDef) {
		Logger.dump(indent, "AbsFunDef " + funDef.position.toString() + ": "
				+ funDef.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(funDef);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmFrame frame = frameDescription.getFrame(funDef);
			if (frame != null)
				Logger.dump(indent + 2, "#framed as " + frame.toString());
		}
		for (AbsParDef par : funDef.getParamaters()) {
			indent += 2;
			par.accept(this);
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
		Logger.dump(indent, "AbsIfExpr " + ifExpr.position.toString() + ":");
		Type typ = symbolDescription.getTypeForAstNode(ifExpr);
		if (typ != null)
			Logger.dump(indent + 2, "#typed as " + typ.toString());
		
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
		Logger.dump(indent, "AbsPar " + par.position.toString() + ": "
				+ par.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(par);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmAccess access = frameDescription.getAccess(par);
			if (access != null)
				Logger.dump(indent + 2, "#accesed as " + access.toString());
		}
		indent += 2;
		par.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsTypeDef typeDef) {
//		Logger.dump(indent, "AbsTypeDef " + typeDef.position.toString() + ": "
//				+ typeDef.getName);
//		{
//			SemType typ = symbolDescription.getTypeForAstNode(typeDef);
//			if (typ != null)
//				Logger.dump(indent + 2, "#typed as " + typ.toString());
//		}
//		indent += 2;
//		typeDef.memberType.accept(this);
//		indent -= 2;
	}

	public void visit(AbsTypeName typeName) {
		Logger.dump(indent, "AbsTypeName " + typeName.position.toString()
				+ ": " + typeName.name);
		{
			AbsDef def = symbolDescription.getDefinitionForAstNode(typeName);
			if (def != null)
				Logger.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(typeName);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AbsUnExpr unExpr) {
		switch (unExpr.oper) {
		case AbsUnExpr.ADD:
			Logger.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": ADD");
			break;
		case AbsUnExpr.SUB:
			Logger.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": SUB");
			break;
		case AbsUnExpr.NOT:
			Logger.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": NOT");
			break;
		case AbsUnExpr.MEM:
			Logger.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": MEM");
			break;
		case AbsUnExpr.VAL:
			Logger.dump(indent, "AbsUnExpr " + unExpr.position.toString()
					+ ": VAL");
			break;
		default:
			Logger.error("Internal error :: compiler.ast.Abstr.visit(AbsBinExpr)");
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(unExpr);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		unExpr.expr.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarDef varDef) {
		Logger.dump(indent, "AbsVarDef " + varDef.position.toString() + ": "
				+ varDef.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(varDef);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmAccess access = frameDescription.getAccess(varDef);
			if (access != null)
				Logger.dump(indent + 2, "#accesed as " + access.toString());
		}
		indent += 2;
		if (varDef.type != null) varDef.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarNameExpr varName) {
		Logger.dump(indent, "AbsVarName " + varName.position.toString() + ": "
				+ varName.name);
		{
			AbsDef def = symbolDescription.getDefinitionForAstNode(varName);
			if (def != null)
				Logger.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(varName);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AbsWhileStmt whileStmt) {
		Logger.dump(indent, "AbsWhileName " + whileStmt.position.toString()
				+ ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(whileStmt);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		whileStmt.cond.accept(this);
		indent -= 2;
		indent += 2;
		whileStmt.body.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsImportDef importDef) {
        Logger.dump(indent, "AbsImportDef " + importDef.position + ":");
        {
            Type typ = symbolDescription.getTypeForAstNode(importDef);
            if (typ != null)
                Logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        indent += 2; importDef.imports.accept(this); indent -= 2;
	}

	@Override
	public void visit(AbsStmts stmts) {
		Logger.dump(indent, "AbsStmts " + stmts.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(stmts);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (AbsStmt s : stmts.statements) {
			indent += 2;
			s.accept(this);
			indent -= 2;
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		Logger.dump(indent, "AbsReturnExpr " + returnExpr.position.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(returnExpr);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Logger.dump(indent, "AbsListExpr " + absListExpr.position.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(absListExpr);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; 
		for (AbsExpr e : absListExpr.expressions)
			e.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsFunType funType) {
		Logger.dump(indent, "AbsFunType " + funType.position.toString() + ":");
		Logger.dump(indent + 2, funType.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(funType);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}		
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		Logger.dump(indent, "AbsControlTransferStmt: " + acceptor.control);
	}
	
	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		Logger.dump(indent, "AbsSwitchStmt " + switchStmt.position.toString() + ":");
		indent += 2;
		{
			Type typ = symbolDescription.getTypeForAstNode(switchStmt);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		
		Logger.dump(indent, "Subject expr:");
		indent += 2;
		switchStmt.subjectExpr.accept(this);
		{
			Type typ = symbolDescription.getTypeForAstNode(switchStmt.subjectExpr);
			if (typ != null)
				Logger.dump(indent, "#typed as " + typ.toString());
		}
		indent -= 2;
		
		for (AbsCaseStmt singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null) {
			Logger.dump(indent, "Default:");
			indent += 2;
			switchStmt.defaultBody.accept(this);
			{
				Type typ = symbolDescription.getTypeForAstNode(switchStmt.defaultBody);
				if (typ != null)
					Logger.dump(indent + 2, "#typed as " + typ.toString());
			}
			indent -= 2;
		}
		
		indent -= 2;		
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		Logger.dump(indent, "Case:");
		indent += 2; 
		for (AbsExpr e : acceptor.exprs) {
			e.accept(this);

			Type typ = symbolDescription.getTypeForAstNode(e);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent -= 2;
		Logger.dump(indent, "Body:");
		indent += 2; acceptor.body.accept(this); indent -= 2;		
	}

	@Override
	public void visit(AbsEnumDef enumDef) {
		Logger.dump(indent, "AbsEnumDef " + enumDef.position.toString() + ": " + enumDef.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(enumDef);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
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
			Type typ = symbolDescription.getTypeForAstNode(acceptor);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		if (acceptor.value != null)
			acceptor.value.accept(this);
	}

	@Override
	public void visit(AbsTupleDef tupleDef) {
		Logger.dump(indent, "AbsTupleDef " + tupleDef.position.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(tupleDef);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}		
	}

	@Override
	public void visit(AbsLabeledExpr labeledExpr) {
		Logger.dump(indent, "AbsLabeledExpr " + labeledExpr.position.toString());
		indent += 2;
		Logger.dump(indent, "Label: " + labeledExpr.name);
		labeledExpr.expr.accept(this);
		{
			Type typ = symbolDescription.getTypeForAstNode(labeledExpr);
			if (typ != null)
				Logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent -= 2;
	}

	@Override
	public void visit(AbsTupleExpr tupleExpr) {
		Logger.dump(indent, "AbsTupleExpr " + tupleExpr.position.toString());
		indent += 2;
		tupleExpr.expressions.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsOptionalType acceptor) {
		acceptor.childType.accept(this);
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr acceptor) {
		acceptor.subExpr.accept(this);
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);
	}

    @Override
    public void visit(AbsExtensionDef acceptor) {
        Logger.dump(indent, "AbsExtensionDef " + acceptor.position.toString() + ":");
        {
            Type typ = symbolDescription.getTypeForAstNode(acceptor);
            if (typ != null)
                Logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        indent += 2;
        acceptor.definitions.accept(this);
        indent -= 2;
    }

    @Override
    public void visit(AbsInterfaceDef acceptor) {
        Logger.dump(indent, "AbsInterfaceDef " + acceptor.position.toString() + ":");
        {
            Type typ = symbolDescription.getTypeForAstNode(acceptor);
            if (typ != null)
                Logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        indent += 2;
        acceptor.definitions.accept(this);
        indent -= 2;
    }
}
