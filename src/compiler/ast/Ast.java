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

package compiler.ast;

import compiler.*;
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

/**
 * @author toni kocjan
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
		if (Logger.dumpFile() == null)
			return;
		indent = 0;
		tree.accept(this);
	}

	// Kot Visitor izpise abstraktno sintaksno drevo:

	/** Trenutni zamik. */
	private int indent;

	public void visit(AbsListType arrType) {
		Logger.dump(indent, "AbsListType " + arrType.position.toString() + ":");
		Logger.dump(indent + 2, "[" + arrType.count + "]");
		indent += 2;
		arrType.type.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsClassDef classDef) {
		Logger.dump(indent, classDef.toString());
		indent += 2;
		Logger.dump(indent, "Member definitions:");
		indent += 2; classDef.definitions.accept(this); indent -= 2;
		Logger.dump(indent, "Default constructor:");
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
		case CHR:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": CHAR(" + atomConst.value + ")");
			break;
		case DOB:
			Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
					+ ": DOUBLE(" + atomConst.value + ")");
			break;
            case NIL:
            Logger.dump(indent, "AbsAtomConst " + atomConst.position.toString()
                    + ": NIL(" + atomConst.value + ")");
            break;
		default:
			Logger.error("Internal error :: compiler.ast.Abstr.visit(AbsAtomConst)");
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
		default:
			Logger.error("Internal error :: compiler.ast.Abstr.visit(AbsAtomType)");
		}
	}

	public void visit(AbsBinExpr binExpr) {
		switch (binExpr.oper) {
		case AbsBinExpr.IOR:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": IOR");
			break;
		case AbsBinExpr.AND:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": AND");
			break;
		case AbsBinExpr.EQU:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": EQU");
			break;
		case AbsBinExpr.NEQ:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": NEQ");
			break;
		case AbsBinExpr.LEQ:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": LEQ");
			break;
		case AbsBinExpr.GEQ:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": GEQ");
			break;
		case AbsBinExpr.LTH:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": LTH");
			break;
		case AbsBinExpr.GTH:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": GTH");
			break;
		case AbsBinExpr.ADD:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": ADD");
			break;
		case AbsBinExpr.SUB:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": SUB");
			break;
		case AbsBinExpr.MUL:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": MUL");
			break;
		case AbsBinExpr.DIV:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": DIV");
			break;
		case AbsBinExpr.MOD:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": MOD");
			break;
		case AbsBinExpr.ARR:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": ARR");
			break;
		case AbsBinExpr.ASSIGN:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": ASSIGN");
			break;
		case AbsBinExpr.DOT:
			Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
					+ ": DOT");
			break;
        case AbsBinExpr.IS:
            Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
                    + ": IS");
        case AbsBinExpr.AS:
            Logger.dump(indent, "AbsBinExpr " + binExpr.position.toString()
                    + ": AS");
            break;
		default:
			Logger.error("Internal error :: compiler.ast.Abstr.visit(AbsBinExpr)");
		}
		indent += 2;
		binExpr.expr1.accept(this);
		indent -= 2;
		indent += 2;
		binExpr.expr2.accept(this);
		indent -= 2;
	}

	public void visit(AbsDefs defs) {
		Logger.dump(indent, "AbsDefs " + defs.position.toString() + ":");
		for (AbsDef def : defs.definitions) {
			indent += 2;
			def.accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsExprs exprs) {
		Logger.dump(indent, "AbsExprs " + exprs.position.toString() + ":");
		for (AbsExpr e : exprs.expressions) {
			indent += 2;
			e.accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsForStmt forStmt) {
		Logger.dump(indent, "AbsFor " + forStmt.position.toString() + ":");
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
		for (int arg = 0; arg < funCall.numArgs(); arg++) {
			indent += 2;
			funCall.arg(arg).accept(this);
			indent -= 2;
		}
	}

	public void visit(AbsFunDef funDef) {
		Logger.dump(indent, "AbsFunDef " + funDef.position.toString() + ": "
				+ funDef.name);
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
		indent += 2;
		for (Condition c : ifExpr.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
		}
		ifExpr.elseBody.accept(this);
		indent -= 2;
	}

	public void visit(AbsParDef par) {
		Logger.dump(indent, "AbsPar " + par.position.toString() + ": "
				+ par.name);
		indent += 2;
		par.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsTypeName typeName) {
		Logger.dump(indent, "AbsTypeName " + typeName.position.toString()
				+ ": " + typeName.name);
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
			Logger.error("Internal error :: compiler.ast.Abstr.visit(AbsUnExpr)");
		}
		indent += 2;
		unExpr.expr.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarDef varDef) {
		Logger.dump(indent, "AbsVarDef " + varDef.position.toString() + ": "
				+ varDef.name);
		indent += 2;
		if (!varDef.isMutable)
			Logger.dump(indent, "#CONSTANT");
		if (varDef.type == null)
			Logger.dump(indent, "?");
		else
			varDef.type.accept(this);
		indent -= 2;
	}

	public void visit(AbsVarNameExpr varName) {
		Logger.dump(indent, "AbsVarName " + varName.position.toString() + ": "
				+ varName.name);
	}

	public void visit(AbsWhileStmt where) {
		Logger.dump(indent, "AbsWhileName " + where.position.toString() + ":");
		indent += 2;
		where.cond.accept(this);
		indent -= 2;
		indent += 2;
		where.body.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsImportDef imp) {
		Logger.dump(indent, "AbsImportDef " + imp.position.toString() + ":");
		Logger.dump(indent + 2, "Filename: " + imp.getName());
	}

	@Override
	public void visit(AbsStmts stmts) {
		Logger.dump(indent, "AbsStmts " + stmts.position.toString() + ":");
		for (AbsStmt s : stmts.statements) {
			indent += 2;
			s.accept(this);
			indent -= 2;
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		Logger.dump(indent, "AbsReturnExpr " + returnExpr.position.toString());
		indent += 2;
		if (returnExpr.expr != null) 
			returnExpr.expr.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Logger.dump(indent, "AbsListExpr");
		indent += 2;
		for (AbsExpr e: absListExpr.expressions)
			e.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsFunType funType) {
		Logger.dump(indent, "AbsFunType " + funType.position.toString() + ":");
		Logger.dump(indent + 2, funType.toString());
	}

	@Override
	public void visit(AbsControlTransferStmt controlTransfer) {
		Logger.dump(indent, "AbsControlTransferStmt: " + controlTransfer.control);
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		Logger.dump(indent, "AbsSwitchStmt " + switchStmt.position.toString() + ":");
		indent += 2;
		
		Logger.dump(indent, "Subject expr:");
		indent += 2;
		switchStmt.subjectExpr.accept(this);
		indent -= 2;
		
		for (AbsCaseStmt singleCase : switchStmt.cases)
			singleCase.accept(this);
		
		if (switchStmt.defaultBody != null) {
			Logger.dump(indent, "Default:");
			indent += 2;
			switchStmt.defaultBody.accept(this);
			indent -= 2;
		}
		
		indent -= 2;
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		Logger.dump(indent, "Case:");
		indent += 2; 
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		indent -= 2;
		Logger.dump(indent, "Body:");
		indent += 2; acceptor.body.accept(this); indent -= 2;
	}

	@Override
	public void visit(AbsEnumDef enumDef) {
		Logger.dump(indent, "AbsEnumDef " + enumDef.position.toString() + ":");
		if (enumDef.type != null) {
			indent += 2;
			enumDef.type.accept(this);
			indent -= 2;
		}
		indent += 2;
		for (AbsDef def : enumDef.definitions)
			def.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsEnumMemberDef def) {
		Logger.dump(indent, "Name:");
		indent += 2;
		def.name.accept(this);
		indent -= 2;
		if (def.value != null) {
			Logger.dump(indent, "Raw value:");
			indent += 2;
			def.value.accept(this);
			indent -= 2;
		}
	}

	@Override
	public void visit(AbsTupleDef tupleDef) {
		Logger.dump(indent, "AbsTupleDef " + tupleDef.position.toString() + ":");
		indent += 2;
		tupleDef.definitions.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsLabeledExpr labeledExpr) {
		Logger.dump(indent, "AbsLabeledExpr " + labeledExpr.position.toString());
		indent += 2;
		Logger.dump(indent, "Label: " + labeledExpr.name);
		labeledExpr.expr.accept(this);
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
	public void visit(AbsOptionalType optionalType) {
		Logger.dump(indent, "AbsOptionalType " + optionalType.position.toString() + ":");
		indent += 2;
		optionalType.childType.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr optionalExpr) {
		Logger.dump(indent, "AbsOptionalEvaluationExpr " + optionalExpr.position.toString() + ":");
		indent += 2;
		optionalExpr.subExpr.accept(this);
		indent -= 2;
	}

	@Override
	public void visit(AbsForceValueExpr forceValueExpr) {
		Logger.dump(indent, "AbsForceValueExpr " + forceValueExpr.position.toString() + ":");
		indent += 2;
		forceValueExpr.subExpr.accept(this);
		indent -= 2;
	}

    @Override
    public void visit(AbsExtensionDef acceptor) {
        Logger.dump(indent, "AbsExtensionDef " + acceptor.position.toString() + ":");
        indent += 2;
        acceptor.extendingType.accept(this);
        acceptor.definitions.accept(this);
        indent -= 2;
    }

    @Override
    public void visit(AbsInterfaceDef acceptor) {
        Logger.dump(indent, "AbsInterfaceDef " + acceptor.position.toString() + ":");
        indent += 2;
        acceptor.definitions.accept(this);
        indent -= 2;
    }
}
