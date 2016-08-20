package compiler.seman;

import java.util.Vector;

import compiler.Report;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsVarDef;
import compiler.abstr.tree.expr.AbsAtomConstExpr;
import compiler.abstr.tree.expr.AbsBinExpr;
import compiler.abstr.tree.expr.AbsExpr;
import compiler.abstr.tree.expr.AbsFunCall;
import compiler.abstr.tree.expr.AbsIfExpr;
import compiler.abstr.tree.expr.AbsListExpr;
import compiler.abstr.tree.expr.AbsReturnExpr;
import compiler.abstr.tree.expr.AbsUnExpr;
import compiler.abstr.tree.expr.AbsVarNameExpr;
import compiler.abstr.tree.stmt.AbsControlTransferStmt;
import compiler.abstr.tree.stmt.AbsFor;
import compiler.abstr.tree.stmt.AbsWhile;
import compiler.abstr.tree.type.AbsAtomType;
import compiler.abstr.tree.type.AbsFunType;
import compiler.abstr.tree.type.AbsListType;
import compiler.abstr.tree.type.AbsType;
import compiler.abstr.tree.type.AbsTypeName;
import compiler.frames.*;
import compiler.lexan.LexAn;
import compiler.synan.SynAn;
import compiler.seman.type.*;

/**
 * Preverjanje in razresevanje imen (razen imen komponent).
 * 
 * @implementation Toni Kocjan
 */
public class NameChecker implements ASTVisitor {
	
