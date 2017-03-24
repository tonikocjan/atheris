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


import com.sun.org.apache.xml.internal.security.Init;
import compiler.Report;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.Condition;
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
import compiler.seman.type.FunctionType;

/**
 * Initialization checking phase of the compiler.
 * @author toni kocjan
 *
 */
public class InitializationChecker implements ASTVisitor {
	
	private boolean shouldCheckIfInitialized = true;
	
	///

	@Override
	public void visit(AbsListType acceptor) {
		///
	}

	@Override
	public void visit(AbsClassDef acceptor) {
		acceptor.definitions.accept(this);

		for (AbsFunDef constructor : acceptor.contrustors) {
            constructor.accept(this);
        }
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
		///
	}

	@Override
	public void visit(AbsAtomType acceptor) {
        ///
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		if (acceptor.oper == AbsBinExpr.ASSIGN) {
			if (acceptor.expr1 instanceof AbsBinExpr) {
				shouldCheckIfInitialized = false;
				acceptor.expr1.accept(this);
				shouldCheckIfInitialized = true;
			}
			else {
				acceptor.expr2.accept(this);
				shouldCheckIfInitialized = false;
				acceptor.expr1.accept(this);
				shouldCheckIfInitialized = true;
			}
		}
		else if (acceptor.oper == AbsBinExpr.DOT) {
			if (!shouldCheckIfInitialized) {
				shouldCheckIfInitialized = true;
				acceptor.expr1.accept(this);
				shouldCheckIfInitialized = false;
				acceptor.expr2.accept(this);
			}
			else {
				acceptor.expr1.accept(this);
				acceptor.expr2.accept(this);
			}
		}
		else {
			acceptor.expr1.accept(this);
			acceptor.expr2.accept(this);
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
        for (AbsDef definition : acceptor.definitions) {
            definition.accept(this);
        }
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (AbsExpr e : acceptor.expressions) {
			e.accept(this);
		}
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		InitTable.newScope();
		InitTable.initialize((AbsVarDef) SymbDesc.getNameDef(acceptor.iterator));
		acceptor.body.accept(this);
		InitTable.oldScope();
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		///
	}

	@Override
	public void visit(AbsFunDef acceptor) {
	    InitTable.newScope();

	    for (AbsParDef parDef : acceptor.pars) {
	        InitTable.initialize(parDef);
        }

        acceptor.func.accept(this);
        InitTable.oldScope();
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);

			InitTable.newScope();
			c.body.accept(this);
			InitTable.oldScope();
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);
		}
	}

	@Override
	public void visit(AbsParDef acceptor) {
        ///
	}

	@Override
	public void visit(AbsTypeName acceptor) {
        ///
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
        ///
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
	    AbsDef definition = SymbDesc.getNameDef(acceptor);

		if (shouldCheckIfInitialized) {
			if (!InitTable.isInitialized(definition)) {
				String errorMsg = definition.isMutable ? "Variable '" : "Constant '";
				Report.error(acceptor.position, errorMsg + definition.name + "' used before being initialized");
			}
		}
		else {
			if (InitTable.isInitialized(definition) && !definition.isMutable)
				Report.error(acceptor.position, "Cannot assign value to a constant '" + definition.name + "'");
			InitTable.initialize(definition);
		}
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		///
	}

	@Override
	public void visit(AbsStmts acceptor) {
		for (AbsStmt s : acceptor.statements) {
			s.accept(this);
		}
	}

	@Override
	public void visit(AbsReturnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsListExpr acceptor) {
		for (AbsExpr e : acceptor.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AbsFunType acceptor) {
		///
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
		
		if (switchStmt.defaultBody != null) {
			InitTable.newScope();
			switchStmt.defaultBody.accept(this);
			InitTable.oldScope();
		}
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		InitTable.newScope();
		acceptor.body.accept(this);
		InitTable.oldScope();
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
//		EnumType type = (EnumType) SymbDesc.getType(acceptor);
//		
//		if (type.definition.statements.numStmts() > 0)
//			InitTable.initialize((AbsVarDef) type.definition.statements.stmt(0));
	}

	@Override
	public void visit(AbsTupleDef acceptor) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(AbsLabeledExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsTupleExpr acceptor) {
		acceptor.expressions.accept(this);
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
}
