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

import compiler.ast.*;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.*;
import compiler.ast.tree.expr.AstExpression;
import compiler.ast.tree.stmt.*;
import compiler.ast.tree.stmt.AstCaseStatement;
import compiler.ast.tree.stmt.AstControlTransferStatement;
import compiler.ast.tree.type.*;
import compiler.ast.tree.type.AstOptionalType;
import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import compiler.seman.*;
import compiler.seman.type.*;

public class PrintAstVisitor implements ASTVisitor {

    private static LoggerInterface logger = LoggerFactory.logger();

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

	public void dump(AstNode tree) {
		if (!dump)
			return;
		if (logger.dumpFile() == null)
			return;
		indent = 0;
		tree.accept(this);
	}

	public void visit(AstListType arrType) {
        logger.dump(indent, "AbsArrType " + arrType.position.toString() + ": "
				+ "[" + arrType.elementCount + "]");
		{
			Type typ = symbolDescription.getTypeForAstNode(arrType);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		arrType.type.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AstClassDefinition classDef) {
        logger.dump(indent, classDef.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(classDef);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
        logger.dump(indent, "Member memberDefinitions:");
		indent += 2; classDef.memberDefinitions.accept(this); indent -= 2;
        logger.dump(indent, "Constructors:");
		indent += 2;
		for (AstFunctionDefinition c : classDef.construstors) {
			c.accept(this);
		}
		indent -= 4;
	}

	public void visit(AstAtomConstExpression atomConst) {
		switch (atomConst.type) {
		case LOG:
            logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": LOGICAL(" + atomConst.value + ")");
			break;
		case INT:
            logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": INTEGER(" + atomConst.value + ")");
			break;
		case STR:
            logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": STRING(" + atomConst.value + ")");
			break;
		case DOB:
            logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": DOUBLE(" + atomConst.value + ")");
			break;
		case CHR:
            logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": CHAR(" + atomConst.value + ")");
		case VOID:
            logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": VOID(" + atomConst.value + ")");
			break;
        case NIL:
            logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
                    + ": NULL");
            break;
		default:
            logger.error("Internal error :: compiler.ast.PrintAstVisitor.visit(AbsAtomConst)");
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(atomConst);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AstAtomType atomType) {
		switch (atomType.type) {
		case LOG:
            logger.dump(indent, "AstAtomType " + atomType.position.toString()
					+ ": LOGICAL");
			break;
		case INT:
            logger.dump(indent, "AstAtomType " + atomType.position.toString()
					+ ": INTEGER");
			break;
		case STR:
            logger.dump(indent, "AstAtomType " + atomType.position.toString()
					+ ": STRING");
			break;
		case DOB:
            logger.dump(indent, "AstAtomType " + atomType.position.toString()
					+ ": DOUBLE");
			break;
		case CHR:
            logger.dump(indent, "AstAtomType " + atomType.position.toString()
					+ ": CHAR");
			break;
		case VOID:
            logger.dump(indent, "AstAtomType " + atomType.position.toString()
					+ ": VOID");
			break;
        case NIL:
            logger.dump(indent, "AstAtomType " + atomType.position.toString()
                    + ": NIL");
            break;
		default:
            logger.error("Internal error :: compiler.ast.PrintAstVisitor.visit(AstAtomType)");
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(atomType);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AstBinaryExpression binExpr) {
        switch (binExpr.oper) {
            case AstBinaryExpression.IOR:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": IOR");
                break;
            case AstBinaryExpression.AND:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": AND");
                break;
            case AstBinaryExpression.EQU:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": EQU");
                break;
            case AstBinaryExpression.NEQ:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": NEQ");
                break;
            case AstBinaryExpression.LEQ:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": LEQ");
                break;
            case AstBinaryExpression.GEQ:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": GEQ");
                break;
            case AstBinaryExpression.LTH:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": LTH");
                break;
            case AstBinaryExpression.GTH:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": GTH");
                break;
            case AstBinaryExpression.ADD:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": ADD");
                break;
            case AstBinaryExpression.SUB:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": SUB");
                break;
            case AstBinaryExpression.MUL:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": MUL");
                break;
            case AstBinaryExpression.DIV:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": DIV");
                break;
            case AstBinaryExpression.MOD:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": MOD");
                break;
            case AstBinaryExpression.ARR:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": ARR");
                break;
            case AstBinaryExpression.ASSIGN:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": ASSIGN");
                break;
            case AstBinaryExpression.DOT:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": DOT");
                break;
            case AstBinaryExpression.IS:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": IS");
                break;
            case AstBinaryExpression.AS:
                logger.dump(indent, "AstBinaryExpression " + binExpr.position.toString() + ": As");
                break;
            default:
                logger.error("Internal error :: compiler.ast.Abstr.visit(AstBinaryExpression)");
        }
        {
            Type typ = symbolDescription.getTypeForAstNode(binExpr);
            if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        indent += 2; binExpr.expr1.accept(this); indent -= 2;
        indent += 2; binExpr.expr2.accept(this); indent -= 2;
	}

	public void visit(AstDefinitions defs) {
        logger.dump(indent, "AstDefinitions " + defs.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(defs);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (AstDefinition def : defs.definitions) {
			indent += 2;
			def.accept(this);
			indent -= 2;
		}
	}

	public void visit(AstExpressions exprs) {
        logger.dump(indent, "AstExpressions " + exprs.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(exprs);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		for (AstExpression e : exprs.expressions)
			e.accept(this);
		indent -= 2;
	}

	public void visit(AstForStatement forStmt) {
        logger.dump(indent, "AbsFor " + forStmt.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(forStmt);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
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

	public void visit(AstFunctionCallExpression funCall) {
        logger.dump(indent, "AstFunctionCallExpression " + funCall.position.toString() + ": "
				+ funCall.name);
		{
			AstDefinition def = symbolDescription.getDefinitionForAstNode(funCall);
			if (def != null && def.position != null)
                logger.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(funCall);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (int arg = 0; arg < funCall.getArgumentCount(); arg++) {
			indent += 2;
			funCall.getArgumentAtIndex(arg).accept(this);
			indent -= 2;
		}
	}

	public void visit(AstFunctionDefinition funDef) {
        logger.dump(indent, "AstFunctionDefinition " + funDef.position.toString() + ": "
				+ funDef.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(funDef);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmFrame frame = frameDescription.getFrame(funDef);
			if (frame != null)
                logger.dump(indent + 2, "#framed as " + frame.toString());
		}
		for (AstParameterDefinition par : funDef.getParamaters()) {
			indent += 2;
			par.accept(this);
			indent -= 2;
		}
		indent += 2;
		funDef.returnType.accept(this);
		indent -= 2;
		indent += 2;
		funDef.functionCode.accept(this);
		indent -= 2;
	}

	public void visit(AstIfStatement ifExpr) {
        logger.dump(indent, "AbsIfExpr " + ifExpr.position.toString() + ":");
		Type typ = symbolDescription.getTypeForAstNode(ifExpr);
		if (typ != null)
            logger.dump(indent + 2, "#typed as " + typ.toString());
		
		indent += 2;
		for (Condition c : ifExpr.conditions) {
			c.condition.accept(this);
			c.body.accept(this);
		}
		if (ifExpr.elseBody != null)
			ifExpr.elseBody.accept(this);
		indent -= 2;
	}

	public void visit(AstParameterDefinition par) {
        logger.dump(indent, "AbsPar " + par.position.toString() + ": "
				+ par.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(par);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmAccess access = frameDescription.getAccess(par);
			if (access != null)
                logger.dump(indent + 2, "#accesed as " + access.toString());
		}
		indent += 2;
		par.type.accept(this);
		indent -= 2;
	}

	public void visit(AstTypeDefinition typeDef) {
//		Logger.dump(indent, "AstTypeDefinition " + typeDef.position.toString() + ": "
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

	public void visit(AstTypeName typeName) {
        logger.dump(indent, "AstTypeName " + typeName.position.toString()
				+ ": " + typeName.name);
		{
			AstDefinition def = symbolDescription.getDefinitionForAstNode(typeName);
			if (def != null)
                logger.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(typeName);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AstUnaryExpression unExpr) {
		switch (unExpr.oper) {
		case AstUnaryExpression.ADD:
            logger.dump(indent, "AstUnaryExpression " + unExpr.position.toString()
					+ ": ADD");
			break;
		case AstUnaryExpression.SUB:
            logger.dump(indent, "AstUnaryExpression " + unExpr.position.toString()
					+ ": SUB");
			break;
		case AstUnaryExpression.NOT:
            logger.dump(indent, "AstUnaryExpression " + unExpr.position.toString()
					+ ": NOT");
			break;
		case AstUnaryExpression.MEM:
            logger.dump(indent, "AstUnaryExpression " + unExpr.position.toString()
					+ ": MEM");
			break;
		case AstUnaryExpression.VAL:
            logger.dump(indent, "AstUnaryExpression " + unExpr.position.toString()
					+ ": VAL");
			break;
		default:
            logger.error("Internal error :: compiler.ast.Abstr.visit(AstBinaryExpression)");
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(unExpr);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		unExpr.expr.accept(this);
		indent -= 2;
	}

	public void visit(AstVariableDefinition varDef) {
        logger.dump(indent, "AstVariableDefinition " + varDef.position.toString() + ": "
				+ varDef.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(varDef);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		{
			FrmAccess access = frameDescription.getAccess(varDef);
			if (access != null)
                logger.dump(indent + 2, "#accesed as " + access.toString());
		}
		indent += 2;
		if (varDef.type != null) varDef.type.accept(this);
		indent -= 2;
	}

	public void visit(AstVariableNameExpression varName) {
        logger.dump(indent, "AbsVarName " + varName.position.toString() + ": "
				+ varName.name);
		{
			AstDefinition def = symbolDescription.getDefinitionForAstNode(varName);
			if (def != null)
                logger.dump(indent + 2,
						"#defined at " + def.position.toString());
		}
		{
			Type typ = symbolDescription.getTypeForAstNode(varName);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
	}

	public void visit(AstWhileStatement whileStmt) {
        logger.dump(indent, "AbsWhileName " + whileStmt.position.toString()
				+ ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(whileStmt);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		whileStmt.condition.accept(this);
		indent -= 2;
		indent += 2;
		whileStmt.body.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AstImportDefinition importDef) {
        logger.dump(indent, "AstImportDefinition " + importDef.position + ":");
        {
            Type typ = symbolDescription.getTypeForAstNode(importDef);
            if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        if (importDef.imports != null) {
            indent += 2;
            importDef.imports.accept(this);
            indent -= 2;
        }
	}

	@Override
	public void visit(AstStatements stmts) {
        logger.dump(indent, "AstStatements " + stmts.position.toString() + ":");
		{
			Type typ = symbolDescription.getTypeForAstNode(stmts);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		for (AstStatement s : stmts.statements) {
			indent += 2;
			s.accept(this);
			indent -= 2;
		}
	}

	@Override
	public void visit(AstReturnExpression returnExpr) {
        logger.dump(indent, "AstReturnExpression " + returnExpr.position.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(returnExpr);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2;
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AstListExpr absListExpr) {
        logger.dump(indent, "AstListExpr " + absListExpr.position.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(absListExpr);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent += 2; 
		for (AstExpression e : absListExpr.expressions)
			e.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AstFunctionType funType) {
        logger.dump(indent, "AstFunctionType " + funType.position.toString() + ":");
        logger.dump(indent + 2, funType.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(funType);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}		
	}

	@Override
	public void visit(AstControlTransferStatement acceptor) {
        logger.dump(indent, "AstControlTransferStatement: " + acceptor.control);
	}
	
	@Override
	public void visit(AstSwitchStatement switchStmt) {
        logger.dump(indent, "AstSwitchStatement " + switchStmt.position.toString() + ":");
		indent += 2;
		{
			Type typ = symbolDescription.getTypeForAstNode(switchStmt);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}

        logger.dump(indent, "Subject expr:");
		indent += 2;
		switchStmt.subjectExpr.accept(this);
		{
			Type typ = symbolDescription.getTypeForAstNode(switchStmt.subjectExpr);
			if (typ != null)
                logger.dump(indent, "#typed as " + typ.toString());
		}
		indent -= 2;
		
		for (AstCaseStatement singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null) {
            logger.dump(indent, "Default:");
			indent += 2;
			switchStmt.defaultBody.accept(this);
			{
				Type typ = symbolDescription.getTypeForAstNode(switchStmt.defaultBody);
				if (typ != null)
                    logger.dump(indent + 2, "#typed as " + typ.toString());
			}
			indent -= 2;
		}
		
		indent -= 2;		
	}

	@Override
	public void visit(AstCaseStatement acceptor) {
        logger.dump(indent, "Case:");
		indent += 2; 
		for (AstExpression e : acceptor.exprs) {
			e.accept(this);

			Type typ = symbolDescription.getTypeForAstNode(e);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent -= 2;
        logger.dump(indent, "Body:");
		indent += 2; acceptor.body.accept(this); indent -= 2;		
	}

	@Override
	public void visit(AstEnumDefinition enumDef) {
        logger.dump(indent, "AstEnumDefinition " + enumDef.position.toString() + ": " + enumDef.name);
		{
			Type typ = symbolDescription.getTypeForAstNode(enumDef);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}

		indent += 2;
		if (enumDef.type != null)
			enumDef.type.accept(this);		
		indent -= 2;
		
		indent += 2;
		for (AstDefinition def : enumDef.definitions)
			def.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AstEnumMemberDefinition acceptor) {
		acceptor.name.accept(this);
		{
			Type typ = symbolDescription.getTypeForAstNode(acceptor);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		if (acceptor.value != null)
			acceptor.value.accept(this);
	}

	@Override
	public void visit(AstTupleDefinition tupleDef) {
        logger.dump(indent, "AstTupleDefinition " + tupleDef.position.toString());
		{
			Type typ = symbolDescription.getTypeForAstNode(tupleDef);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}		
	}

	@Override
	public void visit(AstLabeledExpr labeledExpr) {
        logger.dump(indent, "AstLabeledExpr " + labeledExpr.position.toString());
		indent += 2;
        logger.dump(indent, "Label: " + labeledExpr.label);
		labeledExpr.expr.accept(this);
		{
			Type typ = symbolDescription.getTypeForAstNode(labeledExpr);
			if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
		}
		indent -= 2;
	}

	@Override
	public void visit(AstTupleExpression tupleExpr) {
        logger.dump(indent, "AstTupleExpression " + tupleExpr.position.toString());
		indent += 2;
		tupleExpr.expressions.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AstOptionalType acceptor) {
		acceptor.childType.accept(this);
	}

	@Override
	public void visit(AstOptionalEvaluationExpression acceptor) {
		acceptor.subExpr.accept(this);
	}

	@Override
	public void visit(AstForceValueExpression acceptor) {
		acceptor.subExpr.accept(this);
	}

    @Override
    public void visit(AstExtensionDefinition acceptor) {
        logger.dump(indent, "AstExtensionDefinition " + acceptor.position.toString() + ":");
        {
            Type typ = symbolDescription.getTypeForAstNode(acceptor);
            if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        indent += 2;
        acceptor.definitions.accept(this);
        indent -= 2;
    }

    @Override
    public void visit(AstInterfaceDefinition acceptor) {
        logger.dump(indent, "AstInterfaceDefinition " + acceptor.position.toString() + ":");
        {
            Type typ = symbolDescription.getTypeForAstNode(acceptor);
            if (typ != null)
                logger.dump(indent + 2, "#typed as " + typ.toString());
        }
        indent += 2;
        acceptor.definitions.accept(this);
        indent -= 2;
    }
}
