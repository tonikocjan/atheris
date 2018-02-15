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

package compiler.imcode;

import java.util.*;

import compiler.ast.tree.enums.AtomTypeKind;
import compiler.ast.tree.enums.ControlTransferKind;
import compiler.ast.tree.expr.*;
import compiler.ast.tree.stmt.*;
import compiler.ast.tree.type.*;
import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import compiler.seman.SymbolDescriptionMap;
import utils.Constants;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AstAtomConstExpression;
import compiler.ast.tree.stmt.AstCaseStatement;
import compiler.ast.tree.stmt.AstSwitchStatement;
import compiler.ast.tree.type.AstAtomType;
import compiler.frames.*;
import compiler.seman.type.*;

public class ImcCodeGen implements ASTVisitor {
    private static LoggerInterface logger = LoggerFactory.logger();

    private int virtualTableCount = 0;
    private int currentFunctionScope = 0;
    public ImcCodeChunk entryPointCode = null;
    public List<ImcChunk> chunks;
    private Stack<FrmFrame> returnExprFrameStack = new Stack<>();
    private Stack<FrmLabel> controlTransferStartLabelStack = new Stack<>();
    private Stack<FrmLabel> controlTransferEndLabelStack = new Stack<>();
    private Stack<ImcExpr> switchSubjectExprs = new Stack<>();
    
    private SymbolDescriptionMap symbolDescription;
    private FrameDescriptionMap frameDescription;
    private ImcDescriptionMap imcDescription;

	public ImcCodeGen(FrmFrame entryPoint, SymbolDescriptionMap symbolDescription, FrameDescriptionMap frameDescription, ImcDescriptionMap imcDescription) {
		returnExprFrameStack.add(entryPoint);
		chunks = new ArrayList<>();
        this.symbolDescription = symbolDescription;
        this.frameDescription = frameDescription;
        this.imcDescription = imcDescription;
	}

	@Override
	public void visit(AstListType acceptor) {
        ///
	}

	@Override
	public void visit(AstClassDefinition acceptor) {
        CanType type = (CanType) symbolDescription.getTypeForAstNode(acceptor);
        FrmVirtualTableAccess virtualTableAccess = (FrmVirtualTableAccess) frameDescription.getAccess(acceptor);

        Integer virtualTablePointer = 0;

        if (virtualTableAccess != null) { // structures don't have virtual tables
            // create static instance (singleton)
            ImcDataChunk staticInstance = new ImcDataChunk(virtualTableAccess.label, type.staticSize());
            chunks.add(staticInstance);

            if (type.childType.isClassType()) {
                virtualTablePointer = virtualTableAccess.location;

                chunks.add(virtualTableCount++,
                        new ImcVirtualTableDataChunk(
                            FrmLabel.newNamedLabel(type.friendlyName()),
                            virtualTableAccess.size,
                            (ClassType) type.childType));
            }
        }

		acceptor.memberDefinitions.accept(this);

		ObjectType objectType = (ObjectType) type.childType;
		int size = objectType.sizeInBytes();

        for (AstFunctionDefinition constructor : acceptor.construstors) {
            FrmFrame constructorFrame = frameDescription.getFrame(constructor);

            returnExprFrameStack.push(constructorFrame);
            constructor.functionCode.accept(this);

            ImcSEQ constructorCode = (ImcSEQ) imcDescription.getImcCode(constructor.functionCode);

            // allocate memory for new object
            ImcTEMP framePointer = new ImcTEMP(constructorFrame.FP);
            ImcCONST offset = new ImcCONST(4);
            ImcMEM location = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, framePointer, offset));

            // assign new object as "self" parameter
            constructorCode.stmts.add(0, new ImcMOVE(location, new ImcMALLOC(size)));

            // assign pointer to vtable
            constructorCode.stmts.add(1, new ImcMOVE(new ImcMEM(location), new ImcCONST(virtualTablePointer)));

            // return new object
            constructorCode.stmts.add(new ImcMOVE(new ImcTEMP(constructorFrame.RV), location));

            imcDescription.setImcCode(constructor, constructorCode);
            chunks.add(new ImcCodeChunk(constructorFrame, constructorCode));

