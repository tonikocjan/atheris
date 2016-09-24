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

import java.util.*;

import compiler.*;
import compiler.abstr.*;
import compiler.abstr.tree.*;
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
import compiler.abstr.tree.expr.AbsFunCall;
import compiler.abstr.tree.expr.AbsLabeledExpr;
import compiler.abstr.tree.expr.AbsListExpr;
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
import compiler.abstr.tree.type.AbsType;
import compiler.abstr.tree.type.AbsTypeName;
import compiler.seman.type.*;

/**
 * Preverjanje tipov.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class BasicTypeChecker implements ASTVisitor {

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
		
		// TODO: handle implicit self parameter
		
		for (AbsDef def : acceptor.definitions.definitions)
			def.accept(this);

		for (AbsFunDef c : acceptor.contrustors)
			c.accept(this);

		for (AbsDef def : acceptor.definitions.definitions) {
			Type memberType = SymbDesc.getType(def);
			types.add(memberType);
			
			if (def instanceof AbsVarDef)
				names.add(((AbsVarDef) def).name);
			else if (def instanceof AbsFunDef)
				names.add(((AbsFunDef) def).name);
			else
				Report.error("Semantic error @ AbsClassDef-typeChecker");
		}
		
		ClassType classType = new ClassType(acceptor, names, types);
		SymbDesc.setType(acceptor, new CanType(classType));
		
		for (AbsFunDef c : acceptor.contrustors)
			SymbDesc.setType(c, new FunctionType(new Vector<>(), classType, c));
		
		// add implicit self: classType parameter to instance methods
		for (AbsDef def : acceptor.definitions.definitions) {
			if (def instanceof AbsFunDef) {
				AbsFunDef funDef = (AbsFunDef) def;
				SymbDesc.setType(funDef.getParameterForIndex(0), classType);
				def.accept(this);
			}
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
						"Expected Int type for array index");
			/**
			 * expr1 is of type ARR(n, t)
			 */
			if (t1.isArrayType()) {
				SymbDesc.setType(acceptor, ((ArrayType) t1).type);
			} else
				Report.error(acceptor.expr1.position,
						"Type \'" + t1 + "\' has no subscript members");
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
			
			if (t1.sameStructureAs(t2)) {
				SymbDesc.setType(SymbDesc.getNameDef(acceptor.expr1), t2);
				SymbDesc.setType(acceptor.expr1, t2);
				SymbDesc.setType(acceptor, t2);
				success = true;
			}
			else if (t2.canCastTo(t1)) {
				SymbDesc.setType(acceptor, t1);
				SymbDesc.setType(acceptor.expr2, t2);
				success = true;
			}
			else if (t2.isBuiltinNilType() && t1.isPointerType()) {
				SymbDesc.setType(acceptor.expr2, t1);
				SymbDesc.setType(acceptor, t1);
				success = true;
			}
			
			if (!success)
				Report.error(acceptor.position, "Cannot convert valueof type " + t2
						+ " to type " + t1);
			
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

			if (!t1.containsMember(memberName))
				Report.error(acceptor.expr2.position, 
						"Value of type \"" + t1.toString() + 
						"\" has no member \"" + memberName + "\"");
			
			if (t1.isClassType()) {
				ClassType classType = (ClassType) t1;
	
				AbsDef definition = classType.findMemberForName(memberName);
				
				if (definition.visibilityKind == VisibilityKind.Private)
					Report.error(acceptor.expr2.position,
							"Member '" + memberName + "' is private");
				
				SymbDesc.setNameDef(acceptor.expr2, definition);
				SymbDesc.setNameDef(acceptor, definition);
	
				Type type;
				if (acceptor.expr2 instanceof AbsFunCall) {
					acceptor.expr2.accept(this);
					type = SymbDesc.getType(acceptor.expr2);
				}
				else {
					type = classType.getMembers().get(memberName);
					SymbDesc.setType(acceptor.expr2, type);
				}
	
				SymbDesc.setType(acceptor, type);
				return;
			}
			
			if (t1.isCanType()) {
				EnumType enumType = (EnumType) ((CanType) t1).childType;

				AbsDef definition = enumType.findMemberForName(memberName);
				SymbDesc.setNameDef(acceptor.expr2, definition);
				
				if (acceptor.expr2 instanceof AbsFunCall)
					acceptor.expr2.accept(this);
				
				EnumType newEnumType = new EnumType(enumType.enumDefinition);
				newEnumType.setDefinitionForThisType(memberName);
				
				SymbDesc.setType(acceptor.expr2, enumType);
				SymbDesc.setType(acceptor, newEnumType);

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
		
		if (def instanceof AbsVarDef || def instanceof AbsParDef || 
				def instanceof AbsEnumMemberDef) {
			Type type = SymbDesc.getType(def);
			
			if (!type.isFunctionType())
				Report.error(acceptor.position, "Cannot call value of non-function type \'"
								+ type.toString() + "\'");
			
			FunctionType t = new FunctionType(parameters, 
					((FunctionType)type).resultType, ((FunctionType) type).functionDefinition);
			
			if (!type.sameStructureAs(t)) 
				Report.error("Error todo");
			
			SymbDesc.setNameDef(acceptor, def);
			SymbDesc.setType(acceptor, ((FunctionType) SymbDesc.getType(def)).resultType);
		}
		else {
			AbsFunDef definition = (AbsFunDef) SymbTable.fndFunc(acceptor.name, parameters);
			
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
		Vector<Type> parameters = new Vector<>();
		
		for (AbsParDef par : acceptor.getParamaters()) {
			par.accept(this);
			parameters.add(SymbDesc.getType(par));
		}

		acceptor.type.accept(this);
		Type returnType = SymbDesc.getType(acceptor.type);

		FunctionType funType = new FunctionType(parameters, returnType, acceptor);
		SymbDesc.setType(acceptor, funType);

		// insert function into symbol table
		try {
			SymbTable.insFunc(acceptor.name, parameters, acceptor);
		} catch (SemIllegalInsertException e) {
			Report.error(acceptor.position, "Duplicate method \""
					+ acceptor.name + "\"");
		}
		
		acceptor.func.accept(this);

		// check if return type matches
		for (AbsStmt stmt : acceptor.func.statements) {
			if (stmt instanceof AbsReturnExpr) {
				Type t = SymbDesc.getType(stmt);
				if (!t.sameStructureAs(funType.resultType))
					Report.error(stmt.position,
							"Return type doesn't match, expected \""
									+ funType.resultType.actualType()
											.toString() + "\", got \""
									+ t.actualType().toString() + "\" instead");
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
			Report.error(acceptor.position, "Expected type definition");

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
		EnumType enumType = new EnumType(acceptor);
		AtomType enumRawValueType = null;
		
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			enumRawValueType = (AtomType) SymbDesc.getType(acceptor.type);
		}
		
		String previousValue = null;
		int iterator = 0;
		
		for (AbsDef def : acceptor.definitions) {
			def.accept(this);

			if (def instanceof AbsEnumMemberDef) {
				AbsEnumMemberDef enumMemberDef = (AbsEnumMemberDef) def;
				SymbDesc.setType(enumMemberDef, enumType);
				
				if (enumMemberDef.value != null) {
					if (enumRawValueType == null)
						Report.error(enumMemberDef.value.position, "Enum member cannot have a raw value "
								+ "if the enum doesn't have a raw type");
					
					Type rawValueType = SymbDesc.getType(enumMemberDef.value);
					if (!rawValueType.sameStructureAs(enumRawValueType))
						Report.error(enumMemberDef.value.position, "Cannot convert value of type \"" + 
								rawValueType.toString() + "\" to type \"" + enumRawValueType.toString() + "\"");

					SymbDesc.setType(enumMemberDef, enumType);
					previousValue = enumMemberDef.value.value;
				}
				else if (enumRawValueType != null) {
					if (enumRawValueType.type != AtomTypeKind.STR &&
							enumRawValueType.type != AtomTypeKind.INT)
						Report.error(enumMemberDef.position, "Enum members require explicit raw values when the raw type is not integer or string literal");
					
					String value = null;
					
					if (enumRawValueType.type == AtomTypeKind.STR)
						value = enumMemberDef.name.name;
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
			}
			else if (def instanceof AbsFunDef) {
				FunctionType fnType = (FunctionType) SymbDesc.getType(def);
				Vector<Type> parTypes = fnType.parameterTypes;
				parTypes.add(0, enumType);
				
				FunctionType newFnType = new FunctionType(parTypes, 
						fnType.resultType, (AbsFunDef) def);
				SymbDesc.setType(def, newFnType);
			}
		}
		
		SymbDesc.setType(acceptor, new CanType(enumType));
	}

	@Override
	public void visit(AbsEnumMemberDef acceptor) {
		acceptor.name.accept(this);
		
		if (acceptor.value != null) {
			acceptor.value.accept(this);
		}
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
		
		SymbDesc.setType(acceptor, SymbDesc.getType(acceptor.expr));
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
}
