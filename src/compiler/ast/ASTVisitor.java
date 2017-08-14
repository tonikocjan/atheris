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
import compiler.ast.tree.expr.*;
import compiler.ast.tree.expr.AstAtomConstExpression;
import compiler.ast.tree.stmt.*;
import compiler.ast.tree.stmt.AstCaseStatement;
import compiler.ast.tree.type.*;
import compiler.ast.tree.type.AstFunctionType;

public interface ASTVisitor {
    void visit(AstListType acceptor);
    void visit(AstClassDefinition acceptor);
    void visit(AstAtomConstExpression acceptor);
    void visit(AstAtomType acceptor);
    void visit(AstBinaryExpression acceptor);
    void visit(AstDefinitions acceptor);
    void visit(AstExpressions acceptor);
    void visit(AstForStatement acceptor);
    void visit(AstFunctionCallExpression acceptor);
    void visit(AstFunctionDefinition acceptor);
    void visit(AstIfStatement accpetor);
    void visit(AstParameterDefinition acceptor);
    void visit(AstTypeName acceptor);
    void visit(AstUnaryExpression acceptor);
    void visit(AstVariableDefinition acceptor);
    void visit(AstVariableNameExpression acceptor);
    void visit(AstWhileStatement acceptor);
    void visit(AstImportDefinition acceptor);
    void visit(AstStatements acceptor);
    void visit(AstReturnExpression acceptor);
    void visit(AstListExpr acceptor);
    void visit(AstFunctionType acceptor);
    void visit(AstControlTransferStatement acceptor);
    void visit(AstSwitchStatement acceptor);
    void visit(AstCaseStatement acceptor);
    void visit(AstEnumDefinition acceptor);
    void visit(AstEnumMemberDefinition acceptor);
    void visit(AstTupleDefinition acceptor);
    void visit(AstLabeledExpr acceptor);
    void visit(AstTupleExpression acceptor);
    void visit(AstOptionalType acceptor);
    void visit(AstOptionalEvaluationExpression acceptor);
    void visit(AstForceValueExpression acceptor);
    void visit(AstExtensionDefinition acceptor);
    void visit(AstInterfaceDefinition absInterfaceDef);
}
