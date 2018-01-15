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

package compiler.synan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;
import java.util.stream.Collectors;

import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import compiler.ast.tree.enums.AtomTypeKind;
import compiler.ast.tree.enums.ControlTransferKind;
import compiler.ast.tree.enums.DefinitionModifier;
import compiler.ast.tree.expr.*;
import compiler.ast.tree.stmt.*;
import compiler.ast.tree.type.*;
import utils.Constants;
import compiler.Position;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AstExpression;
import compiler.ast.tree.type.AstAtomType;
import compiler.lexan.*;

public class SynAn {

    private static LoggerInterface logger = LoggerFactory.logger();

	private LexicalAnalyzer lexAn;
	private boolean dump;
	private Symbol symbol = null;
	private Symbol previous = null;

	public SynAn(LexicalAnalyzer lexAn, boolean dump) {
		this.lexAn = lexAn;
		this.dump = dump;

		this.symbol = this.lexAn.nextSymbol();
		this.previous = this.symbol;
	}

	public AstNode parse() {
		if (symbol == null)
            logger.error("Error accessing LexAn");

		if (symbol.tokenType() == TokenType.NEWLINE)
			nextSymbol();

		if (symbol.tokenType() == TokenType.EOF)
		    return new AstDefinitions(symbol.position(), new ArrayList<>());

		dump("source -> statements");
		AstNode abstrTree = parseStatements();

		if (symbol.tokenType() != TokenType.EOF && symbol.tokenType() != TokenType.NEWLINE)
            logger.error(symbol.position(), "Syntax error on token \""
					+ previous.getLexeme() + "\"");

		return abstrTree;
	}

	private AstStatements parseStatements() {
		dump("statements -> statement statements'");

        ArrayList<AstStatement> astStatements = new ArrayList<>();
		if (symbol.tokenType() == TokenType.RBRACE)
			return new AstStatements(symbol.position(), astStatements);

		AstStatement statement = parseStatement();
        astStatements.add(statement);
		astStatements.addAll(parseStatements_(statement));

		return new AstStatements(new Position(astStatements.get(0).position, astStatements.get(astStatements.size() - 1).position), astStatements);
	}

	private ArrayList<AstStatement> parseStatements_(AstStatement prevStmt) {
		switch (symbol.tokenType()) {
		case EOF:
			dump("statements' -> $");
			return new ArrayList<>();
		case RBRACE:
			dump("statements' -> e");
			return new ArrayList<>();
		case SEMIC:
			nextSymbol();
			if (symbol.tokenType() == TokenType.NEWLINE)
				nextSymbol();

			if (symbol.tokenType() == TokenType.EOF || symbol.tokenType() == TokenType.RBRACE)
				return new ArrayList<>();
		case IDENTIFIER:
			AstStatement statement = parseStatement();
            ArrayList<AstStatement> astStatements = parseStatements_(statement);
			astStatements.add(0, statement);
			return astStatements;
		case NEWLINE:
			nextSymbol();

			if (symbol.tokenType() == TokenType.EOF || symbol.tokenType() == TokenType.RBRACE ||
				symbol.tokenType() == TokenType.KW_CASE || symbol.tokenType() == TokenType.KW_DEFAULT)
				return new ArrayList<>();

			statement = parseStatement();
			astStatements = parseStatements_(statement);
			astStatements.add(0, statement);
			return astStatements;
		case ASSIGN:
			if (!(prevStmt instanceof AstVariableDefinition))
                logger.error(prevStmt.position, "Syntax error");

			nextSymbol();
			dump("var_definition -> = expression");

			AstVariableDefinition var = (AstVariableDefinition) prevStmt;
			AstVariableNameExpression varName = new AstVariableNameExpression(var.position, var.name);

			AstExpression e = parseExpression();
			astStatements = parseStatements_(null);

			astStatements.add(0, new AstBinaryExpression(new Position(var.position, e.position),
					AstBinaryExpression.ASSIGN, varName, e));

			return astStatements;
		default:
            logger.error(symbol.position(), "Consecutive statements must be separated by separator");
		}
		return null;
	}

	private AstStatement parseStatement() {
		switch (symbol.tokenType()) {
		/**
		 * Parse definition.
		 */
		case KW_VAR:
		case KW_LET:
		case KW_STRUCT:
		case KW_CLASS:
		case KW_ENUM:
        case KW_INTERFACE:
		case KW_FUN:
		case KW_IMPORT:
        case KW_EXTENSION:
        case KW_ABSTRACT:
			dump("statement -> definition");
			return parseDefinition();

		/**
		 * Parse control transfer statement.
		 */
		case KW_CONTINUE:
			dump("expression -> continue");
			nextSymbol();
			return new AstControlTransferStatement(symbol.position(),
					ControlTransferKind.Continue);
		case KW_BREAK:
			dump("expression -> break");
			nextSymbol();
			return new AstControlTransferStatement(symbol.position(),
					ControlTransferKind.Break);

		/**
		 * Parse conditional statements
		 */
		case KW_IF:
			dump("atom_expression -> if_statement");
			return parseIf();
		case KW_SWITCH:
			dump("atom_expression -> switch_statement");
			return parseSwitch();
		case KW_WHILE:
			dump("atom_expression -> while expression { expression }");
			return parseWhileLoop();
		case KW_FOR:
			dump("atom_expression -> for indentifier in expression { expression }");
			return parseForLoop();

		/**
		 * Parse expression.
		 */
		default:
			dump("statement -> expression");
			return parseExpression();
		}
	}

	private AstDefinitions parseDefinitions() {
		dump("memberDefinitions -> definition memberDefinitions'");

        if (symbol.tokenType() == TokenType.NEWLINE) {
            nextSymbol();
        }

        if (symbol.tokenType() == TokenType.RBRACE) {
            return new AstDefinitions(symbol.position(), new ArrayList<>());
        }

        AstDefinition definition = parseDefinition();

        ArrayList<AstDefinition> absDefs = parseDefinitions_();
		absDefs.add(0, definition);
		return new AstDefinitions(new Position(absDefs.get(0).position, absDefs.get(absDefs.size() - 1).position), absDefs);
	}

	private ArrayList<AstDefinition> parseDefinitions_() {
		switch (symbol.tokenType()) {
		case EOF:
			dump("memberDefinitions' -> $");

			return new ArrayList<>();
		case RBRACE:
			dump("memberDefinitions' -> e");
			nextSymbol();

			return new ArrayList<>();
		case SEMIC:
        case NEWLINE:
			dump("memberDefinitions' -> ; memberDefinitions");
			nextSymbol();
			if (symbol.tokenType() == TokenType.NEWLINE)
				nextSymbol();

			if (symbol.tokenType() == TokenType.EOF || symbol.tokenType() == TokenType.RBRACE)
				return new ArrayList<>();

			AstDefinition definition = parseDefinition();
            ArrayList<AstDefinition> absDefs = parseDefinitions_();
			absDefs.add(0, definition);
			return absDefs;
		default:
            logger.error(symbol.position(), "Syntax error on token \""
					+ previous.getLexeme()
					+ "\", expected \";\" or \"}\" after this token");
		}
		return null;
	}

