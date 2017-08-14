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
import utils.Constants;
import compiler.Position;
import compiler.ast.tree.*;
import compiler.ast.tree.def.*;
import compiler.ast.tree.expr.AbsAtomConstExpr;
import compiler.ast.tree.expr.AbsBinExpr;
import compiler.ast.tree.expr.AbsExpr;
import compiler.ast.tree.expr.AbsForceValueExpr;
import compiler.ast.tree.expr.AbsFunCall;
import compiler.ast.tree.expr.AbsLabeledExpr;
import compiler.ast.tree.expr.AbsListExpr;
import compiler.ast.tree.expr.AbsOptionalEvaluationExpr;
import compiler.ast.tree.expr.AbsReturnExpr;
import compiler.ast.tree.expr.AbsTupleExpr;
import compiler.ast.tree.expr.AbsUnExpr;
import compiler.ast.tree.expr.AbsVarNameExpr;
import compiler.ast.tree.stmt.AbsCaseStmt;
import compiler.ast.tree.stmt.AbsConditionalStmt;
import compiler.ast.tree.stmt.AbsControlTransferStmt;
import compiler.ast.tree.stmt.AbsForStmt;
import compiler.ast.tree.stmt.AbsIfStmt;
import compiler.ast.tree.stmt.AbsSwitchStmt;
import compiler.ast.tree.stmt.AbsWhileStmt;
import compiler.ast.tree.type.AbsAtomType;
import compiler.ast.tree.type.AbsFunType;
import compiler.ast.tree.type.AbsListType;
import compiler.ast.tree.type.AbsOptionalType;
import compiler.ast.tree.type.AbsType;
import compiler.ast.tree.type.AbsTypeName;
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
	public AbsTree parse() {
		if (symbol == null)
			Logger.error("Error accessing LexAn");

		if (symbol.getTokenType() == TokenType.NEWLINE)
			getNextSymbol();

		dump("source -> statements");
		AbsTree abstrTree = parseStatements();

		if (symbol.getTokenType() != TokenType.EOF && symbol.getTokenType() != TokenType.NEWLINE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\"");

		return abstrTree;
	}

	private AbsStmts parseStatements() {
		dump("statements -> statement statements'");

        ArrayList<AbsStmt> absStmts = new ArrayList<>();
		if (symbol.getTokenType() == TokenType.RBRACE)
			return new AbsStmts(symbol.getPosition(), absStmts);

		AbsStmt statement = parseStatement();

        absStmts.add(statement);
		absStmts.addAll(parseStatements_(statement));

		return new AbsStmts(new Position(absStmts.get(0).position, absStmts.get(absStmts.size() - 1).position), absStmts);
	}

	private ArrayList<AbsStmt> parseStatements_(AbsStmt prevStmt) {
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
			AbsStmt statement = parseStatement();
            ArrayList<AbsStmt> absStmts = parseStatements_(statement);
			absStmts.add(0, statement);
			return absStmts;
		case NEWLINE:
			getNextSymbol();

			if (symbol.getTokenType() == TokenType.EOF || symbol.getTokenType() == TokenType.RBRACE ||
				symbol.getTokenType() == TokenType.KW_CASE || symbol.getTokenType() == TokenType.KW_DEFAULT)
				return new ArrayList<>();

			statement = parseStatement();
			absStmts = parseStatements_(statement);
			absStmts.add(0, statement);
			return absStmts;
		case ASSIGN:
			if (!(prevStmt instanceof AbsVarDef))
				Logger.error(prevStmt.position, "Syntax error");

			getNextSymbol();
			dump("var_definition -> = expression");

			AbsVarDef var = (AbsVarDef) prevStmt;
			AbsVarNameExpr varName = new AbsVarNameExpr(var.position, var.name);

			AbsExpr e = parseExpression();
			absStmts = parseStatements_(null);

			absStmts.add(0, new AbsBinExpr(new Position(var.position, e.position),
					AbsBinExpr.ASSIGN, varName, e));

			return absStmts;
		default:
			Logger.error(symbol.getPosition(), "Consecutive statements must be separated by separator");
		}
		return null;
	}

	private AbsStmt parseStatement() {
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
			return new AbsControlTransferStmt(symbol.getPosition(),
					ControlTransferKind.Continue);
		case KW_BREAK:
			dump("expression -> break");
			getNextSymbol();
			return new AbsControlTransferStmt(symbol.getPosition(),
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

	private AbsDefs parseDefinitions() {
		dump("definitions -> definition definitions'");

        if (symbol.getTokenType() == TokenType.NEWLINE) {
            getNextSymbol();
        }

        if (symbol.getTokenType() == TokenType.RBRACE) {
            return new AbsDefs(symbol.getPosition(), new ArrayList<>());
        }

        AbsDef definition = parseDefinition();

        ArrayList<AbsDef> absDefs = parseDefinitions_();
		absDefs.add(0, definition);
		return new AbsDefs(new Position(absDefs.get(0).position, absDefs.get(absDefs.size() - 1).position), absDefs);
	}

	private ArrayList<AbsDef> parseDefinitions_() {
		switch (symbol.getTokenType()) {
		case EOF:
			dump("definitions' -> $");

			return new ArrayList<>();
		case RBRACE:
			dump("definitions' -> e");
			getNextSymbol();

			return new ArrayList<>();
		case SEMIC:
        case NEWLINE:
			dump("definitions' -> ; definitions");
			getNextSymbol();
			if (symbol.getTokenType() == TokenType.NEWLINE)
				getNextSymbol();

			if (symbol.getTokenType() == TokenType.EOF || symbol.getTokenType() == TokenType.RBRACE)
				return new ArrayList<>();

			AbsDef definition = parseDefinition();
            ArrayList<AbsDef> absDefs = parseDefinitions_();
			absDefs.add(0, definition);
			return absDefs;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme()
					+ "\", expected \";\" or \"}\" after this tokenType");
		}
		return null;
	}

	private AbsDef parseDefinition() {
		AbsDef definition = null;

		HashSet<Modifier> modifiers = parseModifiers();

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

	private HashSet<Modifier> parseModifiers() {
	    HashSet<Modifier> modifiers = new HashSet<>();

	    Modifier modifier = parseModifier();

        while (modifier != Modifier.none) {
	        if (modifiers.contains(modifier)) {
	            Logger.error(symbol.getPosition(), "Duplicate modifier");
            }

            modifiers.add(modifier);
            modifier = parseModifier();
        }

	    return modifiers;
    }

    private Modifier parseModifier() {
	    if (symbol.getTokenType() == TokenType.KW_STATIC) {
	        getNextSymbol();
	        return Modifier.isStatic;
        }
        if (symbol.getTokenType() == TokenType.KW_FINAL) {
            getNextSymbol();
            return Modifier.isFinal;
        }
        if (symbol.getTokenType() == TokenType.KW_OVERRIDE) {
            getNextSymbol();
            return Modifier.isOverriding;
        }
        if (symbol.getTokenType() == TokenType.KW_PUBLIC) {
            getNextSymbol();
            return Modifier.isPublic;
        }
        if (symbol.getTokenType() == TokenType.KW_PRIVATE) {
            getNextSymbol();
            return Modifier.isPrivate;
        }

        return Modifier.none;
    }

	private AbsFunDef parseFunDefinition() {
		Position startPos = symbol.getPosition();

        if (symbol.getTokenType() == TokenType.KW_FUN) {
			Symbol functionName = getNextSymbol(TokenType.IDENTIFIER, "identifier");

			getNextSymbol(TokenType.LPARENT, "(");
			getNextSymbol();
			dump("function_definition -> func identifier ( parameters ) function_definition'");

            ArrayList<AbsParDef> params = parseParameters();

			return parseFunDefinition_(startPos, functionName, params, false);
		}
		Logger.error(previous.getPosition(), "Syntax error on tokenType \""
				+ previous.getLexeme() + "\", expected keyword \"functionDefinition\"");

		return null;
	}

    private AbsFunDef parseConstructorDefinition() {
        Position startPos = symbol.getPosition();

        if (symbol.getTokenType() == TokenType.KW_INIT) {
            Symbol functionName = symbol;

            getNextSymbol(TokenType.LPARENT, "(");
            getNextSymbol();
            dump("constructor_definition -> init ( parameters ) function_definition'");

            ArrayList<AbsParDef> params = parseParameters();

            return parseFunDefinition_(startPos, functionName, params, true);
        }

        Logger.error(previous.getPosition(), "Syntax error on tokenType \""
                + previous.getLexeme() + "\", expected keyword \"init\"");

        return null;
    }

	private AbsFunDef parseFunDefinition_(Position startPos, Symbol functionName, ArrayList<AbsParDef> params, boolean isConstructor) {
		AbsType type;

		if (symbol.getTokenType() == TokenType.LBRACE) {
			dump("function_definition' -> { statements } ");
			type = new AbsAtomType(symbol.getPosition(), AtomTypeKind.VOID);
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

		AbsStmts expr = parseStatements();
		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected \"}\" after this tokenType");
		getNextSymbol();

		return new AbsFunDef(new Position(startPos, expr.position), functionName.getLexeme(), params, type, expr, isConstructor);
	}

	private AbsDef parseVarDefinition() {
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

		AbsType type = null;

		if (symbol.getTokenType() == TokenType.ASSIGN) {
			dump("var_definition -> variableDefinition identifier = expr");
			return new AbsVarDef(startPos, id.getLexeme(), type, isMutable);
		}
		else if (symbol.getTokenType() != TokenType.COLON) {
            Logger.error(previous.getPosition(), "Syntax error on tokenType \""
                    + previous.getLexeme() + "\", expected \":\"");
        }

		getNextSymbol();

		dump("var_definition -> variableDefinition identifier : memberType");

		type = parseType();
		return new AbsVarDef(new Position(startPos, type.position), id.getLexeme(), type, isMutable);
	}

	private AbsImportDef parseImportDefinition() {
		Position pos = symbol.getPosition();
		getNextSymbol(TokenType.IDENTIFIER, "IDENTIFIER");

		String file = symbol.getLexeme();
		getNextSymbol();

		if (symbol.getTokenType() == TokenType.DOT) {
			getNextSymbol();
			return parseImportDefinition_(new AbsImportDef(pos, file));
		}
		else {
            return new AbsImportDef(pos, file);
        }
	}

	private AbsImportDef parseImportDefinition_(AbsImportDef def) {
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

	private AbsImportDef parseImportDefinition__(AbsImportDef def) {
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
	private AbsClassDef parseClassDefinition(boolean parseStructure) {
		Position start = symbol.getPosition();
		getNextSymbol();

		if (!symbol.isIdentifier()) {
		    Logger.error(symbol.getPosition(), "Expected identifier");
        }

		String className = symbol.getLexeme();
		getNextSymbol();

		AbsType baseClass = null;

        // parse conformances
        ArrayList<AbsType> conformances = parseConformances();

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
            return new AbsStructDef(className, definitionPosition, baseClass, definitions, defaultConstructor, constructors);
        }

        return new AbsClassDef(className, definitionPosition, baseClass, conformances, definitions, defaultConstructor, constructors, staticConstructor);
	}

	@SuppressWarnings("rawtypes")
	private ArrayList[] parseClassMemberDefinitions() {
        ArrayList<AbsDef> definitions = new ArrayList<>();
        ArrayList<AbsFunDef> constructors = new ArrayList<>();
        ArrayList<AbsStmt> defaultConstructor = new ArrayList<>();
        ArrayList<AbsStmt> staticConstructor = new ArrayList<>();

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

            AbsDef definition = parseDefinition();

            if (definition instanceof AbsFunDef && ((AbsFunDef) definition).isConstructor) {
                constructors.add((AbsFunDef) definition);
            }
            else {
                definitions.add(definition);

                if (definition instanceof AbsVarDef) {
                    if (symbol.getTokenType() == TokenType.ASSIGN) {
                        getNextSymbol();
                        dump("var_definition -> = expression");

                        AbsVarNameExpr varNameExpr = new AbsVarNameExpr(definition.position, ((AbsVarDef) definition).name);
                        AbsExpr valueExpr = parseExpression();
                        AbsBinExpr dotExpr = new AbsBinExpr(
                                new Position(definition.position, valueExpr.position), AbsBinExpr.DOT,
                                new AbsVarNameExpr(definition.position, Constants.selfParameterIdentifier), varNameExpr);
                        AbsBinExpr assignExpr = new AbsBinExpr(definition.position, AbsBinExpr.ASSIGN, dotExpr, valueExpr);

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

	private ArrayList<AbsType> parseConformances() {
        ArrayList<AbsType> conformances = new ArrayList<>();

        if (symbol.getTokenType() == TokenType.COLON) {
            do {
                getNextSymbol();
                conformances.add(parseType());
            }
            while (symbol.getTokenType() == TokenType.COMMA);
        }

        return conformances;
    }

	private AbsInterfaceDef parseInterfaceDefinition() {
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

        ArrayList<AbsDef> defs = parseInterfaceDefinitions();
        Position positition = start;

        if (defs.size() > 0) {
            positition = new Position(start, defs.get(defs.size() - 1).position);
        }

        if (symbol.getTokenType() != TokenType.RBRACE) {
            Logger.error(symbol.getPosition(), "Expected \"}\"");
        }

        getNextSymbol();

        return new AbsInterfaceDef(positition, interfaceName, new AbsDefs(positition, defs));
    }

    private ArrayList<AbsDef> parseInterfaceDefinitions() {
        ArrayList<AbsDef> defs = new ArrayList<>();

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

    private AbsFunDef parseFunctionPrototype() {
        Position startPos = symbol.getPosition();

        if (symbol.getTokenType() == TokenType.KW_FUN) {
            Symbol functionName = getNextSymbol(TokenType.IDENTIFIER, "identifier");

            getNextSymbol(TokenType.LPARENT, "(");
            getNextSymbol();
            dump("function_prototype -> func identifier ( parameters ) function_prototype'");

            ArrayList<AbsParDef> params = parseParameters();

            AbsType type;

            if (symbol.getTokenType() == TokenType.NEWLINE || symbol.getTokenType() == TokenType.SEMIC) {
                dump("function_prototype' -> $ ");
                getNextSymbol();
                type = new AbsAtomType(symbol.getPosition(), AtomTypeKind.VOID);
            }
            else {
                dump("function_prototype' -> memberType ");
                type = parseType();
            }

            return new AbsFunDef(new Position(startPos, type.position), functionName.getLexeme(), params, type, new AbsStmts(startPos, new ArrayList<>()));
        }
        Logger.error(previous.getPosition(), "Syntax error on tokenType \"" + previous.getLexeme() + "\", expected keyword \"functionDefinition\"");

        return null;
    }

	private AbsEnumDef parseEnumDefinition() {
		Position start = symbol.getPosition();
		String name = getNextSymbol(TokenType.IDENTIFIER, "IDENTIFIER").getLexeme();
		AbsType type = null;

		getNextSymbol();
		if (symbol.getTokenType() == TokenType.COLON) {
			getNextSymbol();
			type = parseType();
		}

		if (symbol.getTokenType() != TokenType.LBRACE)
			Logger.error(symbol.getPosition(), "Enum must begin with \"{\"");

		getNextSymbol(TokenType.NEWLINE, "\n");
		getNextSymbol(TokenType.KW_CASE, "CASE");

        ArrayList<AbsDef> enumDefinitions = parseEnumMemberDefinitions();
		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", expected \"}\"");
		getNextSymbol();

		Position end = enumDefinitions.get(enumDefinitions.size() - 1).position;
		return new AbsEnumDef(new Position(start, end), name, enumDefinitions, (AbsAtomType) type);
	}

	private ArrayList<AbsDef> parseEnumMemberDefinitions() {
        ArrayList<AbsDef> definitions = new ArrayList<>();

		while (true) {
			AbsDef definition = parseDefinition();
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

	private AbsEnumMemberDef parseEnumCaseDefinition() {
		if (symbol.getTokenType() == TokenType.KW_CASE)
			getNextSymbol(TokenType.IDENTIFIER, "identifier");
		AbsVarNameExpr name = new AbsVarNameExpr(symbol.getPosition(), symbol.getLexeme());

		getNextSymbol();
		if (symbol.getTokenType() == TokenType.ASSIGN) {
			getNextSymbol();

			AbsExpr value = parseExpression();
			if (!(value instanceof AbsAtomConstExpr))
				Logger.error(value.position, "Raw value for enum definition must be literal");

			Position definitionPos = new Position(name.position, value.position);
			return new AbsEnumMemberDef(definitionPos, name, (AbsAtomConstExpr) value);
		}
		return new AbsEnumMemberDef(name.position, name, null);
	}

	private AbsExtensionDef parseExtensionDefinition() {
	    getNextSymbol();
        if (!symbol.isIdentifier()) {
            Logger.error(symbol.getPosition(), "Expected memberType getName");
        }

        AbsType type = parseChildType();

        ArrayList<AbsType> conformances = parseConformances();

        if (symbol.getTokenType() != TokenType.LBRACE) {
            Logger.error(symbol.getPosition(), "Expected \"{\"");
        }

        getNextSymbol();

        AbsDefs defs = parseDefinitions();

        if (symbol.getTokenType() != TokenType.RBRACE) {
            Logger.error(symbol.getPosition(), "Expected \"}\"");
        }

        getNextSymbol();

        return new AbsExtensionDef(
                new Position(
                        symbol.getPosition(),
                        defs.position),
                type.getName(),
                type,
                defs,
                conformances);
    }

	private AbsType parseType() {
		AbsType type = parseChildType();

		if (symbol.getTokenType() == TokenType.QMARK || symbol.getTokenType() == TokenType.NOT) {
		    boolean isForced = symbol.getTokenType() == TokenType.NOT;

			Position pos = new Position(type.position, symbol.getPosition());
			getNextSymbol();

			return new AbsOptionalType(pos, type, isForced);
		}

		return type;
	}

	private AbsType parseChildType() {
		Symbol s = symbol;

		switch (symbol.getTokenType()) {
		case IDENTIFIER:
            if (symbol.getLexeme().equals("Bool")) {
                dump("memberType -> logical");
                getNextSymbol();

                return new AbsAtomType(s.getPosition(), AtomTypeKind.LOG);
            }
            if (symbol.getLexeme().equals("Int")) {
                dump("memberType -> integer");
                getNextSymbol();

                return new AbsAtomType(s.getPosition(), AtomTypeKind.INT);
            }
            if (symbol.getLexeme().equals("Double")) {
                dump("memberType -> double");
                getNextSymbol();

                return new AbsAtomType(s.getPosition(), AtomTypeKind.DOB);
            }
            if (symbol.getLexeme().equals("String")) {
                dump("memberType -> string");
                getNextSymbol();

                return new AbsAtomType(s.getPosition(), AtomTypeKind.STR);
            }
            if (symbol.getLexeme().equals("Char")) {
                dump("memberType -> char");
                getNextSymbol();

                return new AbsAtomType(s.getPosition(), AtomTypeKind.CHR);
            }
            if (symbol.getLexeme().equals("Void")) {
                dump("memberType -> void");
                getNextSymbol();

                return new AbsAtomType(s.getPosition(), AtomTypeKind.VOID);
            }

			dump("memberType -> identifier");
			getNextSymbol();

			return new AbsTypeName(s.getPosition(), s.getLexeme());
		case LBRACKET:
			getNextSymbol();
			dump("memberType -> [ memberType ]");
			AbsType type = parseType();

			if (symbol.getTokenType() != TokenType.RBRACKET)
				Logger.error(symbol.getPosition(), "Syntax error, insert \"]\"");

			getNextSymbol();
			return new AbsListType(new Position(s.getPosition(), type.position), 0,
					type);
		case LPARENT:
			Position start = symbol.getPosition();
			dump("memberType -> (parameters) -> memberType");
			getNextSymbol();

            ArrayList<AbsType> parameters = new ArrayList<>();
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
			return new AbsFunType(new Position(start, type.position), parameters, type);
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", expected \"variable memberType\"");
		}

		return null;
	}

	private ArrayList<AbsParDef> parseParameters() {
		if (symbol.getTokenType() == TokenType.RPARENT) {
			getNextSymbol();
			return new ArrayList<>();
		}

		dump("parameters -> parameter parameters'");

		AbsParDef paramater = parseParameter();

        ArrayList<AbsParDef> params = new ArrayList<>();
		params.add(paramater);
		params.addAll(parseParameters_());

		return params;
	}

	private Vector<AbsParDef> parseParameters_() {
		if (symbol.getTokenType() == TokenType.COMMA) {
			dump("parameters' -> parameters");
			getNextSymbol();

			AbsParDef parameter = parseParameter();
			Vector<AbsParDef> params = new Vector<>();
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

	private AbsParDef parseParameter() {
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

			AbsType type = parseType();
			return new AbsParDef(new Position(argumentLabel.getPosition(), type.position),
					parameterName, argumentLabel.getLexeme(), type);
		}

		Logger.error(symbol.getPosition(),
				"Syntax error, expected paramater definition");

		return null;
	}

	private Vector<AbsExpr> parseExpressions() {
		AbsExpr e = null;

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

			Vector<AbsExpr> expressions = new Vector<>();
			expressions.add(e);
			expressions.addAll(parseExpressions_());

			return expressions;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private Vector<AbsExpr> parseExpressions_() {
		switch (symbol.getTokenType()) {
		case COMMA:
			dump("expressions' -> , expression expression'");
			getNextSymbol();

			AbsExpr e = parseExpression();

			Vector<AbsExpr> expressions = new Vector<>();
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

	private AbsExpr parseExpression() {
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

	private AbsExpr parseExpression_(AbsExpr e) {
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
			AbsExpr e2 = parseExpression();
			return new AbsBinExpr(new Position(e.position, e2.position), AbsBinExpr.ASSIGN, e, e2);
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AbsExpr parseIorExpression() {
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

	private AbsExpr parseIorExpression_(AbsExpr e) {
		switch (symbol.getTokenType()) {
		case IOR:
			dump("logical_ior_expression' -> | log_ior_expression");
			getNextSymbol();
			AbsExpr expr = parseAndExpression();
			return parseIorExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.IOR, e, expr));
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

	private AbsExpr parseAndExpression() {
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

	private AbsExpr parseAndExpression_(AbsExpr e) {
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

			AbsExpr expr = parseCmpExpression();
			return parseAndExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.AND, e, expr));
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

	private AbsExpr parseCmpExpression() {
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

	private AbsExpr parseCmpExpression_(AbsExpr e) {
		AbsExpr expr = null;
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
			oper = AbsBinExpr.EQU;
			break;
        case KW_IS:
            dump("compare_expression' -> is compare_expression");
            getNextSymbol();

            expr = parseAddExpression();
            oper = AbsBinExpr.IS;
            break;

        case KW_AS:
            dump("compare_expression' -> as identifier");
            getNextSymbol();

            expr = new AbsVarNameExpr(symbol.getPosition(), symbol.getLexeme());;
            getNextSymbol();

            oper = AbsBinExpr.AS;
            break;
		case NEQ:
			dump("compare_expression' -> != compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AbsBinExpr.NEQ;
			break;
		case GTH:
			dump("compare_expression' -> > compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AbsBinExpr.GTH;
			break;
		case LTH:
			dump("compare_expression' -> < compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AbsBinExpr.LTH;
			break;
		case GEQ:
			dump("compare_expression' -> >= compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AbsBinExpr.GEQ;
			break;
		case LEQ:
			dump("compare_expression' -> <= compare_expression");
			getNextSymbol();

			expr = parseAddExpression();
			oper = AbsBinExpr.LEQ;
			break;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return new AbsBinExpr(new Position(e.position, expr.position), oper, e, expr);
	}

	private AbsExpr parseAddExpression() {
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

	private AbsExpr parseAddExpression_(AbsExpr e) {
		AbsExpr expr = null;

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
			return parseAddExpression_(new AbsBinExpr(new Position(e.position,
					expr.position), AbsBinExpr.ADD, e, expr));
		case SUB:
			dump("add_expression' -> - add_expression");
			getNextSymbol();

			expr = parseMulExpression();
			return parseAddExpression_(new AbsBinExpr(e.position,
					AbsBinExpr.SUB, e, expr));
		default:
			Logger.error(symbol.getPosition(), "Syntax error on parseAddExpression_");
		}

		return null;
	}

	private AbsExpr parseMulExpression() {
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

	private AbsExpr parseMulExpression_(AbsExpr e) {
		AbsExpr expr = null;
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
			oper = AbsBinExpr.MUL;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			getNextSymbol();
			expr = parsePrefixExpression();
			break;
		case DIV:
			oper = AbsBinExpr.DIV;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			getNextSymbol();
			expr = parsePrefixExpression();
			break;
		case MOD:
			oper = AbsBinExpr.MOD;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			getNextSymbol();
			expr = parsePrefixExpression();
			break;
		case DOT:
			oper = AbsBinExpr.DOT;
			dump("multiplicative_expression' -> prefix_expression multiplicative_expression'");
			getNextSymbol();
			expr = parsePrefixExpression();

			break;
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		expr = new AbsBinExpr(new Position(e.position, expr.position), oper, e, expr);

        if (expr instanceof AbsBinExpr) {
            AbsBinExpr binExpr = (AbsBinExpr) expr;

            // TODO: - Fix this (object.data[index] is not parsed in the right way at the moment
            // so this is a temporary solution
            if (binExpr.oper == AbsBinExpr.DOT && binExpr.expr2 instanceof AbsBinExpr) {
                AbsBinExpr binExpr2 = (AbsBinExpr) binExpr.expr2;

                if (binExpr2.oper == AbsBinExpr.ARR) {
                    expr = new AbsBinExpr(binExpr.position, AbsBinExpr.ARR,
                            new AbsBinExpr(binExpr.expr1.position, AbsBinExpr.DOT, binExpr.expr1, binExpr2.expr1), binExpr2.expr2);
                }
            }
        }


        return parseMulExpression_(expr);
	}

	private AbsExpr parsePrefixExpression() {
		AbsExpr e = null;
		Symbol op = symbol;

		switch (symbol.getTokenType()) {
		case ADD:
			dump("prefix_expression -> + prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.getPosition(), e.position),
					AbsUnExpr.ADD, e);
		case SUB:
			dump("prefix_expression -> - prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.getPosition(), e.position),
					AbsUnExpr.SUB, e);
		case NOT:
			dump("prefix_expression -> ! prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.getPosition(), e.position),
					AbsUnExpr.NOT, e);
		case AND:
			dump("prefix_expression -> & prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.getPosition(), e.position),
					AbsUnExpr.MEM, e);
		case MUL:
			dump("prefix_expression -> * prefix_expression");
			getNextSymbol();

			e = parsePrefixExpression();
			return new AbsUnExpr(new Position(op.getPosition(), e.position),
					AbsUnExpr.VAL, e);
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

	private AbsExpr parsePostfixExpression() {
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

	private AbsExpr parsePostfixExpression_(AbsExpr e) {
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
			AbsExpr expr = parseExpression();
			if (symbol.getTokenType() != TokenType.RBRACKET)
				Logger.error(previous.getPosition(),
						"Syntax error, insert \"]\" to complete expression");
			getNextSymbol();

			return parsePostfixExpression_(new AbsBinExpr(new Position(
					e.position, expr.position), AbsBinExpr.ARR, e, expr));
		case QMARK:
			dump("postfix_expression' -> optional evaluation expression");
			getNextSymbol();
			return new AbsOptionalEvaluationExpr(new Position(e.position, symbolPos), e);
		case NOT:
			dump("postfix_expression' -> force value expression");
			getNextSymbol();
			return new AbsForceValueExpr(
					new Position(e.position, symbolPos), e);
		default:
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ symbol.getLexeme() + "\", delete this tokenType");
		}

		return null;
	}

	private AbsExpr parseAtomExpression() {
		Symbol current = symbol;

		switch (symbol.getTokenType()) {
		case LOG_CONST:
			dump("atom_expression -> log_const");
			getNextSymbol();

			return new AbsAtomConstExpr(current.getPosition(), AtomTypeKind.LOG,
					current.getLexeme());
		case INT_CONST:
			dump("atom_expression -> int_const");
			getNextSymbol();

			return new AbsAtomConstExpr(current.getPosition(), AtomTypeKind.INT,
					current.getLexeme());
		case STR_CONST:
			dump("atom_expression -> str_const");
			getNextSymbol();

			return new AbsAtomConstExpr(current.getPosition(), AtomTypeKind.STR,
					current.getLexeme());
		case CHAR_CONST:
			dump("atom_expression -> char_const");
			getNextSymbol();

			return new AbsAtomConstExpr(current.getPosition(), AtomTypeKind.CHR,
					current.getLexeme());
		case DOUBLE_CONST:
			dump("atom_expression -> double_const");
			getNextSymbol();

			return new AbsAtomConstExpr(current.getPosition(), AtomTypeKind.DOB,
					current.getLexeme());
		case KW_NULL:
			dump("atom_expression -> nil");
			getNextSymbol();

			return new AbsAtomConstExpr(current.getPosition(), AtomTypeKind.NIL,
					current.getLexeme());
		case IDENTIFIER:
			getNextSymbol();
			if (symbol.getTokenType() == TokenType.LPARENT) {
				getNextSymbol();

				if (symbol.getTokenType() == TokenType.RPARENT) {
					dump("atom_expression -> identifier ( )");
					getNextSymbol();
					return new AbsFunCall(symbol.getPosition(), current.getLexeme(), new ArrayList<>());
				}

				dump("atom_expression -> identifier ( expressions )");

				// TODO: - Optimize this
				// FIXME: - Function arguments should be parsed with their own method
                ArrayList<AbsExpr> arguments = parseTupleExpressions(true);

                ArrayList<AbsLabeledExpr> absExprs = new ArrayList<>();
				for (AbsExpr e : arguments)
					absExprs.add((AbsLabeledExpr) e);

				if (symbol.getTokenType() != TokenType.RPARENT)
					Logger.error(symbol.getPosition(), "Expected ')'");
				getNextSymbol();

				return new AbsFunCall(new Position(current.getPosition(), absExprs.get(absExprs.size() - 1).position), current.getLexeme(), absExprs);
			} else {
				dump("atom_expression -> identifier");
				return new AbsVarNameExpr(current.getPosition(), current.getLexeme());
			}
		case LBRACKET:
			return parseBracket();
		case KW_RETURN:
			Position pos = symbol.getPosition();
			getNextSymbol();
			// FIXME
			if (symbol.getTokenType() == TokenType.SEMIC || symbol.getTokenType() == TokenType.NEWLINE) {
				dump("atom_expression -> return");
				return new AbsReturnExpr(pos, null);
			}
			dump("atom_expression -> return expression");
			AbsExpr e = parseExpression();
			return new AbsReturnExpr(new Position(pos, e.position), e);
		case LPARENT:
			return parseTupleExpression(false);
		default:
			Logger.error("Syntax error on tokenType \"" + symbol.getLexeme() + "\", delete this tokenType");
		}
		return null;
	}

	private AbsConditionalStmt parseForLoop() {
		if (symbol.getTokenType() == TokenType.KW_FOR) {
			Position start = symbol.getPosition();
			Symbol count = getNextSymbol(TokenType.IDENTIFIER, "identifier");
			getNextSymbol();

			if (symbol.getTokenType() != TokenType.KW_IN)
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected keyword \"in\" after this tokenType");
			getNextSymbol();

			AbsExpr e = parseExpression();
			if (symbol.getTokenType() != TokenType.LBRACE)
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"{\" after this tokenType");

			getNextSymbol(TokenType.NEWLINE, "NEWLINE");
			getNextSymbol();

			AbsStmts s = parseStatements();

			if (symbol.getTokenType() != TokenType.RBRACE)
				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
						+ previous.getLexeme()
						+ "\", expected \"}\" after this tokenType");
			getNextSymbol();

			return new AbsForStmt(
			        new Position(start, s.position),
                    new AbsVarNameExpr(
                            count.getPosition(),
                            count.getLexeme()),
                    e,
                    s);
		}

		Logger.error(symbol.getPosition(), "Syntax error, expected keyword \"for\"");

		return null;
	}

	private AbsConditionalStmt parseWhileLoop() {
		if (symbol.getTokenType() == TokenType.KW_WHILE) {
			Position start = symbol.getPosition();
			getNextSymbol();
			AbsExpr e1 = parseExpression();
			if (symbol.getTokenType() == TokenType.LBRACE) {
				getNextSymbol(TokenType.NEWLINE, "NEWLINE");
				getNextSymbol();

				AbsStmts s = parseStatements();

				if (symbol.getTokenType() != TokenType.RBRACE)
					Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
							+ previous.getLexeme()
							+ "\", expected '}' after this tokenType");
				getNextSymbol();

				return new AbsWhileStmt(new Position(start, s.position), e1, s);
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
		AbsExpr condition = parseExpression();
		if (symbol.getTokenType() != TokenType.LBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected '{' after this tokenType");
		getNextSymbol(TokenType.NEWLINE, "NEWLINE");
		getNextSymbol();
		AbsStmts s = parseStatements();
		if (symbol.getTokenType() != TokenType.RBRACE)
			Logger.error(symbol.getPosition(), "Syntax error on tokenType \""
					+ previous.getLexeme() + "\", expected '}' after this tokenType");
		getNextSymbol();
		return new Condition(condition, s);
	}

	private AbsIfStmt parseIf() {
		if (symbol.getTokenType() == TokenType.KW_IF) {
			Position start = symbol.getPosition();
			return parseIf_(start, parseIfCondition());
		}
		Logger.error(previous.getPosition(),
				"Syntax error, expected keyword \"while\"");
		return null;
	}

	private AbsIfStmt parseIf_(Position start, Condition condition) {
		if (symbol.getTokenType() == TokenType.NEWLINE)
			getNextSymbol();

        ArrayList<Condition> conditions = new ArrayList<>();
		conditions.add(condition);
		AbsStmts elseBody = null;

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
		return new AbsIfStmt(new Position(condition.cond.position, lastPos), conditions, elseBody);
	}

	private AbsSwitchStmt parseSwitch() {
		if (symbol.getTokenType() != TokenType.KW_SWITCH)
			Logger.error(symbol.getPosition(),
					"Syntax error, expected keyword \"switch\"");

		Position start = symbol.getPosition();
		getNextSymbol();

		AbsExpr subjectExpr = parseExpression();
		getNextSymbol(TokenType.NEWLINE, "newline");
		getNextSymbol();

		if (symbol.getTokenType() != TokenType.KW_CASE)
			Logger.error(symbol.getPosition(), "Syntax error, \"switch\" must be followed by at least one \"case\" statement");

        ArrayList<AbsCaseStmt> cases = new ArrayList<>();
		AbsStmts defaultBody = null;

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

		return new AbsSwitchStmt(switchPos, subjectExpr, cases, defaultBody);
	}

	private AbsCaseStmt parseCase() {
		if (symbol.getTokenType() != TokenType.KW_CASE)
			Logger.error(symbol.getPosition(),
					"Syntax error, expected keyword \"case\"");

		Position start = symbol.getPosition();
		getNextSymbol();

        ArrayList<AbsExpr> expressions = new ArrayList<>();
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

		AbsStmts body = parseStatements();
		Position casePos = new Position(start, body.position);

		return new AbsCaseStmt(casePos, expressions, body);
	}

	private AbsExpr parseBracket() {
		dump("atom_expression -> []");

		Position start = symbol.getPosition();
		getNextSymbol();

		if (symbol.getTokenType() == TokenType.RBRACKET) {
			getNextSymbol();
			return new AbsListExpr(start, new ArrayList<>());
		}

		AbsExpr e1 = parseExpression();

//		if (symbol.getTokenType() == Token.KW_FOR) {
//			dump("[] -> [ expression for identifier in expression ]");
//			Vector<AbsStmt> stmt = new Vector<>();
//			stmt.add(e1);
//			AbsStmts s = new AbsStmts(new Position(start, e1.position), stmt);
//
//			if (symbol.getTokenType() != Token.KW_FOR)
//				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected keyword \"for\" after this tokenType");
//
//			Symbol count = getNextSymbol(new Symbol(Token.IDENTIFIER, "identifier", null));
//			AbsVarNameExpr variableDefinition = new AbsVarNameExpr(count.position, count.lexeme);
//			getNextSymbol();
//
//			if (symbol.getTokenType() != Token.KW_IN)
//				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected keyword \"in\" after this tokenType");
//			getNextSymbol();
//
//			AbsExpr e2 = parseExpression();
//
//			if (symbol.getTokenType() != Token.RBRACKET)
//				Logger.error(previous.getPosition(), "Syntax error on tokenType \""
//						+ previous.getLexeme()
//						+ "\", expected \"]\" after this tokenType");
//			getNextSymbol();
//
//			return new AbsForStmt(new Position(start, e2.position), variableDefinition, e2, s);
//		}

		/*else */if (symbol.getTokenType() == TokenType.COMMA) {
			dump("[] -> [expression, expressions']");
            ArrayList<AbsExpr> elements = new ArrayList<>();
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
			return new AbsListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}
		else if (symbol.getTokenType() == TokenType.RBRACKET) {
			dump("[] -> [expression]");
            ArrayList<AbsExpr> elements = new ArrayList<>();
			elements.add(e1);
			getNextSymbol();
			return new AbsListExpr(new Position(elements.get(0).position, elements.get(elements.size() - 1).position), elements);
		}

		return null;
	}


	private AbsTupleExpr parseTupleExpression(boolean argumentTuple) {
		Position start = symbol.getPosition();
		getNextSymbol();

        ArrayList<AbsExpr> expressions = parseTupleExpressions(argumentTuple);

		if (symbol.getTokenType() != TokenType.RPARENT)
			Logger.error(symbol.getPosition(), "Expected ')'");

		Position tuplePos = new Position(start, symbol.getPosition());
		getNextSymbol();

		return new AbsTupleExpr(tuplePos, expressions);
	}

	private ArrayList<AbsExpr> parseTupleExpressions(boolean argumentTuple) {
		int index = 0;
        ArrayList<AbsExpr> expressions = new ArrayList<>();

		while (true) {
			AbsExpr e1 = parseExpression();

			if (symbol.getTokenType() == TokenType.COLON) {
				if (!(e1 instanceof AbsVarNameExpr))
					Logger.error(e1.position, "Expected identifier for tuple member getName");

				String memberName = ((AbsVarNameExpr) e1).name;

				// TODO
//				if (names.contains(memberName))
//					Logger.error(e1.position, "This tuple already contains member named \"" + memberName + "\"");

				getNextSymbol();
				AbsExpr e2 = parseExpression();
				Position pos = new Position(e1.position, e2.position);
				expressions.add(new AbsLabeledExpr(pos, e2, memberName));
			}
			else {
				String memberName;

				if (argumentTuple)
					memberName = "_";
				else
					memberName = String.valueOf(index);

				expressions.add(new AbsLabeledExpr(e1.position, e1, memberName));
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
