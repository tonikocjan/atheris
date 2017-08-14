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
import java.util.Vector;

import compiler.Logger;
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

/**
 * Syntax parser.
 *
 * @author Toni Kocjan
 */
public class SynAn {

	/** Leksikalni analizator. */
	private LexAn lexAn;

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/** Current & previous symbol */
	private Symbol symbol = null;
	private Symbol previous = null;

	public boolean parseStandardLibrary = false;

	/**
	 * Ustvari nov sintaksni analizator.
	 *
	 * @param lexAn
	 *            Leksikalni analizator.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public SynAn(LexAn lexAn, boolean dump) {
		this.lexAn = lexAn;
		this.dump = dump;

		this.symbol = this.lexAn.nextSymbol();
		this.previous = this.symbol;
	}

	/**
	 * Opravi sintaksno analizo.
	 */
	public AstNode parse() {
		if (symbol == null)
			Logger.error("Error accessing LexAn");

		if (symbol.getTokenType() == TokenType.NEWLINE)
			getNextSymbol();

		dump("source -> statements");
		AstNode abstrTree = parseStatements();

		if (symbol.getTokenType() != TokenType.EOF && symbol.getTokenType() != TokenType.NEWLINE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\"");

		return abstrTree;
	}

	private AstStatements parseStatements() {
		dump("statements -> statement statements'");

        ArrayList<AstStatement> astStatements = new ArrayList<>();
		if (symbol.getTokenType() == TokenType.RBRACE)
			return new AstStatements(symbol.getPosition(), astStatements);

		AstStatement statement = parseStatement();

        astStatements.add(statement);
		astStatements.addAll(parseStatements_(statement));

		return new AstStatements(new Position(astStatements.get(0).position, astStatements.get(astStatements.size() - 1).position), astStatements);
	}

	private ArrayList<AstStatement> parseStatements_(AstStatement prevStmt) {
		switch (symbol.getTokenType()) {
		case EOF:
			dump("statements' -> $");
			return new ArrayList<>();
		case RBRACE:
			dump("statements' -> e");
			return new ArrayList<>();
		case SEMIC:
			getNextSymbol();
			if (symbol.getTokenType() == TokenType.NEWLINE)
				getNextSymbol();

			if (symbol.getTokenType() == TokenType.EOF || symbol.getTokenType() == TokenType.RBRACE)
				return new ArrayList<>();
		case IDENTIFIER:
			AstStatement statement = parseStatement();
            ArrayList<AstStatement> astStatements = parseStatements_(statement);
			astStatements.add(0, statement);
			return astStatements;
		case NEWLINE:
			getNextSymbol();

			if (symbol.getTokenType() == TokenType.EOF || symbol.getTokenType() == TokenType.RBRACE ||
				symbol.getTokenType() == TokenType.KW_CASE || symbol.getTokenType() == TokenType.KW_DEFAULT)
				return new ArrayList<>();

			statement = parseStatement();
			astStatements = parseStatements_(statement);
			astStatements.add(0, statement);
			return astStatements;
		case ASSIGN:
			if (!(prevStmt instanceof AstVariableDefinition))
				Logger.error(prevStmt.position, "Syntax error");

			getNextSymbol();
			dump("var_definition -> = expression");

			AstVariableDefinition var = (AstVariableDefinition) prevStmt;
			AstVariableNameExpression varName = new AstVariableNameExpression(var.position, var.name);

			AstExpression e = parseExpression();
			astStatements = parseStatements_(null);

			astStatements.add(0, new AstBinaryExpression(new Position(var.position, e.position),
					AstBinaryExpression.ASSIGN, varName, e));

			return astStatements;
		default:
			Logger.error(symbol.getPosition(), "Consecutive statements must be separated by separator");
		}
		return null;
	}

	private AstStatement parseStatement() {
		switch (symbol.getTokenType()) {
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
			dump("statement -> definition");
			return parseDefinition();

		/**
		 * Parse control transfer statement.
		 */
		case KW_CONTINUE:
			dump("expression -> continue");
			getNextSymbol();
			return new AstControlTransferStatement(symbol.getPosition(),
					ControlTransferKind.Continue);
		case KW_BREAK:
			dump("expression -> break");
			getNextSymbol();
			return new AstControlTransferStatement(symbol.getPosition(),
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

        if (symbol.getTokenType() == TokenType.NEWLINE) {
            getNextSymbol();
        }

        if (symbol.getTokenType() == TokenType.RBRACE) {
            return new AstDefinitions(symbol.getPosition(), new ArrayList<>());
        }

        AstDefinition definition = parseDefinition();

        ArrayList<AstDefinition> absDefs = parseDefinitions_();
		absDefs.add(0, definition);
		return new AstDefinitions(new Position(absDefs.get(0).position, absDefs.get(absDefs.size() - 1).position), absDefs);
	}

	private ArrayList<AstDefinition> parseDefinitions_() {
		switch (symbol.getTokenType()) {
		case EOF:
			dump("memberDefinitions' -> $");

			return new ArrayList<>();
		case RBRACE:
			dump("memberDefinitions' -> e");
			getNextSymbol();

			return new ArrayList<>();
		case SEMIC:
        case NEWLINE:
			dump("memberDefinitions' -> ; memberDefinitions");
			getNextSymbol();
			if (symbol.getTokenType() == TokenType.NEWLINE)
				getNextSymbol();

			if (symbol.getTokenType() == TokenType.EOF || symbol.getTokenType() == TokenType.RBRACE)
				return new ArrayList<>();

			AstDefinition definition = parseDefinition();
            ArrayList<AstDefinition> absDefs = parseDefinitions_();
			absDefs.add(0, definition);
			return absDefs;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme()
					+ "\", expected \";\" or \"}\" after this tokenType");
		}
		return null;
	}

	private AstDefinition parseDefinition() {
		AstDefinition definition = null;

		HashSet<DefinitionModifier> modifiers = parseModifiers();

		switch (symbol.getTokenType()) {
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
			definition = parseClassDefinition(symbol.getTokenType() == TokenType.KW_STRUCT);
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
		default:
			if (symbol.getTokenType() != TokenType.EOF)
				Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
						+ symbol.getLexeme() + "\", delete this tokenType");
			else
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme() + "\", delete this tokenType");
		}

