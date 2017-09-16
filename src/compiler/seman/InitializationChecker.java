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
//import compiler.LoggerInterface.Logger;
//import compiler.ast.ASTVisitor;
//import compiler.ast.tree.AstDefinitions;
//import compiler.ast.tree.AstExpressions;
//import compiler.ast.tree.AbsStmt;
//import compiler.ast.tree.AstStatements;
//import compiler.ast.tree.Condition;
//import compiler.ast.tree.def.*;
//import compiler.ast.tree.expr.AstAtomConstExpression;
//import compiler.ast.tree.expr.AstBinaryExpression;
//import compiler.ast.tree.expr.AstExpression;
//import compiler.ast.tree.expr.AstForceValueExpression;
//import compiler.ast.tree.expr.AstFunctionCallExpression;
//import compiler.ast.tree.expr.AstLabeledExpr;
//import compiler.ast.tree.expr.AstListExpr;
//import compiler.ast.tree.expr.AstOptionalEvaluationExpression;
//import compiler.ast.tree.expr.AstReturnExpression;
//import compiler.ast.tree.expr.AstTupleExpression;
//import compiler.ast.tree.expr.AstUnaryExpression;
//import compiler.ast.tree.expr.AstVariableNameExpression;
//import compiler.ast.tree.stmt.AbsCaseStmt;
//import compiler.ast.tree.stmt.AstControlTransferStatement;
//import compiler.ast.tree.stmt.AstForStatement;
//import compiler.ast.tree.stmt.AstIfStatement;
//import compiler.ast.tree.stmt.AstSwitchStatement;
//import compiler.ast.tree.stmt.AstWhileStatement;
//import compiler.ast.tree.memberType.AstAtomType;
//import compiler.ast.tree.memberType.AstFunctionType;
//import compiler.ast.tree.memberType.AstListType;
//import compiler.ast.tree.memberType.AstOptionalType;
//import compiler.ast.tree.memberType.AstTypeName;
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
//	public void visit(AstListType acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AstClassDefinition acceptor) {
//	    InitTable.initialize(acceptor);
//
//		acceptor.memberDefinitions.accept(this);
//
//		for (AstFunctionDefinition constructor : acceptor.construstors) {
//            constructor.accept(this);
//        }
//	}
//
//	@Override
//	public void visit(AstAtomConstExpression acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AstAtomType acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AstBinaryExpression acceptor) {
//		if (acceptor.oper == AstBinaryExpression.ASSIGN) {
//			if (acceptor.expr1 instanceof AstBinaryExpression) {
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
//		else if (acceptor.oper == AstBinaryExpression.DOT) {
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
//	public void visit(AstDefinitions acceptor) {
//        for (AstDefinition definition : acceptor.memberDefinitions) {
//            definition.accept(this);
//        }
//	}
//
//	@Override
//	public void visit(AstExpressions acceptor) {
//		for (AstExpression e : acceptor.expressions) {
//			e.accept(this);
//		}
//	}
//
//	@Override
//	public void visit(AstForStatement acceptor) {
//		InitTable.newScope();
//		InitTable.initialize((AstVariableDefinition) SymbolDescription.getDefinitionForAstNode(acceptor.iterator));
//		acceptor.body.accept(this);
//		InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AstFunctionCallExpression acceptor) {
//		for (AstExpression getArgumentAtIndex : acceptor.arguments) {
//		    getArgumentAtIndex.accept(this);
//        }
//	}
//
//	@Override
//	public void visit(AstFunctionDefinition acceptor) {
//	    InitTable.newScope();
//
//	    for (AstParameterDefinition parDef : acceptor.pars) {
//	        InitTable.initialize(parDef);
//        }
//
//        acceptor.functionCode.accept(this);
//        InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AstIfStatement acceptor) {
//		for (Condition c : acceptor.conditions) {
//			c.condition.accept(this);
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
//	public void visit(AstParameterDefinition acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AstTypeName acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AstUnaryExpression acceptor) {
//		acceptor.expr.accept(this);
//	}
//
//	@Override
//	public void visit(AstVariableDefinition acceptor) {
//        ///
//	}
//
//	@Override
//	public void visit(AstVariableNameExpression acceptor) {
//	    AstDefinition definition = SymbolDescription.getDefinitionForAstNode(acceptor);
//
//	    if (definition == null || definition instanceof AstParameterDefinition) {
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
//	public void visit(AstWhileStatement acceptor) {
//		acceptor.condition.accept(this);
//		InitTable.newScope();
//		acceptor.body.accept(this);
//        InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AstImportDefinition acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AstStatements acceptor) {
//		for (AbsStmt s : acceptor.statements) {
//			s.accept(this);
//		}
//	}
//
//	@Override
//	public void visit(AstReturnExpression acceptor) {
//		acceptor.expr.accept(this);
//	}
//
//	@Override
//	public void visit(AstListExpr acceptor) {
//		for (AstExpression e : acceptor.expressions)
//			e.accept(this);
//	}
//
//	@Override
//	public void visit(AstFunctionType acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AstControlTransferStatement acceptor) {
//		///
//	}
//
//	@Override
//	public void visit(AstSwitchStatement switchStmt) {
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
//		for (AstExpression e : acceptor.exprs)
//			e.accept(this);
//		InitTable.newScope();
//		acceptor.body.accept(this);
//		InitTable.oldScope();
//	}
//
//	@Override
//	public void visit(AstEnumDefinition acceptor) {
//	    InitTable.initialize(acceptor);
//
//		for (AstDefinition def : acceptor.memberDefinitions)
//			def.accept(this);
//	}
//
//	@Override
//	public void visit(AstEnumMemberDefinition acceptor) {
//        InitTable.initialize(acceptor);
//	}
//
//	@Override
//	public void visit(AstTupleDefinition acceptor) {
//	    ///
//	}
//
//	@Override
//	public void visit(AstLabeledExpr acceptor) {
//		acceptor.expr.accept(this);
//	}
//
//	@Override
//	public void visit(AstTupleExpression acceptor) {
//		acceptor.expressions.accept(this);
//	}
//
//	@Override
//	public void visit(AstOptionalType acceptor) {
//		acceptor.childType.accept(this);
//	}
//
//	@Override
//	public void visit(AstOptionalEvaluationExpression acceptor) {
//		acceptor.subExpr.accept(this);
//	}
//
//	@Override
//	public void visit(AstForceValueExpression acceptor) {
//		acceptor.subExpr.accept(this);
//	}
//
//    @Override
//    public void visit(AstExtensionDefinition acceptor) {
//        ///
//    }
//
//    @Override
//    public void visit(AstInterfaceDefinition absInterfaceDef) {
//        ///
//    }
//}
