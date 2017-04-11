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

import java.util.LinkedList;
import java.util.Stack;

import Utils.Constants;
import compiler.Report;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AtomTypeKind;
import compiler.abstr.tree.Condition;
import compiler.abstr.tree.ControlTransferKind;
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
import compiler.frames.*;
import compiler.seman.SymbDesc;
import compiler.seman.type.ArrayType;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;
import compiler.seman.type.EnumType;
import compiler.seman.type.TupleType;
import compiler.seman.type.Type;

public class ImcCodeGen implements ASTVisitor {

	/**
	 * Data and code chunks.
	 */
	public LinkedList<ImcChunk> chunks;
	private int virtualTableCount = 0;
	
	/**
	 * Code to be executed at the beginning of execution.
	 */
	public ImcCodeChunk entryPointCode = null;
	
	///
	
	/**
	 * Frame stack (used for return expr). 
	 */
	private Stack<FrmFrame> frameStack = new Stack<>();
	
	/**
	 *  Labels for control transfer (break and continue) statements.
	 */
	private Stack<FrmLabel> startLabelStack = new Stack<>();
	private Stack<FrmLabel> endLabelStack = new Stack<>();

	/**
	 * Current function scope.
	 * If scope equals zero, code being generated is entry point code.
	 */
	private int scope = 0;
	
	/**
	 * Switch stmt subject expr stack 
	 */
	private Stack<ImcExpr> switchSubjectExprs = new Stack<>();
	
	///

	public ImcCodeGen(FrmFrame entryPoint) {
		frameStack.add(entryPoint);
		chunks = new LinkedList<ImcChunk>();
	}

	@Override
	public void visit(AbsListType acceptor) {
        ///
	}

	@Override
	public void visit(AbsClassDef acceptor) {
        FrmVirtualTableAccess access = (FrmVirtualTableAccess) FrmDesc.getAccess(acceptor);
        CanType varType = (CanType) SymbDesc.getType(access.classDef);

        chunks.add(virtualTableCount++, new ImcVirtualTableDataChunk(
                FrmLabel.newLabel(varType.friendlyName()),
                access.size,
                (ClassType) varType.childType));

		for (AbsDef def : acceptor.definitions.definitions) {
            def.accept(this);
        }

		CanType type = (CanType) SymbDesc.getType(acceptor);
		ClassType classType = (ClassType) type.childType;
		int size = classType.size();

        for (AbsFunDef constructor : acceptor.contrustors) {
            FrmFrame constructorFrame = FrmDesc.getFrame(constructor);

            frameStack.push(constructorFrame);
            constructor.func.accept(this);

            ImcSEQ constructorCode = (ImcSEQ) ImcDesc.getImcCode(constructor.func);

            // allocate memory for new object
            ImcTEMP framePointer = new ImcTEMP(constructorFrame.FP);
            ImcCONST offset = new ImcCONST(4);
            ImcMEM location = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, framePointer, offset));

            // assign new object as "self" parameter
            constructorCode.stmts.addFirst(new ImcMOVE(location, new ImcMALLOC(size)));

            // assign pointer to vtable
            constructorCode.stmts.add(1, new ImcMOVE(new ImcMEM(location), new ImcCONST(access.location)));

            // return new object
            constructorCode.stmts.add(new ImcMOVE(new ImcTEMP(constructorFrame.RV), location));

            ImcDesc.setImcCode(constructor, constructorCode);
            chunks.add(new ImcCodeChunk(constructorFrame, constructorCode));

