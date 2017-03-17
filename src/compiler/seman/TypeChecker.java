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

package compiler.seman;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Vector;

import compiler.Report;
import compiler.abstr.ASTVisitor;
import compiler.abstr.tree.AbsDefs;
import compiler.abstr.tree.AbsExprs;
import compiler.abstr.tree.AbsStmt;
import compiler.abstr.tree.AbsStmts;
import compiler.abstr.tree.AtomTypeKind;
import compiler.abstr.tree.Condition;
import compiler.abstr.tree.VisibilityKind;
import compiler.abstr.tree.def.AbsClassDef;
import compiler.abstr.tree.def.AbsDef;
import compiler.abstr.tree.def.AbsEnumDef;
import compiler.abstr.tree.def.AbsEnumMemberDef;
import compiler.abstr.tree.def.AbsFunDef;
import compiler.abstr.tree.def.AbsImportDef;
import compiler.abstr.tree.def.AbsParDef;
import compiler.abstr.tree.def.AbsTupleDef;
import compiler.abstr.tree.def.AbsTypeDef;
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
import compiler.abstr.tree.type.AbsType;
import compiler.abstr.tree.type.AbsTypeName;
import compiler.seman.type.ArrayType;
import compiler.seman.type.AtomType;
import compiler.seman.type.CanType;
import compiler.seman.type.ClassType;
import compiler.seman.type.EnumType;
import compiler.seman.type.FunctionType;
import compiler.seman.type.OptionalType;
import compiler.seman.type.TupleType;
import compiler.seman.type.Type;
import managers.LanguageManager;

/**
 * Preverjanje tipov.
 * 
 * @author toni
 */
public class TypeChecker implements ASTVisitor {

	enum TraversalState {
		normal, definitions
	}

	/**
	 * Current state of traversal
	 */
	private TraversalState traversalState = TraversalState.normal;


	// MARK: - Methods
	@Override
	public void visit(AbsListType acceptor) {
		acceptor.type.accept(this);
		SymbDesc.setType(acceptor, new ArrayType(SymbDesc.getType(acceptor.type), 
				acceptor.count));
	}

	@Override
	public void visit(AbsClassDef acceptor) {
		ArrayList<Type> types = new ArrayList<>();
		ArrayList<String> names = new ArrayList<>();

		traversalState = TraversalState.definitions;

		for (AbsDef def : acceptor.definitions.definitions)
			def.accept(this);
		
		traversalState = TraversalState.normal;
		
		for (AbsFunDef c : acceptor.contrustors)
			c.accept(this);

		for (AbsDef def : acceptor.definitions.definitions) {
			types.add(SymbDesc.getType(def));
			names.add(def.name);
		}
		
		ClassType classType = new ClassType(acceptor, names, types);
		SymbDesc.setType(acceptor, new CanType(classType));
		
		for (AbsFunDef c : acceptor.contrustors)
			SymbDesc.setType(c, new FunctionType(new Vector<>(), classType, c));
		
		// add implicit self: classType parameter to instance methods
		for (AbsDef def : acceptor.definitions.definitions) {
			if (def instanceof AbsFunDef) {
				AbsFunDef funDef = (AbsFunDef) def;
				AbsParDef selfParDef = funDef.getParameterForIndex(0);
				selfParDef.type = new AbsTypeName(selfParDef.position, acceptor.name);
				
				SymbDesc.setNameDef(selfParDef.type, acceptor);
				SymbDesc.setType(selfParDef, classType);
			}
			
			def.accept(this);
		}
	}

	@Override
	public void visit(AbsAtomConstExpr acceptor) {
		SymbDesc.setType(acceptor, new AtomType(acceptor.type));
	}

	@Override
	public void visit(AbsAtomType acceptor) {
		SymbDesc.setType(acceptor, new AtomType(acceptor.type));
	}

