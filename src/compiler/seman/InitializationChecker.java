///**
// * Copyright 2016 Toni Kocjan
// *
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// *
// *     http://www.apache.org/licenses/LICENSE-2.0
// *
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// *
// */
//
//package compiler.seman;
//
//
//import compiler.Logger;
//import compiler.ast.ASTVisitor;
//import compiler.ast.tree.AbsDefs;
//import compiler.ast.tree.AbsExprs;
//import compiler.ast.tree.AbsStmt;
//import compiler.ast.tree.AbsStmts;
//import compiler.ast.tree.Condition;
//import compiler.ast.tree.def.*;
//import compiler.ast.tree.expr.AbsAtomConstExpr;
//import compiler.ast.tree.expr.AbsBinExpr;
//import compiler.ast.tree.expr.AbsExpr;
//import compiler.ast.tree.expr.AbsForceValueExpr;
//import compiler.ast.tree.expr.AbsFunCall;
//import compiler.ast.tree.expr.AbsLabeledExpr;
//import compiler.ast.tree.expr.AbsListExpr;
//import compiler.ast.tree.expr.AbsOptionalEvaluationExpr;
//import compiler.ast.tree.expr.AbsReturnExpr;
//import compiler.ast.tree.expr.AbsTupleExpr;
//import compiler.ast.tree.expr.AbsUnExpr;
//import compiler.ast.tree.expr.AbsVarNameExpr;
//import compiler.ast.tree.stmt.AbsCaseStmt;
//import compiler.ast.tree.stmt.AbsControlTransferStmt;
//import compiler.ast.tree.stmt.AbsForStmt;
//import compiler.ast.tree.stmt.AbsIfStmt;
//import compiler.ast.tree.stmt.AbsSwitchStmt;
//import compiler.ast.tree.stmt.AbsWhileStmt;
//import compiler.ast.tree.memberType.AbsAtomType;
//import compiler.ast.tree.memberType.AbsFunType;
//import compiler.ast.tree.memberType.AbsListType;
//import compiler.ast.tree.memberType.AbsOptionalType;
//import compiler.ast.tree.memberType.AbsTypeName;
//
///**
// * Initialization checking phase of the compiler.
// * @author toni kocjan
// *
// */
//public class InitializationChecker implements ASTVisitor {
//
//	private boolean shouldCheckIfInitialized = true;
//
//	///
//
//	@Override
//	public void visit(AbsListType acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AbsClassDef acceptor) {
//	    InitTable.initialize(acceptor);
//
//		acceptor.definitions.accept(this);
//
//		for (AbsFunDef constructor : acceptor.construstors) {
//            constructor.accept(this);
//        }
//	}
//
//	@Override
//	public void visit(AbsAtomConstExpr acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AbsAtomType acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AbsBinExpr acceptor) {
//		if (acceptor.oper == AbsBinExpr.ASSIGN) {
//			if (acceptor.expr1 instanceof AbsBinExpr) {
//				shouldCheckIfInitialized = false;
//				acceptor.expr1.accept(this);
//				shouldCheckIfInitialized = true;
//			}
//			else {
//				acceptor.expr2.accept(this);
//				shouldCheckIfInitialized = false;
//				acceptor.expr1.accept(this);
//				shouldCheckIfInitialized = true;
//			}
//		}
//		else if (acceptor.oper == AbsBinExpr.DOT) {
//			if (!shouldCheckIfInitialized) {
//				shouldCheckIfInitialized = true;
//				acceptor.expr1.accept(this);
//				shouldCheckIfInitialized = false;
//				acceptor.expr2.accept(this);
//			}
//			else {
//				acceptor.expr1.accept(this);
//				acceptor.expr2.accept(this);
//			}
//		}
//		else {
//			acceptor.expr1.accept(this);
//			acceptor.expr2.accept(this);
//		}
//	}
//
//	@Override
//	public void visit(AbsDefs acceptor) {
//        for (AbsDef definition : acceptor.definitions) {
//            definition.accept(this);
//        }
//	}
//
//	@Override
//	public void visit(AbsExprs acceptor) {
//		for (AbsExpr e : acceptor.expressions) {
//			e.accept(this);
//		}
//	}
//
//	@Override
//	public void visit(AbsForStmt acceptor) {
//		InitTable.newScope();
//		InitTable.initialize((AbsVarDef) SymbolDescription.getDefinitionForAstNode(acceptor.iterator));
//		acceptor.body.accept(this);
//		InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AbsFunCall acceptor) {
//		for (AbsExpr arg : acceptor.args) {
//		    arg.accept(this);
//        }
//	}
//
//	@Override
//	public void visit(AbsFunDef acceptor) {
//	    InitTable.newScope();
//
//	    for (AbsParDef parDef : acceptor.pars) {
//	        InitTable.initialize(parDef);
//        }
//
//        acceptor.func.accept(this);
//        InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AbsIfStmt acceptor) {
//		for (Condition c : acceptor.conditions) {
//			c.cond.accept(this);
//
//			InitTable.newScope();
//			c.body.accept(this);
//			InitTable.oldScope();
//		}
//
//		if (acceptor.elseBody != null) {
//			acceptor.elseBody.accept(this);
//		}
//	}
//
//	@Override
//	public void visit(AbsParDef acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AbsTypeName acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AbsUnExpr acceptor) {
//		acceptor.expr.accept(this);
//	}
//
//	@Override
//	public void visit(AbsVarDef acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AbsVarNameExpr acceptor) {
//	    AbsDef definition = SymbolDescription.getDefinitionForAstNode(acceptor);
//
//	    if (definition == null || definition instanceof AbsParDef) {
//	        // TODO: - Parameters should't be handled separately
//	        return;
//        }
//
//		if (shouldCheckIfInitialized) {
//            if (InitTable.isInitialized(definition)) {
//                return;
//            }
//
//            String errorMsg = definition.isMutable ? "Variable '" : "Constant '";
//            Logger.error(acceptor.position, errorMsg + definition.getName + "' used before being initialized");
//        }
//		else {
//			if (InitTable.isInitialized(definition) && !definition.isMutable) {
//                Logger.error(acceptor.position, "Cannot assign value to a constant '" + definition.getName + "'");
//            }
//
//			InitTable.initialize(definition);
//		}
//	}
//
//	@Override
//	public void visit(AbsWhileStmt acceptor) {
//		acceptor.cond.accept(this);
//		InitTable.newScope();
//		acceptor.body.accept(this);
//        InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AbsImportDef acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AbsStmts acceptor) {
//		for (AbsStmt s : acceptor.statements) {
//			s.accept(this);
//		}
//	}
//
//	@Override
//	public void visit(AbsReturnExpr acceptor) {
//		acceptor.expr.accept(this);
//	}
//
//	@Override
//	public void visit(AbsListExpr acceptor) {
//		for (AbsExpr e : acceptor.expressions)
//			e.accept(this);
//	}
//
//	@Override
//	public void visit(AbsFunType acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AbsControlTransferStmt acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AbsSwitchStmt switchStmt) {
//		switchStmt.subjectExpr.accept(this);
//
//		for (AbsCaseStmt singleCase : switchStmt.cases)
//			singleCase.accept(this);
//
//		if (switchStmt.defaultBody != null) {
//			InitTable.newScope();
//			switchStmt.defaultBody.accept(this);
//			InitTable.oldScope();
//		}
//	}
//
//	@Override
//	public void visit(AbsCaseStmt acceptor) {
//		for (AbsExpr e : acceptor.exprs)
//			e.accept(this);
//		InitTable.newScope();
//		acceptor.body.accept(this);
//		InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AbsEnumDef acceptor) {
//	    InitTable.initialize(acceptor);
//
//		for (AbsDef def : acceptor.definitions)
//			def.accept(this);
//	}
//
//	@Override
//	public void visit(AbsEnumMemberDef acceptor) {
//        InitTable.initialize(acceptor);
//	}
//
//	@Override
//	public void visit(AbsTupleDef acceptor) {
//	    ///
//	}
//
//	@Override
//	public void visit(AbsLabeledExpr acceptor) {
//		acceptor.expr.accept(this);
//	}
//
//	@Override
//	public void visit(AbsTupleExpr acceptor) {
//		acceptor.expressions.accept(this);
//	}
//
//	@Override
//	public void visit(AbsOptionalType acceptor) {
//		acceptor.childType.accept(this);
//	}
//
//	@Override
//	public void visit(AbsOptionalEvaluationExpr acceptor) {
//		acceptor.subExpr.accept(this);
//	}
//
//	@Override
//	public void visit(AbsForceValueExpr acceptor) {
//		acceptor.subExpr.accept(this);
//	}
//
//    @Override
//    public void visit(AbsExtensionDef acceptor) {
//        ///
//    }
//
//    @Override
//    public void visit(AbsInterfaceDef absInterfaceDef) {
//        ///
//    }
//}