	private AstDefinition parseDefinition() {
		AstDefinition definition = null;

		HashSet<DefinitionModifier> modifiers = parseModifiers();

		switch (symbol.tokenType()) {
		case KW_FUN:
            dump("definition -> function_definition");
            definition = parseFunDefinition();
            break;
        case KW_INIT:
            dump("definition -> constructor_definition");
            definition = parseConstructorDefinition();
			break;
		case KW_VAR:
		case KW_LET:
			dump("definition -> variable_definition");
			definition = parseVarDefinition();
			break;
		case KW_IMPORT:
			dump("definition -> import_definition");
			definition = parseImportDefinition();
			break;
		case KW_CLASS:
        case KW_STRUCT:
			dump("definition -> class_definition");
			definition = parseClassDefinition(symbol.tokenType() == TokenType.KW_STRUCT);
			break;
        case KW_INTERFACE:
            dump("definition -> interface_definition");
            definition = parseInterfaceDefinition();
            break;
		case KW_ENUM:
			dump("definition -> enum_definition");
			definition = parseEnumDefinition();
			break;
        case KW_EXTENSION:
            dump("definition -> extension_definition");
            definition = parseExtensionDefinition();
            break;
		case KW_CASE:
			dump("definition -> enum_member_definition");
			definition = parseEnumCaseDefinition();
			break;
        case KW_ABSTRACT:
            dump("definition -> abstract def");
            nextSymbol();
            if (symbol.tokenType() == TokenType.KW_CLASS) {
                AstClassDefinition classDefinition = parseClassDefinition(false);
                classDefinition.setModifier(DefinitionModifier.isAbstract);
                return classDefinition;
            }
            else if (symbol.tokenType() == TokenType.KW_FUN) {
                AstFunctionDefinition functionPrototype = parseFunctionPrototype();
                functionPrototype.setModifier(DefinitionModifier.isAbstract);
                return functionPrototype;
            }
            else {
                logger.error(symbol.position(), "Syntax error on token \"" + symbol.getLexeme() + "\", delete this token");
            }
		default:
			if (symbol.tokenType() != TokenType.EOF) {
                logger.error(symbol.position(), "Syntax error on token \"" + symbol.getLexeme() + "\", delete this token");
            }
			else {
                logger.error(previous.position(), "Syntax error on token \"" + previous.getLexeme() + "\", delete this token");
            }
		}

		definition.setModifiers(modifiers);
		return definition;
	}

	private HashSet<DefinitionModifier> parseModifiers() {
	    HashSet<DefinitionModifier> modifiers = new HashSet<>();

	    DefinitionModifier modifier = parseModifier();

        while (modifier != DefinitionModifier.none) {
	        if (modifiers.contains(modifier)) {
                logger.error(symbol.position(), "Duplicate modifier");
            }

            modifiers.add(modifier);
            modifier = parseModifier();
        }

	    return modifiers;
    }

    private DefinitionModifier parseModifier() {
	    if (symbol.tokenType() == TokenType.KW_STATIC) {
	        nextSymbol();
	        return DefinitionModifier.isStatic;
        }
        if (symbol.tokenType() == TokenType.KW_FINAL) {
            nextSymbol();
            return DefinitionModifier.isFinal;
        }
        if (symbol.tokenType() == TokenType.KW_OVERRIDE) {
            nextSymbol();
            return DefinitionModifier.isOverriding;
        }
        if (symbol.tokenType() == TokenType.KW_PUBLIC) {
            nextSymbol();
            return DefinitionModifier.isPublic;
        }
        if (symbol.tokenType() == TokenType.KW_PRIVATE) {
            nextSymbol();
            return DefinitionModifier.isPrivate;
        }

        return DefinitionModifier.none;
    }

	private AstFunctionDefinition parseFunDefinition() {
		Position startPos = symbol.position();

        if (symbol.tokenType() == TokenType.KW_FUN) {
			Symbol functionName = nextSymbol(TokenType.IDENTIFIER, "identifier");

			nextSymbol(TokenType.LPARENT, "(");
			nextSymbol();
			dump("function_definition -> functionCode identifier ( parameters ) function_definition'");

            ArrayList<AstParameterDefinition> params = parseParameters();

			return parseFunDefinition_(startPos, functionName, params, false);
		}
        logger.error(previous.position(), "Syntax error on token \""
				+ previous.getLexeme() + "\", expected keyword \"functionDefinition\"");

		return null;
	}

    private AstFunctionDefinition parseConstructorDefinition() {
        Position startPos = symbol.position();

        if (symbol.tokenType() == TokenType.KW_INIT) {
            Symbol functionName = symbol;

            nextSymbol(TokenType.LPARENT, "(");
            nextSymbol();
            dump("constructor_definition -> init ( parameters ) function_definition'");

            ArrayList<AstParameterDefinition> params = parseParameters();

            return parseFunDefinition_(startPos, functionName, params, true);
        }

        logger.error(previous.position(), "Syntax error on token \""
                + previous.getLexeme() + "\", expected keyword \"init\"");

        return null;
    }

	private AstFunctionDefinition parseFunDefinition_(Position startPos, Symbol functionName, ArrayList<AstParameterDefinition> params, boolean isConstructor) {
		AstType type;

		if (symbol.tokenType() == TokenType.LBRACE) {
			dump("function_definition' -> { statements } ");
			type = new AstAtomType(symbol.position(), AtomTypeKind.VOID);
		}
		else {
			dump("function_definition' -> type { statements } ");
			type = parseType();
		}

		if (symbol.tokenType() != TokenType.LBRACE) {
            logger.error(symbol.position(), "Syntax error on token \""
                    + previous.getLexeme() + "\", expected \"{\" after this token");
        }

		nextSymbol();
		if (symbol.tokenType() == TokenType.NEWLINE)
		    nextSymbol();

		AstStatements expr = parseStatements();
		if (symbol.tokenType() != TokenType.RBRACE)
            logger.error(symbol.position(), "Syntax error on token \""
					+ previous.getLexeme() + "\", expected \"}\" after this token");
		nextSymbol();

		return new AstFunctionDefinition(new Position(startPos, expr.position), functionName.getLexeme(), params, type, expr, isConstructor);
	}

	private AstDefinition parseVarDefinition() {
		Position startPos = symbol.position();
		boolean isMutable = symbol.tokenType() == TokenType.KW_VAR;
		Symbol id = nextSymbol(TokenType.IDENTIFIER, "identifier");

		nextSymbol();

		if (symbol.tokenType() == TokenType.ASSIGN) {
			dump("var_definition -> variableDefinition identifier = expr");
			return new AstVariableDefinition(startPos, id.getLexeme(), null, isMutable);
		}
		else if (symbol.tokenType() != TokenType.COLON) {
            logger.error(previous.position(), "Syntax error on token \""
                    + previous.getLexeme() + "\", expected \":\"");
        }

		nextSymbol();

		dump("var_definition -> variableDefinition identifier : member");

        AstType type = parseType();
		return new AstVariableDefinition(new Position(startPos, type.position), id.getLexeme(), type, isMutable);
	}

	private AstImportDefinition parseImportDefinition() {
		Position pos = symbol.position();
		nextSymbol(TokenType.IDENTIFIER, "IDENTIFIER");

		String file = symbol.getLexeme();
		nextSymbol();

		if (symbol.tokenType() == TokenType.DOT) {
			nextSymbol();
			return parseImportDefinition_(new AstImportDefinition(pos, file));
		}
		else {
            return new AstImportDefinition(pos, file);
        }
	}

	private AstImportDefinition parseImportDefinition_(AstImportDefinition def) {
		switch (symbol.tokenType()) {
		case IDENTIFIER:
			def.selectedDefinitions.add(symbol.getLexeme());
			nextSymbol();
			return parseImportDefinition__(def);
		default:
            logger.error(symbol.position(),
					"Syntax error, expected \"IDENTIFIER\"");
			return null;
		}
	}

