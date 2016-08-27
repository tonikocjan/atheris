package compiler.imcode;

import java.util.*;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsVarDef;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsExpr;
import compiler.abstr.tree.expr.AbsFunCall;
import compiler.abstr.tree.expr.AbsListExpr;
import compiler.abstr.tree.expr.AbsReturnExpr;
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
import compiler.abstr.tree.type.AbsTypeName;
import compiler.frames.FrmAccess;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmFunAccess;
import compiler.frames.FrmLabel;
import compiler.frames.FrmLocAccess;
import compiler.frames.FrmMemberAccess;
import compiler.frames.FrmParAccess;
import compiler.frames.FrmTemp;
import compiler.frames.FrmVarAccess;
import compiler.seman.SymbDesc;
import compiler.seman.type.ArrayType;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;
import compiler.seman.type.EnumType;
import compiler.seman.type.PointerType;
import compiler.seman.type.Type;

public class ImcCodeGen implements ASTVisitor {

	/**
	 * Data and code chunks.
	 */
	public LinkedList<ImcChunk> chunks;
	
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

	}

	@Override
	public void visit(AbsClassDef acceptor) {
		ClassType type = (ClassType) ((CanType) SymbDesc.getType(acceptor)).childType;
		int size = type.size();
		
		for (AbsDef def : acceptor.definitions.definitions)
			def.accept(this);

		for (AbsFunDef c : acceptor.contrustors) {
			FrmFrame frame = FrmDesc.getFrame(c);
			ImcSEQ seq = new ImcSEQ();
			
			ImcTEMP location = new ImcTEMP(new FrmTemp());
			seq.stmts.add(new ImcMOVE(location, new ImcMALLOC(size)));

			for (AbsStmt s : c.func.statements) {
				if (!(s instanceof AbsBinExpr)) 
					Report.error("internal error");
				
				AbsBinExpr expr = (AbsBinExpr) s;
				AbsVarDef var = (AbsVarDef) SymbDesc.getNameDef(expr.expr1);
				int offset = type.offsetOf(var.name);
				
				expr.expr2.accept(this);
				ImcExpr code = (ImcExpr) ImcDesc.getImcCode(expr.expr2);
				
				ImcExpr dst = new ImcBINOP(ImcBINOP.ADD, location, new ImcCONST(offset));
				ImcMOVE move = new ImcMOVE(new ImcMEM(dst), code);
				seq.stmts.add(move);
			}

			seq.stmts.add(new ImcMOVE(new ImcTEMP(frame.RV), location));
			ImcDesc.setImcCode(c, seq);
			chunks.add(new ImcCodeChunk(frame, seq));
		}
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
		if (acceptor.type == AtomTypeKind.INT)
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(Integer.parseInt(acceptor.value)));
		else if (acceptor.type == AtomTypeKind.LOG)
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(acceptor.value.equals("true") ? 1 : 0));
		else if (acceptor.type == AtomTypeKind.STR) {
			FrmLabel l = FrmLabel.newLabel();
			ImcDataChunk str = new ImcDataChunk(l, 4);
			str.data = new String(acceptor.value.substring(1,
					acceptor.value.length() - 1)
					+ "\0");
			chunks.add(str);
			ImcDesc.setImcCode(acceptor, new ImcMEM(new ImcNAME(l)));
		} else if (acceptor.type == AtomTypeKind.CHR) {
			ImcDesc.setImcCode(acceptor, new ImcCONST(acceptor.value.charAt(0)));
		} else if (acceptor.type == AtomTypeKind.DOB) {
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(Double.parseDouble(acceptor.value)));
		} else if (acceptor.type == AtomTypeKind.NIL) {
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

		if (c1 instanceof ImcEXP)
			e1 = ((ImcEXP) c1).expr;
		else
			e1 = (ImcExpr) c1;

		if (c2 instanceof ImcESEQ)
			e2 = (ImcESEQ) c2;
		else if (c2 instanceof ImcEXP)
			e2 = ((ImcEXP) c2).expr;
		else
			e2 = (ImcExpr) c2;

		ImcCode code = null;

		if (acceptor.oper >= 0 && acceptor.oper <= 11)
			code = new ImcBINOP(acceptor.oper, e1, e2);
		else if (acceptor.oper == AbsBinExpr.ASSIGN) {
			code = new ImcMOVE(e1, e2);
		} else if (acceptor.oper == AbsBinExpr.ARR) {
			ArrayType type = (ArrayType) SymbDesc.getType(acceptor.expr1);
			int size = type.type.size();
			
			code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, new ImcBINOP(
					ImcBINOP.MUL, e2, new ImcCONST(size))));
		} else if (acceptor.oper == ImcBINOP.MOD) {
			ImcBINOP div = new ImcBINOP(ImcBINOP.DIV, e1, e2);
			ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, div, e2);
			ImcBINOP sub = new ImcBINOP(ImcBINOP.SUB, e1, mul);
			code = sub;
		} 
		else if (acceptor.oper == AbsBinExpr.DOT) {
			Type t = SymbDesc.getType(acceptor.expr1);

			if (t instanceof EnumType) {
				EnumType enumType = (EnumType) t;
				if (c2 == null)
					code = ImcDesc.getImcCode(enumType.getDefinitionForThisType());
				else
					code = c2;
			}
			else if (t instanceof ClassType) {
				code = c2;
				
				// member acces code
				if (!(acceptor.expr2 instanceof AbsFunCall))
					code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, e2));
			} 
			else if (t instanceof ArrayType) {
				code = new ImcCONST(((ArrayType) t).count);
			}
			else if (t instanceof CanType) {
				EnumType enumType = (EnumType) ((CanType) t).childType;
				AbsVarNameExpr memberName = (AbsVarNameExpr) acceptor.expr2;
				
				code = new ImcCONST(enumType.offsetForDefinitionName(memberName.name));
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
		if (acceptor.numExprs() == 0) {
			ImcDesc.setImcCode(acceptor, new ImcCONST(0));
			return;
		}

		ImcSEQ statements = new ImcSEQ();

		for (int i = 0; i < acceptor.numExprs(); i++) {
			acceptor.expr(i).accept(this);

			ImcCode code = ImcDesc.getImcCode(acceptor.expr(i));
			if (code instanceof ImcStmt)
				statements.stmts.add((ImcStmt) code);
			else
				statements.stmts.add(new ImcEXP((ImcExpr) code));
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
		if (type instanceof ArrayType) {
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

		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();

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
		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);

		FrmFrame frame = FrmDesc.getFrame(SymbDesc.getNameDef(acceptor));
		ImcCALL fnCall = new ImcCALL(frame.label);

		int diff = frameStack.peek().level - frame.level;
		if (diff == 0)
			fnCall.args.add(new ImcCONST(0xFFAAFF));
		else {
			ImcExpr SL = new ImcTEMP(frameStack.peek().FP);
			if (diff > 0)
				for (int i = 0; i < diff + 1; i++)
					SL = new ImcMEM(SL);

			fnCall.args.add(SL);
		}
		for (int i = 0; i < acceptor.numArgs(); i++) {
			ImcExpr e = (ImcExpr) ImcDesc.getImcCode(acceptor.arg(i));
			fnCall.args.add(e);
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
			FrmLabel l1 = FrmLabel.newLabel(),
					 l2 = FrmLabel.newLabel();
	
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
		FrmAccess x = FrmDesc.getAccess(acceptor);
		Type y = SymbDesc.getType(acceptor);
		int size = y.size();
		if (y instanceof PointerType)
			size = 4;

		if (x instanceof FrmVarAccess)
			chunks.add(new ImcDataChunk(((FrmVarAccess) x).label, size));
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
		FrmAccess access = FrmDesc.getAccess(SymbDesc.getNameDef(acceptor));
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
			
//			ImcExpr address = (ImcExpr) ImcDesc.getImcCode(member.memberDef.getParemtDefinition());
			ImcExpr offset = new ImcCONST(member.offsetForMember());
//			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, address, offset));
			expr = offset;
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
				// Jump to break label (end of the loop)
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
		
		acceptor.body.accept(this);
		
		ImcStmt body = (ImcStmt) ImcDesc.getImcCode(acceptor.body);
		
		
		FrmLabel startLabel = FrmLabel.newLabel();
		FrmLabel endLabel = FrmLabel.newLabel();
		
		ImcSEQ caseCode = new ImcSEQ();
		caseCode.stmts.add(new ImcCJUMP(caseCondition, startLabel, endLabel));
		caseCode.stmts.add(new ImcLABEL(startLabel));
		caseCode.stmts.add(body);
		caseCode.stmts.add(new ImcLABEL(endLabel));
		
		ImcDesc.setImcCode(acceptor, caseCode);
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		ImcSEQ enumCode = new ImcSEQ();
		
		if (acceptor.type != null) {
			int typeSize = SymbDesc.getType(acceptor.type).size();
			int offset = 0;
			
			ImcTEMP location = new ImcTEMP(new FrmTemp());
			enumCode.stmts.add(new ImcMOVE(location, new ImcMALLOC(typeSize * acceptor.definitions.size())));
			
			for (AbsDef d : acceptor.definitions) {
				d.accept(this);
				
				if (d instanceof AbsEnumMemberDef) {
					ImcExpr value = (ImcExpr) ImcDesc.getImcCode(d);
					ImcExpr dst = new ImcBINOP(ImcBINOP.ADD, location, new ImcCONST(offset));
					ImcMOVE move = new ImcMOVE(new ImcMEM(dst), value);
					enumCode.stmts.add(move);
					
					offset += typeSize;
				}
				else if (d instanceof AbsFunDef) {

				}
			}
		}
		
		ImcDesc.setImcCode(acceptor, enumCode);
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		if (acceptor.value != null) {
			acceptor.value.accept(this);
			ImcDesc.setImcCode(acceptor, ImcDesc.getImcCode(acceptor.value));
		}
	}

}
