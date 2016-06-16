package compiler.seman;

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
import compiler.seman.type.*;

/**
 * Preverjanje tipov.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class TypeChecker implements Visitor {

	/**
	 *  
	 *
	 */
	private enum TraversalState {
		ETS_imports, ETS_typeNames, ETS_typeDefs, ETS_variables, ETS_prototypes, ETS_functions
	}

	/**
	 * Current traversal state.
	 */
	TraversalState currentState;

	@Override
	public void visit(AbsArrType acceptor) {
		acceptor.type.accept(this);
		SemArrType type = new SemArrType(acceptor.length,
				SymbDesc.getType(acceptor.type));
		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsPtrType acceptor) {
		acceptor.type.accept(this);
		SemPtrType type = new SemPtrType(SymbDesc.getType(acceptor.type));
		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsStructType acceptor) {
		ArrayList<SemType> types = new ArrayList<>();
		ArrayList<String> names = new ArrayList<>();

		for (int i = 0; i < acceptor.getDefinitions().numDefs(); i++) {
			AbsDef def = acceptor.getDefinitions().def(i);
			if (def instanceof AbsVarDef) {
				AbsVarDef def_ = (AbsVarDef) def;
				def_.type.accept(this);
				SemType type = SymbDesc.getType(def_.type);

				types.add(type);
				names.add(def_.name);
			}
		}
		SymbDesc.setType(acceptor, new SemStructType(acceptor.getName(), names,
				types));
	}

	@Override
	public void visit(AbsAtomConst acceptor) {
		SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
	}

	@Override
	public void visit(AbsAtomType acceptor) {
		SymbDesc.setType(acceptor, new SemAtomType(acceptor.type));
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		acceptor.expr2.accept(this);

		SemType t1 = SymbDesc.getType(acceptor.expr1);
		SemType t2 = SymbDesc.getType(acceptor.expr2);

		SemType integer = new SemAtomType(SemAtomType.INT);
		SemType logical = new SemAtomType(SemAtomType.LOG);

		int oper = acceptor.oper;

		/**
		 * expr1[expr2]
		 */
		if (oper == AbsBinExpr.ARR) {
			if (!t2.sameStructureAs(integer))
				Report.error(acceptor.expr2.position,
						"Expected INTEGER type for array index");
			/**
			 * expr1 is of type ARR(n, t)
			 */
			if (t1 instanceof SemArrType) {
				SymbDesc.setType(acceptor, ((SemArrType) t1).type);
			} 
			else if (t1 instanceof SemPtrType) {
				if (t2.sameStructureAs(integer))
					SymbDesc.setType(acceptor, ((SemPtrType) t1).type);
			}
			else
				Report.error(acceptor.expr1.position,
						"Left side of ARR expression must be of type ARRAY");
			return;
		}

		/**
		 * expr1 = expr2
		 */
		if (oper == AbsBinExpr.ASSIGN) {
			if (t1.sameStructureAs(t2))
				SymbDesc.setType(acceptor, t1);
			else if (t1.sameStructureAs(t2))
				SymbDesc.setType(acceptor, t2);
			else
				Report.error(acceptor.position, "Cannot assign type " + t2
						+ " to type " + t1);
			return;
		}

		/**
		 * identifier.identifier
		 */
		if (oper == AbsBinExpr.DOT) {
			if (!(acceptor.expr1 instanceof AbsVarName && acceptor.expr2 instanceof AbsVarName))
				Report.error(acceptor.position,
						"Expressions around DOT operator must be identifiers");

			/**
			 * Handle array.length
			 */
			if (t1 instanceof SemArrType) {
				String name = ((AbsVarName) acceptor.expr2).name;
				if (!name.equals("length"))
					Report.error("Arrays have no attribute named \"" + name
							+ "\"");

				SymbDesc.setType(acceptor, integer);
				return;
			}

			if (!(t1 instanceof SemTypeName))
				Report.error(acceptor.position,
						"Left expression's type must be a structure to use DOT operator");

			SemTypeName typ_ = (SemTypeName) t1;

			if (!(typ_.getType() instanceof SemStructType))
				Report.error(acceptor.position,
						"Left expression's type must be a structure to use DOT operator");

			String name = ((AbsVarName) acceptor.expr2).name;
			SemStructType sType = (SemStructType) typ_.getType();
			SemType type = sType.getMembers().get(name);

			if (type == null)
				Report.error(acceptor.position, "\"" + name
						+ "\" is not defined in struct \"" + sType.getName()
						+ "\"");

			SymbDesc.setType(acceptor, type);
			return;
		}

		/**
		 * expr1 and expr2 are of type LOGICAL
		 */
		if (t1.sameStructureAs(logical) && t1.sameStructureAs(t2)) {
			// ==, !=, <=, >=, <, >, &, |
			if (oper >= 0 && oper <= 7)
				SymbDesc.setType(acceptor, logical);
			else
				Report.error(
						acceptor.position,
						"Numeric operations \"+\", \"-\", \"*\", \"/\" and \"%\" are undefined for type LOGICAL");
		}
		/**
		 * expr1 and expr2 are of type INTEGER
		 */
		else if (t1.sameStructureAs(integer) && t1.sameStructureAs(t2)) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12)
				SymbDesc.setType(acceptor, integer);
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7)
				SymbDesc.setType(acceptor, logical);
			else
				Report.error(acceptor.position,
						"Logical operations \"&\" and \"|\" are undefined for type INTEGER");
		} else {
			Report.error(acceptor.position, "No viable operation for types "
					+ t1 + " and " + t2);
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (TraversalState state : TraversalState.values()) {
			currentState = state;
			for (int def = 0; def < acceptor.numDefs(); def++)
				acceptor.def(def).accept(this);
		}
	}

	@Override
	public void visit(AbsExprs acceptor) {
		for (int expr = 0; expr < acceptor.numExprs(); expr++)
			acceptor.expr(expr).accept(this);

		SymbDesc.setType(acceptor,
				SymbDesc.getType(acceptor.expr(acceptor.numExprs() - 1)));
	}

	@Override
	public void visit(AbsFor acceptor) {
		acceptor.count.accept(this);
		acceptor.lo.accept(this);
		acceptor.hi.accept(this);
		acceptor.step.accept(this);
		acceptor.body.accept(this);

		SemType integer = new SemAtomType(SemAtomType.INT);
		SemType lo_ = SymbDesc.getType(acceptor.lo);
		SemType hi_ = SymbDesc.getType(acceptor.hi);
		SemType step_ = SymbDesc.getType(acceptor.step);

		if (lo_.sameStructureAs(integer) && lo_.sameStructureAs(hi_)
				&& lo_.sameStructureAs(step_))
			SymbDesc.setType(acceptor, new SemAtomType(SemAtomType.VOID));
		else
			Report.error(acceptor.position,
					"Lower bound, upper bound and step expressions must be of type INTEGER");
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		SemFunType type = (SemFunType) SymbDesc.getType(SymbDesc
				.getNameDef(acceptor));

		if (type.getNumPars() != acceptor.numArgs())
			Report.error(acceptor.position,
					"Number of arguments doesn't match for function \""
							+ acceptor.name + "\"");

		for (int arg = 0; arg < acceptor.numArgs(); arg++) {
			acceptor.arg(arg).accept(this);
			SemType parType = SymbDesc.getType(acceptor.arg(arg));
			
			if (parType instanceof SemArrType)
				parType = new SemPtrType(((SemArrType) parType).type);

			if (!type.getParType(arg).sameStructureAs(parType))
				Report.error(acceptor.arg(arg).position,
						"Parameter type doesn't match");
		}

		SymbDesc.setType(acceptor, type.resultType);
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		if (currentState == TraversalState.ETS_prototypes) {
			Vector<SemType> parameters = new Vector<>();
			for (int par = 0; par < acceptor.numPars(); par++) {
				acceptor.par(par).accept(this);
				parameters.add(SymbDesc.getType(acceptor.par(par)));
			}

			acceptor.type.accept(this);
			SymbDesc.setType(acceptor,
					new SemFunType(parameters, SymbDesc.getType(acceptor.type)));
		} else if (currentState == TraversalState.ETS_functions) {
			acceptor.expr.accept(this);
			SemType returnType = SymbDesc.getType(acceptor.expr);
			SemFunType funType = (SemFunType) SymbDesc.getType(acceptor);

			if (!returnType.sameStructureAs(funType.resultType))
				Report.error(acceptor.expr.position,
						"Return type doesn't match");
		}
	}

	@Override
	public void visit(AbsIfThen acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new SemAtomType(SemAtomType.LOG)))
			SymbDesc.setType(acceptor, new SemAtomType(SemAtomType.VOID));
		else
			Report.error(acceptor.cond.position,
					"Condition must be of type LOGICAL");
	}

	@Override
	public void visit(AbsIfThenElse acceptor) {
		acceptor.cond.accept(this);
		acceptor.thenBody.accept(this);
		acceptor.elseBody.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new SemAtomType(SemAtomType.LOG)))
			SymbDesc.setType(acceptor, new SemAtomType(SemAtomType.VOID));
		else
			Report.error(acceptor.cond.position,
					"Condition must be of type LOGICAL");
	}

	@Override
	public void visit(AbsPar acceptor) {
		acceptor.type.accept(this);
		SemType type = SymbDesc.getType(acceptor.type);

		if (type instanceof SemArrType)
			SymbDesc.setType(acceptor, new SemPtrType(((SemArrType) type).type));
		else
			SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsTypeDef acceptor) {
		if (currentState == TraversalState.ETS_typeNames)
			SymbDesc.setType(acceptor, new SemTypeName(acceptor.name));
		else if (currentState == TraversalState.ETS_typeDefs) {
			SemTypeName type = (SemTypeName) SymbDesc.getType(acceptor);
			acceptor.type.accept(this);
			type.setType(SymbDesc.getType(acceptor.type));
		}
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		AbsDef definition = SymbDesc.getNameDef(acceptor);
		if (!(definition instanceof AbsTypeDef))
			Report.error(acceptor.position, "Expected type definition");

		SemType type = SymbDesc.getType(definition);

		if (type == null)
			Report.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
		SemType type = SymbDesc.getType(acceptor.expr);

		if (acceptor.oper == AbsUnExpr.NOT) {
			if (type.sameStructureAs(new SemAtomType(SemAtomType.LOG)))
				SymbDesc.setType(acceptor, new SemAtomType(SemAtomType.LOG));
			else
				Report.error(acceptor.position,
						"Operator \"!\" is not defined for type " + type);
		} else if (acceptor.oper == AbsUnExpr.ADD
				|| acceptor.oper == AbsUnExpr.SUB) {
			if (type.sameStructureAs(new SemAtomType(SemAtomType.INT)))
				SymbDesc.setType(acceptor, new SemAtomType(SemAtomType.INT));
			else
				Report.error(acceptor.position,
						"Operators \"+\" and \"-\" are not defined for type "
								+ type);
		} else if (acceptor.oper == AbsUnExpr.MEM) {
			SymbDesc.setType(acceptor, new SemPtrType(type));
		} else if (acceptor.oper == AbsUnExpr.VAL) {
			if (!(type instanceof SemPtrType) && !(type instanceof SemArrType))
				Report.error(acceptor.position,
						"Error, operator \"*\" requires pointer operand");
			if (type instanceof SemPtrType)
				SymbDesc.setType(acceptor, ((SemPtrType) type).type);
			if (type instanceof SemArrType)
				SymbDesc.setType(acceptor, ((SemArrType) type).type);
		}
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		if (currentState == TraversalState.ETS_variables) {
			acceptor.type.accept(this);
			SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.type));
		}
	}

	@Override
	public void visit(AbsVarName acceptor) {
		SymbDesc.setType(acceptor,
				SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
	}

	@Override
	public void visit(AbsWhere acceptor) {
		acceptor.defs.accept(this);
		acceptor.expr.accept(this);
		SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.expr));
	}

	@Override
	public void visit(AbsWhile acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new SemAtomType(SemAtomType.LOG)))
			SymbDesc.setType(acceptor, new SemAtomType(SemAtomType.VOID));
		else
			Report.error(acceptor.cond.position,
					"Condition must be of type LOGICAL");
	}

	@Override
	public void visit(AbsImportDef importDef) {
		if (currentState == TraversalState.ETS_imports) {
			String tmp = Report.fileName;
			Report.fileName = importDef.fileName;

			for (TraversalState state : TraversalState.values()) {
				currentState = state;
				for (int def = 0; def < importDef.imports.numDefs(); def++)
					importDef.imports.def(def).accept(this);
			}

			currentState = TraversalState.ETS_imports;
			Report.fileName = tmp;
		}
	}

}