            constructorCode.stmts.add(new ImcLABEL(frameStack.peek().endLabel));
            frameStack.pop();
        }
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
		if (acceptor.type == AtomTypeKind.INT) {
            ImcDesc.setImcCode(acceptor,
                    new ImcCONST(Integer.parseInt(acceptor.value)));
        }
		else if (acceptor.type == AtomTypeKind.LOG) {
            ImcDesc.setImcCode(acceptor,
                    new ImcCONST(acceptor.value.equals("true") ? 1 : 0));
        }
		else if (acceptor.type == AtomTypeKind.STR) {
			FrmLabel l = FrmLabel.newLabel();
			ImcDataChunk str = new ImcDataChunk(l, acceptor.value.length());
			str.data = acceptor.value;
			chunks.add(str);
			ImcDesc.setImcCode(acceptor, new ImcMEM(new ImcNAME(l)));
		}
		else if (acceptor.type == AtomTypeKind.CHR) {
			ImcDesc.setImcCode(acceptor, new ImcCONST(acceptor.value.charAt(0)));
		}
		else if (acceptor.type == AtomTypeKind.DOB) {
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(Double.parseDouble(acceptor.value)));
		}
		else if (acceptor.type == AtomTypeKind.NIL) {
			ImcDesc.setImcCode(acceptor, new ImcCONST(0));
		}
	}

	@Override
	public void visit(AbsAtomType acceptor) {
		///
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);

		ImcCode c1 = ImcDesc.getImcCode(acceptor.expr1);
		ImcCode c2 = ImcDesc.getImcCode(acceptor.expr2);

		ImcExpr e1 = null;
		ImcExpr e2 = null;

		if (c1 instanceof ImcEXP) e1 = ((ImcEXP) c1).expr;
		else e1 = (ImcExpr) c1;

		if (c2 instanceof ImcESEQ) e2 = (ImcESEQ) c2;
		else if (c2 instanceof ImcEXP) e2 = ((ImcEXP) c2).expr;
		else e2 = (ImcExpr) c2;

		ImcCode code = null;

		if (acceptor.oper >= 0 && acceptor.oper <= 11) {
            code = new ImcBINOP(acceptor.oper, e1, e2);
        }
		else if (acceptor.oper == AbsBinExpr.ASSIGN) {
            code = new ImcMOVE(e1, e2);
        }
        else if (acceptor.oper == AbsBinExpr.IS) {
            int dstDescriptor = Type.getDescriptorForType(SymbDesc.getType(acceptor.expr2));

            FrmLabel l1 = FrmLabel.newLabel(),
                     l2 = FrmLabel.newLabel(),
                     l3 = FrmLabel.newLabel(),
                     l4 = FrmLabel.newLabel(),
                     l5 = FrmLabel.newLabel();

            startLabelStack.push(l1);
            endLabelStack.push(l3);

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

            ImcDesc.setImcCode(acceptor, statements);
            startLabelStack.pop();
            endLabelStack.pop();

            code = new ImcESEQ(statements, new ImcBINOP(ImcBINOP.EQU, result, new ImcCONST(1)));
        }
		else if (acceptor.oper == AbsBinExpr.ARR) {
		    // TODO: -
			ArrayType type = (ArrayType) SymbDesc.getType(acceptor.expr1);
			int size = type.type.size();
			
			code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, new ImcBINOP(
					ImcBINOP.MUL, e2, new ImcCONST(size))));
		} 
		else if (acceptor.oper == ImcBINOP.MOD) {
			ImcBINOP div = new ImcBINOP(ImcBINOP.DIV, e1, e2);
			ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, div, e2);
			ImcBINOP sub = new ImcBINOP(ImcBINOP.SUB, e1, mul);
			code = sub;
		} 
		else if (acceptor.oper == AbsBinExpr.DOT) {
			Type t = SymbDesc.getType(acceptor.expr1);
			
			String memberName;
			if (acceptor.expr2 instanceof AbsVarNameExpr)
				memberName = ((AbsVarNameExpr) acceptor.expr2).name;
			else if (acceptor.expr2 instanceof AbsFunCall)
				memberName = ((AbsFunCall) acceptor.expr2).name;
			else
				memberName = ((AbsAtomConstExpr) acceptor.expr2).value;

			/**
			 * Handle enumerations.
			 */
			if (t.isEnumType()) {
				EnumType enumType = (EnumType) t;
				if (enumType.selectedMember != null) {
					AbsDef memberDef = enumType.findMemberForName(enumType.selectedMember);
					code = ImcDesc.getImcCode(memberDef);
				}
				else {
                    code = new ImcCONST(enumType.memberOffsetForName(memberName));
                }
			}

			/**
			 * Handle classes.
			 */
			else if (t.isClassType()) {
				// member access code
				if (acceptor.expr2 instanceof AbsFunCall) {
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
                if (acceptor.expr2 instanceof AbsFunCall) {
                    code = c2;
                }
                else {
                    int offset = tupleType.offsetOf(memberName);
                    code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, new ImcCONST(offset)));
                }
            }

            /**
             * Handle array.length.
             */
            // TODO: - Remove this
            else if (t.isArrayType()) {
                code = new ImcCONST(((ArrayType) t).count);
            }
		}

		ImcDesc.setImcCode(acceptor, code);
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		if (acceptor.expressions.size() == 0) {
			ImcDesc.setImcCode(acceptor, new ImcCONST(0));
			return;
		}

		ImcSEQ statements = new ImcSEQ();

		for (AbsExpr e : acceptor.expressions) {
			e.accept(this);

			ImcCode code = ImcDesc.getImcCode(e);
			if (code instanceof ImcStmt) {
                statements.stmts.add((ImcStmt) code);
            }
			else {
                statements.stmts.add(new ImcEXP((ImcExpr) code));
            }
		}

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		SymbDesc.getNameDef(acceptor.iterator).accept(this);

		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);

		ImcSEQ bodyCode = (ImcSEQ) ImcDesc.getImcCode(acceptor.body);
		Type type = SymbDesc.getType(acceptor.collection);
		int count = 0;
		
		if (type.isArrayType()) {
			count = ((ArrayType) type).count;
			type = ((ArrayType) type).type;
		}

		ImcExpr size = new ImcCONST(type.size());
		ImcExpr hi = new ImcCONST(count);
		ImcTEMP counter = new ImcTEMP(new FrmTemp());
		ImcExpr iterator = (ImcExpr) ImcDesc.getImcCode(acceptor.iterator);
		ImcExpr collection = (ImcExpr) ImcDesc.getImcCode(acceptor.collection);

		ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, counter, size);
		ImcBINOP add = new ImcBINOP(ImcBINOP.ADD, collection, mul);

		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcMOVE(counter, new ImcCONST(0)));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcMOVE(iterator, new ImcMEM(add)));
		statements.stmts.add(new ImcCJUMP(new ImcBINOP(ImcBINOP.LTH, counter,
				hi), l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(bodyCode);
		statements.stmts.add(new ImcMOVE(counter, new ImcBINOP(ImcBINOP.ADD,
				counter, new ImcCONST(1))));
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		for (AbsLabeledExpr arg: acceptor.args)
			arg.accept(this);

		FrmFrame frame = FrmDesc.getFrame(SymbDesc.getNameDef(acceptor));
		ImcCALL fnCall = new ImcCALL(frame.label);

		int diff = frameStack.peek().level - frame.level;

		if (diff == 0) {
            fnCall.args.add(new ImcCONST(0xFFAAFF));
        }
		else {
			ImcExpr staticLink = new ImcTEMP(frameStack.peek().FP);

			if (diff > 0) {
                for (int i = 0; i < diff + 1; i++) {
                    staticLink = new ImcMEM(staticLink);
                }
            }

			fnCall.args.add(staticLink);
		}

		boolean isConstructor = ((AbsFunDef) SymbDesc.getNameDef(acceptor)).isConstructor;

		for (AbsExpr arg : acceptor.args) {
            // skip first ("self") argument if function is constructor
            if (isConstructor && arg == acceptor.args.firstElement()) {
                ImcCONST noData = new ImcCONST(0);
                fnCall.args.add(noData);

                continue;
            }

			ImcExpr argExpression = (ImcExpr) ImcDesc.getImcCode(arg);
			fnCall.args.add(argExpression);
		}

		ImcDesc.setImcCode(acceptor, fnCall);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		frameStack.push(FrmDesc.getFrame(acceptor));

		acceptor.func.accept(this);
		
		ImcSEQ code = (ImcSEQ) ImcDesc.getImcCode(acceptor.func);
		code.stmts.add(new ImcLABEL(frameStack.peek().endLabel));
		chunks.add(new ImcCodeChunk(frameStack.peek(), code));
		
		frameStack.pop();
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		ImcSEQ statements = new ImcSEQ();
		FrmLabel endLabel = FrmLabel.newLabel();

		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);
			c.body.accept(this);

			ImcExpr cond = (ImcExpr) ImcDesc.getImcCode(c.cond);
			ImcStmt body = (ImcStmt) ImcDesc.getImcCode(c.body);

			FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel();
	
			statements.stmts.add(new ImcCJUMP(cond, l1, l2));
			statements.stmts.add(new ImcLABEL(l1));
			statements.stmts.add(body);
			statements.stmts.add(new ImcJUMP(endLabel));
			statements.stmts.add(new ImcLABEL(l2));
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);	
			
			ImcStmt body = (ImcStmt) ImcDesc.getImcCode(acceptor.elseBody);
			statements.stmts.add(body);
		}

		statements.stmts.add(new ImcLABEL(endLabel));
		ImcDesc.setImcCode(acceptor, statements);
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

		ImcCode expr = ImcDesc.getImcCode(acceptor.expr);

		if (acceptor.oper == AbsUnExpr.SUB) {
			ImcDesc.setImcCode(acceptor, new ImcBINOP(ImcBINOP.SUB,
					new ImcCONST(0), (ImcExpr) expr));
		} else if (acceptor.oper == AbsUnExpr.NOT) {
			ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, (ImcExpr) expr,
					new ImcCONST(1));
			ImcBINOP not = new ImcBINOP(ImcBINOP.ADD, mul, new ImcCONST(1));
			ImcDesc.setImcCode(acceptor, not);
		} else if (acceptor.oper == AbsUnExpr.MEM) {
			if (expr instanceof ImcStmt)
				Report.error(acceptor.position, "Error");
			ImcDesc.setImcCode(acceptor, ((ImcMEM) expr).expr);
		} else if (acceptor.oper == AbsUnExpr.VAL) {
			if (expr instanceof ImcStmt)
				Report.error(acceptor.position, "Error");
			ImcDesc.setImcCode(acceptor, new ImcMEM((ImcExpr) expr));
		} else
			ImcDesc.setImcCode(acceptor, expr);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		FrmAccess access = FrmDesc.getAccess(acceptor);
		Type varType = SymbDesc.getType(acceptor);
		
		int size = varType.size();
		if (varType.isPointerType())
			size = 4;

		if (access instanceof FrmVarAccess)
			chunks.add(new ImcDataChunk(((FrmVarAccess) access).label, size));
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
		AbsDef nameDefinition = SymbDesc.getNameDef(acceptor);
		
		FrmAccess access = FrmDesc.getAccess(nameDefinition);
		if (access == null) return;
		
		ImcExpr expr = null;

		if (access instanceof FrmVarAccess)
			expr = new ImcMEM(new ImcNAME(((FrmVarAccess) access).label));
		else if (access instanceof FrmLocAccess) {
			FrmLocAccess loc = (FrmLocAccess) access;
			int diff = frameStack.peek().level - loc.frame.level;

			ImcExpr fp = new ImcTEMP(frameStack.peek().FP);
			for (int i = 0; i < diff; i++)
				fp = new ImcMEM(fp);

			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(
					loc.offset)));
		} 
		else if (access instanceof FrmMemberAccess) {
			FrmMemberAccess member = (FrmMemberAccess) access;
			expr = new ImcCONST(member.offsetForMember());
		}
		else if (access instanceof FrmParAccess) {
			FrmParAccess loc = (FrmParAccess) access;
			int diff = frameStack.peek().level - loc.frame.level;

			ImcExpr fp = new ImcTEMP(frameStack.peek().FP);
			for (int i = 0; i < diff; i++)
				fp = new ImcMEM(fp);

			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(
					loc.offset)));
		} 
		else if (access instanceof FrmFunAccess)
			expr = new ImcNAME(((FrmFunAccess) access).label);

		ImcDesc.setImcCode(acceptor, expr);
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		FrmLabel l1 = FrmLabel.newLabel(), 
				 l2 = FrmLabel.newLabel(), 
				 l3 = FrmLabel.newLabel();
		startLabelStack.push(l1);
		endLabelStack.push(l3);
		
		acceptor.cond.accept(this);
		acceptor.body.accept(this);
		
		ImcSEQ body = (ImcSEQ) ImcDesc.getImcCode(acceptor.body);
		ImcExpr cond = (ImcExpr) ImcDesc.getImcCode(acceptor.cond);
		
		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP(cond, l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(body);
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		ImcDesc.setImcCode(acceptor, statements);
		startLabelStack.pop();
		endLabelStack.pop();
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		acceptor.imports.accept(this);
	}

	@Override
	public void visit(AbsStmts acceptor) {
		scope++;
		ImcSEQ seq = new ImcSEQ();
		for (AbsStmt stmt : acceptor.statements) {
			stmt.accept(this);

			ImcCode code = ImcDesc.getImcCode(stmt);
			if (code != null) {
				ImcStmt s = null;

				if (code instanceof ImcStmt)
					s = (ImcStmt) code;
				else
					s = new ImcEXP((ImcExpr) code);

				seq.stmts.add(s);
			}
		}
		ImcDesc.setImcCode(acceptor, seq);

		scope--;
		if (scope == 0) {
			entryPointCode = new ImcCodeChunk(frameStack.peek(), seq);
			chunks.add(entryPointCode);
		}
	}

	@Override
	public void visit(AbsReturnExpr acceptor) {
		ImcSEQ seq = new ImcSEQ();
		ImcTEMP rv = new ImcTEMP(frameStack.peek().RV);

		if (acceptor.expr != null) {
			acceptor.expr.accept(this);

			seq.stmts.add(new ImcMOVE(rv, (ImcExpr) ImcDesc
					.getImcCode(acceptor.expr)));
		} else
			ImcDesc.setImcCode(acceptor, new ImcRETURN(null));

		seq.stmts.add(new ImcJUMP(frameStack.peek().endLabel));
		ImcDesc.setImcCode(acceptor, seq);
	}

	@Override
	public void visit(AbsListExpr acceptor) {
		int size = ((ArrayType) SymbDesc.getType(acceptor)).count;
		int elSize = ((ArrayType) SymbDesc.getType(acceptor)).type.size();

		FrmLabel label = FrmLabel.newLabel();
		ImcDataChunk chunk = new ImcDataChunk(label, size * elSize);
		ImcSEQ seq = new ImcSEQ();

		int i = 0;
		for (AbsExpr e : acceptor.expressions) {
			e.accept(this);

			ImcCode code = ImcDesc.getImcCode(e);
			ImcExpr expr;
			if (code instanceof ImcEXP)
				expr = ((ImcEXP) code).expr;
			else
				expr = (ImcExpr) code;

			ImcMOVE move = new ImcMOVE(new ImcMEM(new ImcBINOP(ImcBINOP.ADD,
					new ImcNAME(label), new ImcBINOP(ImcBINOP.MUL,
							new ImcCONST(i), new ImcCONST(elSize)))), expr);
			i++;
			seq.stmts.add(move);
		}

		chunks.add(chunk);
		ImcDesc.setImcCode(acceptor, new ImcESEQ(seq, new ImcNAME(label)));
	}

	@Override
	public void visit(AbsFunType funType) {
		///
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		if (acceptor.control == ControlTransferKind.Continue) {
			if (!startLabelStack.isEmpty()) 
				// Jump to continue label (beginning of the loop)
				ImcDesc.setImcCode(acceptor, new ImcJUMP(startLabelStack.peek()));
		}
		else {
			if (!endLabelStack.isEmpty())
				// Jump to break label
				ImcDesc.setImcCode(acceptor, new ImcJUMP(endLabelStack.peek()));
		}
	}

	@Override
	public void visit(AbsSwitchStmt acceptor) {
		acceptor.subjectExpr.accept(this);

		ImcSEQ switchCode = new ImcSEQ();
		
		ImcExpr subjectExpr = (ImcExpr) ImcDesc.getImcCode(acceptor.subjectExpr);
		switchSubjectExprs.push(subjectExpr);
		
		FrmLabel endLabel = FrmLabel.newLabel();
		endLabelStack.push(endLabel);
		
		for (AbsCaseStmt singleCase : acceptor.cases) {
			singleCase.accept(this);
			switchCode.stmts.add((ImcStmt) ImcDesc.getImcCode(singleCase));
		}
		
		if (acceptor.defaultBody != null) {
			acceptor.defaultBody.accept(this);
			switchCode.stmts.add((ImcStmt) ImcDesc.getImcCode(acceptor.defaultBody));
		}
		
		switchCode.stmts.add(new ImcLABEL(endLabel));
		endLabelStack.pop();
		ImcDesc.setImcCode(acceptor, switchCode);
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		ImcExpr caseCondition = null;
		
		for (AbsExpr e : acceptor.exprs) {
			e.accept(this);
			ImcExpr expr = (ImcExpr) ImcDesc.getImcCode(e);
			ImcExpr cond = new ImcBINOP(ImcBINOP.EQU, expr, switchSubjectExprs.peek());
			
			if (caseCondition == null)
				caseCondition = cond; 
			else
				caseCondition = new ImcBINOP(ImcBINOP.OR, caseCondition, cond);
		}

		FrmLabel startLabel = FrmLabel.newLabel();
		FrmLabel endLabel = FrmLabel.newLabel();
		
		acceptor.body.accept(this);
		ImcStmt body = (ImcStmt) ImcDesc.getImcCode(acceptor.body);
		
		ImcSEQ caseCode = new ImcSEQ();
		caseCode.stmts.add(new ImcCJUMP(caseCondition, startLabel, endLabel));
		caseCode.stmts.add(new ImcLABEL(startLabel));
		caseCode.stmts.add(body);
		caseCode.stmts.add(new ImcLABEL(endLabel));
		
		ImcDesc.setImcCode(acceptor, caseCode);
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		if (acceptor.type != null) {
			for (AbsDef d : acceptor.definitions)
				d.accept(this);
		}
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		if (acceptor.value != null) {
			acceptor.value.accept(this);
			ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(acceptor.value));
		}
	}

	@Override
	public void visit(AbsTupleDef acceptor) {
		// TODO Auto-generated method stub
	}

	@Override
	public void visit(AbsLabeledExpr acceptor) {
		acceptor.expr.accept(this);
		ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(acceptor.expr));
	}

	@Override
	public void visit(AbsTupleExpr acceptor) {
		TupleType tupleType = (TupleType) SymbDesc.getType(acceptor);
		int size = tupleType.size();

		ImcSEQ seq = new ImcSEQ();
		ImcTEMP location = new ImcTEMP(new FrmTemp());
		seq.stmts.add(new ImcMOVE(location, new ImcMALLOC(size)));
		
		for (AbsExpr e : acceptor.expressions.expressions) {
			e.accept(this);
			
			int memberOffset = tupleType.offsetOf(((AbsLabeledExpr) e).name);
			ImcExpr exprCode = (ImcExpr) ImcDesc.getImcCode(e);
			ImcExpr dst = new ImcMEM(
					new ImcBINOP(ImcBINOP.ADD, location, new ImcCONST(memberOffset)));
			ImcMOVE move = new ImcMOVE(dst, exprCode);
			seq.stmts.add(move);
		}
		
		ImcDesc.setImcCode(acceptor, new ImcESEQ(seq, location));
	}

	@Override
	public void visit(AbsOptionalType acceptor) {
		acceptor.childType.accept(this);
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr acceptor) {
		acceptor.subExpr.accept(this);
		
		ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(acceptor.subExpr));
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);

		ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(acceptor.subExpr));
	}
}
