package compiler.imcode;

import java.util.*;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.frames.FrmAccess;
import compiler.frames.FrmDesc;
import compiler.frames.FrmFrame;
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

public class ImcCodeGen implements Visitor {

	public LinkedList<ImcChunk> chunks;
	public ImcCodeChunk entryPointCode = null;
	private FrmFrame currentFrame = null;

	public ImcCodeGen(FrmFrame entryPoint) {
		currentFrame = entryPoint;
		chunks = new LinkedList<ImcChunk>();
	}

	@Override
	public void visit(AbsListType acceptor) {

	}

	@Override
	public void visit(AbsClassDef acceptor) {
		int size = SymbDesc.getType(acceptor).size();

		for (AbsFunDef c : acceptor.contrustors) {
			c.accept(this);
			ImcSEQ code = (ImcSEQ) ImcDesc.getImcCode(c);

			FrmFrame frame = FrmDesc.getFrame(c);
			ImcSEQ seq = new ImcSEQ();
			if (code != null)
				seq.stmts.add(code);
			seq.stmts.add(new ImcMOVE(new ImcTEMP(frame.RV), new ImcMALLOC(size)));

			ImcDesc.setImcCode(c, seq);
			chunks.removeLast();
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
			SemClassType type = null;

			if (t instanceof SemClassType) {
				type = (SemClassType) t;
				String var = ((AbsVarName) acceptor.expr2).name;
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

		int diff = currentFrame.level - frame.level;
		if (diff == 0)
			fnCall.args.add(new ImcCONST(0xFFAAFF));
		else {
			ImcExpr SL = new ImcTEMP(currentFrame.FP);
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
		FrmFrame frame = FrmDesc.getFrame(acceptor);
		FrmFrame tmpFr = currentFrame;
		currentFrame = frame;

		acceptor.func.accept(this);
		ImcSEQ code = (ImcSEQ) ImcDesc.getImcCode(acceptor.func);
		code.stmts.add(new ImcLABEL(currentFrame.endLabel));

		chunks.add(new ImcCodeChunk(frame, code));
		currentFrame = tmpFr;
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);

		ImcCode thenBody = ImcDesc.getImcCode(acceptor.thenBody);
		ImcStmt expr = null;

		if (thenBody instanceof ImcExpr)
			expr = new ImcEXP((ImcExpr) thenBody);
		else
			expr = (ImcStmt) thenBody;

		ImcExpr cond = (ImcExpr) ImcDesc.getImcCode(acceptor.cond);
		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcCJUMP(cond, l1, l2));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(expr);
		statements.stmts.add(new ImcLABEL(l2));

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
		acceptor.elseBody.accept(this);

		ImcCode e1 = ImcDesc.getImcCode(acceptor.thenBody);
		ImcCode e2 = ImcDesc.getImcCode(acceptor.elseBody);

		ImcExpr cond = (ImcExpr) ImcDesc.getImcCode(acceptor.cond);
		ImcStmt expr1 = (e1 instanceof ImcStmt) ? (ImcStmt) e1 : new ImcEXP(
				(ImcExpr) e1);
		ImcStmt expr2 = (e2 instanceof ImcStmt) ? (ImcStmt) e2 : new ImcEXP(
				(ImcExpr) e2);

		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcCJUMP(cond, l1, l2));
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(expr1);
		statements.stmts.add(new ImcJUMP(l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add(expr2);
		statements.stmts.add(new ImcLABEL(l3));

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsPar acceptor) {

	}

	@Override
	public void visit(AbsTypeName acceptor) {

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
			int diff = currentFrame.level - loc.frame.level;

			ImcExpr fp = new ImcTEMP(currentFrame.FP);
			for (int i = 0; i < diff; i++)
				fp = new ImcMEM(fp);

			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(
					loc.offset)));
		} else if (access instanceof FrmParAccess) {
			FrmParAccess loc = (FrmParAccess) access;
			int diff = currentFrame.level - loc.frame.level;

			ImcExpr fp = new ImcTEMP(currentFrame.FP);
			for (int i = 0; i < diff; i++)
				fp = new ImcMEM(fp);

			expr = new ImcMEM(new ImcBINOP(ImcBINOP.ADD, fp, new ImcCONST(
					loc.offset)));
		} 
//		else if (access == null) {
//			FrmFrame frame = FrmDesc.getFrame(SymbDesc.getNameDef(acceptor));
//			expr = new ImcMEM(new ImcNAME(frame.label));
//		}

		ImcDesc.setImcCode(acceptor, expr);
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		FrmLabel l1 = FrmLabel.newLabel(), l2 = FrmLabel.newLabel(), l3 = FrmLabel
				.newLabel();

		ImcSEQ statements = new ImcSEQ();
		statements.stmts.add(new ImcLABEL(l1));
		statements.stmts.add(new ImcCJUMP((ImcExpr) ImcDesc
				.getImcCode(acceptor.cond), l2, l3));
		statements.stmts.add(new ImcLABEL(l2));
		statements.stmts.add((ImcStmt) ImcDesc.getImcCode(acceptor.body));
		statements.stmts.add(new ImcJUMP(l1));
		statements.stmts.add(new ImcLABEL(l3));

		ImcDesc.setImcCode(acceptor, statements);
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		acceptor.imports.accept(this);
	}

	int scope = 0;

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
		scope--;
		ImcDesc.setImcCode(acceptor, seq);

		if (scope == 0) {
			entryPointCode = new ImcCodeChunk(currentFrame, seq);
			chunks.add(entryPointCode);
		}
	}

	@Override
	public void visit(AbsReturnExpr acceptor) {
		ImcSEQ seq = new ImcSEQ();
		ImcTEMP rv = new ImcTEMP(currentFrame.RV);

		if (acceptor.expr != null) {
			acceptor.expr.accept(this);

			seq.stmts.add(new ImcMOVE(rv, (ImcExpr) ImcDesc
					.getImcCode(acceptor.expr)));
		} else
			ImcDesc.setImcCode(acceptor, new ImcRETURN(null));

		seq.stmts.add(new ImcJUMP(currentFrame.endLabel));
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
		
	}

}
