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

import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import compiler.ast.tree.enums.AtomTypeKind;
import compiler.ast.tree.enums.DefinitionModifier;
import compiler.ast.tree.expr.*;
import compiler.ast.tree.stmt.*;
import compiler.ast.tree.type.*;
import utils.Constants;
import compiler.ast.ASTVisitor;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AstAtomConstExpression;
import compiler.ast.tree.stmt.AstCaseStatement;
import compiler.ast.tree.stmt.AstWhileStatement;
import compiler.ast.tree.type.AstAtomType;
import compiler.seman.type.*;
import managers.LanguageManager;

public class TypeChecker implements ASTVisitor {

    private static LoggerInterface logger = LoggerFactory.logger();

    private SymbolTableMap symbolTable;
    private SymbolDescriptionMap symbolDescription;
    private TraversalStates traversalState = TraversalStates.extensions;
    private boolean isRootNode = true;
    private boolean assigningToVariable = false;
    private Stack<Type> lhsTypes = new Stack<>();
    private Stack<CanType> declarationContext = new Stack<>();
    private Stack<Type> functionReturnTypes = new Stack<>();
    private boolean resolveTypeOnly = false;
    private boolean typeCheckingClass = false;

    private enum TraversalStates {
        extensions, normal
    }

    public TypeChecker(SymbolTableMap symbolTable, SymbolDescriptionMap symbolDescription) {
        this.symbolTable = symbolTable;
        this.symbolDescription = symbolDescription;
    }

	@Override
	public void visit(AstListType acceptor) {
		acceptor.type.accept(this);

		Type type = symbolDescription.getTypeForAstNode(acceptor.type);
		if (type.isCanType()) {
		    type = ((CanType) type).childType;
        }

		symbolDescription.setTypeForAstNode(acceptor, new ArrayType(type, acceptor.elementCount));
	}