	@Override
	public void visit(AbsBinExpr acceptor) {
		acceptor.expr1.accept(this);
		
		if (acceptor.oper != AbsBinExpr.DOT)
			acceptor.expr2.accept(this);

		Type t1 = SymbDesc.getType(acceptor.expr1);
		Type t2 = SymbDesc.getType(acceptor.expr2);

		int oper = acceptor.oper;

		/**
		 * expr1[expr2]
		 */
		if (oper == AbsBinExpr.ARR) {
			if (!t2.isBuiltinIntType())
				Report.error(acceptor.expr2.position,
						LanguageManager.localize("type_error_expected_int_for_subscript"));
			/**
			 * expr1 is of type ARR(n, t)
			 */
			if (t1.isArrayType()) {
				SymbDesc.setType(acceptor, ((ArrayType) t1).type);
			} else
				Report.error(acceptor.expr1.position,
						LanguageManager.localize("type_error_type_has_no_subscripts", 
								t1.toString()));
			return;
		}

		/**
		 * expr1 = expr2
		 */
		if (oper == AbsBinExpr.ASSIGN) {
			boolean success = false;
			
			// if left variable doesn't have type, assign right type
			if (t1 == null) {
				t1 = t2;
				SymbDesc.setType(acceptor.expr1, t1);
				SymbDesc.setType(SymbDesc.getNameDef(acceptor.expr1), t1);
				return;
			}
			
			// t1 and t2 are of same structure
			if (t1.sameStructureAs(t2)) {
				SymbDesc.setType(SymbDesc.getNameDef(acceptor.expr1), t2);
				SymbDesc.setType(acceptor.expr1, t2);
				SymbDesc.setType(acceptor, t2);
				success = true;
			}
			// t2 can be casted to t1
			else if (t2.canCastTo(t1)) {
				SymbDesc.setType(acceptor, t1);
				SymbDesc.setType(acceptor.expr2, t2);
				success = true;
			}
			// nil can be assigned to any pointer type
			else if (t2.isBuiltinNilType() && t1.isPointerType()) {
				SymbDesc.setType(acceptor.expr2, t1);
				SymbDesc.setType(acceptor, t1);
				success = true;
			}
			// optionals
			else if (t1.isOptionalType() && ((OptionalType) t1).childType.sameStructureAs(t2)) {
				SymbDesc.setType(acceptor, t1);
				success = true;
			}
			
			if (!success)
				Report.error(acceptor.position, 
						LanguageManager.localize("type_error_cannot_convert_type",
								t2.toString(), t1.toString()));
			
			return;
		}

		/**
		 * identifier.identifier
		 */
		if (oper == AbsBinExpr.DOT) {
			/**
			 * Handle list.length
			 */
			// FIXME:
			if (t1.isArrayType()) {
				String name = ((AbsVarNameExpr) acceptor.expr2).name;
				if (!name.equals("count"))
					Report.error("Lists have no attribute named \"" + name
							+ "\"");

				SymbDesc.setType(acceptor, Type.intType);
				return;
			}
			
			String memberName;
			if (acceptor.expr2 instanceof AbsVarNameExpr)
				memberName = ((AbsVarNameExpr) acceptor.expr2).name;
			else if (acceptor.expr2 instanceof AbsFunCall)
				memberName = ((AbsFunCall) acceptor.expr2).name;
			else
				memberName = ((AbsAtomConstExpr) acceptor.expr2).value;
			
			if (t1.isClassType()) {
				if (!t1.containsMember(memberName))
					Report.error(acceptor.expr2.position, 
							LanguageManager.localize("type_error_member_not_found", 
									t1.toString(), 
									memberName));
				
				if (acceptor.expr2 instanceof AbsFunCall) {
					AbsFunCall funCall = (AbsFunCall) acceptor.expr2;
					for (AbsExpr arg: funCall.args)
						arg.accept(this);
				}
				
				AbsDef definition = t1.findMemberForName(memberName);;
				
				if (definition.getVisibility() == VisibilityKind.Private)
					Report.error(acceptor.expr2.position,
							"Member '" + memberName + "' is private");
				
				SymbDesc.setNameDef(acceptor.expr2, definition);
				SymbDesc.setNameDef(acceptor, definition);
	
				Type memberType = ((ClassType) t1).getMemberTypeForName(memberName);
				
				SymbDesc.setType(acceptor.expr2, memberType);
				SymbDesc.setType(acceptor, memberType);
				
				return;
			}
			
			if (t1.isEnumType()) {
				EnumType enumType = (EnumType) t1;
				
				if (enumType.selectedMember == null) {
					if (!enumType.containsMember(memberName))
						Report.error(acceptor.expr2.position, 
								LanguageManager.localize("type_error_member_not_found", 
										enumType.friendlyName(), 
										memberName));
					
					AbsDef definition = enumType.findMemberForName(memberName);
					
					if (definition.getVisibility() == VisibilityKind.Private)
						Report.error(acceptor.expr2.position,
								"Member '" + memberName + "' is private");
					
					SymbDesc.setNameDef(acceptor.expr2, definition);
					SymbDesc.setNameDef(acceptor, definition);
		
					EnumType memberType = new EnumType(enumType, memberName);
					
					SymbDesc.setType(acceptor.expr2, memberType);
					SymbDesc.setType(acceptor, memberType);
				}
				else {
					ClassType memberType = enumType.getMemberTypeForName(enumType.selectedMember);
					
					if (!memberType.containsMember(memberName))
						Report.error(acceptor.expr2.position, 
								LanguageManager.localize("type_error_member_not_found", 
										t1.toString(), 
										memberName));
					
					Type memberRawValueType = memberType.getMemberTypeForName(memberName);

					SymbDesc.setType(acceptor.expr2, memberRawValueType);
					SymbDesc.setType(acceptor, memberRawValueType);
				}
				
				return;
			}
			
			if (t1.isTupleType()) {
				TupleType tupleType = (TupleType) t1;
				
				SymbDesc.setType(acceptor.expr2, tupleType.typeForName(memberName));
				SymbDesc.setType(acceptor, tupleType.typeForName(memberName));
			}
			
			return;
		}

		/**
		 * expr1 and expr2 are of type Bool
		 */
		if (t1.isBuiltinBoolType() && t2.isBuiltinBoolType()) {
			// ==, !=, <=, >=, <, >, &, |
			if (oper >= 0 && oper <= 7)
				SymbDesc.setType(acceptor, Type.boolType);
			else
				Report.error(
						acceptor.position,
						"Numeric operations \"+\", \"-\", \"*\", \"/\" and \"%\" are undefined for type Bool");
		}
		/**
		 * expr1 and expr2 are of type Int
		 */
		else if (t1.isBuiltinIntType() && t2.isBuiltinIntType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12)
				SymbDesc.setType(acceptor, Type.intType);
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7)
				SymbDesc.setType(acceptor, Type.boolType);
			else
				Report.error(acceptor.position,
						"Logical operations \"&\" and \"|\" are undefined for type Int");
		}
		/**
		 * expr1 and expr2 are of type Double
		 */
		else if (t1.isBuiltinDoubleType() && t2.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12)
				SymbDesc.setType(acceptor, Type.doubleType);
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7)
				SymbDesc.setType(acceptor, Type.boolType);
			else
				Report.error(acceptor.position,
						"Logical operations \"&\" and \"|\" are undefined for type Double");
		}
		/**
		 * expr1 or expr2 is Double and the other is Int (implicit cast to Double)
		 */
		// FIXME
		else if (t1.isBuiltinDoubleType() && t2.isBuiltinIntType()
				|| t1.isBuiltinType() && t2.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12)
				SymbDesc.setType(acceptor, Type.doubleType);
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7)
				SymbDesc.setType(acceptor, Type.boolType);
			else
				Report.error(acceptor.position,
						"Logical operations \"&\" and \"|\" are undefined for type Double");
		}
		
		/**
		 * Enumerations comparison.
		 */
		else if (t1.isEnumType() && t1.sameStructureAs(t2)) {
			if (oper == AbsBinExpr.EQU)
				SymbDesc.setType(acceptor, Type.boolType);
			else
				Report.error(acceptor.position,
						"Operator cannot be applied to operands of type \"" +
							t1.toString() + "\" and \"" + t2.toString() + "\"");
		}
		else {
			Report.error(acceptor.position, "No viable operation for types "
					+ t1 + " and " + t2);
		}
	}

	@Override
	public void visit(AbsDefs acceptor) {
		for (AbsDef def : acceptor.definitions)
			def.accept(this);
	}

	@Override
	public void visit(AbsExprs acceptor) {
		if (acceptor.expressions.size() == 0)
			SymbDesc.setType(acceptor, new AtomType(AtomTypeKind.VOID));
		else {
			for (AbsExpr e : acceptor.expressions)
				e.accept(this);
		}
	}

	@Override
	public void visit(AbsForStmt acceptor) {
		acceptor.collection.accept(this);
		Type type = ((ArrayType)SymbDesc.getType(acceptor.collection)).type;

		SymbDesc.setType(SymbDesc.getNameDef(acceptor.iterator), type);
		SymbDesc.setType(acceptor, new AtomType(AtomTypeKind.VOID));
		
		acceptor.iterator.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AbsFunCall acceptor) {
		Vector<Type> parameters = new Vector<>();
		for (AbsExpr arg: acceptor.args) {
			arg.accept(this);
			
			Type parType = SymbDesc.getType(arg);
			parameters.add(parType);
		}

		AbsDef def = SymbDesc.getNameDef(acceptor);
		
		if (def instanceof AbsVarDef || def instanceof AbsParDef || def instanceof AbsEnumMemberDef) {
			Type type = SymbDesc.getType(def);
			
			if (!type.isFunctionType())
				Report.error(acceptor.position, "Cannot call value of non-function type \'"
								+ type.toString() + "\'");
			
			FunctionType t = new FunctionType(parameters, 
					((FunctionType) type).resultType, ((FunctionType) type).functionDefinition);
			
			if (!type.sameStructureAs(t)) 
				Report.error("Error todo");
			
			SymbDesc.setNameDef(acceptor, def);
			SymbDesc.setType(acceptor, ((FunctionType) SymbDesc.getType(def)).resultType);
		}
		else {
			AbsFunDef definition = (AbsFunDef) SymbTable.fnd(acceptor.getStringRepresentation());
			
			if (definition == null) {
				Report.error(acceptor.position, "Method " + acceptor.name
						+ new FunctionType(parameters, null, null).toString()
						+ " is undefined");
			}
			
			SymbDesc.setNameDef(acceptor, definition);
			SymbDesc.setType(acceptor,
					((FunctionType) SymbDesc.getType(definition)).resultType);
		}
	}

	@Override
	public void visit(AbsFunDef acceptor) {
		if (traversalState == TraversalState.normal || traversalState == TraversalState.definitions) {
			Vector<Type> parameters = new Vector<>();

			for (AbsParDef par : acceptor.getParamaters()) {
				par.accept(this);
				parameters.add(SymbDesc.getType(par));
			}

			acceptor.type.accept(this);
			Type returnType = SymbDesc.getType(acceptor.type);

			FunctionType funType = new FunctionType(parameters, returnType, acceptor);
			SymbDesc.setType(acceptor, funType);
		}

		if (traversalState != TraversalState.definitions) {
			FunctionType funType = (FunctionType) SymbDesc.getType(acceptor);

			acceptor.func.accept(this);

			// check if return type matches
			for (AbsStmt stmt : acceptor.func.statements) {
				if (stmt instanceof AbsReturnExpr) {
					Type t = SymbDesc.getType(stmt);

					if (!t.sameStructureAs(funType.resultType))
						Report.error(stmt.position,
								"Return type doesn't match, expected \""
										+ funType.resultType.toString()
										+ "\", got \""
										+ t.toString()
										+ "\" instead");
				}
			}
		}
	}

	@Override
	public void visit(AbsIfStmt acceptor) {
		for (Condition c : acceptor.conditions) {
			c.cond.accept(this);
			c.body.accept(this);
			
			if (SymbDesc.getType(c.cond).sameStructureAs(
					new AtomType(AtomTypeKind.LOG)))
				SymbDesc.setType(acceptor, new AtomType(AtomTypeKind.VOID));
			else
				Report.error(c.cond.position,
						"Condition must be of type Bool");
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);
		}
	}

	@Override
	public void visit(AbsParDef acceptor) {
		acceptor.type.accept(this);
		
		Type type = SymbDesc.getType(acceptor.type);
		if (type.isCanType())
			type = ((CanType) type).childType;

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsTypeName acceptor) {
		AbsDef definition = SymbDesc.getNameDef(acceptor);
		
		if (!(definition instanceof AbsTypeDef))
			Report.error(acceptor.position, "Use of undeclared type \'" + definition.name + "\'");

		Type type = SymbDesc.getType(definition);

		if (type == null)
			Report.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsUnExpr acceptor) {
		acceptor.expr.accept(this);
		Type type = SymbDesc.getType(acceptor.expr);

		if (acceptor.oper == AbsUnExpr.NOT) {
			if (type.sameStructureAs(new AtomType(AtomTypeKind.LOG)))
				SymbDesc.setType(acceptor, new AtomType(AtomTypeKind.LOG));
			else
				Report.error(acceptor.position,
						"Operator \"!\" is not defined for type " + type);
		} else if (acceptor.oper == AbsUnExpr.ADD
				|| acceptor.oper == AbsUnExpr.SUB) {
			if (type.sameStructureAs(new AtomType(AtomTypeKind.INT)))
				SymbDesc.setType(acceptor, new AtomType(AtomTypeKind.INT));
			else
				Report.error(acceptor.position,
						"Operators \"+\" and \"-\" are not defined for type "
								+ type);
		}
	}

	@Override
	public void visit(AbsVarDef acceptor) {
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			Type type = SymbDesc.getType(acceptor.type);
			if (type.isCanType())
				type = ((CanType) type).childType;
			
			SymbDesc.setType(acceptor, type);
		}
	}

	@Override
	public void visit(AbsVarNameExpr acceptor) {
		SymbDesc.setType(acceptor,
				SymbDesc.getType(SymbDesc.getNameDef(acceptor)));
	}

	@Override
	public void visit(AbsWhileStmt acceptor) {
		acceptor.cond.accept(this);
		acceptor.body.accept(this);

		if (SymbDesc.getType(acceptor.cond).sameStructureAs(
				new AtomType(AtomTypeKind.LOG)))
			SymbDesc.setType(acceptor, new AtomType(AtomTypeKind.VOID));
		else
			Report.error(acceptor.cond.position,
					"Condition must be typed as Boolean");
	}

	@Override
	public void visit(AbsImportDef importDef) {
		String tmp = Report.fileName;
		Report.fileName = importDef.getName();

		for (AbsDef def : importDef.imports.definitions)
			def.accept(this);

		Report.fileName = tmp;
	}

	@Override
	public void visit(AbsStmts stmts) {
		for (AbsStmt s : stmts.statements) {
			s.accept(this);
		}
	}

	@Override
	public void visit(AbsReturnExpr returnExpr) {
		if (returnExpr.expr != null) {
			returnExpr.expr.accept(this);
			SymbDesc.setType(returnExpr, SymbDesc.getType(returnExpr.expr));
		} else
			SymbDesc.setType(returnExpr, new AtomType(AtomTypeKind.VOID));
	}

	@Override
	public void visit(AbsListExpr absListExpr) {
		Vector<Type> vec = new Vector<>();
		for (AbsExpr e : absListExpr.expressions) {
			e.accept(this);
			Type t = SymbDesc.getType(e);

			if (!vec.isEmpty() && !vec.firstElement().sameStructureAs(t))
				Report.error(e.position, "Error, invalid expression type");

			vec.add(SymbDesc.getType(e));
		}

		SymbDesc.setType(absListExpr, new ArrayType(vec.firstElement(), vec.size()));
	}

	@Override
	public void visit(AbsFunType funType) {
		Vector<Type> parameters = new Vector<>();
		for (AbsType t : funType.parameterTypes) {
			t.accept(this);
			parameters.add(SymbDesc.getType(t));
		}
		funType.returnType.accept(this);
		
		SymbDesc.setType(funType, new FunctionType(parameters, 
				SymbDesc.getType(funType.returnType), null));
	}

	@Override
	public void visit(AbsControlTransferStmt acceptor) {
		///
	}

	@Override
	public void visit(AbsSwitchStmt switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		Type switchType = SymbDesc.getType(switchStmt.subjectExpr);
		
		for (AbsCaseStmt singleCase : switchStmt.cases) {
			singleCase.accept(this);
			for (AbsExpr e : singleCase.exprs) {
				Type caseType = SymbDesc.getType(e);
				if (!caseType.sameStructureAs(switchType))
					Report.error(e.position, 
							"Expression of type \"" + caseType.toString() + 
							"\" cannot match values of type \"" + switchType.toString() +"\"");
			}
		}
		
		if (switchStmt.defaultBody != null)
			switchStmt.defaultBody.accept(this);
		
		SymbDesc.setType(switchStmt, new AtomType(AtomTypeKind.VOID));
	}

	@Override
	public void visit(AbsCaseStmt acceptor) {
		for (AbsExpr e : acceptor.exprs)
			e.accept(this);
		SymbTable.newScope();
		acceptor.body.accept(this);
		SymbTable.oldScope();
	}

	@Override
	public void visit(AbsEnumDef acceptor) {
		AtomType enumRawValueType = null;
		
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			enumRawValueType = (AtomType) SymbDesc.getType(acceptor.type);
		}
		
		String previousValue = null;
		int iterator = 0;
		
		ArrayList<String> names = new ArrayList<>(acceptor.definitions.size());
		ArrayList<ClassType> types = new ArrayList<>(acceptor.definitions.size());
		
		for (AbsDef def : acceptor.definitions) {
			if (def instanceof AbsEnumMemberDef) {
				AbsEnumMemberDef enumMemberDef = (AbsEnumMemberDef) def;
				
				if (enumRawValueType != null && enumMemberDef.value == null) {
					if (!enumRawValueType.isBuiltinStringType() && 
							!enumRawValueType.isBuiltinIntType())
						Report.error(enumMemberDef.position, 
								"Enum members require explicit raw values when "
								+ "the raw type is not Int or String literal");
					
					String value = null;
					
					if (enumRawValueType.type == AtomTypeKind.STR)
						value = "\"" + enumMemberDef.name.name + "\"";
					else if (enumRawValueType.type == AtomTypeKind.INT) {
						if (previousValue == null)
							value = "" + iterator;
						else
							value = "" + (Integer.parseInt(previousValue) + 1);
					}
					
					enumMemberDef.value = new AbsAtomConstExpr(enumMemberDef.position, enumRawValueType.type, value);
	
					previousValue = value;
					iterator++;
				}

				def.accept(this);
				ClassType defType = (ClassType) SymbDesc.getType(def);
				
				if (defType.containsMember("rawValue")) {
					Type rawValueType = defType.getMemberTypeForName("rawValue");
					
					if (enumRawValueType == null)
						Report.error(enumMemberDef.value.position, 
								"Enum member cannot have a raw value "
								+ "if the enum doesn't have a raw type");
					
					
					if (!rawValueType.sameStructureAs(enumRawValueType))
						Report.error(enumMemberDef.value.position, 
								"Cannot convert value of type \"" + 
								rawValueType.toString() + "\" to type \"" + 
								enumRawValueType.toString() + "\"");

					previousValue = enumMemberDef.value.value;
				}
			}
			else if (def instanceof AbsFunDef) {
//				FunctionType fnType = (FunctionType) SymbDesc.getType(def);
//				Vector<Type> parTypes = fnType.parameterTypes;
//				parTypes.add(0, enumType);
//				
//				FunctionType newFnType = new FunctionType(parTypes, 
//						fnType.resultType, (AbsFunDef) def);
//				
//				SymbDesc.setType(def, newFnType);
			}
			
			types.add((ClassType) SymbDesc.getType(def));
			names.add(def.getName());
		}
		
		SymbDesc.setType(acceptor, new EnumType(acceptor, names, types));
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		ArrayList<String> names = new ArrayList<>(1);
		ArrayList<Type> types = new ArrayList<>(1);
		LinkedList<AbsDef> definitions = new LinkedList<>();
		
		acceptor.name.accept(this);
		
		if (acceptor.value != null) {
			acceptor.value.accept(this);
			
			names.add("rawValue");
			types.add(SymbDesc.getType(acceptor.value));
			definitions.add(new AbsVarDef(acceptor.position, "rawValue", 
					new AbsAtomType(null, null)));
		}
		
		AbsClassDef classDef = new AbsClassDef(acceptor.getName(), acceptor.position, 
				 definitions, new LinkedList<>());
		ClassType type = new ClassType(classDef, names, types);
		SymbDesc.setType(acceptor, type);
	}

	@Override
	public void visit(AbsTupleDef acceptor) {
		LinkedList<Type> types = new LinkedList<>();
		LinkedList<String> names = new LinkedList<>();
		
		for (AbsDef def : acceptor.definitions.definitions) {
			def.accept(this);
			
			names.add(def.getName());
			types.add(SymbDesc.getType(def));
		}
		
		TupleType tupleType = new TupleType(acceptor, types, names);
		SymbDesc.setType(acceptor, tupleType);
	}

	@Override
	public void visit(AbsLabeledExpr acceptor) {
		acceptor.expr.accept(this);
		
		Type exprType = SymbDesc.getType(acceptor.expr);
		SymbDesc.setType(acceptor, exprType);
	}

	@Override
	public void visit(AbsTupleExpr acceptor) {
		acceptor.expressions.accept(this);
		
		LinkedList<Type> types = new LinkedList<>();
		LinkedList<String> names = new LinkedList<>();

		for (AbsExpr e : acceptor.expressions.expressions) {
			AbsLabeledExpr labeledExpr = (AbsLabeledExpr) e;
			
			types.add(SymbDesc.getType(labeledExpr));
			names.add(labeledExpr.name);
		}
		
		TupleType tupleType = new TupleType(types, names);
		SymbDesc.setType(acceptor, tupleType);
	}

	@Override
	public void visit(AbsOptionalType acceptor) {
		acceptor.childType.accept(this);
		
		Type childType = SymbDesc.getType(acceptor.childType);
		SymbDesc.setType(acceptor, new OptionalType(childType));
	}

	@Override
	public void visit(AbsOptionalEvaluationExpr acceptor) {
		acceptor.subExpr.accept(this);

		Type childType = SymbDesc.getType(acceptor.subExpr);
		SymbDesc.setType(acceptor, new OptionalType(childType));
	}

	@Override
	public void visit(AbsForceValueExpr acceptor) {
		acceptor.subExpr.accept(this);

		Type type = SymbDesc.getType(acceptor.subExpr);
		
		if (type.isOptionalType())
			SymbDesc.setType(acceptor, ((OptionalType) type).childType);
		else
			Report.error(acceptor.position, 
					"Cannot unwrap value of non-optional type '" + type.toString() + "'");
	}
}