		definition.setModifiers(modifiers);

		return definition;
	}

	private HashSet<DefinitionModifier> parseModifiers() {
	    HashSet<DefinitionModifier> modifiers = new HashSet<>();

	    DefinitionModifier modifier = parseModifier();

        while (modifier != DefinitionModifier.none) {
	        if (modifiers.contains(modifier)) {
	            Logger.error(symbol.getPosition(), "Duplicate modifier");
            }

            modifiers.add(modifier);
            modifier = parseModifier();
        }

	    return modifiers;
    }

    private DefinitionModifier parseModifier() {
	    if (symbol.getTokenType() == TokenType.KW_STATIC) {
	        getNextSymbol();
	        return DefinitionModifier.isStatic;
        }
        if (symbol.getTokenType() == TokenType.KW_FINAL) {
            getNextSymbol();
            return DefinitionModifier.isFinal;
        }
        if (symbol.getTokenType() == TokenType.KW_OVERRIDE) {
            getNextSymbol();
            return DefinitionModifier.isOverriding;
        }
        if (symbol.getTokenType() == TokenType.KW_PUBLIC) {
            getNextSymbol();
            return DefinitionModifier.isPublic;
        }
        if (symbol.getTokenType() == TokenType.KW_PRIVATE) {
            getNextSymbol();
            return DefinitionModifier.isPrivate;
        }

        return DefinitionModifier.none;
    }

	private AstFunctionDefinition parseFunDefinition() {
		Position startPos = symbol.getPosition();

        if (symbol.getTokenType() == TokenType.KW_FUN) {
			Symbol functionName = getNextSymbol(TokenType.IDENTIFIER, "identifier");

			getNextSymbol(TokenType.LPARENT, "(");
			getNextSymbol();
			dump("function_definition -> functionCode identifier ( parameters ) function_definition'");

            ArrayList<AstParameterDefinition> params = parseParameters();

			return parseFunDefinition_(startPos, functionName, params, false);
		}
		Logger.error(previous.getPosition(), "Syntax error on tokenType \""
				+ previous.getLexeme() + "\", expected keyword \"functionDefinition\"");

		return null;
	}

    private AstFunctionDefinition parseConstructorDefinition() {
        Position startPos = symbol.getPosition();

        if (symbol.getTokenType() == TokenType.KW_INIT) {
            Symbol functionName = symbol;

            getNextSymbol(TokenType.LPARENT, "(");
            getNextSymbol();
            dump("constructor_definition -> init ( parameters ) function_definition'");

            ArrayList<AstParameterDefinition> params = parseParameters();

            return parseFunDefinition_(startPos, functionName, params, true);
        }

        Logger.error(previous.getPosition(), "Syntax error on tokenType \""
                + previous.getLexeme() + "\", expected keyword \"init\"");

        return null;
    }

	private AstFunctionDefinition parseFunDefinition_(Position startPos, Symbol functionName, ArrayList<AstParameterDefinition> params, boolean isConstructor) {
		AstType type;

		if (symbol.getTokenType() == TokenType.LBRACE) {
			dump("function_definition' -> { statements } ");
			type = new AstAtomType(symbol.getPosition(), AtomTypeKind.VOID);
		}
		else {
			dump("function_definition' -> memberType { statements } ");
			type = parseType();
		}

		if (symbol.getTokenType() != TokenType.LBRACE) {
            Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
                    + previous.getLexeme() + "\", expected \"{\" after this tokenType");
        }

		getNextSymbol(TokenType.NEWLINE, "NEWLINE");
		getNextSymbol();

		AstStatements expr = parseStatements();
		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected \"}\" after this tokenType");
		getNextSymbol();

		return new AstFunctionDefinition(new Position(startPos, expr.position), functionName.getLexeme(), params, type, expr, isConstructor);
	}

	private AstDefinition parseVarDefinition() {
		Position startPos = symbol.getPosition();
		boolean isMutable = true;
		Symbol id = null;

		if (symbol.getTokenType() == TokenType.KW_VAR) {
            id = getNextSymbol(TokenType.IDENTIFIER, "identifier");

            isMutable = true;
        }
		else {
			id = getNextSymbol(TokenType.IDENTIFIER, "identifier");

            isMutable = false;
		}

		getNextSymbol();

		AstType type = null;

		if (symbol.getTokenType() == TokenType.ASSIGN) {
			dump("var_definition -> variableDefinition identifier = expr");
			return new AstVariableDefinition(startPos, id.getLexeme(), type, isMutable);
		}
		else if (symbol.getTokenType() != TokenType.COLON) {
            Logger.error(previous.getPosition(), "Syntax error on tokenType \""
                    + previous.getLexeme() + "\", expected \":\"");
        }

		getNextSymbol();

		dump("var_definition -> variableDefinition identifier : memberType");

		type = parseType();
		return new AstVariableDefinition(new Position(startPos, type.position), id.getLexeme(), type, isMutable);
	}

	private AstImportDefinition parseImportDefinition() {
		Position pos = symbol.getPosition();
		getNextSymbol(TokenType.IDENTIFIER, "IDENTIFIER");

		String file = symbol.getLexeme();
		getNextSymbol();

		if (symbol.getTokenType() == TokenType.DOT) {
			getNextSymbol();
			return parseImportDefinition_(new AstImportDefinition(pos, file));
		}
		else {
            return new AstImportDefinition(pos, file);
        }
	}

	private AstImportDefinition parseImportDefinition_(AstImportDefinition def) {
		switch (symbol.getTokenType()) {
		case IDENTIFIER:
			def.definitions.add(symbol.getLexeme());
			getNextSymbol();
			return parseImportDefinition__(def);
		default:
			Logger.error(symbol.getPosition(),
					"Syntax error, expected \"IDENTIFIER\"");
			return null;
		}
	}

	private AstImportDefinition parseImportDefinition__(AstImportDefinition def) {
		switch (symbol.getTokenType()) {
		case COMMA:
			getNextSymbol();
			def.definitions.add(symbol.getLexeme());
			getNextSymbol();
			return parseImportDefinition__(def);
		default:
			return def;
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private AstClassDefinition parseClassDefinition(boolean parseStructure) {
		Position start = symbol.getPosition();
		getNextSymbol();

		if (!symbol.isIdentifier()) {
		    Logger.error(symbol.getPosition(), "Expected identifier");
        }

		String className = symbol.getLexeme();
		getNextSymbol();

		AstType baseClass = null;

        // parse conformances
        ArrayList<AstType> conformances = parseConformances();

        if (conformances.size() > 0) {
            baseClass = conformances.get(0);
            conformances.remove(0);
        }

        if (symbol.getTokenType() != TokenType.LBRACE) {
		    Logger.error(symbol.getPosition(), "Expected \"{\"");
        }

		getNextSymbol();

        ArrayList[] data = parseClassMemberDefinitions();
        ArrayList definitions = data[0];
        ArrayList defaultConstructor = data[1];
        ArrayList constructors = data[2];
        ArrayList staticConstructor = data[3];

		if (symbol.getTokenType() != TokenType.RBRACE) {
            Logger.error(symbol.getPosition(), "Syntax error on tokenType \"" + symbol.getLexeme() + "\", expected \"}\"");
        }

		Position end = symbol.getPosition();
		Position definitionPosition = new Position(start, end);

		getNextSymbol();

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

        if (symbol.getTokenType() == TokenType.RBRACE) {
            return new ArrayList[] { definitions, defaultConstructor, constructors, staticConstructor };
        }

        if (symbol.getTokenType() != TokenType.NEWLINE) {
            Logger.error(symbol.getPosition(), "Invalid tokenType");
        }
        getNextSymbol();

		while (true) {
            if (symbol.getTokenType() == TokenType.RBRACE) {
                return new ArrayList[] { definitions, defaultConstructor, constructors, staticConstructor };
            }

            AstDefinition definition = parseDefinition();

            if (definition instanceof AstFunctionDefinition && ((AstFunctionDefinition) definition).isConstructor) {
                constructors.add((AstFunctionDefinition) definition);
            }
            else {
                definitions.add(definition);

                if (definition instanceof AstVariableDefinition) {
                    if (symbol.getTokenType() == TokenType.ASSIGN) {
                        getNextSymbol();
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


            if (symbol.getTokenType() == TokenType.RBRACE) {
                return new ArrayList[] { definitions, defaultConstructor, constructors, staticConstructor };
            }

            if (symbol.getTokenType() == TokenType.NEWLINE || symbol.getTokenType() == TokenType.SEMIC) {
                getNextSymbol();
                continue;
            }

            Logger.error(symbol.getPosition(), "Consecutive statements must be separated by a separator");
        }
	}

	private ArrayList<AstType> parseConformances() {
        ArrayList<AstType> conformances = new ArrayList<>();

        if (symbol.getTokenType() == TokenType.COLON) {
            do {
                getNextSymbol();
                conformances.add(parseType());
            }
            while (symbol.getTokenType() == TokenType.COMMA);
        }

        return conformances;
    }

	private AstInterfaceDefinition parseInterfaceDefinition() {
        Position start = symbol.getPosition();
        String interfaceName = getNextSymbol(TokenType.IDENTIFIER, "identifier").getLexeme();
        getNextSymbol();

        if (symbol.getTokenType() != TokenType.LBRACE) {
            Logger.error(symbol.getPosition(), "Expected \"{\"");
        }

        getNextSymbol();

        if (symbol.getTokenType() == TokenType.NEWLINE) {
            getNextSymbol();
        }

        ArrayList<AstDefinition> defs = parseInterfaceDefinitions();
        Position positition = start;

        if (defs.size() > 0) {
            positition = new Position(start, defs.get(defs.size() - 1).position);
        }

        if (symbol.getTokenType() != TokenType.RBRACE) {
            Logger.error(symbol.getPosition(), "Expected \"}\"");
        }

        getNextSymbol();

        return new AstInterfaceDefinition(positition, interfaceName, new AstDefinitions(positition, defs));
    }

    private ArrayList<AstDefinition> parseInterfaceDefinitions() {
        ArrayList<AstDefinition> defs = new ArrayList<>();

        if (symbol.getTokenType() == TokenType.NEWLINE) {
            getNextSymbol();
        }
        while (symbol.getTokenType() == TokenType.KW_FUN) {
            defs.add(parseFunctionPrototype());

            if (symbol.getTokenType() == TokenType.SEMIC) {
                getNextSymbol();
            }
            if (symbol.getTokenType() == TokenType.NEWLINE) {
                getNextSymbol();
            }
        }

	    return defs;
    }

    private AstFunctionDefinition parseFunctionPrototype() {
        Position startPos = symbol.getPosition();

        if (symbol.getTokenType() == TokenType.KW_FUN) {
            Symbol functionName = getNextSymbol(TokenType.IDENTIFIER, "identifier");

            getNextSymbol(TokenType.LPARENT, "(");
            getNextSymbol();
            dump("function_prototype -> functionCode identifier ( parameters ) function_prototype'");

            ArrayList<AstParameterDefinition> params = parseParameters();

            AstType type;

            if (symbol.getTokenType() == TokenType.NEWLINE || symbol.getTokenType() == TokenType.SEMIC) {
                dump("function_prototype' -> $ ");
                getNextSymbol();
                type = new AstAtomType(symbol.getPosition(), AtomTypeKind.VOID);
            }
            else {
                dump("function_prototype' -> memberType ");
                type = parseType();
            }

            return new AstFunctionDefinition(new Position(startPos, type.position), functionName.getLexeme(), params, type, new AstStatements(startPos, new ArrayList<>()));
        }
        Logger.error(previous.getPosition(), "Syntax error on tokenType \"" + previous.getLexeme() + "\", expected keyword \"functionDefinition\"");

        return null;
    }

	private AstEnumDefinition parseEnumDefinition() {
		Position start = symbol.getPosition();
		String name = getNextSymbol(TokenType.IDENTIFIER, "IDENTIFIER").getLexeme();
		AstType type = null;

		getNextSymbol();
		if (symbol.getTokenType() == TokenType.COLON) {
			getNextSymbol();
			type = parseType();
		}

		if (symbol.getTokenType() != TokenType.LBRACE)
			Logger.error(symbol.getPosition(), "Enum must begin with \"{\"");

		getNextSymbol(TokenType.NEWLINE, "\n");
		getNextSymbol(TokenType.KW_CASE, "CASE");

        ArrayList<AstDefinition> enumDefinitions = parseEnumMemberDefinitions();
		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", expected \"}\"");
		getNextSymbol();

		Position end = enumDefinitions.get(enumDefinitions.size() - 1).position;
		return new AstEnumDefinition(new Position(start, end), name, enumDefinitions, (AstAtomType) type);
	}

	private ArrayList<AstDefinition> parseEnumMemberDefinitions() {
        ArrayList<AstDefinition> definitions = new ArrayList<>();

		while (true) {
			AstDefinition definition = parseDefinition();
			definitions.add(definition);

			if (symbol.getTokenType() == TokenType.COMMA) {
				while (symbol.getTokenType() == TokenType.COMMA) {
					getNextSymbol();
					if (symbol.getTokenType() != TokenType.IDENTIFIER)
						Logger.error(symbol.getPosition(), "Expected idenfitifer "
								+ "after comma in enum member definition");
					definitions.add(parseEnumCaseDefinition());
				}
			}

			if (symbol.getTokenType() == TokenType.NEWLINE) {
				getNextSymbol();
				if (symbol.getTokenType() == TokenType.RBRACE)
					break;

				if (symbol.getTokenType() == TokenType.IDENTIFIER)
					Logger.error(symbol.getPosition(), "Enum member definition "
							+ "must begin with \"case\" keyword");
			}
		}

		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error("todo");

		return definitions;
	}

	private AstEnumMemberDefinition parseEnumCaseDefinition() {
		if (symbol.getTokenType() == TokenType.KW_CASE)
			getNextSymbol(TokenType.IDENTIFIER, "identifier");
		AstVariableNameExpression name = new AstVariableNameExpression(symbol.getPosition(), symbol.getLexeme());

		getNextSymbol();
		if (symbol.getTokenType() == TokenType.ASSIGN) {
			getNextSymbol();

			AstExpression value = parseExpression();
			if (!(value instanceof AstAtomConstExpression))
				Logger.error(value.position, "Raw value for enum definition must be literal");

			Position definitionPos = new Position(name.position, value.position);
			return new AstEnumMemberDefinition(definitionPos, name, (AstAtomConstExpression) value);
		}
		return new AstEnumMemberDefinition(name.position, name, null);
	}

	private AstExtensionDefinition parseExtensionDefinition() {
	    getNextSymbol();
        if (!symbol.isIdentifier()) {
            Logger.error(symbol.getPosition(), "Expected memberType getName");
        }

        AstType type = parseChildType();

        ArrayList<AstType> conformances = parseConformances();

        if (symbol.getTokenType() != TokenType.LBRACE) {
            Logger.error(symbol.getPosition(), "Expected \"{\"");
        }

        getNextSymbol();

        AstDefinitions defs = parseDefinitions();

        if (symbol.getTokenType() != TokenType.RBRACE) {
            Logger.error(symbol.getPosition(), "Expected \"}\"");
        }

        getNextSymbol();

        return new AstExtensionDefinition(
                new Position(
                        symbol.getPosition(),
                        defs.position),
                type.getName(),
                type,
                defs,
                conformances);
    }

	private AstType parseType() {
		AstType type = parseChildType();

		if (symbol.getTokenType() == TokenType.QMARK || symbol.getTokenType() == TokenType.NOT) {
		    boolean isForced = symbol.getTokenType() == TokenType.NOT;

			Position pos = new Position(type.position, symbol.getPosition());
			getNextSymbol();

			return new AstOptionalType(pos, type, isForced);
		}

		return type;
	}

	private AstType parseChildType() {
		Symbol s = symbol;

		switch (symbol.getTokenType()) {
		case IDENTIFIER:
            if (symbol.getLexeme().equals("Bool")) {
                dump("memberType -> logical");
                getNextSymbol();

                return new AstAtomType(s.getPosition(), AtomTypeKind.LOG);
            }
            if (symbol.getLexeme().equals("Int")) {
                dump("memberType -> integer");
                getNextSymbol();

                return new AstAtomType(s.getPosition(), AtomTypeKind.INT);
            }
            if (symbol.getLexeme().equals("Double")) {
                dump("memberType -> double");
                getNextSymbol();

                return new AstAtomType(s.getPosition(), AtomTypeKind.DOB);
            }
            if (symbol.getLexeme().equals("String")) {
                dump("memberType -> string");
                getNextSymbol();

                return new AstAtomType(s.getPosition(), AtomTypeKind.STR);
            }
            if (symbol.getLexeme().equals("Char")) {
                dump("memberType -> char");
                getNextSymbol();

                return new AstAtomType(s.getPosition(), AtomTypeKind.CHR);
            }
            if (symbol.getLexeme().equals("Void")) {
                dump("memberType -> void");
                getNextSymbol();

                return new AstAtomType(s.getPosition(), AtomTypeKind.VOID);
            }

			dump("memberType -> identifier");
			getNextSymbol();

			return new AstTypeName(s.getPosition(), s.getLexeme());
		case LBRACKET:
			getNextSymbol();
			dump("memberType -> [ memberType ]");
			AstType type = parseType();

			if (symbol.getTokenType() != TokenType.RBRACKET)
				Logger.error(symbol.getPosition(), "Syntax error, insert \"]\"");

			getNextSymbol();
			return new AstListType(new Position(s.getPosition(), type.position), 0,
					type);
		case LPARENT:
			Position start = symbol.getPosition();
			dump("memberType -> (parameters) -> memberType");
			getNextSymbol();

            ArrayList<AstType> parameters = new ArrayList<>();
			if (symbol.getTokenType() != TokenType.RPARENT) {
				while (true) {
					parameters.add(parseType());
					if (symbol.getTokenType() != TokenType.COMMA)
						break;
					getNextSymbol();
				}
			}
			if (symbol.getTokenType() != TokenType.RPARENT)
				Logger.error(symbol.getPosition(),
						"Syntax error, insert \")\" to complete function declaration");
			getNextSymbol(TokenType.ARROW, "->");
			getNextSymbol();

			type = parseType();
			return new AstFunctionType(new Position(start, type.position), parameters, type);
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", expected \"variable memberType\"");
		}

		return null;
	}

	private ArrayList<AstParameterDefinition> parseParameters() {
		if (symbol.getTokenType() == TokenType.RPARENT) {
			getNextSymbol();
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
		if (symbol.getTokenType() == TokenType.COMMA) {
			dump("parameters' -> parameters");
			getNextSymbol();

			AstParameterDefinition parameter = parseParameter();
			Vector<AstParameterDefinition> params = new Vector<>();
			params.add(parameter);
			params.addAll(parseParameters_());
			return params;
		} else if (symbol.getTokenType() != TokenType.RPARENT)
			Logger.error(symbol.getPosition(),
					"Syntax error, insert \")\" to complete function declaration");

		dump("parameters' -> e");
		getNextSymbol();

		return new Vector<>();
	}

	private AstParameterDefinition parseParameter() {
		if (symbol.getTokenType() == TokenType.IDENTIFIER) {
			Symbol argumentLabel = symbol;
			String parameterName = null;

			getNextSymbol();

			if (symbol.getTokenType() == TokenType.IDENTIFIER) {
                parameterName = symbol.getLexeme();
                getNextSymbol();
            }
			else if (symbol.getTokenType() != TokenType.COLON)
                Logger.error(symbol.getPosition(),
                        "Syntax error, expected paramater definition");
			getNextSymbol();

			dump("parameter -> identifier : memberType");

			AstType type = parseType();
			return new AstParameterDefinition(new Position(argumentLabel.getPosition(), type.position),
					parameterName, argumentLabel.getLexeme(), type);
		}

		Logger.error(symbol.getPosition(),
				"Syntax error, expected paramater definition");

		return null;
	}

	private Vector<AstExpression> parseExpressions() {
		AstExpression e = null;

		switch (symbol.getTokenType()) {
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
			e = parseExpression();

			Vector<AstExpression> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());

			return expressions;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private Vector<AstExpression> parseExpressions_() {
		switch (symbol.getTokenType()) {
		case COMMA:
			dump("expressions' -> , expression expression'");
			getNextSymbol();

			AstExpression e = parseExpression();

			Vector<AstExpression> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());

			return expressions;
		case RPARENT:
			dump("expressions' -> e");
			getNextSymbol();
			break;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme()
					+ "\", expected \",\" or \")\" to end expression");
		}
		return new Vector<>();
	}

	private AstExpression parseExpression() {
		switch (symbol.getTokenType()) {
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseExpression_(AstExpression e) {
		switch (symbol.getTokenType()) {
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
			getNextSymbol();
			AstExpression e2 = parseExpression();
			return new AstBinaryExpression(new Position(e.position, e2.position), AstBinaryExpression.ASSIGN, e, e2);
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseIorExpression() {
		switch (symbol.getTokenType()) {
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseIorExpression_(AstExpression e) {
		switch (symbol.getTokenType()) {
		case IOR:
			dump("logical_ior_expression' -> | log_ior_expression");
			getNextSymbol();
			AstExpression expr = parseAndExpression();
			return parseIorExpression_(new AstBinaryExpression(new Position(e.position,
					expr.position), AstBinaryExpression.IOR, e, expr));
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseAndExpression() {
		switch (symbol.getTokenType()) {
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseAndExpression_(AstExpression e) {
		switch (symbol.getTokenType()) {
            case LOG_CONST:
                break;
            case INT_CONST:
                break;
            case STR_CONST:
                break;
            case DOUBLE_CONST:
                break;
            case CHAR_CONST:
                break;
            case AND:
			dump("logical_and_expression' -> & logical_and_expression");
			getNextSymbol();

			AstExpression expr = parseCmpExpression();
			return parseAndExpression_(new AstBinaryExpression(new Position(e.position,
					expr.position), AstBinaryExpression.AND, e, expr));
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
            break;
        default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseCmpExpression() {
		switch (symbol.getTokenType()) {
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseCmpExpression_(AstExpression e) {
		AstExpression expr = null;
		int oper = -1;

		switch (symbol.getTokenType()) {
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
			getNextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.EQU;
			break;
        case KW_IS:
            dump("compare_expression' -> is compare_expression");
            getNextSymbol();

            expr = parseAddExpression();
            oper = AstBinaryExpression.IS;
            break;

        case KW_AS:
            dump("compare_expression' -> as identifier");
            getNextSymbol();

            expr = new AstVariableNameExpression(symbol.getPosition(), symbol.getLexeme());;
            getNextSymbol();

            oper = AstBinaryExpression.AS;
            break;
		case NEQ:
			dump("compare_expression' -> != compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.NEQ;
			break;
		case GTH:
			dump("compare_expression' -> > compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.GTH;
			break;
		case LTH:
			dump("compare_expression' -> < compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.LTH;
			break;
		case GEQ:
			dump("compare_expression' -> >= compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.GEQ;
			break;
		case LEQ:
			dump("compare_expression' -> <= compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AstBinaryExpression.LEQ;
			break;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return new AstBinaryExpression(new Position(e.position, expr.position), oper, e, expr);
	}

	private AstExpression parseAddExpression() {
		switch (symbol.getTokenType()) {
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseAddExpression_(AstExpression e) {
		AstExpression expr = null;

		switch (symbol.getTokenType()) {
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
			getNextSymbol();

			expr = parseMulExpression();
			return parseAddExpression_(new AstBinaryExpression(new Position(e.position,
					expr.position), AstBinaryExpression.ADD, e, expr));
		case SUB:
			dump("add_expression' -> - add_expression");
			getNextSymbol();

			expr = parseMulExpression();
			return parseAddExpression_(new AstBinaryExpression(e.position,
					AstBinaryExpression.SUB, e, expr));
		default:
			Logger.error(symbol.getPosition(), "Syntax error on parseAddExpression_");
		}

		return null;
	}

	private AstExpression parseMulExpression() {
		switch (symbol.getTokenType()) {
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected prefix expression");
		}

		return null;
	}

	private AstExpression parseMulExpression_(AstExpression e) {
		AstExpression expr = null;
		int oper = -1;

		switch (symbol.getTokenType()) {
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
			getNextSymbol();
			expr = parsePrefixExpression();
			break;
		case DIV:
			oper = AstBinaryExpression.DIV;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			getNextSymbol();
			expr = parsePrefixExpression();
			break;
		case MOD:
			oper = AstBinaryExpression.MOD;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			getNextSymbol();
			expr = parsePrefixExpression();
			break;
		case DOT:
			oper = AstBinaryExpression.DOT;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			getNextSymbol();
			expr = parsePrefixExpression();

			break;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		expr = new AstBinaryExpression(new Position(e.position, expr.position), oper, e, expr);

        if (expr instanceof AstBinaryExpression) {
            AstBinaryExpression binExpr = (AstBinaryExpression) expr;

            // TODO: - Fix this (object.data[index] is not parsed in the right way at the moment
            // so this is a temporary solution
            if (binExpr.oper == AstBinaryExpression.DOT && binExpr.expr2 instanceof AstBinaryExpression) {
                AstBinaryExpression binExpr2 = (AstBinaryExpression) binExpr.expr2;

                if (binExpr2.oper == AstBinaryExpression.ARR) {
                    expr = new AstBinaryExpression(binExpr.position, AstBinaryExpression.ARR,
                            new AstBinaryExpression(binExpr.expr1.position, AstBinaryExpression.DOT, binExpr.expr1, binExpr2.expr1), binExpr2.expr2);
                }
            }
        }


        return parseMulExpression_(expr);
	}

	private AstExpression parsePrefixExpression() {
		AstExpression e = null;
		Symbol op = symbol;

		switch (symbol.getTokenType()) {
		case ADD:
			dump("prefix_expression -> + prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.getPosition(), e.position),
					AstUnaryExpression.ADD, e);
		case SUB:
			dump("prefix_expression -> - prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.getPosition(), e.position),
					AstUnaryExpression.SUB, e);
		case NOT:
			dump("prefix_expression -> ! prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.getPosition(), e.position),
					AstUnaryExpression.NOT, e);
		case AND:
			dump("prefix_expression -> & prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.getPosition(), e.position),
					AstUnaryExpression.MEM, e);
		case MUL:
			dump("prefix_expression -> * prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AstUnaryExpression(new Position(op.getPosition(), e.position),
					AstUnaryExpression.VAL, e);
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
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parsePostfixExpression() {
		switch (symbol.getTokenType()) {
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

			return parsePostfixExpression_(parseAtomExpression());
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parsePostfixExpression_(AstExpression e) {
		Position symbolPos = symbol.getPosition();

		switch (symbol.getTokenType()) {
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
		case DOT:
		case KW_FOR:
			dump("postfix_expression' -> e");
			return e;
		case LBRACKET:
			dump("postfix_expression' -> [ expression ] postfix_expression'");
			getNextSymbol();
			AstExpression expr = parseExpression();
			if (symbol.getTokenType() != TokenType.RBRACKET)
				Logger.error(previous.getPosition(),
						"Syntax error, insert \"]\" to complete expression");
			getNextSymbol();

			return parsePostfixExpression_(new AstBinaryExpression(new Position(
					e.position, expr.position), AstBinaryExpression.ARR, e, expr));
		case QMARK:
			dump("postfix_expression' -> optional evaluation expression");
			getNextSymbol();
			return new AstOptionalEvaluationExpression(new Position(e.position, symbolPos), e);
		case NOT:
			dump("postfix_expression' -> force value expression");
			getNextSymbol();
			return new AstForceValueExpression(
					new Position(e.position, symbolPos), e);
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AstExpression parseAtomExpression() {
		Symbol current = symbol;

		switch (symbol.getTokenType()) {
		case LOG_CONST:
			dump("atom_expression -> log_const");
			getNextSymbol();

			return new AstAtomConstExpression(current.getPosition(), AtomTypeKind.LOG,
					current.getLexeme());
		case INT_CONST:
			dump("atom_expression -> int_const");
			getNextSymbol();

			return new AstAtomConstExpression(current.getPosition(), AtomTypeKind.INT,
					current.getLexeme());
		case STR_CONST:
			dump("atom_expression -> str_const");
			getNextSymbol();

			return new AstAtomConstExpression(current.getPosition(), AtomTypeKind.STR,
					current.getLexeme());
		case CHAR_CONST:
			dump("atom_expression -> char_const");
			getNextSymbol();

			return new AstAtomConstExpression(current.getPosition(), AtomTypeKind.CHR,
					current.getLexeme());
		case DOUBLE_CONST:
			dump("atom_expression -> double_const");
			getNextSymbol();

			return new AstAtomConstExpression(current.getPosition(), AtomTypeKind.DOB,
					current.getLexeme());
		case KW_NULL:
			dump("atom_expression -> nil");
			getNextSymbol();

			return new AstAtomConstExpression(current.getPosition(), AtomTypeKind.NIL,
					current.getLexeme());
		case IDENTIFIER:
			getNextSymbol();
			if (symbol.getTokenType() == TokenType.LPARENT) {
				getNextSymbol();

				if (symbol.getTokenType() == TokenType.RPARENT) {
					dump("atom_expression -> identifier ( )");
					getNextSymbol();
					return new AstFunctionCallExpression(symbol.getPosition(), current.getLexeme(), new ArrayList<>());
				}

				dump("atom_expression -> identifier ( expressions )");

				// TODO: - Optimize this
				// FIXME: - Function arguments should be parsed with their own method
                ArrayList<AstExpression> arguments = parseTupleExpressions(true);

                ArrayList<AstLabeledExpr> absExprs = new ArrayList<>();
				for (AstExpression e : arguments)
					absExprs.add((AstLabeledExpr) e);

				if (symbol.getTokenType() != TokenType.RPARENT)
					Logger.error(symbol.getPosition(), "Expected ')'");
				getNextSymbol();

				return new AstFunctionCallExpression(new Position(current.getPosition(), absExprs.get(absExprs.size() - 1).position), current.getLexeme(), absExprs);
			} else {
				dump("atom_expression -> identifier");
				return new AstVariableNameExpression(current.getPosition(), current.getLexeme());
			}
		case LBRACKET:
			return parseBracket();
		case KW_RETURN:
			Position pos = symbol.getPosition();
			getNextSymbol();
			// FIXME
			if (symbol.getTokenType() == TokenType.SEMIC || symbol.getTokenType() == TokenType.NEWLINE) {
				dump("atom_expression -> return");
				return new AstReturnExpression(pos, null);
			}
			dump("atom_expression -> return expression");
			AstExpression e = parseExpression();
			return new AstReturnExpression(new Position(pos, e.position), e);
		case LPARENT:
			return parseTupleExpression(false);
		default:
			Logger.error("Syntax error on tokenType \"" + symbol.getLexeme() + "\", delete this tokenType");
		}
		return null;
	}

	private AstConditionalStatement parseForLoop() {
		if (symbol.getTokenType() == TokenType.KW_FOR) {
			Position start = symbol.getPosition();
			Symbol count = getNextSymbol(TokenType.IDENTIFIER, "identifier");
			getNextSymbol();

			if (symbol.getTokenType() != TokenType.KW_IN)
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected keyword \"in\" after this tokenType");
			getNextSymbol();

			AstExpression e = parseExpression();
			if (symbol.getTokenType() != TokenType.LBRACE)
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"{\" after this tokenType");

			getNextSymbol(TokenType.NEWLINE, "NEWLINE");
			getNextSymbol();

			AstStatements s = parseStatements();

			if (symbol.getTokenType() != TokenType.RBRACE)
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"}\" after this tokenType");
			getNextSymbol();

			return new AstForStatement(
			        new Position(start, s.position),
                    new AstVariableNameExpression(
                            count.getPosition(),
                            count.getLexeme()),
                    e,
                    s);
		}

		Logger.error(symbol.getPosition(), "Syntax error, expected keyword \"for\"");

		return null;
	}

	private AstConditionalStatement parseWhileLoop() {
		if (symbol.getTokenType() == TokenType.KW_WHILE) {
			Position start = symbol.getPosition();
			getNextSymbol();
			AstExpression e1 = parseExpression();
			if (symbol.getTokenType() == TokenType.LBRACE) {
				getNextSymbol(TokenType.NEWLINE, "NEWLINE");
				getNextSymbol();

				AstStatements s = parseStatements();

				if (symbol.getTokenType() != TokenType.RBRACE)
					Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
							+ previous.getLexeme()
							+ "\", expected '}' after this tokenType");
				getNextSymbol();

				return new AstWhileStatement(new Position(start, s.position), e1, s);
			}
			Logger.error(previous.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected \"{\" after this tokenType");
		}
		Logger.error(previous.getPosition(),
				"Syntax error, expected keyword \"while\"");

		return null;
	}

	private Condition parseIfCondition() {
		dump("if_expression -> if epression { statements }");

		getNextSymbol();
		AstExpression condition = parseExpression();
		if (symbol.getTokenType() != TokenType.LBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected '{' after this tokenType");
		getNextSymbol(TokenType.NEWLINE, "NEWLINE");
		getNextSymbol();
		AstStatements s = parseStatements();
		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected '}' after this tokenType");
		getNextSymbol();
		return new Condition(condition, s);
	}

	private AstIfStatement parseIf() {
		if (symbol.getTokenType() == TokenType.KW_IF) {
			Position start = symbol.getPosition();
			return parseIf_(start, parseIfCondition());
		}
		Logger.error(previous.getPosition(),
				"Syntax error, expected keyword \"while\"");
		return null;
	}

	private AstIfStatement parseIf_(Position start, Condition condition) {
		if (symbol.getTokenType() == TokenType.NEWLINE)
			getNextSymbol();

        ArrayList<Condition> conditions = new ArrayList<>();
		conditions.add(condition);
		AstStatements elseBody = null;

		while (true) {
			if (symbol.getTokenType() == TokenType.KW_ELSE) {
				getNextSymbol();

				if (symbol.getTokenType() == TokenType.KW_IF) {
					dump("if_expression' -> else if { statements }");
					conditions.add(parseIfCondition());

					if (symbol.getTokenType() == TokenType.NEWLINE)
						getNextSymbol();
					continue;
				}

				if (symbol.getTokenType() != TokenType.LBRACE)
					Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
							+ previous.getLexeme() + "\", expected '{' after this tokenType");

				dump("if_expression' -> else { statements }");

				getNextSymbol(TokenType.NEWLINE, "NEWLINE");
				getNextSymbol();

				elseBody = parseStatements();
				getNextSymbol();
			}
			break;
		}

		Position lastPos = elseBody != null ?
				elseBody.position : conditions.get(conditions.size() - 1).body.position;
		return new AstIfStatement(new Position(condition.condition.position, lastPos), conditions, elseBody);
	}

	private AstSwitchStatement parseSwitch() {
		if (symbol.getTokenType() != TokenType.KW_SWITCH)
			Logger.error(symbol.getPosition(),
					"Syntax error, expected keyword \"switch\"");

		Position start = symbol.getPosition();
		getNextSymbol();

		AstExpression subjectExpr = parseExpression();
		getNextSymbol(TokenType.NEWLINE, "newline");
		getNextSymbol();

		if (symbol.getTokenType() != TokenType.KW_CASE)
			Logger.error(symbol.getPosition(), "Syntax error, \"switch\" must be followed by at least one \"case\" statement");

        ArrayList<AstCaseStatement> cases = new ArrayList<>();
		AstStatements defaultBody = null;

		while (symbol.getTokenType() == TokenType.KW_CASE)
			cases.add(parseCase());

		if (symbol.getTokenType() == TokenType.KW_DEFAULT) {
			getNextSymbol(TokenType.COLON, ":");
			getNextSymbol(TokenType.NEWLINE, "newline");
			getNextSymbol();
			defaultBody = parseStatements();
		}

		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error(symbol.getPosition(),
					"Syntax error, expected \"}\"");
		getNextSymbol();

		Position switchPos = new Position(start,
				defaultBody != null ? defaultBody.position : cases.get(cases.size() - 1).position);

		return new AstSwitchStatement(switchPos, subjectExpr, cases, defaultBody);
	}

	private AstCaseStatement parseCase() {
		if (symbol.getTokenType() != TokenType.KW_CASE)
			Logger.error(symbol.getPosition(),
					"Syntax error, expected keyword \"case\"");

		Position start = symbol.getPosition();
		getNextSymbol();

        ArrayList<AstExpression> expressions = new ArrayList<>();
		expressions.add(parseExpression());

		// join conditions
		while (symbol.getTokenType() == TokenType.COMMA) {
			getNextSymbol();
			expressions.add(parseExpression());
		}

		if (symbol.getTokenType() != TokenType.COLON)
			Logger.error(symbol.getPosition(),
					"Syntax error, case expression must be followed by \":\"");
		getNextSymbol();

		if (symbol.getTokenType() == TokenType.NEWLINE)
			getNextSymbol();

		if (symbol.getTokenType() == TokenType.KW_CASE || symbol.getTokenType() == TokenType.KW_DEFAULT)
			Logger.error(symbol.getPosition(), "Case should have at least one executable statement");

		AstStatements body = parseStatements();
		Position casePos = new Position(start, body.position);

		return new AstCaseStatement(casePos, expressions, body);
	}

	private AstExpression parseBracket() {
		dump("atom_expression -> []");

		Position start = symbol.getPosition();
		getNextSymbol();

		if (symbol.getTokenType() == TokenType.RBRACKET) {
			getNextSymbol();
			return new AstListExpr(start, new ArrayList<>());
		}

		AstExpression e1 = parseExpression();

//		if (symbol.getTokenType() == Token.KW_FOR) {
//			dump("[] -> [ expression for identifier in expression ]");
//			Vector<AbsStmt> stmt = new Vector<>();
//			stmt.add(e1);
//			AstStatements s = new AstStatements(new Position(start, e1.position), stmt);
//
//			if (symbol.getTokenType() != Token.KW_FOR)
//				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected keyword \"for\" after this tokenType");
//
//			Symbol elementCount = getNextSymbol(new Symbol(Token.IDENTIFIER, "identifier", null));
//			AstVariableNameExpression variableDefinition = new AstVariableNameExpression(elementCount.position, elementCount.lexeme);
//			getNextSymbol();
//
//			if (symbol.getTokenType() != Token.KW_IN)
//				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected keyword \"in\" after this tokenType");
//			getNextSymbol();
//
//			AstExpression e2 = parseExpression();
//
//			if (symbol.getTokenType() != Token.RBRACKET)
//				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected \"]\" after this tokenType");
//			getNextSymbol();
//
//			return new AstForStatement(new Position(start, e2.position), variableDefinition, e2, s);
//		}

		/*else */if (symbol.getTokenType() == TokenType.COMMA) {
			dump("[] -> [expression, expressions']");
            ArrayList<AstExpression> elements = new ArrayList<>();
			elements.add(e1);
			while (symbol.getTokenType() == TokenType.COMMA) {
				getNextSymbol();
				elements.add(parseExpression());
			}
			if (symbol.getTokenType() != TokenType.RBRACKET)
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"]\" after this tokenType");
			getNextSymbol();
			return new AstListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}
		else if (symbol.getTokenType() == TokenType.RBRACKET) {
			dump("[] -> [expression]");
            ArrayList<AstExpression> elements = new ArrayList<>();
			elements.add(e1);
			getNextSymbol();
			return new AstListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}

		return null;
	}


	private AstTupleExpression parseTupleExpression(boolean argumentTuple) {
		Position start = symbol.getPosition();
		getNextSymbol();

        ArrayList<AstExpression> expressions = parseTupleExpressions(argumentTuple);

		if (symbol.getTokenType() != TokenType.RPARENT)
			Logger.error(symbol.getPosition(), "Expected ')'");

		Position tuplePos = new Position(start, symbol.getPosition());
		getNextSymbol();

		return new AstTupleExpression(tuplePos, expressions);
	}

	private ArrayList<AstExpression> parseTupleExpressions(boolean argumentTuple) {
		int index = 0;
        ArrayList<AstExpression> expressions = new ArrayList<>();

		while (true) {
			AstExpression e1 = parseExpression();

			if (symbol.getTokenType() == TokenType.COLON) {
				if (!(e1 instanceof AstVariableNameExpression))
					Logger.error(e1.position, "Expected identifier for tuple member getName");

				String memberName = ((AstVariableNameExpression) e1).name;

				// TODO
//				if (names.contains(memberName))
//					Logger.error(e1.position, "This tuple already contains member named \"" + memberName + "\"");

				getNextSymbol();
				AstExpression e2 = parseExpression();
				Position pos = new Position(e1.position, e2.position);
				expressions.add(new AstLabeledExpr(pos, e2, memberName));
			}
			else {
				String memberName;

				if (argumentTuple)
					memberName = "_";
				else
					memberName = String.valueOf(index);

				expressions.add(new AstLabeledExpr(e1.position, e1, memberName));
			}

			if (symbol.getTokenType() == TokenType.RPARENT)
				break;
			if (symbol.getTokenType() != TokenType.COMMA)
				Logger.error(symbol.getPosition(), "Insert ',' separator");
			getNextSymbol();
			if (symbol.getTokenType() == TokenType.NEWLINE)
				getNextSymbol();

			index++;
		}

		return expressions;
	}

	private Symbol getNextSymbol() {
		previous = symbol;
		symbol = lexAn.nextSymbol();
		return symbol;
	}

	private Symbol getNextSymbol(TokenType expectedType, String lexeme) {
		if (getNextSymbol().getTokenType() != expectedType)
			Logger.error(previous.getPosition(), "Syntax error on token \""
					+ previous.getLexeme() + "\", expected \"" + lexeme);
		return symbol;
	}

	private void dump(String production) {
		if (!dump)
			return;
		if (Logger.dumpFile() == null)
			return;
		Logger.dumpFile().println(production);
	}
}