	@Override
	public void visit(AstClassDefinition acceptor) {
        typeCheckingClass = true;
	    if (traversalState == TraversalStates.extensions) {
            ArrayList<Type> types = new ArrayList<>();
            ArrayList<String> names = new ArrayList<>();

            ArrayList<Type> staticTypes = new ArrayList<>();
            ArrayList<String> staticNames = new ArrayList<>();
            ArrayList<AstDefinition> staticDefinitions = new ArrayList<>();

            CanType baseClass = null;

            // check whether inheritance is legal
            if (acceptor.baseClass != null) {
                acceptor.baseClass.accept(this);

                Type type = symbolDescription.getTypeForAstNode(acceptor.baseClass);

                if (acceptor instanceof AstStructureDefinition) {
                    // only classes are allowed to inherit
                    logger.error("Structs are not allowed to inherit");
                }

                if (!type.isInterfaceType() && (!type.isCanType() || !((CanType) type).childType.isClassType())) {
                    logger.error(acceptor.baseClass.position,
                            "Inheritance from non-class memberType \"" + type.friendlyName() + "\" is not allowed");
                }

                if (type.isInterfaceType()) {
                    acceptor.conformances.add(0, acceptor.baseClass);
                }
                else {
                    baseClass = (CanType) type;
                }
            }

            // check whether conformance is legal
            for (AstType conformance : acceptor.conformances) {
                conformance.accept(this);

                if (!symbolDescription.getTypeForAstNode(conformance).isInterfaceType()) {
                    if (baseClass != null) {
                        logger.error(conformance.position, "Multiple inheritance is not allowed");
                    }
                    else {
                        logger.error(conformance.position, "Super class must appear first in inheritance clause");
                    }
                }
            }

            // type inference
            for (AstStatement stmt: acceptor.defaultConstructor.functionCode.statements) {
                AstBinaryExpression initExpr = (AstBinaryExpression) stmt;
                initExpr.expr2.accept(this);
                Type type = symbolDescription.getTypeForAstNode(initExpr.expr2);

                AstVariableDefinition definition = (AstVariableDefinition) acceptor.memberDefinitions.findDefinitionForName(((AstVariableNameExpression) ((AstBinaryExpression)initExpr.expr1).expr2).name);
                symbolDescription.setTypeForAstNode(definition, type);
            }

            resolveTypeOnly = true;

            for (AstDefinition def : acceptor.memberDefinitions.definitions) {
                if (names.contains(def.getName())) {
                    logger.error(def.position, "Invalid redeclaration of \"" + def.getName() + "\"");
                }
                else if (baseClass != null) {
                    // check whether inheritance (overriding) is legal
                    if (def.isOverriding()) {
                        AstDefinition baseDefinition = baseClass.childType.findMemberDefinitionWithName(def.getName());

                        if (baseDefinition == null || baseDefinition.isStatic()) {
                            logger.error(def.position, "Method does not override any method from it's super class");
                        }
                        else if (baseDefinition.isFinal()) {
                            logger.error(def.position, "Cannot override \"final\" instance method");
                        }
                    }
                    else if (baseClass.containsMember(def.getName())) {
                        logger.error(def.position, "Invalid redeclaration of \"" + def.getName() + "\"");
                    }
                }
                else if (def.isOverriding()) {
                    logger.error(def.position, "Method does not override any method from it's super class");
                }

                def.accept(this);

                if (def.isStatic()) {
                    staticNames.add(def.getName());
                    staticDefinitions.add(def);
                }
                else {
                    names.add(def.getName());
                }
            }

            for (AstFunctionDefinition c : acceptor.construstors) {
                c.accept(this);
            }

            for (AstDefinition def : acceptor.memberDefinitions.definitions) {
                def.accept(this);

                if (def.isStatic()) {
                    staticTypes.add(symbolDescription.getTypeForAstNode(def));
                }
                else {
                    types.add(symbolDescription.getTypeForAstNode(def));
                }
            }

            ObjectType type;
            if (acceptor instanceof AstStructureDefinition) {
                type = new StructType(acceptor, names, types);
            }
            else {
                type = new ClassType(acceptor, names, types, baseClass);
            }

            CanType canType = new CanType(type);
            symbolDescription.setTypeForAstNode(acceptor, canType);

            // add static member memberDefinitions to canType
            Iterator<String> namesIterator = staticNames.iterator();
            Iterator<Type> typesIterator = staticTypes.iterator();
            Iterator<AstDefinition> defsIterator = staticDefinitions.iterator();

            while (namesIterator.hasNext()) {
                canType.addStaticDefinition(defsIterator.next(), namesIterator.next(), typesIterator.next());
            }
        }
        else {
            resolveTypeOnly = false;

            CanType staticType = (CanType) symbolDescription.getTypeForAstNode(acceptor);
            ObjectType objectType = (ObjectType) staticType.childType;
	        CanType baseClass = objectType.baseClass;

	        declarationContext.push(staticType);

            AstFunctionDefinition baseClassDefaultConstructor = null;
            if (baseClass != null) {
                baseClassDefaultConstructor = ((ClassType) baseClass.childType).classDefinition.defaultConstructor;
            }

            // check if all methods in conforming interfaces are implemented
            for (AstType conformance : acceptor.conformances) {
                InterfaceType interfaceType = (InterfaceType) symbolDescription.getTypeForAstNode(conformance);

                if (!objectType.conformsTo(interfaceType)) {
                    logger.error(conformance.position, "Type \"" + objectType.friendlyName() + "\" does not conform to interface \"" + interfaceType.friendlyName() + "\"");
                }
            }

            // add implicit "self: classType" parameter to constructors
            for (AstFunctionDefinition constructor : acceptor.construstors) {
                AstParameterDefinition selfParDef = constructor.getParameterAtIndex(0);
                selfParDef.type = new AstTypeName(selfParDef.position, acceptor.name);

                symbolDescription.setDefinitionForAstNode(selfParDef.type, acceptor);
                symbolDescription.setTypeForAstNode(selfParDef, objectType);

                // append base classes' initialization code
                if (baseClassDefaultConstructor != null) {
                    constructor.functionCode.statements.addAll(0, baseClassDefaultConstructor.functionCode.statements);
                }

                constructor.accept(this);

                FunctionType funType = (FunctionType) symbolDescription.getTypeForAstNode(constructor);
                funType.resultType = objectType;

                symbolDescription.setTypeForAstNode(constructor, funType);
            }

            for (AstDefinition def : acceptor.memberDefinitions.definitions) {
                if (!def.isStatic() && def instanceof AstFunctionDefinition) {
                    // add implicit "self: classType" parameter to instance methods
                    AstFunctionDefinition funDef = (AstFunctionDefinition) def;
                    AstParameterDefinition selfParDef = funDef.getParameterAtIndex(0);
                    selfParDef.type = new AstTypeName(selfParDef.position, acceptor.name);

                    symbolDescription.setDefinitionForAstNode(selfParDef.type, acceptor);
                }

                def.accept(this);

                // push constructors from nested class definition
                if (def instanceof AstClassDefinition) {
                    CanType context = declarationContext.peek();
                    if (context != null) {
                        AstClassDefinition classDef = (AstClassDefinition) def;
                        String className = classDef.name;
                        classDef.name = context.childType.friendlyName() + "." + classDef.getName();

                        for (AstFunctionDefinition constructor: classDef.construstors) {
                            context.addStaticMember(
                                    constructor,
                                    constructor.getStringRepresentation(className),
                                    symbolDescription.getTypeForAstNode(constructor));

                        }
                    }
                }
            }

            declarationContext.pop();

//            objectType.debugPrint();
        }
        typeCheckingClass = false;
	}

	@Override
	public void visit(AstAtomConstExpression acceptor) {
		symbolDescription.setTypeForAstNode(acceptor, Type.atomType(acceptor.type));
	}

	@Override
	public void visit(AstAtomType acceptor) {
		symbolDescription.setTypeForAstNode(acceptor, Type.atomType(acceptor.type));
	}