            constructorCode.stmts.add(new ImcLABEL(returnExprFrameStack.peek().endLabel));
            returnExprFrameStack.pop();
        }
	}

	@Override
	public void visit(AstAtomConstExpression acceptor) {
		if (acceptor.type == AtomTypeKind.INT) {
            imcDescription.setImcCode(acceptor, new ImcCONST(Integer.parseInt(acceptor.value)));
        }
		else if (acceptor.type == AtomTypeKind.LOG) {
            imcDescription.setImcCode(acceptor, new ImcCONST(acceptor.value.equals("true") ? 1 : 0));
        }
		else if (acceptor.type == AtomTypeKind.STR) {
			FrmLabel l = FrmLabel.newAnonymousLabel();
			ImcDataChunk str = new ImcDataChunk(l, 4);
			str.data = acceptor.value;
			chunks.add(str);
			imcDescription.setImcCode(acceptor, new ImcMEM(new ImcNAME(l)));
		}
		else if (acceptor.type == AtomTypeKind.CHR) {
			imcDescription.setImcCode(acceptor, new ImcCONST(acceptor.value.charAt(0)));
		}
		else if (acceptor.type == AtomTypeKind.DOB) {
			imcDescription.setImcCode(acceptor, new ImcCONST(Double.parseDouble(acceptor.value)));
		}
		else if (acceptor.type == AtomTypeKind.NIL) {
			imcDescription.setImcCode(acceptor, new ImcCONST(0));
		}
	}

	@Override
	public void visit(AstAtomType acceptor) {
		///
	}

	@Override
	public void visit(AstBinaryExpression acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);

		ImcCode c1 = imcDescription.getImcCode(acceptor.expr1);
		ImcCode c2 = imcDescription.getImcCode(acceptor.expr2);

		ImcExpr e1;
		ImcExpr e2;

		if (c1 instanceof ImcEXP) e1 = ((ImcEXP) c1).expr;
		else e1 = (ImcExpr) c1;

		if (c2 instanceof ImcESEQ) e2 = (ImcESEQ) c2;
		else if (c2 instanceof ImcEXP) e2 = ((ImcEXP) c2).expr;
		else e2 = (ImcExpr) c2;

		ImcCode code = null;

		if (acceptor.oper >= 0 && acceptor.oper <= 11) {
            code = new ImcBINOP(acceptor.oper, e1, e2);
        }
		else if (acceptor.oper == AstBinaryExpression.ASSIGN) {
		    Type expressionType = symbolDescription.getTypeForAstNode(acceptor.expr1);

		    if (expressionType.isStructType()) {
		        if (acceptor.expr2 instanceof AstFunctionCallExpression && ((AstFunctionDefinition) symbolDescription.getDefinitionForAstNode(acceptor.expr2)).isConstructor) {
                    code = new ImcMOVE(e1, e2);
                }
		        else {
                    // copy content of one struct into another
                    StructType structType = (StructType) expressionType;
                    ImcSEQ copyCode = new ImcSEQ();

                    e1 = ((ImcMEM) e1).expr;
                    if (e2 instanceof ImcMEM)
                        e2 = ((ImcMEM) e2).expr;
                    else {
                        ImcTEMP temp = new ImcTEMP(new FrmTemp());
                        copyCode.stmts.add(new ImcMOVE(temp, e2));
                        e2 = temp;
                    }

                    Iterator<Type> types = structType.getTypes();

                    int offset = 0;
                    while (types.hasNext()) {
                        Type t = types.next();

                        // FIXME: - This won't work for variables with function type
                        if (t.isFunctionType()) continue;

                        ImcMEM dst = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, new ImcCONST(offset)));
                        ImcMEM src = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e2, new ImcCONST(offset)));

                        ImcMOVE move = new ImcMOVE(dst, src);
                        copyCode.stmts.add(move);

                        offset += t.sizeInBytes();
                    }

                    code = copyCode;
                }
            }
            else {
                code = new ImcMOVE(e1, e2);
            }
        }
        else if (acceptor.oper == AstBinaryExpression.IS) {
		    Type type = symbolDescription.getTypeForAstNode(acceptor.expr2);
		    if (type.isCanType()) type = ((CanType) type).childType;

            int dstDescriptor = type.descriptor;

            FrmLabel l1 = FrmLabel.newAnonymousLabel(),
                     l2 = FrmLabel.newAnonymousLabel(),
                     l3 = FrmLabel.newAnonymousLabel(),
                     l4 = FrmLabel.newAnonymousLabel(),
                     l5 = FrmLabel.newAnonymousLabel();

            controlTransferStartLabelStack.push(l1);
            controlTransferEndLabelStack.push(l3);

            ImcMEM virtualTable = new ImcMEM(e1);

            ImcTEMP virtualTablePointer = new ImcTEMP(new FrmTemp());
            ImcTEMP result = new ImcTEMP(new FrmTemp());

            ImcSEQ statements = new ImcSEQ();
            statements.stmts.add(new ImcMOVE(result, new ImcCONST(0)));
            statements.stmts.add(new ImcMOVE(virtualTablePointer, virtualTable));
            statements.stmts.add(new ImcLABEL(l1));
            statements.stmts.add(new ImcCJUMP(new ImcBINOP(ImcBINOP.NEQ, virtualTablePointer, new ImcCONST(0)), l2, l3));
            statements.stmts.add(new ImcLABEL(l2));
            statements.stmts.add(new ImcCJUMP(new ImcBINOP(ImcBINOP.EQU, new ImcMEM(virtualTablePointer), new ImcCONST(dstDescriptor)), l4, l5));
            statements.stmts.add(new ImcLABEL(l4));
            statements.stmts.add(new ImcMOVE(result, new ImcCONST(1)));
            statements.stmts.add(new ImcJUMP(l3));
            statements.stmts.add(new ImcLABEL(l5));
            statements.stmts.add(new ImcMOVE(virtualTablePointer, new ImcMEM(new ImcBINOP(ImcBINOP.ADD, virtualTablePointer, new ImcCONST(4)))));
            statements.stmts.add(new ImcJUMP(l1));
            statements.stmts.add(new ImcLABEL(l3));

            controlTransferStartLabelStack.pop();
            controlTransferEndLabelStack.pop();

            code = new ImcESEQ(statements, new ImcBINOP(ImcBINOP.EQU, result, new ImcCONST(1)));
        }
        else if (acceptor.oper == AstBinaryExpression.AS) {
            Type type = symbolDescription.getTypeForAstNode(acceptor.expr2);
            if (type.isCanType()) type = ((CanType) type).childType;

            int dstDescriptor = type.descriptor;

            FrmLabel l1 = FrmLabel.newAnonymousLabel(),
                     l2 = FrmLabel.newAnonymousLabel(),
                     l3 = FrmLabel.newAnonymousLabel(),
                     l4 = FrmLabel.newAnonymousLabel(),
                     l5 = FrmLabel.newAnonymousLabel(),
                     l6 = FrmLabel.newAnonymousLabel();

            controlTransferStartLabelStack.push(l1);
            controlTransferEndLabelStack.push(l3);

            ImcMEM virtualTable = new ImcMEM(e1);

            ImcTEMP virtualTablePointer = new ImcTEMP(new FrmTemp());
            ImcTEMP result = new ImcTEMP(new FrmTemp());

            // This is safe casting; if casting fails, object points to null
            // swift equivalent would be 'as?'
            // TODO: - Implement non-safe casting (as!)

            ImcSEQ statements = new ImcSEQ();
            statements.stmts.add(new ImcMOVE(virtualTablePointer, virtualTable));
            statements.stmts.add(new ImcLABEL(l1));
            statements.stmts.add(new ImcCJUMP(new ImcBINOP(ImcBINOP.NEQ, virtualTablePointer, new ImcCONST(0)), l2, l6));
            statements.stmts.add(new ImcLABEL(l2));
            statements.stmts.add(new ImcCJUMP(new ImcBINOP(ImcBINOP.EQU, new ImcMEM(virtualTablePointer), new ImcCONST(dstDescriptor)), l4, l5));
            statements.stmts.add(new ImcLABEL(l4));
            statements.stmts.add(new ImcMOVE(result, e1));
            statements.stmts.add(new ImcJUMP(l3));
            statements.stmts.add(new ImcLABEL(l5));
            statements.stmts.add(new ImcMOVE(virtualTablePointer, new ImcMEM(new ImcBINOP(ImcBINOP.ADD, virtualTablePointer, new ImcCONST(4)))));
            statements.stmts.add(new ImcJUMP(l1));
            statements.stmts.add(new ImcLABEL(l6));
            statements.stmts.add(new ImcMOVE(result, new ImcCONST(0)));
            statements.stmts.add(new ImcLABEL(l3));

            controlTransferStartLabelStack.pop();
            controlTransferEndLabelStack.pop();

            code = new ImcESEQ(statements, result);
        }
		else if (acceptor.oper == AstBinaryExpression.ARR) {
			ArrayType type = (ArrayType) symbolDescription.getTypeForAstNode(acceptor.expr1);
			int size = type.memberType.sizeInBytes();

            ImcBINOP index = new ImcBINOP(ImcBINOP.ADD, e2, new ImcCONST(1));
			code = new ImcMEM(
			        new ImcBINOP(
			                ImcBINOP.ADD,
                            e1,
                            new ImcBINOP(
                                    ImcBINOP.MUL,
                                    index,
                                    new ImcCONST(size))));
		} 
		else if (acceptor.oper == ImcBINOP.MOD) {
			ImcBINOP div = new ImcBINOP(ImcBINOP.DIV, e1, e2);
			ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, div, e2);
			ImcBINOP sub = new ImcBINOP(ImcBINOP.SUB, e1, mul);
			code = sub;
		} 
		else if (acceptor.oper == AstBinaryExpression.DOT) {
			Type t = symbolDescription.getTypeForAstNode(acceptor.expr1);

			String memberName = null;
            AstExpression memberExpression;
            if (acceptor.expr2 instanceof AstBinaryExpression) {
                memberExpression = ((AstBinaryExpression) acceptor.expr2).expr1;
            }
            else {
                memberExpression = acceptor.expr2;
            }

            if (memberExpression instanceof AstVariableNameExpression) {
                memberName = ((AstVariableNameExpression) memberExpression).name;
            }
            else if (memberExpression instanceof AstFunctionCallExpression) {
                memberName = ((AstFunctionCallExpression) memberExpression).getStringRepresentation();
            }
            else if (memberExpression instanceof AstAtomConstExpression) {
                memberName = ((AstAtomConstExpression) memberExpression).value;
            }

            if (memberName == null) {
                logger.error("Something went terribly wrong!");
            }

			/**
			 * Handle enumerations.
			 */
			if (t.isEnumType()) {
				EnumType enumType = (EnumType) t;
				if (enumType.selectedMember != null) {
					AstDefinition memberDef = enumType.findMemberDefinitionWithName(enumType.selectedMember);
					code = imcDescription.getImcCode(memberDef);
				}
				else {
                    code = new ImcCONST(enumType.offsetForMember(memberName));
                }
			}

			/**
			 * Handle classes.
			 */
			else if (t.isObjectType()) {
				// member access code
				if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                    boolean isDynamic = symbolDescription.getDefinitionForAstNode(memberExpression).isDynamic();

                    if (isDynamic) {
                        // dynamic dispatch
                        ClassType classType = (ClassType) t;

                        int indexForMember = classType.indexForMember(((AstFunctionCallExpression) memberExpression).getStringRepresentation());
                        int offset = (indexForMember + 2) * Constants.Byte;

                        FrmTemp frmTemp = new FrmTemp();
                        ImcTEMP temp = new ImcTEMP(frmTemp);

                        ImcSEQ methodCallCode = new ImcSEQ();
                        methodCallCode.stmts.add(
                                new ImcMOVE(temp,
                                        new ImcBINOP(ImcBINOP.ADD,
                                                new ImcMEM(e1),
                                                new ImcCONST(offset))
                                )
                        );

                        ImcMethodCALL methodCall = new ImcMethodCALL(frmTemp);
                        methodCall.args = ((ImcCALL) c2).args;

                        code = new ImcESEQ(methodCallCode, methodCall);
                    }
                    else {
                        // static dispatch
                        code = c2;
                    }
                }
				else {
                    code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, e2));
                }
			}

            /**
             * Handle interfaces.
             */
            else if (t.isInterfaceType()) {
                // dynamic dispatch
                InterfaceType interfaceType = (InterfaceType) t;

                int indexForMember = interfaceType.indexForMember(((AstFunctionCallExpression) acceptor.expr2).getStringRepresentation());
                int offset = (indexForMember + 2) * Constants.Byte;

                FrmTemp frmTemp = new FrmTemp();
                ImcTEMP temp = new ImcTEMP(frmTemp);

                ImcSEQ methodCallCode = new ImcSEQ();
                methodCallCode.stmts.add(
                        new ImcMOVE(temp,
                                new ImcBINOP(ImcBINOP.ADD,
                                        new ImcMEM(e1),
                                        new ImcCONST(offset))
                        )
                );

                ImcMethodCALL methodCall = new ImcMethodCALL(frmTemp);
                methodCall.args = ((ImcCALL) c2).args;

                code = new ImcESEQ(methodCallCode, methodCall);
            }

            /**
             * Handle Can Type (static members).
             */
            else if (t.isCanType()) {
                if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                    code = c2;
                }
                else {
                    code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, e2));
                }
            }

            /**
             * Handle tuples.
             */
            else if (t.isTupleType()) {
                TupleType tupleType = (TupleType) t;

                // member access code
                if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                    code = c2;
                }
                else {
                    int offset = tupleType.offsetOfMember(memberName);
                    code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, new ImcCONST(offset)));
                }
            }

            /**
             * Handle array.count.
             */
            else if (t.isArrayType()) {
                code = new ImcMEM(e1); // TODO: - Reimplement this (array.count)
            }
		}

		imcDescription.setImcCode(acceptor, code);
	}

	@Override
	public void visit(AstDefinitions acceptor) {
		for (AstDefinition def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AstExpressions acceptor) {
		if (acceptor.expressions.size() == 0) {
			imcDescription.setImcCode(acceptor, new ImcCONST(0));
			return;
		}

		ImcSEQ statements = new ImcSEQ();

		for (AstExpression e : acceptor.expressions) {
			e.accept(this);

			ImcCode code = imcDescription.getImcCode(e);
			if (code instanceof ImcStmt) {
                statements.stmts.add((ImcStmt) code);
            }
			else {
                statements.stmts.add(new ImcEXP((ImcExpr) code));
            }
		}

		imcDescription.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AstForStatement acceptor) {
		symbolDescription.getDefinitionForAstNode(acceptor.iterator).accept(this);

		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);

		ImcSEQ bodyCode = (ImcSEQ) imcDescription.getImcCode(acceptor.body);
		Type type = symbolDescription.getTypeForAstNode(acceptor.collection);

		if (type.isArrayType()) {
			type = ((ArrayType) type).memberType;
		}

		ImcExpr size = new ImcCONST(type.sizeInBytes());
        ImcTEMP hi = new ImcTEMP(new FrmTemp());
		ImcTEMP counter = new ImcTEMP(new FrmTemp());
		ImcExpr iterator = (ImcExpr) imcDescription.getImcCode(acceptor.iterator);
		ImcExpr collection = (ImcExpr) imcDescription.getImcCode(acceptor.collection);

		ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, counter, size);
		ImcBINOP add = new ImcBINOP(ImcBINOP.ADD, collection, mul);

		FrmLabel l1 = FrmLabel.newAnonymousLabel(), l2 = FrmLabel.newAnonymousLabel(), l3 = FrmLabel.newAnonymousLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcMOVE(hi, new ImcMEM(collection)));
		statements.stmts.add(new ImcMOVE(counter, new ImcCONST(1)));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP(new ImcBINOP(ImcBINOP.LTH, counter, hi), l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
        statements.stmts.add(new ImcMOVE(iterator, new ImcMEM(add)));
		statements.stmts.add(bodyCode);
		statements.stmts.add(new ImcMOVE(counter, new ImcBINOP(ImcBINOP.ADD, counter, new ImcCONST(1))));
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		imcDescription.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AstFunctionCallExpression acceptor) {
		for (AstLabeledExpr arg: acceptor.arguments)
			arg.accept(this);

		FrmFrame frame = frameDescription.getFrame(symbolDescription.getDefinitionForAstNode(acceptor));
		ImcCALL fnCall = new ImcCALL(frame.entryLabel);

		int diff = returnExprFrameStack.peek().staticLevel - frame.staticLevel;

		if (diff == 0) {
            fnCall.args.add(new ImcCONST(0xFFAAFF));
        }
		else {
			ImcExpr staticLink = new ImcTEMP(returnExprFrameStack.peek().FP);

			if (diff > 0) {
                for (int i = 0; i < diff + 1; i++) {
                    staticLink = new ImcMEM(staticLink);
                }
            }

			fnCall.args.add(staticLink);
		}

		boolean isConstructor = ((AstFunctionDefinition) symbolDescription.getDefinitionForAstNode(acceptor)).isConstructor;

		for (AstExpression arg : acceptor.arguments) {
            // skip first ("self") argument if function is constructor
            if (isConstructor && arg == acceptor.arguments.get(0)) {
                ImcCONST nil = new ImcCONST(0);
                fnCall.args.add(nil);
                continue;
            }

			ImcExpr argExpression = (ImcExpr) imcDescription.getImcCode(arg);
			fnCall.args.add(argExpression);
		}

		imcDescription.setImcCode(acceptor, fnCall);
	}

	@Override
	public void visit(AstFunctionDefinition acceptor) {
		returnExprFrameStack.push(frameDescription.getFrame(acceptor));

		acceptor.functionCode.accept(this);
		
		ImcSEQ code = (ImcSEQ) imcDescription.getImcCode(acceptor.functionCode);
		code.stmts.add(new ImcLABEL(returnExprFrameStack.peek().endLabel));
        code.stmts.add(0, new ImcMOVE(new ImcTEMP(returnExprFrameStack.peek().RV), new ImcCONST(-555)));
		chunks.add(new ImcCodeChunk(returnExprFrameStack.peek(), code));
		
		returnExprFrameStack.pop();
	}

	@Override
	public void visit(AstIfStatement acceptor) {
		ImcSEQ statements = new ImcSEQ();
		FrmLabel endLabel = FrmLabel.newAnonymousLabel();

		for (Condition c : acceptor.conditions) {
			c.condition.accept(this);
			c.body.accept(this);

			ImcExpr cond = (ImcExpr) imcDescription.getImcCode(c.condition);
			ImcStmt body = (ImcStmt) imcDescription.getImcCode(c.body);

			FrmLabel l1 = FrmLabel.newAnonymousLabel(), l2 = FrmLabel.newAnonymousLabel();
	
			statements.stmts.add(new ImcCJUMP(cond, l1, l2));
			statements.stmts.add(new ImcLABEL(l1));
			statements.stmts.add(body);
			statements.stmts.add(new ImcJUMP(endLabel));
			statements.stmts.add(new ImcLABEL(l2));
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);	
			
			ImcStmt body = (ImcStmt) imcDescription.getImcCode(acceptor.elseBody);
			statements.stmts.add(body);
		}

		statements.stmts.add(new ImcLABEL(endLabel));
		imcDescription.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AstParameterDefinition acceptor) {
		///
	}

	@Override
	public void visit(AstTypeName acceptor) {
		///
	}

	@Override
	public void visit(AstUnaryExpression acceptor) {
		acceptor.expr.accept(this);

		ImcCode expr = imcDescription.getImcCode(acceptor.expr);

		if (acceptor.oper == AstUnaryExpression.SUB) {
			imcDescription.setImcCode(acceptor, new ImcBINOP(ImcBINOP.SUB, new ImcCONST(0), (ImcExpr) expr));
		}
		else if (acceptor.oper == AstUnaryExpression.NOT) {
			ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, (ImcExpr) expr, new ImcCONST(1));
			ImcBINOP not = new ImcBINOP(ImcBINOP.ADD, mul, new ImcCONST(1));
			imcDescription.setImcCode(acceptor, not);
		}
		else if (acceptor.oper == AstUnaryExpression.MEM) {
			if (expr instanceof ImcStmt) {
                logger.error(acceptor.position, "Error");
            }
			imcDescription.setImcCode(acceptor, ((ImcMEM) expr).expr);
		}
		else if (acceptor.oper == AstUnaryExpression.VAL) {
			if (expr instanceof ImcStmt) {
                logger.error(acceptor.position, "Error");
            }
			imcDescription.setImcCode(acceptor, new ImcMEM((ImcExpr) expr));
		}
		else {
            imcDescription.setImcCode(acceptor, expr);
        }
	}

	@Override
	public void visit(AstVariableDefinition acceptor) {
		FrmAccess access = frameDescription.getAccess(acceptor);
		Type varType = symbolDescription.getTypeForAstNode(acceptor);
		
		int size = varType.sizeInBytes();
		if (varType.isReferenceType()) {
            size = 4;
        }

		if (access instanceof FrmVarAccess) {
            chunks.add(new ImcDataChunk(((FrmVarAccess) access).label, size));
        }
	}

	@Override
	public void visit(AstVariableNameExpression acceptor) {
		AstDefinition nameDefinition = symbolDescription.getDefinitionForAstNode(acceptor);
		
		FrmAccess access = frameDescription.getAccess(nameDefinition);
		if (access == null) return;
		
		ImcExpr expr = null;

		if (access instanceof FrmVarAccess) {
            expr = new ImcMEM(new ImcNAME(((FrmVarAccess) access).label));
        }
		else if (access instanceof FrmLocAccess) {
			FrmLocAccess loc = (FrmLocAccess) access;
            int diff = returnExprFrameStack.peek().staticLevel - loc.frame.staticLevel;

            ImcExpr fp = new ImcTEMP(returnExprFrameStack.peek().FP);
            for (int i = 0; i < diff; i++) {
                fp = new ImcMEM(fp);
            }

            expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(loc.framePointerOffset)));
		} 
		else if (access instanceof FrmMemberAccess) {
			FrmMemberAccess memberAccess = (FrmMemberAccess) access;
			expr = new ImcCONST(memberAccess.offsetForMember());
		}
        else if (access instanceof FrmStaticAccess) {
            FrmStaticAccess staticAccess = (FrmStaticAccess) access;
            expr = new ImcCONST(staticAccess.offsetForStaticMember());
        }
		else if (access instanceof FrmParAccess) {
			FrmParAccess loc = (FrmParAccess) access;
			int diff = returnExprFrameStack.peek().staticLevel - loc.frame.staticLevel;

			ImcExpr fp = new ImcTEMP(returnExprFrameStack.peek().FP);
			for (int i = 0; i < diff; i++)
				fp = new ImcMEM(fp);

			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(loc.framePointerOffset)));
		} 
		else if (access instanceof FrmFunAccess) {
            expr = new ImcNAME(((FrmFunAccess) access).label);
        }
        else if (access instanceof FrmVirtualTableAccess) {
		    FrmVirtualTableAccess virtualAccess = (FrmVirtualTableAccess) access;
            expr = new ImcNAME(virtualAccess.label);
        }

		imcDescription.setImcCode(acceptor, expr);
	}

	@Override
	public void visit(AstWhileStatement acceptor) {
		FrmLabel l1 = FrmLabel.newAnonymousLabel(),
				 l2 = FrmLabel.newAnonymousLabel(),
				 l3 = FrmLabel.newAnonymousLabel();
		controlTransferStartLabelStack.push(l1);
		controlTransferEndLabelStack.push(l3);
		
		acceptor.condition.accept(this);
		acceptor.body.accept(this);
		
		ImcSEQ body = (ImcSEQ) imcDescription.getImcCode(acceptor.body);
		ImcExpr cond = (ImcExpr) imcDescription.getImcCode(acceptor.condition);
		
		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP(cond, l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(body);
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		imcDescription.setImcCode(acceptor, statements);
		controlTransferStartLabelStack.pop();
		controlTransferEndLabelStack.pop();
	}

	@Override
	public void visit(AstImportDefinition acceptor) {
		acceptor.imports.accept(this);
	}

	@Override
	public void visit(AstStatements acceptor) {
		currentFunctionScope++;
		ImcSEQ seq = new ImcSEQ();
		for (AstStatement stmt : acceptor.statements) {
			stmt.accept(this);

			ImcCode code = imcDescription.getImcCode(stmt);
			if (code != null) {
				ImcStmt s = null;

				if (code instanceof ImcStmt) {
                    s = (ImcStmt) code;
                }
				else {
                    s = new ImcEXP((ImcExpr) code);
                }

				seq.stmts.add(s);
			}
		}
		imcDescription.setImcCode(acceptor, seq);

		currentFunctionScope--;
		if (currentFunctionScope == 0) {
            seq.stmts.add(0, new ImcMOVE(new ImcTEMP(returnExprFrameStack.peek().RV), new ImcCONST(-555)));
			entryPointCode = new ImcCodeChunk(returnExprFrameStack.peek(), seq);
			chunks.add(entryPointCode);
		}
	}

	@Override
	public void visit(AstReturnExpression acceptor) {
		ImcSEQ seq = new ImcSEQ();
		ImcTEMP rv = new ImcTEMP(returnExprFrameStack.peek().RV);

		if (acceptor.expr != null) {
			acceptor.expr.accept(this);

			seq.stmts.add(new ImcMOVE(rv, (ImcExpr) imcDescription.getImcCode(acceptor.expr)));
		}
		else {
            imcDescription.setImcCode(acceptor, new ImcRETURN(null));
        }

		seq.stmts.add(new ImcJUMP(returnExprFrameStack.peek().endLabel));
		imcDescription.setImcCode(acceptor, seq);
	}

	@Override
	public void visit(AstListExpr acceptor) {
		int size = acceptor.expressions.size();
		int elementSize = ((ArrayType) symbolDescription.getTypeForAstNode(acceptor)).memberType.sizeInBytes();

		FrmLabel label = FrmLabel.newAnonymousLabel();
        ImcNAME labelCode = new ImcNAME(label);
		ImcDataChunk chunk = new ImcDataChunk(label, size * elementSize);
		ImcSEQ seq = new ImcSEQ();

		seq.stmts.add(new ImcMOVE(new ImcMEM(labelCode), new ImcCONST(size)));

		int i = 1;
		for (AstExpression e : acceptor.expressions) {
			e.accept(this);

			ImcCode code = imcDescription.getImcCode(e);
			ImcExpr expr;
			if (code instanceof ImcEXP) {
                expr = ((ImcEXP) code).expr;
            }
			else {
                expr = (ImcExpr) code;
            }

			ImcMOVE move = new ImcMOVE(
			        new ImcMEM(
			                new ImcBINOP(
			                        ImcBINOP.ADD,
                                    labelCode,
                                    new ImcBINOP(ImcBINOP.MUL,
                                            new ImcCONST(i),
                                            new ImcCONST(elementSize)))),
                    expr);
			seq.stmts.add(move);

			i++;
		}

		chunks.add(chunk);
		imcDescription.setImcCode(acceptor, new ImcESEQ(seq, labelCode));
	}

	@Override
	public void visit(AstFunctionType funType) {
		///
	}

	@Override
	public void visit(AstControlTransferStatement acceptor) {
		if (acceptor.control == ControlTransferKind.Continue) {
			if (!controlTransferStartLabelStack.isEmpty()) {
                // Jump to continue label (beginning of the loop)
                imcDescription.setImcCode(acceptor, new ImcJUMP(controlTransferStartLabelStack.peek()));
            }
		}
		else {
			if (!controlTransferEndLabelStack.isEmpty()) {
                // Jump to break label
                imcDescription.setImcCode(acceptor, new ImcJUMP(controlTransferEndLabelStack.peek()));
            }
		}
	}

	@Override
	public void visit(AstSwitchStatement acceptor) {
		acceptor.subjectExpr.accept(this);

		ImcSEQ switchCode = new ImcSEQ();
		
		ImcExpr subjectExpr = (ImcExpr) imcDescription.getImcCode(acceptor.subjectExpr);
		switchSubjectExprs.push(subjectExpr);
		
		FrmLabel endLabel = FrmLabel.newAnonymousLabel();
		controlTransferEndLabelStack.push(endLabel);
		
		for (AstCaseStatement singleCase : acceptor.cases) {
			singleCase.accept(this);
			switchCode.stmts.add((ImcStmt) imcDescription.getImcCode(singleCase));
		}
		
		if (acceptor.defaultBody != null) {
			acceptor.defaultBody.accept(this);
			switchCode.stmts.add((ImcStmt) imcDescription.getImcCode(acceptor.defaultBody));
		}
		
		switchCode.stmts.add(new ImcLABEL(endLabel));
		controlTransferEndLabelStack.pop();
		imcDescription.setImcCode(acceptor, switchCode);
	}

	@Override
	public void visit(AstCaseStatement acceptor) {
		ImcExpr caseCondition = null;
		
		for (AstExpression e : acceptor.exprs) {
			e.accept(this);
			ImcExpr expr = (ImcExpr) imcDescription.getImcCode(e);
			ImcExpr cond = new ImcBINOP(ImcBINOP.EQU, expr, switchSubjectExprs.peek());
			
			if (caseCondition == null)
				caseCondition = cond; 
			else
				caseCondition = new ImcBINOP(ImcBINOP.OR, caseCondition, cond);
		}

		FrmLabel startLabel = FrmLabel.newAnonymousLabel();
		FrmLabel endLabel = FrmLabel.newAnonymousLabel();
		
		acceptor.body.accept(this);
		ImcStmt body = (ImcStmt) imcDescription.getImcCode(acceptor.body);
		
		ImcSEQ caseCode = new ImcSEQ();
		caseCode.stmts.add(new ImcCJUMP(caseCondition, startLabel, endLabel));
		caseCode.stmts.add(new ImcLABEL(startLabel));
		caseCode.stmts.add(body);
		caseCode.stmts.add(new ImcLABEL(endLabel));
		
		imcDescription.setImcCode(acceptor, caseCode);
	}

	@Override
	public void visit(AstEnumDefinition acceptor) {
		if (acceptor.type != null) {
			for (AstDefinition d : acceptor.definitions)
				d.accept(this);
		}
	}

	@Override
	public void visit(AstEnumMemberDefinition acceptor) {
		if (acceptor.value != null) {
			acceptor.value.accept(this);
			imcDescription.setImcCode(acceptor, imcDescription.getImcCode(acceptor.value));
		}
	}

	@Override
	public void visit(AstTupleType acceptor) {
        ///
	}

	@Override
	public void visit(AstLabeledExpr acceptor) {
		acceptor.expr.accept(this);
		imcDescription.setImcCode(acceptor, imcDescription.getImcCode(acceptor.expr));
	}

	@Override
	public void visit(AstTupleExpression acceptor) {
		TupleType tupleType = (TupleType) symbolDescription.getTypeForAstNode(acceptor);
		int size = tupleType.sizeInBytes();

		ImcSEQ seq = new ImcSEQ();
		ImcTEMP location = new ImcTEMP(new FrmTemp());
		seq.stmts.add(new ImcMOVE(location, new ImcMALLOC(size)));
		
		for (AstExpression e : acceptor.expressions.expressions) {
			e.accept(this);
			
			int memberOffset = tupleType.offsetOfMember(((AstLabeledExpr) e).label);
			ImcExpr exprCode = (ImcExpr) imcDescription.getImcCode(e);
			ImcExpr dst = new ImcMEM(
					new ImcBINOP(ImcBINOP.ADD, location, new ImcCONST(memberOffset)));
			ImcMOVE move = new ImcMOVE(dst, exprCode);
			seq.stmts.add(move);
		}
		
		imcDescription.setImcCode(acceptor, new ImcESEQ(seq, location));
	}

	@Override
	public void visit(AstOptionalType acceptor) {
		acceptor.childType.accept(this);
	}

	@Override
	public void visit(AstOptionalEvaluationExpression acceptor) {
		acceptor.subExpr.accept(this);
		
		imcDescription.setImcCode(acceptor, imcDescription.getImcCode(acceptor.subExpr));
	}

	@Override
	public void visit(AstForceValueExpression acceptor) {
		acceptor.subExpr.accept(this);

		imcDescription.setImcCode(acceptor, imcDescription.getImcCode(acceptor.subExpr));
	}

    @Override
    public void visit(AstExtensionDefinition acceptor) {
        acceptor.extendingType.accept(this);
        acceptor.definitions.accept(this);
    }

    @Override
    public void visit(AstInterfaceDefinition absInterfaceDef) {
        ///
    }
}