	private AstImportDefinition parseImportDefinition__(AstImportDefinition def) {
		switch (symbol.tokenType()) {
		case COMMA:
			nextSymbol();
			def.selectedDefinitions.add(symbol.getLexeme());
			nextSymbol();
			return parseImportDefinition__(def);
		default:
			return def;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private AstClassDefinition parseClassDefinition(boolean parseStructure) {
		Position start = symbol.position();
		nextSymbol();

		if (!symbol.isIdentifier()) {
            logger.error(symbol.position(), "Expected identifier");
        }

		String className = symbol.getLexeme();
		nextSymbol();

		AstType baseClass = null;

        // parse conformances
        ArrayList<AstType> conformances = parseConformances();

        if (conformances.size() > 0) {
            baseClass = conformances.get(0);
            conformances.remove(0);
        }

        if (symbol.tokenType() != TokenType.LBRACE) {
            logger.error(symbol.position(), "Expected \"{\"");
        }

		nextSymbol();

        ArrayList[] data = parseClassMemberDefinitions();
        ArrayList definitions = data[0];
        ArrayList defaultConstructor = data[1];
        ArrayList constructors = data[2];
        ArrayList staticConstructor = data[3];

		if (symbol.tokenType() != TokenType.RBRACE) {
            logger.error(symbol.position(), "Syntax error on token \"" + symbol.getLexeme() + "\", expected \"}\"");
        }

		Position end = symbol.position();
		Position definitionPosition = new Position(start, end);

		nextSymbol();

        if (parseStructure) {
            return new AstStructureDefinition(className, definitionPosition, baseClass, definitions, defaultConstructor, constructors);
        }

        return new AstClassDefinition(className, definitionPosition, baseClass, conformances, definitions, defaultConstructor, constructors, staticConstructor);
	}

	@SuppressWarnings("rawtypes")
	private ArrayList[] parseClassMemberDefinitions() {
        ArrayList<AstDefinition> definitions = new ArrayList<>();
        ArrayList<AstFunctionDefinition> constructors = new ArrayList<>();
        ArrayList<AstStatement> defaultConstructor = new ArrayList<>();
        ArrayList<AstStatement> staticConstructor = new ArrayList<>();

        if (symbol.tokenType() == TokenType.RBRACE) {
            return new ArrayList[] { definitions, defaultConstructor, constructors, staticConstructor };
        }

        if (symbol.tokenType() != TokenType.NEWLINE) {
            logger.error(symbol.position(), "Invalid tokenType");
        }
        nextSymbol();

		while (true) {
            if (symbol.tokenType() == TokenType.RBRACE) {
                return new ArrayList[] { definitions, defaultConstructor, constructors, staticConstructor };
            }

            AstDefinition definition = parseDefinition();

            if (definition instanceof AstFunctionDefinition && ((AstFunctionDefinition) definition).isConstructor) {
                constructors.add((AstFunctionDefinition) definition);
            }
            else {
                definitions.add(definition);

                if (definition instanceof AstVariableDefinition) {
                    if (symbol.tokenType() == TokenType.ASSIGN) {
                        nextSymbol();
                        dump("var_definition -> = expression");

                        AstVariableNameExpression varNameExpr = new AstVariableNameExpression(definition.position, ((AstVariableDefinition) definition).name);
                        AstExpression valueExpr = parseExpression();
                        AstBinaryExpression dotExpr = new AstBinaryExpression(
                                new Position(definition.position, valueExpr.position), AstBinaryExpression.DOT,
                                new AstVariableNameExpression(definition.position, Constants.selfParameterIdentifier), varNameExpr);
                        AstBinaryExpression assignExpr = new AstBinaryExpression(definition.position, AstBinaryExpression.ASSIGN, dotExpr, valueExpr);

                        if (!definition.isStatic()) {
                            defaultConstructor.add(assignExpr);
                        }
                        else {
                            staticConstructor.add(assignExpr);
                        }
                    }
                }
            }


            if (symbol.tokenType() == TokenType.RBRACE) {
                return new ArrayList[] { definitions, defaultConstructor, constructors, staticConstructor };
            }

            if (symbol.tokenType() == TokenType.NEWLINE || symbol.tokenType() == TokenType.SEMIC) {
                nextSymbol();
                continue;
            }

            logger.error(symbol.position(), "Consecutive statements must be separated by a separator");
        }
	}

	private ArrayList<AstType> parseConformances() {
        ArrayList<AstType> conformances = new ArrayList<>();

        if (symbol.tokenType() == TokenType.COLON) {
            do {
                nextSymbol();
                conformances.add(parseType());
            }
            while (symbol.tokenType() == TokenType.COMMA);
        }

        return conformances;
    }

	private AstInterfaceDefinition parseInterfaceDefinition() {
        Position start = symbol.position();
        String interfaceName = nextSymbol(TokenType.IDENTIFIER, "identifier").getLexeme();
        nextSymbol();

        if (symbol.tokenType() != TokenType.LBRACE) {
            logger.error(symbol.position(), "Expected \"{\"");
        }

        nextSymbol();

        if (symbol.tokenType() == TokenType.NEWLINE) {
            nextSymbol();
        }

        ArrayList<AstDefinition> defs = parseInterfaceDefinitions();
        Position positition = start;

        if (defs.size() > 0) {
            positition = new Position(start, defs.get(defs.size() - 1).position);
        }

        if (symbol.tokenType() != TokenType.RBRACE) {
            logger.error(symbol.position(), "Expected \"}\"");
        }

        nextSymbol();

        return new AstInterfaceDefinition(positition, interfaceName, new AstDefinitions(positition, defs));
    }

    private ArrayList<AstDefinition> parseInterfaceDefinitions() {
        ArrayList<AstDefinition> defs = new ArrayList<>();

        if (symbol.tokenType() == TokenType.NEWLINE) {
            nextSymbol();
        }
        while (symbol.tokenType() == TokenType.KW_FUN) {
            defs.add(parseFunctionPrototype());

            if (symbol.tokenType() == TokenType.SEMIC) {
                nextSymbol();
            }
            if (symbol.tokenType() == TokenType.NEWLINE) {
                nextSymbol();
            }
        }

	    return defs;
    }

    private AstFunctionDefinition parseFunctionPrototype() {
        Position startPos = symbol.position();

        if (symbol.tokenType() == TokenType.KW_FUN) {
            Symbol functionName = nextSymbol(TokenType.IDENTIFIER, "identifier");

            nextSymbol(TokenType.LPARENT, "(");
            nextSymbol();
            dump("function_prototype -> func identifier ( parameters ) function_prototype'");

            ArrayList<AstParameterDefinition> params = parseParameters();

            AstType type;

            if (symbol.tokenType() == TokenType.NEWLINE || symbol.tokenType() == TokenType.SEMIC) {
                dump("function_prototype' -> $ ");
                type = new AstAtomType(symbol.position(), AtomTypeKind.VOID);
            }
            else {
                dump("function_prototype' -> type ");
                type = parseType();
            }

            return new AstFunctionDefinition(new Position(startPos, type.position), functionName.getLexeme(), params, type, new AstStatements(startPos, new ArrayList<>()));
        }
        logger.error(previous.position(), "Syntax error on token \"" + previous.getLexeme() + "\", expected keyword \"func\"");

        return null;
    }

	private AstEnumDefinition parseEnumDefinition() {
		Position start = symbol.position();
		String name = nextSymbol(TokenType.IDENTIFIER, "IDENTIFIER").getLexeme();
		AstType type = null;

		nextSymbol();
		if (symbol.tokenType() == TokenType.COLON) {
			nextSymbol();
			type = parseType();
		}

		if (symbol.tokenType() != TokenType.LBRACE)
            logger.error(symbol.position(), "Enum must begin with \"{\"");

		nextSymbol(TokenType.NEWLINE, "\n");
		nextSymbol(TokenType.KW_CASE, "CASE");

        ArrayList<AstDefinition> enumDefinitions = parseEnumMemberDefinitions();
		if (symbol.tokenType() != TokenType.RBRACE)
            logger.error(symbol.position(), "Syntax error on token \""
					+ symbol.getLexeme() + "\", expected \"}\"");
		nextSymbol();

		Position end = enumDefinitions.get(enumDefinitions.size() - 1).position;
		return new AstEnumDefinition(new Position(start, end), name, enumDefinitions, (AstAtomType) type);
	}

	private ArrayList<AstDefinition> parseEnumMemberDefinitions() {
        ArrayList<AstDefinition> definitions = new ArrayList<>();

		while (true) {
			AstDefinition definition = parseDefinition();
			definitions.add(definition);

			if (symbol.tokenType() == TokenType.COMMA) {
				while (symbol.tokenType() == TokenType.COMMA) {
					nextSymbol();
					if (symbol.tokenType() != TokenType.IDENTIFIER)
                        logger.error(symbol.position(), "Expected identifier after comma in enums member definition");
					definitions.add(parseEnumCaseDefinition());
				}
			}

			if (symbol.tokenType() == TokenType.NEWLINE) {
				nextSymbol();
				if (symbol.tokenType() == TokenType.RBRACE)
					break;

				if (symbol.tokenType() == TokenType.IDENTIFIER)
                    logger.error(symbol.position(), "Enum member definition must begin with \"case\" keyword");
			}
		}

		if (symbol.tokenType() != TokenType.RBRACE)
            logger.error("todo");

		return definitions;
	}

	private AstEnumMemberDefinition parseEnumCaseDefinition() {
		if (symbol.tokenType() == TokenType.KW_CASE)
			nextSymbol(TokenType.IDENTIFIER, "identifier");
		AstVariableNameExpression name = new AstVariableNameExpression(symbol.position(), symbol.getLexeme());

		nextSymbol();
		if (symbol.tokenType() == TokenType.ASSIGN) {
			nextSymbol();

			AstExpression value = parseExpression();
			if (!(value instanceof AstAtomConstExpression))
                logger.error(value.position, "Raw value for enums definition must be literal");

			Position definitionPos = new Position(name.position, value.position);
			return new AstEnumMemberDefinition(definitionPos, name, (AstAtomConstExpression) value);
		}
		return new AstEnumMemberDefinition(name.position, name, null);
	}

	private AstExtensionDefinition parseExtensionDefinition() {
	    nextSymbol();
        if (!symbol.isIdentifier()) {
            logger.error(symbol.position(), "Expected memberType getName");
        }

        AstType type = parseChildType();

        ArrayList<AstType> conformances = parseConformances();

        if (symbol.tokenType() != TokenType.LBRACE) {
            logger.error(symbol.position(), "Expected \"{\"");
        }

        nextSymbol();

        AstDefinitions defs = parseDefinitions();

        if (symbol.tokenType() != TokenType.RBRACE) {
            logger.error(symbol.position(), "Expected \"}\"");
        }

        nextSymbol();

        List<AstDefinition> variableDefinitions = defs.definitions.stream().filter(def -> def instanceof AstVariableDefinition).collect(Collectors.toList());
        if (!variableDefinitions.isEmpty()) {
            logger.error(variableDefinitions.get(0).position, "Extension cannot contain variable definitions");
        }

        return new AstExtensionDefinition(
                new Position(symbol.position(), defs.position),
                type.getName(),
                type,
                defs,
                conformances);
    }

	private AstType parseType() {
		AstType type = parseChildType();

		if (symbol.tokenType() == TokenType.QMARK || symbol.tokenType() == TokenType.NOT) {
		    boolean isForced = symbol.tokenType() == TokenType.NOT;

			Position pos = new Position(type.position, symbol.position());
			nextSymbol();

			return new AstOptionalType(pos, type, isForced);
		}

		return type;
	}

	private AstType parseChildType() {
		Symbol s = symbol;

		switch (symbol.tokenType()) {
		case IDENTIFIER:
            if (symbol.getLexeme().equals("Bool")) {
                dump("memberType -> logical");
                nextSymbol();

                return new AstAtomType(s.position(), AtomTypeKind.LOG);
            }
            if (symbol.getLexeme().equals("Int")) {
                dump("memberType -> integer");
                nextSymbol();

                return new AstAtomType(s.position(), AtomTypeKind.INT);
            }
            if (symbol.getLexeme().equals("Double")) {
                dump("memberType -> double");
                nextSymbol();

                return new AstAtomType(s.position(), AtomTypeKind.DOB);
            }
            if (symbol.getLexeme().equals("String")) {
                dump("memberType -> string");
                nextSymbol();

                return new AstAtomType(s.position(), AtomTypeKind.STR);
            }
            if (symbol.getLexeme().equals("Char")) {
                dump("memberType -> char");
                nextSymbol();

                return new AstAtomType(s.position(), AtomTypeKind.CHR);
            }
            if (symbol.getLexeme().equals("Void")) {
                dump("memberType -> void");
                nextSymbol();

                return new AstAtomType(s.position(), AtomTypeKind.VOID);
            }

			dump("memberType -> identifier");
			nextSymbol();

			return new AstTypeName(s.position(), s.getLexeme());
		case LBRACKET:
			nextSymbol();
			dump("memberType -> [ memberType ]");
			AstType type = parseType();

			if (symbol.tokenType() != TokenType.RBRACKET)
                logger.error(symbol.position(), "Syntax error, insert \"]\"");

			nextSymbol();
			return new AstListType(new Position(s.position(), type.position), 0,
					type);
		case LPARENT:
		    // TODO
//			Position start = symbol.position();
//			dump("memberType -> tuple type");
//			nextSymbol();
//
//            ArrayList<AstLabeledExpr> expressions = new ArrayList<>();
//			if (symbol.tokenType() != TokenType.RPARENT) {
//				while (true) {
//					if (symbol.tokenType() == TokenType.IDENTIFIER) {
//					    Position startPos = symbol.position();
//					    String identifier = symbol.getLexeme();
//					    nextSymbol();
//					    if (symbol.tokenType() == TokenType.COLON) {
//					        nextSymbol();
//					        AstType expressionType = parseType();
//					        expressions.add(new AstLabeledExpr(new Position(startPos, expressionType.position), expressionType, identifier));
//                        }
//                        else {
//					        logger.error(symbol.position(), "Expected ':'");
//                        }
//                    }
//                    else {
//
//                    }
//					if (symbol.tokenType() != TokenType.COMMA)
//						break;
//					nextSymbol();
//				}
//			}
//			if (symbol.tokenType() != TokenType.RPARENT) {
//                logger.error(symbol.position(), "Syntax error, insert \")\" to complete function declaration");
//            }
//
//			nextSymbol(TokenType.ARROW, "->");
//			nextSymbol();
//
//			type = parseType();
//			return new AstFunctionType(new Position(start, type.position), expressions, type);
		default:
            logger.error(symbol.position(), "Syntax error on token \""
					+ symbol.getLexeme() + "\", expected \"variable member\"");
		}

		return null;
	}

	private ArrayList<AstParameterDefinition> parseParameters() {
		if (symbol.tokenType() == TokenType.RPARENT) {
			nextSymbol();
			return new ArrayList<>();
		}

		dump("parameters -> parameter parameters'");

		AstParameterDefinition paramater = parseParameter();

        ArrayList<AstParameterDefinition> params = new ArrayList<>();
		params.add(paramater);
		params.addAll(parseParameters_());

		return params;
	}

	private Vector<AstParameterDefinition> parseParameters_() {
		if (symbol.tokenType() == TokenType.COMMA) {
			dump("parameters' -> parameters");
			nextSymbol();

			AstParameterDefinition parameter = parseParameter();
			Vector<AstParameterDefinition> params = new Vector<>();
			params.add(parameter);
			params.addAll(parseParameters_());
			return params;
		} else if (symbol.tokenType() != TokenType.RPARENT)
            logger.error(symbol.position(),
					"Syntax error, insert \")\" to complete function declaration");

		dump("parameters' -> e");
		nextSymbol();

		return new Vector<>();
	}

	private AstParameterDefinition parseParameter() {
		if (symbol.tokenType() == TokenType.IDENTIFIER) {
			Symbol argumentLabel = symbol;
			String parameterName = null;

			nextSymbol();

			if (symbol.tokenType() == TokenType.IDENTIFIER) {
                parameterName = symbol.getLexeme();
                nextSymbol();
            }
			else if (symbol.tokenType() != TokenType.COLON)
                logger.error(symbol.position(),
                        "Syntax error, expected paramater definition");
			nextSymbol();

			dump("parameter -> identifier : memberType");

			AstType type = parseType();
			return new AstParameterDefinition(new Position(argumentLabel.position(), type.position),
					parameterName, argumentLabel.getLexeme(), type);
		}

        logger.error(symbol.position(),
				"Syntax error, expected paramater definition");

		return null;
	}

	private Vector<AstExpression> parseExpressions() {
		switch (symbol.tokenType()) {
		case ADD:
		case SUB:
		case NOT:
		case AND:
		case MUL:
		case LOG_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case INT_CONST:
		case STR_CONST:
		case LBRACE:
		case LPARENT:
		case LBRACKET:
		case IDENTIFIER:
		case KW_RETURN:
			dump("expressions -> expression expression'");
            AstExpression e = parseExpression();
			Vector<AstExpression> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());
			return expressions;
		default:
            logger.error(symbol.position(), "Syntax error on token \""
					+ symbol.getLexeme() + "\", delete this token");
		}

		return null;
	}

	private Vector<AstExpression> parseExpressions_() {
		switch (symbol.tokenType()) {
		case COMMA:
			dump("expressions' -> , expression expression'");
			nextSymbol();

			AstExpression e = parseExpression();

			Vector<AstExpression> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());

			return expressions;
		case RPARENT:
			dump("expressions' -> e");
			nextSymbol();
			break;
		default:
            logger.error(symbol.position(), "Syntax error on token \""
					+ previous.getLexeme()
					+ "\", expected \",\" or \")\" to end expression");
		}
		return new Vector<>();
	}