	public NameChecker() {
		try {
			{
				String name = "print";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				
				parTypes.add(new SemAtomType(AtomTypeEnum.INT));
				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeEnum.INT)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbTable.ins("print", print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomTypeEnum.DOB));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeEnum.DOB)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomTypeEnum.STR));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeEnum.INT)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomTypeEnum.CHR));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeEnum.CHR)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "print";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomTypeEnum.LOG));

				pars.add(new AbsParDef(null, "x", new AbsAtomType(null,
						AtomTypeEnum.LOG)));

				AbsFunDef print = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.VOID), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, print);
				SymbDesc.setType(print, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.VOID)));

				FrmFrame frame = new FrmFrame(print, 1);
				frame.numPars = 1;
				frame.sizePars = 4;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(print, frame);
			}
			{
				String name = "time";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				AbsFunDef time = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.INT), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, time);
				SymbTable.ins("time", time);
				SymbDesc.setType(time, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.INT)));

				FrmFrame frame = new FrmFrame(time, 1);
				frame.numPars = 0;
				frame.sizePars = 0;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(time, frame);
			}
			{
				String name = "rand";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				AbsFunDef rand = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.INT), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, rand);
				SymbTable.ins("rand", rand);
				SymbDesc.setType(rand, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.INT)));

				FrmFrame frame = new FrmFrame(rand, 1);
				frame.numPars = 0;
				frame.sizePars = 0;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(rand, frame);
			}
			{
				String name = "rand";
				Vector<AbsParDef> pars = new Vector<>();
				Vector<SemType> parTypes = new Vector<>();
				parTypes.add(new SemAtomType(AtomTypeEnum.INT));
				pars.add(new AbsParDef(null, "bound", new AbsAtomType(null,
						AtomTypeEnum.INT)));

				AbsFunDef rand = new AbsFunDef(null, name, pars,
						new AbsAtomType(null, AtomTypeEnum.INT), new AbsStmts(
								null, new Vector<>()));
				SymbTable.insFunc(name, parTypes, rand);
				SymbDesc.setType(rand, new SemFunType(parTypes,
						new SemAtomType(AtomTypeEnum.INT)));

				FrmFrame frame = new FrmFrame(rand, 1);
				frame.numPars = 0;
				frame.sizePars = 0;
				frame.label = FrmLabel.newLabel(name);
				FrmDesc.setFrame(rand, frame);
			}
		} catch (Exception e) {
		}
	}

	@Override
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsClassDef acceptor) {
		try {
			SymbTable.ins(acceptor.getName(), acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Invalid redeclaration of \'" + acceptor.getName()
					+ "\'");
		}
		SymbTable.newScope();
		acceptor.statements.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {

	}

	@Override
	public void visit(AbsAtomType acceptor) {

	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);

		if (acceptor.oper != AbsBinExpr.DOT) {
			acceptor.expr2.accept(this);
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (int def = 0; def < acceptor.numDefs(); def++)
			acceptor.def(def).accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int expr = 0; expr < acceptor.numExprs(); expr++)
			acceptor.expr(expr).accept(this);
	}

	@Override
	public void visit(AbsFor acceptor) {
		SymbTable.newScope();

		AbsVarDef var = new AbsVarDef(
				acceptor.iterator.position, acceptor.iterator.name, null, false);
		try {
			SymbTable.ins(acceptor.iterator.name, var);
			SymbDesc.setNameDef(acceptor.iterator, var);
		} catch (SemIllegalInsertException e) {
			Report.error("Error @ NameChecker::AbsFor");
		}
		acceptor.iterator.accept(this);
		acceptor.collection.accept(this);

		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		AbsDef definition = SymbTable.fnd(acceptor.name);
		if (definition == null)
			Report.error(acceptor.position, "Method " + acceptor.name
					+ " is undefined");
		
		SymbDesc.setNameDef(acceptor, definition);
		
		for (int arg = 0; arg < acceptor.numArgs(); arg++)
			acceptor.arg(arg).accept(this);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		try {
			SymbTable.ins(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			
		}
		
		SymbTable.newScope();

		for (int par = 0; par < acceptor.numPars(); par++)
			acceptor.par(par).accept(this);
		acceptor.type.accept(this);
		acceptor.func.accept(this);

		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsIfExpr acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);

			SymbTable.newScope();
			c.body.accept(this);
			SymbTable.oldScope();
		}

		if (acceptor.elseBody != null) {
			SymbTable.newScope();
			acceptor.elseBody.accept(this);
			SymbTable.oldScope();
		}
	}

	@Override
	public void visit(AbsParDef acceptor) {
		try {
			SymbTable.ins(acceptor.name, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate parameter \""
					+ acceptor.name + "\"");
		}
		acceptor.type.accept(this);
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		AbsDef definition = SymbTable.fnd(acceptor.name);

		if (definition == null)
			Report.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		try {
			SymbTable.ins(acceptor.name, acceptor);
			if (acceptor.type != null)
				acceptor.type.accept(this);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate variable \""
					+ acceptor.name + "\"");
		}
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
		AbsDef definition = SymbTable.fnd(acceptor.name);
		
		if (definition == null)
			Report.error(acceptor.position, "Use of unresolved indentifier \""
					+ acceptor.name + "\"");

		SymbDesc.setNameDef(acceptor, definition);
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);

		SymbTable.newScope();
		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsImportDef acceptor) {
		String tmp = Report.fileName;
		Report.fileName = acceptor.fileName;

		// parse the file
		// TODO hardcodano notr test/
		SynAn synAn = new SynAn(new LexAn("test/" + acceptor.fileName + ".ar",
				false), false);
		AbsStmts source = (AbsStmts) synAn.parse();

		Vector<AbsDef> definitions = new Vector<>();
		for (int i = 0; i < source.numStmts(); i++) {
			AbsStmt s = source.stmt(i);

			// skip statements which are not definitions
			if (!(s instanceof AbsDef))
				continue;

			AbsDef d = (AbsDef) s;

			if (acceptor.definitions.size() > 0) {
				String name = null;

				if (d instanceof AbsVarDef)
					name = ((AbsVarDef) d).name;
				if (d instanceof AbsFunDef)
					name = ((AbsFunDef) d).name;
				if (d instanceof AbsClassDef)
					name = ((AbsClassDef) d).getName();

				if (!acceptor.definitions.contains(name))
					continue;
			}
			definitions.add(d);
		}

		acceptor.imports = new AbsDefs(source.position, definitions);
		acceptor.imports.accept(this);

		Report.fileName = tmp;
	}

	@Override
	public void visit(AbsStmts stmts) {
		for (int stmt = 0; stmt < stmts.numStmts(); stmt++) {
			stmts.stmt(stmt).accept(this);
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		if (returnExpr.expr != null)
			returnExpr.expr.accept(this);
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		for (AbsExpr e : absListExpr.expressions)
			e.accept(this);
	}

	@Override
	public void visit(AbsFunType funType) {
		for (AbsType t : funType.parameterTypes)
			t.accept(this);
		funType.returnType.accept(this);
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

}
