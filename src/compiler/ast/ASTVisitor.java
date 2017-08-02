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

import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AbsAtomConstExpr;
import compiler.ast.tree.expr.AbsBinExpr;
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
 * @author toni
 */
public interface ASTVisitor {
	public void visit(AbsListType acceptor);
	public void visit(AbsClassDef acceptor);
	public void visit(AbsAtomConstExpr acceptor);
	public void visit(AbsAtomType acceptor);
	public void visit(AbsBinExpr acceptor);
	public void visit(AbsDefs acceptor);
    public void visit(AbsExprs acceptor);
    public void visit(AbsForStmt acceptor);
	public void visit(AbsFunCall acceptor);
	public void visit(AbsFunDef acceptor);
	public void visit(AbsIfStmt accpetor);
	public void visit(AbsParDef acceptor);
	public void visit(AbsTypeName acceptor);
	public void visit(AbsUnExpr acceptor);
	public void visit(AbsVarDef acceptor);
	public void visit(AbsVarNameExpr acceptor);
	public void visit(AbsWhileStmt acceptor);
	public void visit(AbsImportDef acceptor);
	public void visit(AbsStmts acceptor);
	public void visit(AbsReturnExpr acceptor);
	public void visit(AbsListExpr acceptor);
	public void visit(AbsFunType acceptor);
	public void visit(AbsControlTransferStmt acceptor);
	public void visit(AbsSwitchStmt acceptor);
	public void visit(AbsCaseStmt acceptor);
	public void visit(AbsEnumDef acceptor);
	public void visit(AbsEnumMemberDef acceptor);
	public void visit(AbsTupleDef acceptor);
	public void visit(AbsLabeledExpr acceptor);
	public void visit(AbsTupleExpr acceptor);
	public void visit(AbsOptionalType acceptor);
	public void visit(AbsOptionalEvaluationExpr acceptor);
	public void visit(AbsForceValueExpr acceptor);
    public void visit(AbsExtensionDef acceptor);
    public void visit(AbsInterfaceDef absInterfaceDef);
}