	@Override
	public void visit(AstBinaryExpression acceptor) {
	    assigningToVariable = acceptor.oper == AstBinaryExpression.ASSIGN;

		acceptor.expr1.accept(this);
        Type lhs = symbolDescription.getTypeForAstNode(acceptor.expr1);

		lhsTypes.push(lhs);
		
		if (acceptor.oper != AstBinaryExpression.DOT)
			acceptor.expr2.accept(this);

        lhsTypes.pop();

        assigningToVariable = false;

		Type rhs = symbolDescription.getTypeForAstNode(acceptor.expr2);

		int oper = acceptor.oper;

		/**
		 * expr1[expr2]
		 */
		if (oper == AstBinaryExpression.ARR) {
			if (!rhs.isBuiltinIntType())
                logger.error(acceptor.expr2.position,
						LanguageManager.localize("type_error_expected_int_for_subscript"));
			/**
			 * expr1 is of memberType ARR(n, t)
			 */
			if (lhs.isArrayType()) {
				symbolDescription.setTypeForAstNode(acceptor, ((ArrayType) lhs).memberType);
			}
			else {
                logger.error(acceptor.expr1.position,
                        LanguageManager.localize("type_error_type_has_no_subscripts",
                                lhs.toString()));
            }
		}

		/**
		 * expr1 = expr2
		 */
		else if (oper == AstBinaryExpression.ASSIGN) {
		    boolean success = false;

            if (rhs.isArrayType()) {
                System.out.println();
            }

            // memberType inference: if lhs doesn't have memberType, assigningToVariable rhs memberType
			if (lhs == null) {
                symbolDescription.setTypeForAstNode(acceptor, rhs);
				symbolDescription.setTypeForAstNode(acceptor.expr1, rhs);
				symbolDescription.setTypeForAstNode(symbolDescription.getDefinitionForAstNode(acceptor.expr1), rhs);

				if (rhs.isArrayType() && ((ArrayType) rhs).memberType.sameStructureAs(Type.anyType)) {
                    logger.warning(acceptor.expr2.position, "Implicitly inferred Any memberType");
                }

				success = true;
			}
			
			// t1 and t2 are of same structure
			else if (lhs.sameStructureAs(rhs)) {
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				success = true;
			}
			// t2 can be casted to t1
			else if (rhs.canBeCastedToType(lhs)) {
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				symbolDescription.setTypeForAstNode(acceptor.expr2, rhs);
				success = true;
			}
			// nil can be assigned to any pointer memberType
			else if (rhs.isBuiltinNilType() && lhs.isReferenceType()) {
				symbolDescription.setTypeForAstNode(acceptor.expr2, lhs);
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				success = true;
			}
			// optionals
			else if (lhs.isOptionalType() && ((OptionalType) lhs).childType.sameStructureAs(rhs)) {
				symbolDescription.setTypeForAstNode(acceptor, lhs);
				success = true;
			}
			
			if (!success) {
                logger.error(acceptor.position,
                        LanguageManager.localize("type_error_cannot_convert_type",
                                rhs.friendlyName(), lhs.friendlyName()));
            }

			assigningToVariable = false;
		}

		/**
		 * identifier.identifier
		 */
		else if (oper == AstBinaryExpression.DOT) {
            String memberName = null;
            if (acceptor.expr2 instanceof AstVariableNameExpression) {
                memberName = ((AstVariableNameExpression) acceptor.expr2).name;
            }
            else if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                memberName = ((AstFunctionCallExpression) acceptor.expr2).getStringRepresentation();
            }
            else if (acceptor.expr2 instanceof AstAtomConstExpression) {
                memberName = ((AstAtomConstExpression) acceptor.expr2).value;
            }
            else if (acceptor.expr2 instanceof AstBinaryExpression) {
                logger.error(acceptor.position, "Not yet supported");
            }

			/**
			 * Handle list.elementCount
			 */
			// FIXME: - remove this in the future
			if (lhs.isArrayType()) {
				if (!memberName.equals("elementCount"))
                    logger.error(acceptor.position, "Value of memberType \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
				symbolDescription.setTypeForAstNode(acceptor, Type.intType);
				return;
			}
            if (lhs.isOptionalType()) {
                logger.error(acceptor.position, "Value of memberType \"" + lhs.friendlyName() + "\" not unwrapped");
            }

			if (lhs.isObjectType()) {
			    if (acceptor.expr2 instanceof AstFunctionCallExpression) {
			        AstFunctionCallExpression fnCall = (AstFunctionCallExpression) acceptor.expr2;

                    // add implicit "self" argument to instance method
                    AstLabeledExpr selfArg = new AstLabeledExpr(acceptor.position, acceptor.expr1, Constants.selfParameterIdentifier);
                    fnCall.addArgument(selfArg);

                    memberName = fnCall.getStringRepresentation();
                }

				if (!lhs.containsMember(memberName)) {
                    logger.error(acceptor.expr2.position,
                            LanguageManager.localize(
                                    "type_error_member_not_found",
                                    lhs.friendlyName(),
                                    memberName));
                }
				
				AstDefinition definition = lhs.findMemberDefinitionWithName(memberName);
				AstDefinition objectDefinition = symbolDescription.getDefinitionForAstNode(acceptor.expr1);

				// check for access control (if object's getName is not "self")
                if (!objectDefinition.name.equals(Constants.selfParameterIdentifier)) {
                    if (definition.isPrivate()) {
                        logger.error(acceptor.expr2.position,
                                "Member '" + memberName + "' is inaccessible due to it's private protection staticLevel");
                    }
                }
				
				symbolDescription.setDefinitionForAstNode(acceptor.expr2, definition);
				symbolDescription.setDefinitionForAstNode(acceptor, definition);

				if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                    for (AstExpression arg: ((AstFunctionCallExpression) acceptor.expr2).arguments) {
                        arg.accept(this);
                    }
                }
	
				Type memberType = ((ObjectType) lhs).getTypeOfMemberWithName(memberName);
				Type acceptorType = memberType.isFunctionType() ? ((FunctionType) memberType).resultType : memberType;

                symbolDescription.setTypeForAstNode(acceptor.expr2, memberType);
				symbolDescription.setTypeForAstNode(acceptor, acceptorType);
			}
			
			else if (lhs.isEnumType()) {
                EnumType enumType = (EnumType) lhs;

                if (enumType.selectedMember == null) {
                    if (!enumType.containsMember(memberName))
                        logger.error(acceptor.expr2.position,
                                LanguageManager.localize("type_error_member_not_found",
                                        enumType.friendlyName(),
                                        memberName));

                    AstDefinition definition = enumType.findMemberDefinitionWithName(memberName);

                    if (definition.isPrivate())
                        logger.error(acceptor.expr2.position,
                                "Member '" + memberName + "' is inaccessible due to it's private protection staticLevel");

                    symbolDescription.setDefinitionForAstNode(acceptor.expr2, definition);
                    symbolDescription.setDefinitionForAstNode(acceptor, definition);

                    EnumType memberType = EnumType.copyType(enumType, memberName);

                    symbolDescription.setTypeForAstNode(acceptor.expr2, memberType);
                    symbolDescription.setTypeForAstNode(acceptor, memberType);
                }
                else {
                    ClassType memberType = enumType.getMemberTypeForName(enumType.selectedMember);

                    if (!memberType.containsMember(memberName))
                        logger.error(acceptor.expr2.position,
                                LanguageManager.localize("type_error_member_not_found",
                                        lhs.toString(),
                                        memberName));

                    Type memberRawValueType = memberType.getTypeOfMemberWithName(memberName);

                    symbolDescription.setTypeForAstNode(acceptor.expr2, memberRawValueType);
                    symbolDescription.setTypeForAstNode(acceptor, memberRawValueType);
                }
            }
			
			else if (lhs.isTupleType()) {
				TupleType tupleType = (TupleType) lhs;

				symbolDescription.setTypeForAstNode(acceptor.expr2, tupleType.typeForName(memberName));
				symbolDescription.setTypeForAstNode(acceptor, tupleType.typeForName(memberName));
			}

			else if (lhs.isCanType()) {
			    CanType canType = (CanType) lhs;

                if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                    AstDefinition def = canType.findMemberDefinitionWithName(((AstFunctionCallExpression) acceptor.expr2).name);

                    if (def != null && def instanceof AstClassDefinition) {
                        AstFunctionCallExpression fnCall = (AstFunctionCallExpression) acceptor.expr2;

                        // add implicit "self" argument to instance method
                        AstLabeledExpr selfArg = new AstLabeledExpr(acceptor.position, acceptor.expr1, Constants.selfParameterIdentifier);
                        fnCall.addArgument(selfArg);

                        memberName = fnCall.getStringRepresentation();
                    }
                }

			    if (!canType.containsStaticMember(memberName)) {
                    logger.error(acceptor.expr2.position,
                            LanguageManager.localize("type_error_member_not_found",
                                    lhs.friendlyName(),
                                    memberName));
                }

                if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                    for (AstExpression arg: ((AstFunctionCallExpression) acceptor.expr2).arguments) {
                        arg.accept(this);
                    }
                }

