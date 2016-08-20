package compiler.imcode;

import java.util.*;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.frames.FrmAccess;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
import compiler.frames.FrmFunAccess;
import compiler.frames.FrmLabel;
import compiler.frames.FrmLocAccess;
import compiler.frames.FrmParAccess;
import compiler.frames.FrmTemp;
import compiler.frames.FrmVarAccess;
import compiler.seman.SymbDesc;
import compiler.seman.type.SemListType;
import compiler.seman.type.SemClassType;
import compiler.seman.type.SemPtrType;
import compiler.seman.type.SemType;

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
	 * Current frame (used for return expr). 
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
		SemClassType type = (SemClassType) SymbDesc.getType(acceptor);
		int size = type.size(); 

		for (AbsFunDef c : acceptor.contrustors) {
			FrmFrame frame = FrmDesc.getFrame(c);
			ImcSEQ seq = new ImcSEQ();
			
			ImcTEMP location = new ImcTEMP(new FrmTemp());
			seq.stmts.add(new ImcMOVE(location, new ImcMALLOC(size)));

			for (int i = c.func.numStmts() - 1; i >= 0; i--) {
				AbsStmt s = c.func.stmt(i);
				
				if (s instanceof AbsDef)
					break;
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
	public void visit(AbsAtomConst acceptor) {
		if (acceptor.type == AtomType.INT)
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(Integer.parseInt(acceptor.value)));
		else if (acceptor.type == AtomType.LOG)
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(acceptor.value.equals("true") ? 1 : 0));
		else if (acceptor.type == AtomType.STR) {
			FrmLabel l = FrmLabel.newLabel();
			ImcDataChunk str = new ImcDataChunk(l, 4);
			str.data = new String(acceptor.value.substring(1,
					acceptor.value.length() - 1)
					+ "\0");
			chunks.add(str);
			ImcDesc.setImcCode(acceptor, new ImcMEM(new ImcNAME(l)));
		} else if (acceptor.type == AtomType.CHR) {
			ImcDesc.setImcCode(acceptor, new ImcCONST(acceptor.value.charAt(0)));
		} else if (acceptor.type == AtomType.DOB) {
			ImcDesc.setImcCode(acceptor,
					new ImcCONST(Double.parseDouble(acceptor.value)));
		} else if (acceptor.type == AtomType.NIL) {
			ImcDesc.setImcCode(acceptor, new ImcCONST(0));
		}
	}

	@Override
	public void visit(AbsAtomType acceptor) {

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
			SemListType type = (SemListType) SymbDesc.getType(acceptor.expr1);
			int size = type.type.size();
			
			code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, e1, new ImcBINOP(
					ImcBINOP.MUL, e2, new ImcCONST(size))));
		} else if (acceptor.oper == ImcBINOP.MOD) {
			ImcBINOP div = new ImcBINOP(ImcBINOP.DIV, e1, e2);
			ImcBINOP mul = new ImcBINOP(ImcBINOP.MUL, div, e2);
			ImcBINOP sub = new ImcBINOP(ImcBINOP.SUB, e1, mul);
			code = sub;
		} else if (acceptor.oper == AbsBinExpr.DOT) {
			SemType t = SymbDesc.getType(acceptor.expr1).actualType();

			if (t instanceof SemClassType) {
				SemClassType type  = (SemClassType) t;
				String var;
				
				if (acceptor.expr2 instanceof AbsVarName)
					var = ((AbsVarName) acceptor.expr2).name;
				else
					var = ((AbsFunCall) acceptor.expr2).name;
				
				int offset = type.offsetOf(var);

				code = new ImcMEM(new ImcBINOP(ImcBINOP.ADD,
						e1, new ImcCONST(offset)));
			} else if (t instanceof SemListType) {
				code = new ImcCONST(((SemListType) t).count);
			}
		}

		ImcDesc.setImcCode(acceptor, code);
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int i = 0; i < acceptor.numDefs(); i++)
			acceptor.def(i).accept(this);
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
	public void visit(AbsFor acceptor) {
		SymbDesc.getNameDef(acceptor.iterator).accept(this);
		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);
		acceptor.body.accept(this);

		ImcSEQ bodyCode = (ImcSEQ) ImcDesc.getImcCode(acceptor.body);
		SemType type = SymbDesc.getType(acceptor.collection);
		int count = 0;
		if (type instanceof SemListType) {
			count = ((SemListType) type).count;
			type = ((SemListType) type).type;
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
	public void visit(AbsIfExpr acceptor) {
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
	public void visit(AbsPar acceptor) {
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
		SemType y = SymbDesc.getType(acceptor);
		int size = y.size();
		if (y instanceof SemPtrType)
			size = 4;

		if (x instanceof FrmVarAccess)
			chunks.add(new ImcDataChunk(((FrmVarAccess) x).label, size));
	}

	@Override
	public void visit(AbsVarName acceptor) {
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
		} else if (access instanceof FrmParAccess) {
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
	public void visit(AbsWhile acceptor) {
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
		for (int stmt = 0; stmt < acceptor.numStmts(); stmt++) {
			acceptor.stmt(stmt).accept(this);

			ImcCode code = ImcDesc.getImcCode(acceptor.stmt(stmt));
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
		int size = ((SemListType) SymbDesc.getType(acceptor)).count;
		int elSize = ((SemListType) SymbDesc.getType(acceptor)).type.size();

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
		if (startLabelStack.isEmpty() || endLabelStack.isEmpty())
			return;
		
		if (acceptor.control == ControlTransferEnum.Continue)
			// Jump to continue label (beginning of the loop)
			ImcDesc.setImcCode(acceptor, new ImcJUMP(startLabelStack.peek()));
		else
			// Jump to break label (end of the loop)
			ImcDesc.setImcCode(acceptor, new ImcJUMP(endLabelStack.peek()));
	}

}