	private AstExpression parseExpression() {
		switch (symbol.tokenType()) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case LPARENT:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case IDENTIFIER:
		case KW_RETURN:
			dump("expression -> logical_ior_expression");
			return parseExpression_(parseIorExpression());
		default:
            logger.error(symbol.position(), "Syntax error on token \"" + symbol.getLexeme() + "\", delete this token");
		}

		return null;
	}

	private AstExpression parseExpression_(AstExpression e) {
		switch (symbol.tokenType()) {
		case SEMIC:
		case COLON:
		case NEWLINE:
		case RPARENT:
		case RBRACE:
		case RBRACKET:
		case KW_ELSE:
		case LBRACE:
		case IDENTIFIER:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("expression' -> e");
			return e;
		case ASSIGN:
			nextSymbol();
			AstExpression e2 = parseExpression();
			return new AstBinaryExpression(new Position(e.position, e2.position), AstBinaryExpression.ASSIGN, e, e2);
		default:
            logger.error(symbol.position(), "Syntax error on token \""
					+ symbol.getLexeme() + "\", delete this token");
		}

		return null;
	}

	private AstExpression parseIorExpression() {
		switch (symbol.tokenType()) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("logical_ior_expression -> logical_and_expression logical_ior_expression'");
			return parseIorExpression_(parseAndExpression());
		default:
            logger.error(symbol.position(), "Syntax error on token \""
					+ symbol.getLexeme() + "\", delete this token");
		}

		return null;
	}

	private AstExpression parseIorExpression_(AstExpression e) {
		switch (symbol.tokenType()) {
		case IOR:
			dump("logical_ior_expression' -> | log_ior_expression");
			nextSymbol();
			AstExpression expr = parseAndExpression();
			return parseIorExpression_(new AstBinaryExpression(new Position(e.position, expr.position), AstBinaryExpression.IOR, e, expr));
		case SEMIC:
		case COLON:
		case NEWLINE:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("logical_ior_expression' -> e");
			return e;
		default:
            logger.error(symbol.position(), "Syntax error on token \"symbol.getLexeme()\", delete this token");
		}

		return null;
	}

	private AstExpression parseAndExpression() {
		switch (symbol.tokenType()) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("logical_and_expression -> logical_compare_expression logical_and_expression'");
			return parseAndExpression_(parseCmpExpression());
		default:
            logger.error(symbol.position(), "Syntax error on token \""
					+ symbol.getLexeme() + "\", delete this token");
		}

		return null;
	}

	private AstExpression parseAndExpression_(AstExpression e) {
		switch (symbol.tokenType()) {
        case AND:
			dump("logical_and_expression' -> & logical_and_expression");
			nextSymbol();

			AstExpression expr = parseCmpExpression();
			return parseAndExpression_(new AstBinaryExpression(new Position(e.position, expr.position), AstBinaryExpression.AND, e, expr));
        case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("logical_and_expression' -> e");
			return e;
        case NOT:
        case EQU:
        case KW_IS:
        case KW_AS:
        case NEQ:
        case LTH:
        case GTH:
        case LEQ:
        case GEQ:
        case MUL:
        case DIV:
        case MOD:
        case ADD:
        case SUB:
        case LPARENT:
        case LBRACKET:
        case DOT:
        case ARROW:
        case QMARK:
        case EMARK:
        case KW_FUN:
        case KW_IF:
        case KW_VAR:
        case KW_WHILE:
        case KW_STRUCT:
        case KW_IMPORT:
        case KW_LET:
        case KW_NULL:
        case KW_CLASS:
        case KW_INTERFACE:
        case KW_IN:
        case KW_RETURN:
        case KW_PUBLIC:
        case KW_PRIVATE:
        case KW_CONTINUE:
        case KW_BREAK:
        case KW_SWITCH:
        case KW_CASE:
        case KW_DEFAULT:
        case KW_ENUM:
        case KW_INIT:
        case LOG_CONST:
        case INT_CONST:
        case STR_CONST:
        case DOUBLE_CONST:
        case CHAR_CONST:
            break;
        default:
            logger.error(symbol.position(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseCmpExpression() {
		switch (symbol.tokenType()) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("compare_expression -> add_expression compare_expression'");
			return parseCmpExpression_(parseAddExpression());
		default:
            logger.error(symbol.position(), "Syntax error on tokenType \"" + symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseCmpExpression_(AstExpression e) {
		AstExpression expr = null;
		int oper = -1;

		switch (symbol.tokenType()) {
		case AND:
		case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case KW_FOR:
		case COMMA:
		case EOF:
			dump("compare_expression' -> e");
			return e;
		case EQU:
			dump("compare_expression' -> == compare_expression");
			nextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.EQU;
			break;
        case KW_IS:
            dump("compare_expression' -> is compare_expression");
            nextSymbol();

            expr = parseAddExpression();
            oper = AstBinaryExpression.IS;
            break;

        case KW_AS:
            dump("compare_expression' -> as identifier");
            nextSymbol();

            expr = new AstVariableNameExpression(symbol.position(), symbol.getLexeme());;
            nextSymbol();

            oper = AstBinaryExpression.AS;
            break;
		case NEQ:
			dump("compare_expression' -> != compare_expression");
			nextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.NEQ;
			break;
		case GTH:
			dump("compare_expression' -> > compare_expression");
			nextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.GTH;
			break;
		case LTH:
			dump("compare_expression' -> < compare_expression");
			nextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.LTH;
			break;
		case GEQ:
			dump("compare_expression' -> >= compare_expression");
			nextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.GEQ;
			break;
		case LEQ:
			dump("compare_expression' -> <= compare_expression");
			nextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.LEQ;
			break;
		default:
            logger.error(symbol.position(), "Syntax error on tokenType \"" + symbol.getLexeme() + "\", delete this tokenType");
		}

		return new AstBinaryExpression(new Position(e.position, expr.position), oper, e, expr);
	}

	private AstExpression parseAddExpression() {
		switch (symbol.tokenType()) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("add_expression -> multiplicative_expression add_expression'");

			return parseAddExpression_(parseMulExpression());
		default:
            logger.error(symbol.position(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseAddExpression_(AstExpression e) {
		AstExpression expr = null;

		switch (symbol.tokenType()) {
		case AND:
		case IOR:
		case SEMIC:
		case COLON:
		case NEWLINE:
		case RPARENT:
		case ASSIGN:
		case IDENTIFIER:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case COMMA:
		case EOF:
		case EQU:
        case KW_IS:
        case KW_AS:
		case NEQ:
		case GTH:
		case LTH:
		case GEQ:
		case LEQ:
		case KW_FOR:
			dump("add_expression' -> e");
			return e;
		case ADD:
			dump("add_expression' -> multiplicative_expression add_expresison'");
			nextSymbol();

			expr = parseMulExpression();
			return parseAddExpression_(new AstBinaryExpression(new Position(e.position,
					expr.position), AstBinaryExpression.ADD, e, expr));
		case SUB:
			dump("add_expression' -> - add_expression");
			nextSymbol();

			expr = parseMulExpression();
			return parseAddExpression_(new AstBinaryExpression(e.position,
					AstBinaryExpression.SUB, e, expr));
		default:
            logger.error(symbol.position(), "Syntax error on parseAddExpression_");
		}

		return null;
	}

	private AstExpression parseMulExpression() {
		switch (symbol.tokenType()) {
		case ADD:
		case SUB:
		case NOT:
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("multiplicative_expression -> prefix_expression multiplicative_expression'");
			return parseMulExpression_(parsePrefixExpression());
		default:
            logger.error(symbol.position(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected prefix expression");
		}

		return null;
	}

	private AstExpression parseMulExpression_(AstExpression e) {
		AstExpression expr = null;
		int oper = -1;

		switch (symbol.tokenType()) {
		case AND:
		case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case ASSIGN:
		case RBRACE:
		case LBRACE:
		case IDENTIFIER:
		case RBRACKET:
		case KW_ELSE:
		case COMMA:
		case EOF:
		case EQU:
        case KW_IS:
        case KW_AS:
		case NEQ:
		case GTH:
		case LTH:
		case GEQ:
		case LEQ:
		case ADD:
		case SUB:
		case KW_FOR:
			dump("multiplicative_expression' -> e");
			return e;
		case MUL:
			oper = AstBinaryExpression.MUL;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			nextSymbol();
			expr = parsePrefixExpression();
			break;
		case DIV:
			oper = AstBinaryExpression.DIV;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			nextSymbol();
			expr = parsePrefixExpression();
			break;
		case MOD:
			oper = AstBinaryExpression.MOD;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			nextSymbol();
			expr = parsePrefixExpression();
			break;
		default:
            logger.error(symbol.position(), "Syntax error on token \"" + symbol.getLexeme() + "\", delete this token");
		}

		expr = new AstBinaryExpression(new Position(e.position, expr.position), oper, e, expr);

//        if (expr instanceof AstBinaryExpression) {
//            AstBinaryExpression binExpr = (AstBinaryExpression) expr;
//
//            // TODO: - Fix this (object.data[index] is not parsed in the right way at the moment
//            // so this is a temporary solution
//            if (binExpr.oper == AstBinaryExpression.DOT && binExpr.expr2 instanceof AstBinaryExpression) {
//                AstBinaryExpression binExpr2 = (AstBinaryExpression) binExpr.expr2;
//
//                if (binExpr2.oper == AstBinaryExpression.ARR) {
//                    expr = new AstBinaryExpression(binExpr.position, AstBinaryExpression.ARR,
//                            new AstBinaryExpression(binExpr.expr1.position, AstBinaryExpression.DOT, binExpr.expr1, binExpr2.expr1), binExpr2.expr2);
//                }
//            }
//        }

        return parseMulExpression_(expr);
	}

	private AstExpression parsePrefixExpression() {
		AstExpression e;
		Symbol op = symbol;

		switch (symbol.tokenType()) {
		case ADD:
			dump("prefix_expression -> + prefix_expression");
			nextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.position(), e.position), AstUnaryExpression.ADD, e);
		case SUB:
			dump("prefix_expression -> - prefix_expression");
			nextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.position(), e.position), AstUnaryExpression.SUB, e);
		case NOT:
			dump("prefix_expression -> ! prefix_expression");
			nextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.position(), e.position), AstUnaryExpression.NOT, e);
		case AND:
			dump("prefix_expression -> & prefix_expression");
			nextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.position(), e.position), AstUnaryExpression.MEM, e);
		case MUL:
			dump("prefix_expression -> * prefix_expression");
			nextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.position(), e.position), AstUnaryExpression.VAL, e);
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
			dump("prefix_expression -> postfix_expression");
			return parsePostfixExpression();
		default:
            logger.error(symbol.position(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parsePostfixExpression() {
		switch (symbol.tokenType()) {
		case LOG_CONST:
		case INT_CONST:
		case STR_CONST:
		case CHAR_CONST:
		case DOUBLE_CONST:
		case KW_NULL:
		case LBRACKET:
		case KW_IF:
		case KW_WHILE:
		case KW_FOR:
		case LPARENT:
		case IDENTIFIER:
		case KW_RETURN:
		case QMARK:
		case EMARK:
			dump("postfix_expression -> atom_expression postfix_expression'");
			return parsePostfixExpression_(parseDotExpression());
		default:
            logger.error(symbol.position(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parsePostfixExpression_(AstExpression e) {
		Position symbolPos = symbol.position();

		switch (symbol.tokenType()) {
		case AND:
		case IOR:
		case SEMIC:
		case NEWLINE:
		case COLON:
		case RPARENT:
		case IDENTIFIER:
		case ASSIGN:
		case RBRACE:
		case LBRACE:
		case RBRACKET:
		case KW_ELSE:
		case COMMA:
		case EOF:
        case EQU:
        case KW_IS:
        case KW_AS:
		case NEQ:
		case GTH:
		case LTH:
		case GEQ:
		case LEQ:
		case ADD:
		case SUB:
		case MUL:
		case DIV:
		case MOD:
		case KW_FOR:
			dump("postfix_expression' -> e");
			return e;
		case LBRACKET:
			dump("postfix_expression' -> [ expression ] postfix_expression'");
			nextSymbol();
			AstExpression expr = parseExpression();
			if (symbol.tokenType() != TokenType.RBRACKET)
                logger.error(previous.position(),
						"Syntax error, insert \"]\" to complete expression");
			nextSymbol();

			return parsePostfixExpression_(new AstBinaryExpression(new Position(e.position, expr.position), AstBinaryExpression.ARR, e, expr));
		case QMARK:
			dump("postfix_expression' -> optional evaluation expression");
			nextSymbol();
			return new AstOptionalEvaluationExpression(new Position(e.position, symbolPos), e);
		case NOT:
			dump("postfix_expression' -> force value expression");
			nextSymbol();
			return new AstForceValueExpression(new Position(e.position, symbolPos), e);
        case DOT:
		default:
            logger.error(symbol.position(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseDotExpression() {
        switch (symbol.tokenType()) {
            case LOG_CONST:
            case INT_CONST:
            case STR_CONST:
            case CHAR_CONST:
            case DOUBLE_CONST:
            case KW_NULL:
            case LBRACKET:
            case KW_IF:
            case KW_WHILE:
            case KW_FOR:
            case LPARENT:
            case IDENTIFIER:
            case KW_RETURN:
            case QMARK:
            case EMARK:
                dump("dot_expression -> atom_expression dot_expression'");
                return parseDotExpression_(parseAtomExpression());
            default:
                logger.error(symbol.position(), "Syntax error on tokenType \""
                        + symbol.getLexeme() + "\", delete this tokenType");
        }

        return null;
    }

    private AstExpression parseDotExpression_(AstExpression e) {
        Position symbolPos = symbol.position();

        switch (symbol.tokenType()) {
            case AND:
            case IOR:
            case SEMIC:
            case NEWLINE:
            case COLON:
            case RPARENT:
            case IDENTIFIER:
            case ASSIGN:
            case RBRACE:
            case LBRACE:
            case RBRACKET:
            case KW_ELSE:
            case COMMA:
            case EOF:
            case EQU:
            case KW_IS:
            case KW_AS:
            case NEQ:
            case GTH:
            case LTH:
            case GEQ:
            case LEQ:
            case ADD:
            case SUB:
            case MUL:
            case DIV:
            case MOD:
            case KW_FOR:
                dump("dot_expression' -> e");
                return e;
            case DOT:
//                dump("dot_expression' -> atom_expression dot'");
                nextSymbol();
                AstExpression expr = parseAtomExpression();
                AstBinaryExpression bin = new AstBinaryExpression(new Position(e.position, expr.position), AstBinaryExpression.DOT, e, expr);
                while(symbol.tokenType() == TokenType.DOT) {
                    nextSymbol();
                    expr = parseAtomExpression();
                    bin = new AstBinaryExpression(new Position(bin.position, expr.position), AstBinaryExpression.DOT, bin, expr);
                }
                return bin;
            case LBRACKET:
                dump("postfix_expression' -> [ expression ] postfix_expression'");
                nextSymbol();
                expr = parseExpression();
                if (symbol.tokenType() != TokenType.RBRACKET)
                    logger.error(previous.position(),
                            "Syntax error, insert \"]\" to complete expression");
                nextSymbol();

                return parsePostfixExpression_(new AstBinaryExpression(new Position(e.position, expr.position), AstBinaryExpression.ARR, e, expr));
            default:
                logger.error(symbol.position(), "Syntax error on tokenType \""
                        + symbol.getLexeme() + "\", delete this tokenType");
        }

        return null;
    }

    private AstExpression parseAtomExpression() {
		Symbol current = symbol;

		switch (symbol.tokenType()) {
		case LOG_CONST:
			dump("atom_expression -> log_const");
			nextSymbol();

			return new AstAtomConstExpression(current.position(), AtomTypeKind.LOG,
					current.getLexeme());
		case INT_CONST:
			dump("atom_expression -> int_const");
			nextSymbol();

			return new AstAtomConstExpression(current.position(), AtomTypeKind.INT,
					current.getLexeme());
		case STR_CONST:
			dump("atom_expression -> str_const");
			nextSymbol();

			return new AstAtomConstExpression(current.position(), AtomTypeKind.STR,
					current.getLexeme());
		case CHAR_CONST:
			dump("atom_expression -> char_const");
			nextSymbol();

			return new AstAtomConstExpression(current.position(), AtomTypeKind.CHR,
					current.getLexeme());
		case DOUBLE_CONST:
			dump("atom_expression -> double_const");
			nextSymbol();

			return new AstAtomConstExpression(current.position(), AtomTypeKind.DOB,
					current.getLexeme());
		case KW_NULL:
			dump("atom_expression -> nil");
			nextSymbol();

			return new AstAtomConstExpression(current.position(), AtomTypeKind.NIL,
					current.getLexeme());
		case IDENTIFIER:
			nextSymbol();
			if (symbol.tokenType() == TokenType.LPARENT) {
				nextSymbol();

				if (symbol.tokenType() == TokenType.RPARENT) {
					dump("atom_expression -> identifier ( )");
					nextSymbol();
					return new AstFunctionCallExpression(symbol.position(), current.getLexeme(), new ArrayList<>());
				}

				dump("atom_expression -> identifier ( expressions )");

				// TODO: - Optimize this
				// FIXME: - Function arguments should be parsed with their own method
                ArrayList<AstExpression> arguments = parseTupleExpressions(true);

                ArrayList<AstLabeledExpr> absExprs = new ArrayList<>();
				for (AstExpression e : arguments)
					absExprs.add((AstLabeledExpr) e);

				if (symbol.tokenType() != TokenType.RPARENT)
                    logger.error(symbol.position(), "Expected ')'");
				nextSymbol();

				return new AstFunctionCallExpression(new Position(current.position(), absExprs.get(absExprs.size() - 1).position), current.getLexeme(), absExprs);
			} else {
				dump("atom_expression -> identifier");
				return new AstVariableNameExpression(current.position(), current.getLexeme());
			}
		case LBRACKET:
			return parseBracket();
		case KW_RETURN:
			Position pos = symbol.position();
			nextSymbol();
			// FIXME
			if (symbol.tokenType() == TokenType.SEMIC || symbol.tokenType() == TokenType.NEWLINE) {
				dump("atom_expression -> return");
				return new AstReturnExpression(pos, null);
			}
			dump("atom_expression -> return expression");
			AstExpression e = parseExpression();
			return new AstReturnExpression(new Position(pos, e.position), e);
		case LPARENT:
			return parseTupleExpression(false);
		default:
            logger.error(symbol.position(), "Syntax error on token \"" + symbol.getLexeme() + "\", delete this token");
		}
		return null;
	}

	private AstConditionalStatement parseForLoop() {
		if (symbol.tokenType() == TokenType.KW_FOR) {
			Position start = symbol.position();
			Symbol count = nextSymbol(TokenType.IDENTIFIER, "identifier");
			nextSymbol();

			if (symbol.tokenType() != TokenType.KW_IN)
                logger.error(previous.position(), "Syntax error on token \""
						+ previous.getLexeme()
						+ "\", expected keyword \"in\" after this token");
			nextSymbol();

			AstExpression e = parseExpression();
			if (symbol.tokenType() != TokenType.LBRACE)
                logger.error(previous.position(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"{\" after this tokenType");

			nextSymbol(TokenType.NEWLINE, "NEWLINE");
			nextSymbol();

			AstStatements s = parseStatements();

			if (symbol.tokenType() != TokenType.RBRACE)
                logger.error(previous.position(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"}\" after this tokenType");
			nextSymbol();

			return new AstForStatement(
			        new Position(start, s.position),
                    new AstVariableNameExpression(
                            count.position(),
                            count.getLexeme()),
                    e,
                    s);
		}

        logger.error(symbol.position(), "Syntax error, expected keyword \"for\"");

		return null;
	}

	private AstConditionalStatement parseWhileLoop() {
		if (symbol.tokenType() == TokenType.KW_WHILE) {
			Position start = symbol.position();
			nextSymbol();
			AstExpression e1 = parseExpression();
			if (symbol.tokenType() == TokenType.LBRACE) {
				nextSymbol(TokenType.NEWLINE, "NEWLINE");
				nextSymbol();

				AstStatements s = parseStatements();

				if (symbol.tokenType() != TokenType.RBRACE)
                    logger.error(symbol.position(), "Syntax error on token \""
							+ previous.getLexeme()
							+ "\", expected '}' after this token");
				nextSymbol();

				return new AstWhileStatement(new Position(start, s.position), e1, s);
			}
            logger.error(previous.position(), "Syntax error on token \""
					+ previous.getLexeme() + "\", expected \"{\" after this token");
		}
        logger.error(previous.position(),
				"Syntax error, expected keyword \"while\"");

		return null;
	}

	private Condition parseIfCondition() {
		dump("if_expression -> if epression { statements }");

		nextSymbol();
		AstExpression condition = parseExpression();
		if (symbol.tokenType() != TokenType.LBRACE)
            logger.error(symbol.position(), "Syntax error on token \""
					+ previous.getLexeme() + "\", expected '{' after this token");
		nextSymbol(TokenType.NEWLINE, "NEWLINE");
		nextSymbol();
		AstStatements s = parseStatements();
		if (symbol.tokenType() != TokenType.RBRACE)
            logger.error(symbol.position(), "Syntax error on token \""
					+ previous.getLexeme() + "\", expected '}' after this token");
		nextSymbol();
		return new Condition(condition, s);
	}

	private AstIfStatement parseIf() {
		if (symbol.tokenType() == TokenType.KW_IF) {
			Position start = symbol.position();
			return parseIf_(start, parseIfCondition());
		}
        logger.error(previous.position(),
				"Syntax error, expected keyword \"while\"");
		return null;
	}

	private AstIfStatement parseIf_(Position start, Condition condition) {
		if (symbol.tokenType() == TokenType.NEWLINE)
			nextSymbol();

        ArrayList<Condition> conditions = new ArrayList<>();
		conditions.add(condition);
		AstStatements elseBody = null;

		while (true) {
			if (symbol.tokenType() == TokenType.KW_ELSE) {
				nextSymbol();

				if (symbol.tokenType() == TokenType.KW_IF) {
					dump("if_expression' -> else if { statements }");
					conditions.add(parseIfCondition());

					if (symbol.tokenType() == TokenType.NEWLINE)
						nextSymbol();
					continue;
				}

				if (symbol.tokenType() != TokenType.LBRACE)
                    logger.error(symbol.position(), "Syntax error on token \""
							+ previous.getLexeme() + "\", expected '{' after this token");

				dump("if_expression' -> else { statements }");

				nextSymbol(TokenType.NEWLINE, "NEWLINE");
				nextSymbol();

				elseBody = parseStatements();
				nextSymbol();
			}
			break;
		}

		Position lastPos = elseBody != null ?
				elseBody.position : conditions.get(conditions.size() - 1).body.position;
		return new AstIfStatement(new Position(condition.condition.position, lastPos), conditions, elseBody);
	}

	private AstSwitchStatement parseSwitch() {
		if (symbol.tokenType() != TokenType.KW_SWITCH)
            logger.error(symbol.position(),
					"Syntax error, expected keyword \"switch\"");

		Position start = symbol.position();
		nextSymbol();

		AstExpression subjectExpr = parseExpression();
		nextSymbol(TokenType.NEWLINE, "newline");
		nextSymbol();

		if (symbol.tokenType() != TokenType.KW_CASE)
            logger.error(symbol.position(), "Syntax error, \"switch\" must be followed by at least one \"case\" statement");

        ArrayList<AstCaseStatement> cases = new ArrayList<>();
		AstStatements defaultBody = null;

		while (symbol.tokenType() == TokenType.KW_CASE)
			cases.add(parseCase());

		if (symbol.tokenType() == TokenType.KW_DEFAULT) {
			nextSymbol(TokenType.COLON, ":");
			nextSymbol(TokenType.NEWLINE, "newline");
			nextSymbol();
			defaultBody = parseStatements();
		}

		if (symbol.tokenType() != TokenType.RBRACE)
            logger.error(symbol.position(),
					"Syntax error, expected \"}\"");
		nextSymbol();

		Position switchPos = new Position(start,
				defaultBody != null ? defaultBody.position : cases.get(cases.size() - 1).position);

		return new AstSwitchStatement(switchPos, subjectExpr, cases, defaultBody);
	}

	private AstCaseStatement parseCase() {
		if (symbol.tokenType() != TokenType.KW_CASE)
            logger.error(symbol.position(),
					"Syntax error, expected keyword \"case\"");

		Position start = symbol.position();
		nextSymbol();

        ArrayList<AstExpression> expressions = new ArrayList<>();
		expressions.add(parseExpression());

		// join conditions
		while (symbol.tokenType() == TokenType.COMMA) {
			nextSymbol();
			expressions.add(parseExpression());
		}

		if (symbol.tokenType() != TokenType.COLON)
            logger.error(symbol.position(),
					"Syntax error, case expression must be followed by \":\"");
		nextSymbol();

		if (symbol.tokenType() == TokenType.NEWLINE)
			nextSymbol();

		if (symbol.tokenType() == TokenType.KW_CASE || symbol.tokenType() == TokenType.KW_DEFAULT)
            logger.error(symbol.position(), "Case should have at least one executable statement");

		AstStatements body = parseStatements();
		Position casePos = new Position(start, body.position);

		return new AstCaseStatement(casePos, expressions, body);
	}

	private AstExpression parseBracket() {
		dump("atom_expression -> []");

		Position start = symbol.position();
		nextSymbol();

		if (symbol.tokenType() == TokenType.RBRACKET) {
			nextSymbol();
			return new AstListExpr(start, new ArrayList<>());
		}

		AstExpression e1 = parseExpression();

//		if (symbol.tokenType() == Token.KW_FOR) {
//			dump("[] -> [ expression for identifier in expression ]");
//			Vector<AbsStmt> stmt = new Vector<>();
//			stmt.add(e1);
//			AstStatements s = new AstStatements(new Position(start, e1.position), stmt);
//
//			if (symbol.tokenType() != Token.KW_FOR)
//				Logger.error(previous.position(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected keyword \"for\" after this tokenType");
//
//			Symbol elementCount = nextSymbol(new Symbol(Token.IDENTIFIER, "identifier", null));
//			AstVariableNameExpression variableDefinition = new AstVariableNameExpression(elementCount.position, elementCount.lexeme);
//			nextSymbol();
//
//			if (symbol.tokenType() != Token.KW_IN)
//				Logger.error(previous.position(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected keyword \"in\" after this tokenType");
//			nextSymbol();
//
//			AstExpression e2 = parseExpression();
//
//			if (symbol.tokenType() != Token.RBRACKET)
//				Logger.error(previous.position(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected \"]\" after this tokenType");
//			nextSymbol();
//
//			return new AstForStatement(new Position(start, e2.position), variableDefinition, e2, s);
//		}

		/*else */if (symbol.tokenType() == TokenType.COMMA) {
			dump("[] -> [expression, expressions']");
            ArrayList<AstExpression> elements = new ArrayList<>();
			elements.add(e1);
			while (symbol.tokenType() == TokenType.COMMA) {
				nextSymbol();
				elements.add(parseExpression());
			}
			if (symbol.tokenType() != TokenType.RBRACKET)
                logger.error(previous.position(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"]\" after this tokenType");
			nextSymbol();
			return new AstListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}
		else if (symbol.tokenType() == TokenType.RBRACKET) {
			dump("[] -> [expression]");
            ArrayList<AstExpression> elements = new ArrayList<>();
			elements.add(e1);
			nextSymbol();
			return new AstListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}

		return null;
	}


	private AstTupleExpression parseTupleExpression(boolean argumentTuple) {
		Position start = symbol.position();
		nextSymbol();

        ArrayList<AstExpression> expressions = parseTupleExpressions(argumentTuple);

		if (symbol.tokenType() != TokenType.RPARENT)
            logger.error(symbol.position(), "Expected ')'");

		Position tuplePos = new Position(start, symbol.position());
		nextSymbol();

		return new AstTupleExpression(tuplePos, expressions);
	}

	private ArrayList<AstExpression> parseTupleExpressions(boolean argumentTuple) {
		int index = 0;
        ArrayList<AstExpression> expressions = new ArrayList<>();

		while (true) {
			AstExpression e1 = parseExpression();

			if (symbol.tokenType() == TokenType.COLON) {
				if (!(e1 instanceof AstVariableNameExpression))
                    logger.error(e1.position, "Expected identifier for tuple member name");

				String memberName = ((AstVariableNameExpression) e1).name;

				// TODO
//				if (names.contains(memberName))
//					Logger.error(e1.position, "This tuple already contains member named \"" + memberName + "\"");

				nextSymbol();
				AstExpression e2 = parseExpression();
				Position pos = new Position(e1.position, e2.position);
				expressions.add(new AstLabeledExpr(pos, e2, memberName));
			}
			else {
				String memberName;

				if (argumentTuple) {
                    memberName = "_";
                }
				else {
                    memberName = String.valueOf(index);
                }

				expressions.add(new AstLabeledExpr(e1.position, e1, memberName));
			}

			if (symbol.tokenType() == TokenType.RPARENT)
				break;
			if (symbol.tokenType() != TokenType.COMMA)
                logger.error(symbol.position(), "Insert ',' separator");
			nextSymbol();
			if (symbol.tokenType() == TokenType.NEWLINE)
				nextSymbol();

			index++;
		}

		return expressions;
	}

	private Symbol nextSymbol() {
		previous = symbol;
		symbol = lexAn.nextSymbol();
		return symbol;
	}

	private Symbol nextSymbol(TokenType expectedType, String lexeme) {
		if (nextSymbol().tokenType() != expectedType)
            logger.error(previous.position(), "Syntax error on token \""
					+ previous.getLexeme() + "\", expected \"" + lexeme);
		return symbol;
	}

	private void dump(String production) {
		if (!dump)
			return;
		if (logger.dumpFile() == null)
			return;
        logger.dumpFile().println(production);
	}
}