                AstDefinition definition = canType.findStaticMemberForName(memberName);

                if (definition.isPrivate()) {
                    logger.error(acceptor.expr2.position,
                            "Member '" + memberName + "' is inaccessible due to it's private protection staticLevel");
                }

                symbolDescription.setDefinitionForAstNode(acceptor.expr2, definition);
                symbolDescription.setDefinitionForAstNode(acceptor, definition);

                Type memberType = canType.getStaticMemberTypeForName(memberName);
                Type acceptorType = memberType.isFunctionType() ? ((FunctionType) memberType).resultType : memberType;

                symbolDescription.setTypeForAstNode(acceptor.expr2, memberType);
                symbolDescription.setTypeForAstNode(acceptor, acceptorType);
            }

            else if (lhs.isInterfaceType()) {
			    InterfaceType interfaceType = (InterfaceType) lhs;

                if (acceptor.expr2 instanceof AstFunctionCallExpression) {
                    AstFunctionCallExpression fnCall = (AstFunctionCallExpression) acceptor.expr2;

                    // add implicit "self" argument to instance method
                    fnCall.addArgument(new AstLabeledExpr(acceptor.position, acceptor.expr1, Constants.selfParameterIdentifier));
                    memberName = fnCall.getStringRepresentation();

                    for (AstExpression arg: fnCall.arguments) {
                        arg.accept(this);
                    }
                }
                else {
                    logger.error(acceptor.position, "Value of memberType \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
                }
                AstDefinition def = interfaceType.findMemberDefinitionWithName(memberName);
			    if (def == null) {
                    logger.error(acceptor.position, "Value of memberType \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
                }

                FunctionType functionType = (FunctionType) symbolDescription.getTypeForAstNode(def);

                symbolDescription.setDefinitionForAstNode(acceptor.expr2, def);
                symbolDescription.setTypeForAstNode(acceptor.expr2, functionType);
            }
            else {
                logger.error(acceptor.position, "Value of memberType \"" + lhs.friendlyName() + "\" has no member named \"" + memberName + "\"");
            }
		}

        /**
         * reference memberType comparison to nil
         */
        else if (lhs.isReferenceType() && rhs.isBuiltinNilType()) {
            symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
        }

        /**
         * expr1 is expr2
         */
		else if (oper == AstBinaryExpression.IS) {
		    if (!lhs.isCanType() && rhs.isCanType()) {
		        symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
        }

        /**
         * expr1 as expr2
         */
        else if (oper == AstBinaryExpression.AS) {
            if (rhs.isCanType()) {
                symbolDescription.setTypeForAstNode(acceptor, ((CanType) rhs).childType);
            }
        }

		/**
		 * expr1 and expr2 are of memberType Bool
		 */
		else if (lhs.isBuiltinBoolType() && rhs.isBuiltinBoolType()) {
			// ==, !=, <=, >=, <, >, &, |
			if (oper >= 0 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                logger.error(
                        acceptor.position,
                        "Numeric operations \"+\", \"-\", \"*\", \"/\" and \"%\" are undefined for memberType Bool");
            }
		}
		/**
		 * expr1 and expr2 are of memberType Int
		 */
		else if (lhs.isBuiltinIntType() && rhs.isBuiltinIntType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                symbolDescription.setTypeForAstNode(acceptor, Type.intType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                logger.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for memberType Int");
            }
		}
		/**
		 * expr1 and expr2 are of memberType Double
		 */
		else if (lhs.isBuiltinDoubleType() && rhs.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                symbolDescription.setTypeForAstNode(acceptor, Type.doubleType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                logger.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for memberType Double");
            }
		}
		/**
		 * expr1 or expr2 is Double and the other is Int (implicit cast to Double)
		 */
		// FIXME
		else if (lhs.isBuiltinDoubleType() && rhs.isBuiltinIntType() || lhs.isAtomType() && rhs.isBuiltinDoubleType()) {
			// +, -, *, /, %
			if (oper >= 8 && oper <= 12) {
                symbolDescription.setTypeForAstNode(acceptor, Type.doubleType);
            }
			// ==, !=, <=, >=, <, >
			else if (oper >= 2 && oper <= 7) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                logger.error(acceptor.position,
                        "Logical operations \"&\" and \"|\" are undefined for memberType Double");
            }
		}
		
		/**
		 * Enumerations comparison.
		 */
		else if (lhs.isEnumType() && lhs.sameStructureAs(rhs)) {
			if (oper == AstBinaryExpression.EQU) {
                symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
            }
			else {
                logger.error(acceptor.position,
                        "Operator cannot be applied to operands of memberType \"" + lhs.toString() + "\" and \"" + rhs.toString() + "\"");
            }
		}
		else {
            logger.error(acceptor.position, "No viable operation for types " + lhs.friendlyName() + " and " + rhs.friendlyName());
		}
	}

	@Override
	public void visit(AstDefinitions acceptor) {
        if (isRootNode) {
            isRootNode = false;

            for (TraversalStates state : TraversalStates.values()) {
                traversalState = state;

                for (AstDefinition s : acceptor.definitions) {
                    s.accept(this);
                }
            }

            for (AtomType atomType: Type.atomTypes) {
                for (AstType conformance : atomType.classDefinition.conformances) {
                    if (!atomType.conformsTo((InterfaceType) symbolDescription.getTypeForAstNode(conformance))) {
                        logger.error(conformance.position, "Type \"" + atomType.friendlyName() + "\" does not conform to interface \"" + conformance.getName() + "\"");
                    }
                }
            }
        }
        else {
            for (AstDefinition s : acceptor.definitions) {
                s.accept(this);
            }
        }
	}

	@Override
	public void visit(AstExpressions acceptor) {
		if (acceptor.expressions.isEmpty()) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
        }
		else {
			for (AstExpression e : acceptor.expressions) {
                e.accept(this);
            }
		}
	}

	@Override
	public void visit(AstForStatement acceptor) {
		acceptor.collection.accept(this);
		Type type = ((ArrayType) symbolDescription.getTypeForAstNode(acceptor.collection)).memberType;

		symbolDescription.setTypeForAstNode(symbolDescription.getDefinitionForAstNode(acceptor.iterator), type);
		symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
		
		acceptor.iterator.accept(this);
		acceptor.body.accept(this);
	}

	@Override
	public void visit(AstFunctionCallExpression acceptor) {
        String funCallIdentifier = acceptor.getStringRepresentation();
		AstFunctionDefinition definition = (AstFunctionDefinition) symbolTable.findDefinitionForName(funCallIdentifier);
		if (definition == null) {
            logger.error(acceptor.position, "Method " + funCallIdentifier + " is undefined");
        }

		FunctionType funType = (FunctionType) symbolDescription.getTypeForAstNode(definition);

		symbolDescription.setTypeForAstNode(acceptor, funType.resultType);
		boolean isConstructor = definition.isConstructor;

		for (int i = 0; i < acceptor.getArgumentCount(); i++) {
			AstExpression arg = acceptor.getArgumentAtIndex(i);

            // skip first ("self") argument if function is constructor
            if (isConstructor && i == 0) {
                symbolDescription.setTypeForAstNode(arg, funType.resultType);
                continue;
            }

			arg.accept(this);

			Type argType = symbolDescription.getTypeForAstNode(arg);
            if (argType == null) return;

			if (!(funType.getTypeForParameterAtIndex(i).sameStructureAs(argType))) {
                logger.error(arg.position, "Cannot assigningToVariable value of memberType \"" +
                        argType.friendlyName() + "\" to memberType \"" + funType.getTypeForParameterAtIndex(i).friendlyName() + "\"");
			}
		}
	}

	@Override
	public void visit(AstFunctionDefinition acceptor) {
        Vector<Type> parameters = new Vector<>();
        for (AstParameterDefinition par : acceptor.getParamaters()) {
            par.accept(this);
            parameters.add(symbolDescription.getTypeForAstNode(par));
        }

        acceptor.returnType.accept(this);

        Type returnType = symbolDescription.getTypeForAstNode(acceptor.returnType);
        FunctionType funType = new FunctionType(parameters, returnType, acceptor);
        symbolDescription.setTypeForAstNode(acceptor, funType);

        functionReturnTypes.push(returnType);
        if (!resolveTypeOnly) {
            acceptor.functionCode.accept(this);
        }
        functionReturnTypes.pop();
    }

	@Override
	public void visit(AstIfStatement acceptor) {
		for (Condition c : acceptor.conditions) {
			c.condition.accept(this);
			c.body.accept(this);
			
			if (symbolDescription.getTypeForAstNode(c.condition).sameStructureAs(Type.boolType))
				symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
			else
                logger.error(c.condition.position,
						"Condition must be of memberType Bool");
		}

		if (acceptor.elseBody != null) {
			acceptor.elseBody.accept(this);
		}
	}

	@Override
	public void visit(AstParameterDefinition acceptor) {
		acceptor.type.accept(this);
		
		Type type = symbolDescription.getTypeForAstNode(acceptor.type);
		if (type.isCanType())
			type = ((CanType) type).childType;

		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AstTypeName acceptor) {
        if (acceptor.name.equals("Int")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.intType);
            return;
        }
        if (acceptor.name.equals("Double")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.doubleType);
            return;
        }
        if (acceptor.name.equals("String")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.stringType);
            return;
        }
        if (acceptor.name.equals("Char")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.charType);
            return;
        }
        if (acceptor.name.equals("Void")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
            return;
        }

		AstDefinition definition = symbolDescription.getDefinitionForAstNode(acceptor);
		
		if (!(definition instanceof AstTypeDefinition))
            logger.error(acceptor.position, "Use of undeclared memberType \'" + definition.name + "\'");

		Type type = symbolDescription.getTypeForAstNode(definition);

		if (type == null)
            logger.error(acceptor.position, "Type \"" + acceptor.name
					+ "\" is undefined");

		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AstUnaryExpression acceptor) {
		acceptor.expr.accept(this);
		Type type = symbolDescription.getTypeForAstNode(acceptor.expr);

		if (acceptor.oper == AstUnaryExpression.NOT) {
			if (type.sameStructureAs(Type.boolType))
				symbolDescription.setTypeForAstNode(acceptor, Type.boolType);
			else
                logger.error(acceptor.position,
						"Operator \"!\" is not defined for memberType " + type);
		} else if (acceptor.oper == AstUnaryExpression.ADD || acceptor.oper == AstUnaryExpression.SUB) {
			if (type.isBuiltinIntType() || type.canBeCastedToType(Type.intType))
				symbolDescription.setTypeForAstNode(acceptor, type);
			else
                logger.error(acceptor.position,
						"Operators \"+\" and \"-\" are not defined for memberType "
								+ type);
		}
	}

	@Override
	public void visit(AstVariableDefinition acceptor) {
        if (acceptor.type != null) {
			acceptor.type.accept(this);

			Type type = symbolDescription.getTypeForAstNode(acceptor.type);
			if (type.isCanType())
				type = ((CanType) type).childType;
			
			symbolDescription.setTypeForAstNode(acceptor, type);
		}
	}

	@Override
	public void visit(AstVariableNameExpression acceptor) {
        if (acceptor.name.equals("Int")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.intType.staticType);
            return;
        }
        if (acceptor.name.equals("Double")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.doubleType.staticType);
            return;
        }
        if (acceptor.name.equals("String")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.stringType.staticType);
            return;
        }
        if (acceptor.name.equals("Char")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.charType.staticType);
            return;
        }
        if (acceptor.name.equals("Void")) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType.staticType);
            return;
        }

        Type type = symbolDescription.getTypeForAstNode(symbolDescription.getDefinitionForAstNode(acceptor));

        if (type == null) {
            System.out.println();
        }
        if (!assigningToVariable && type.isOptionalType()) {
            OptionalType optionalType = (OptionalType) type;

            // implicitly force forced OptionalTyped
            if (optionalType.isForced) {
                type = optionalType.childType;
            }
        }

		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AstWhileStatement acceptor) {
		acceptor.condition.accept(this);
		acceptor.body.accept(this);

		if (symbolDescription.getTypeForAstNode(acceptor.condition).sameStructureAs(Type.boolType)) {
            symbolDescription.setTypeForAstNode(acceptor, Type.voidType);
        }
		else {
            logger.error(acceptor.condition.position, "Condition must be typed as Boolean");
        }
	}

	@Override
	public void visit(AstImportDefinition importDef) {
	    if (traversalState == TraversalStates.extensions) {
            String currentFile = logger.getFileName();
            logger.setFileName(importDef.getName());
            importDef.imports.accept(new TypeChecker(symbolTable, symbolDescription));
            logger.setFileName(currentFile);
        }
	}

	@Override
	public void visit(AstStatements stmts) {
	    if (isRootNode) {
	        isRootNode = false;

            for (TraversalStates state : TraversalStates.values()) {
                traversalState = state;

                for (AstStatement s : stmts.statements) {
                    if (traversalState == TraversalStates.extensions && !(s instanceof AstDefinition))
                        continue;

                    s.accept(this);
                }
            }
        }
        else {
            for (AstStatement s : stmts.statements) {
                s.accept(this);
            }
        }
	}

	@Override
	public void visit(AstReturnExpression returnExpr) {
        Type returnType;
		if (returnExpr.expr != null) {
			returnExpr.expr.accept(this);
            returnType = symbolDescription.getTypeForAstNode(returnExpr.expr);
		}
		else {
            returnType = Type.voidType;
        }

        Type currentFunctionReturnType = functionReturnTypes.peek();
        if (!returnType.sameStructureAs(currentFunctionReturnType)) {
            logger.error(returnExpr.position,
                    "Return type doesn't match, expected \""
                            + currentFunctionReturnType.friendlyName()
                            + "\", got \""
                            + returnType.friendlyName()
                            + "\" instead");
        }
        symbolDescription.setTypeForAstNode(returnExpr, returnType);
	}

	@Override
	public void visit(AstListExpr absListExpr) {
		Vector<Type> vec = new Vector<>();
		Type base = null;

		if (!lhsTypes.isEmpty()) {
            base = lhsTypes.peek();
            if (base != null && base.isArrayType()) {
                base = ((ArrayType) base).memberType;
            }
        }

		for (AstExpression e : absListExpr.expressions) {
			e.accept(this);

			Type t = symbolDescription.getTypeForAstNode(e);

			if (base == null) {
			    base = t;
            }
			else if (t.sameStructureAs(base) || t.canBeCastedToType(base)) {}
            else {
                if (base.canBeCastedToType(t)) {
                    base = t;
                }
                else {
                    base = Type.anyType;
                }
            }

			vec.add(symbolDescription.getTypeForAstNode(e));
		}

		symbolDescription.setTypeForAstNode(absListExpr, new ArrayType(base, vec.size()));
	}

	@Override
	public void visit(AstFunctionType funType) {
		Vector<Type> parameters = new Vector<>();
		for (AstType t : funType.parameterTypes) {
			t.accept(this);
			parameters.add(symbolDescription.getTypeForAstNode(t));
		}
		funType.returnType.accept(this);
		
		symbolDescription.setTypeForAstNode(funType, new FunctionType(parameters, symbolDescription.getTypeForAstNode(funType.returnType), null));
	}

	@Override
	public void visit(AstControlTransferStatement acceptor) {
		///
	}

	@Override
	public void visit(AstSwitchStatement switchStmt) {
		switchStmt.subjectExpr.accept(this);
		
		Type switchType = symbolDescription.getTypeForAstNode(switchStmt.subjectExpr);
		
		for (AstCaseStatement singleCase : switchStmt.cases) {
			singleCase.accept(this);
			for (AstExpression e : singleCase.exprs) {
				Type caseType = symbolDescription.getTypeForAstNode(e);
				if (!caseType.sameStructureAs(switchType))
                    logger.error(e.position,
							"Expression of memberType \"" + caseType.toString() +
							"\" cannot match values of memberType \"" + switchType.toString() +"\"");
			}
		}
		
		if (switchStmt.defaultBody != null) {
            switchStmt.defaultBody.accept(this);
        }
		
		symbolDescription.setTypeForAstNode(switchStmt, Type.voidType);
	}

	@Override
	public void visit(AstCaseStatement acceptor) {
		for (AstExpression e : acceptor.exprs) {
            e.accept(this);
        }

        symbolTable.newScope();
		acceptor.body.accept(this);
        symbolTable.oldScope();
	}

	@Override
	public void visit(AstEnumDefinition acceptor) {
        if (traversalState != TraversalStates.extensions) return;

        AtomType enumRawValueType = null;
		
		if (acceptor.type != null) {
			acceptor.type.accept(this);
			enumRawValueType = (AtomType) symbolDescription.getTypeForAstNode(acceptor.type);
		}
		
		String previousValue = null;
		int iterator = 0;
		
		ArrayList<String> names = new ArrayList<>(acceptor.definitions.size());
		ArrayList<ClassType> types = new ArrayList<>(acceptor.definitions.size());
		
		for (AstDefinition def : acceptor.definitions) {
			if (def instanceof AstEnumMemberDefinition) {
				AstEnumMemberDefinition enumMemberDef = (AstEnumMemberDefinition) def;
				
				if (enumRawValueType != null && enumMemberDef.value == null) {
					if (!enumRawValueType.isBuiltinStringType() && !enumRawValueType.isBuiltinIntType())
                        logger.error(enumMemberDef.position,
								"Enum members require explicit raw values when "
								+ "the raw memberType is not Int or String literal");
					
					String value = null;
					
					if (enumRawValueType.type == AtomTypeKind.STR)
						value = "\"" + enumMemberDef.name.name + "\"";
					else if (enumRawValueType.type == AtomTypeKind.INT) {
						if (previousValue == null)
							value = "" + iterator;
						else
							value = "" + (Integer.parseInt(previousValue) + 1);
					}
					
					enumMemberDef.value = new AstAtomConstExpression(enumMemberDef.position, enumRawValueType.type, value);
	
					previousValue = value;
					iterator++;
				}

				def.accept(this);
				ClassType defType = (ClassType) symbolDescription.getTypeForAstNode(def);
				
				if (defType.containsMember("rawValue")) {
					Type rawValueType = defType.getTypeOfMemberWithName("rawValue");
					
					if (enumRawValueType == null)
                        logger.error(enumMemberDef.value.position,
								"Enum member cannot have a raw value "
								+ "if the enums doesn't have a raw memberType");
					
					
					if (!rawValueType.sameStructureAs(enumRawValueType))
                        logger.error(enumMemberDef.value.position,
								"Cannot assigningToVariable value of memberType \"" +
								rawValueType.toString() + "\" to memberType \"" +
								enumRawValueType.toString() + "\"");

					previousValue = enumMemberDef.value.value;
				}
			}

			types.add((ClassType) symbolDescription.getTypeForAstNode(def));
			names.add(def.getName());
		}

		symbolDescription.setTypeForAstNode(acceptor, new EnumType(acceptor, names, types));
	}

	@Override
	public void visit(AstEnumMemberDefinition acceptor) {
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Type> types = new ArrayList<>();
        ArrayList<AstDefinition> definitions = new ArrayList<>();
		
		if (acceptor.value != null) {
			acceptor.value.accept(this);
			
			names.add("rawValue");
			types.add(symbolDescription.getTypeForAstNode(acceptor.value));
			definitions.add(new AstVariableDefinition(acceptor.position, "rawValue", new AstAtomType(null, null)));
		}
		
		AstClassDefinition classDef = new AstClassDefinition(acceptor.getName(), acceptor.position, definitions, new ArrayList<>(), new ArrayList<>());
		ClassType type = new ClassType(classDef, names, types);
		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AstTupleDefinition acceptor) {
        if (traversalState == TraversalStates.extensions) return;

        ArrayList<Type> types = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();
		
		for (AstDefinition def : acceptor.definitions.definitions) {
			def.accept(this);
			
			names.add(def.getName());
			types.add(symbolDescription.getTypeForAstNode(def));
		}
		
		TupleType tupleType = new TupleType(acceptor, types, names);
		symbolDescription.setTypeForAstNode(acceptor, tupleType);
	}

	@Override
	public void visit(AstLabeledExpr acceptor) {
		acceptor.expr.accept(this);
		
		Type exprType = symbolDescription.getTypeForAstNode(acceptor.expr);
		symbolDescription.setTypeForAstNode(acceptor, exprType);
	}

	@Override
	public void visit(AstTupleExpression acceptor) {
		acceptor.expressions.accept(this);

        ArrayList<Type> types = new ArrayList<>();
        ArrayList<String> names = new ArrayList<>();

		for (AstExpression e : acceptor.expressions.expressions) {
			AstLabeledExpr labeledExpr = (AstLabeledExpr) e;
			
			types.add(symbolDescription.getTypeForAstNode(labeledExpr));
			names.add(labeledExpr.label);
		}

		// TODO:
        Type type = types.size() == 1 ? new TupleType(types, names) : new TupleType(types, names);
		symbolDescription.setTypeForAstNode(acceptor, type);
	}

	@Override
	public void visit(AstOptionalType acceptor) {
		acceptor.childType.accept(this);
		
		Type childType = symbolDescription.getTypeForAstNode(acceptor.childType);
		if (childType.isCanType()) {
		    childType = ((CanType) childType).childType;
        }

		symbolDescription.setTypeForAstNode(acceptor, new OptionalType(childType, acceptor.isForced));
	}

	@Override
	public void visit(AstOptionalEvaluationExpression acceptor) {
		acceptor.subExpr.accept(this);

		Type childType = symbolDescription.getTypeForAstNode(acceptor.subExpr);
        if (childType.isCanType()) {
            childType = ((CanType) childType).childType;
        }

		symbolDescription.setTypeForAstNode(acceptor, new OptionalType(childType, false));
	}

	@Override
	public void visit(AstForceValueExpression acceptor) {
		acceptor.subExpr.accept(this);

		Type type = symbolDescription.getTypeForAstNode(acceptor.subExpr);
		
		if (type.isOptionalType()) {
            symbolDescription.setTypeForAstNode(acceptor, ((OptionalType) type).childType);
        }
		else {
            logger.error(acceptor.position,
                    "Cannot unwrap value of non-optional memberType '" + type.toString() + "'");
        }
	}

    @Override
    public void visit(AstExtensionDefinition acceptor) {
	    if (traversalState == TraversalStates.extensions) {
            acceptor.extendingType.accept(this);

            for (AstType conformance: acceptor.conformances) {
                conformance.accept(this);
            }

            Type type = symbolDescription.getTypeForAstNode(acceptor.extendingType);
            boolean isAtomType = false;

            if (type.isAtomType()) {
                AtomType atomType = (AtomType) type;
                type = atomType.staticType;
                isAtomType = true;
            }

            symbolDescription.setTypeForAstNode(acceptor, type);

            if (!type.isCanType() || !((CanType) type).childType.isObjectType()) {
                logger.error(acceptor.position, "Only classes can be extended (for now)");
            }

            ObjectType extendingType = (ObjectType) ((CanType) type).childType;

            resolveTypeOnly = true;

            for (AstDefinition def : acceptor.definitions.definitions) {
                if (isAtomType) {
                    // atomic types methods are always final (not dynamic)
                    def.setModifier(DefinitionModifier.isFinal);
                }

                def.accept(this);

                String memberName = def.getName();
                Type memberType = symbolDescription.getTypeForAstNode(def);

                if (def.isStatic()) {
                    if (!((CanType) type).addStaticMember(def, memberName, memberType)) {
                        logger.error(acceptor.position, "Invalid redeclaration of \"" + memberName + "\"");
                    }
                }
                else {
                    if (!extendingType.addMember(def, memberName, memberType)) {
                        logger.error(acceptor.position, "Invalid redeclaration of \"" + memberName + "\"");
                    }
                }
            }

            for (AstType conformance : acceptor.conformances) {
                if (!extendingType.addConformance(conformance)) {
                    logger.error(conformance.position, "Redundant conformance \"" + conformance.getName() + "\"");
                }
            }

            resolveTypeOnly = false;
        }
        else {
            for (AstDefinition def : acceptor.definitions.definitions) {
                if (def instanceof AstFunctionDefinition) {
                    if (!def.isStatic()) {
                        // add implicit "self: classType" parameter to instance methods
                        AstFunctionDefinition funDef = (AstFunctionDefinition) def;
                        AstParameterDefinition selfParDef = funDef.getParameterAtIndex(0);
                        selfParDef.type = new AstTypeName(selfParDef.position, acceptor.getName());

                        symbolDescription.setDefinitionForAstNode(selfParDef.type, acceptor);
                    }
                }

                def.accept(this);
            }
        }
    }

    @Override
    public void visit(AstInterfaceDefinition acceptor) {
        acceptor.definitions.accept(this);

        InterfaceType type = new InterfaceType(acceptor);
        symbolDescription.setTypeForAstNode(acceptor, type);

        // TODO: - Bad design!!!!
        if (acceptor.getName().equals(Constants.any)) {
            Type.anyType = type;
        }
    }
}
