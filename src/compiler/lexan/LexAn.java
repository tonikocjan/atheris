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

package compiler.lexan;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import utils.Constants;
import managers.LanguageManager;
import compiler.*;

public class LexAn {

    private static final String[] reserverKeywords = new String[]
            {
			/*"Int", "String", "Double", "Bool", "Char", "Void", */
                    "else", "for", "func", "if", "var", "while", "struct", "import",
                    "let", "null", "class", "in", "return", "public",
                    "private", "continue", "break", "switch", "case", "default",
                    "enum", "init", "is", "override", "as", "extension", "final",
                    "static", "interface"
            };

    private static Map<String, TokenType> keywordsMap = null;

	private boolean dump;
	private FileInputStream file = null;
	private StringBuilder currentSymbol = null;
	private int nextCharacter = -1;
	private int startCol = 1, startRow = 1;
	private boolean skipNextCharacter = false;

	public LexAn(String sourceFileName, boolean dump) {
		this.dump = dump;

		try {
			Path current = Paths.get("");
			System.out.println("Working dir: "
					+ current.toAbsolutePath().toString());
			System.out.println("    Opening file: " + sourceFileName);
			this.file = new FileInputStream(sourceFileName);
			currentSymbol = new StringBuilder();

			/**
			 * Construct keyword map.
			 */
			keywordsMap = new HashMap<>();
			for (int i = 0; i < reserverKeywords.length; i++) {
                keywordsMap.put(reserverKeywords[i],
                        TokenType.values()[i + TokenType.KW_ELSE.ordinal()]);
            }

		} catch (FileNotFoundException e) {
			Logger.error(LanguageManager.localize("lexan_error_opening_file", sourceFileName));
		}
	}

	public Symbol nextSymbol() {
		if (file == null) {
            return null;
        }

		try {
			Symbol s = parseSymbol();

			if (s == null) {
                s = new Symbol(TokenType.EOF, "$", startRow, startCol, startRow, startCol + 1);
            }

			dump(s);
			return s;

		} catch (IOException e) {
			Logger.error(LanguageManager.localize("lexan_error_parsing_file"));
		}

		return null;
	}

	private Symbol parseSymbol() throws IOException {
		while (true) {
			startCol += currentSymbol.length();
			currentSymbol = new StringBuilder();

			if (!skipNextCharacter)
				nextCharacter = file.read();
			else
				skipNextCharacter = false;

			/**
			 * Skip characters after '#'
			 */
			if (nextCharacter == '#') {
                parseSingleLineComment();
				continue;
			}
			
			/**
			 * Handle multi-line comments.
			 */
			if (nextCharacter == '/') {
                nextCharacter = file.read();

                if (nextCharacter == '*') {
                    parseMultiLineComment();
                }
                else {
                    // if next character is not *, return division token
                    skipNextCharacter = true;
                    return new Symbol(TokenType.DIV, "/", startRow, startCol, startRow, startCol + 1);
                }

				continue;
			}

			/**
			 * Handle EOF.
			 */
			if (nextCharacter == -1) {
                return new Symbol(TokenType.EOF, "$", startRow, startCol, startRow, startCol);
            }

			/**
			 * Parse string.
			 */
			if (nextCharacter == '\"') {
                return parseString();
			}

			/**
			 * Parse numeric const.
			 */
			if (isNumeric(nextCharacter)) {
                return parseNumericConstant();
			}

			/**
			 * Parse char const.
			 */
			if (nextCharacter == '\'') {
                return parseCharConstant();
			}

			/**
			 * Parse identifier.
			 */
			if (isLegalIdentifier(nextCharacter)) {
                return parseIdentifier();
			}

			/**
			 * Parse operators.
			 */
			Symbol op = isOperator(nextCharacter);
			if (op != null) {
                return parseOperator(op);
			}

			/**
			 * Handle whitespaces.
			 */
			if (isWhiteSpace(nextCharacter)) {
				// update counters
				if (nextCharacter == 32 || nextCharacter == 9)
					startCol += (nextCharacter == 32) ? 1 : 4;
//				else if (nextCharacter == 10) {
//					startCol = 1;
//					startRow++;
//				}
				continue;
			}

			/**
			 * Unknown character. Logger error.
			 */
			Logger.error(new Position(startRow, startCol, startRow, startCol
					+ currentSymbol.length() + 1), LanguageManager.localize("lexan_error_unknown_token",
																	(char) nextCharacter));
		}
	}


    private void parseSingleLineComment() throws IOException {
        while (nextCharacter != -1 && nextCharacter != 10) {
            nextCharacter = file.read();
        }

        skipWhitespaces();
        skipNextCharacter = true;
    }

    private void parseMultiLineComment() throws IOException {
        // else skip characters until '*/'
        do {
            nextCharacter = file.read();
            startCol++;
            if (nextCharacter == '*' && file.read() == '/') {
                nextCharacter = file.read();
                startCol++;
                break;
            }
            if (nextCharacter == '\n') {
                startRow++;
                startCol = 1;
            }
        } while (true);

        skipWhitespaces();
        skipNextCharacter = true;
    }

    private Symbol parseString() throws IOException {
        currentSymbol.append((char) nextCharacter);
        boolean isStringClosed = false;

        while (true) {
            nextCharacter = file.read();
            if (nextCharacter < 32 || nextCharacter > 126) {
                if (isWhiteSpace(nextCharacter) || nextCharacter == -1) {
                    break;
                }

                Logger.error(new Position(startRow, startCol, startRow, startCol + currentSymbol.length() + 1),
                        LanguageManager.localize("lexan_error_invalid_token_in_string_constant"));
            }

            currentSymbol.append((char) nextCharacter);

            if (nextCharacter == '\"') {
                nextCharacter = file.read();
                if (nextCharacter == '\"');
                else {
                    skipNextCharacter = true;
                    isStringClosed = true;
                    break;
                }
            }
        }
        // if last character of the currentSymbol isn't double-quote, report error
        if (!isStringClosed) {
            Logger.error(new Position(startRow, startCol, startRow,
                            startCol + currentSymbol.length()),
                    LanguageManager.localize("lexan_error_string_not_closed"));
        }

        return new Symbol(TokenType.STR_CONST, currentSymbol.toString(), startRow, startCol, startRow, startCol + currentSymbol.length());
    }

    private Symbol parseNumericConstant() throws IOException {
        boolean didParseDouble = false;

        while (true) {
            while (isNumeric(nextCharacter)) {
                currentSymbol.append((char) nextCharacter);
                nextCharacter = file.read();
            }

            if (!didParseDouble && nextCharacter == '.') {
                didParseDouble = true;
                currentSymbol.append((char) nextCharacter);
                nextCharacter = file.read();

                continue;
            }

            break;
        }

        skipNextCharacter = true;

        TokenType t = didParseDouble ? TokenType.DOUBLE_CONST : TokenType.INT_CONST;
        return new Symbol(t, currentSymbol.toString(), startRow, startCol, startRow, startCol + currentSymbol.length());
    }

    private Symbol parseCharConstant() throws IOException {
        nextCharacter = file.read();
        Symbol s = new Symbol(TokenType.CHAR_CONST, "" + (char) nextCharacter, startRow, startCol, startRow, startCol + 2);
        nextCharacter = file.read();

        if (nextCharacter != '\'') {
            Logger.error(new Position(startRow, startCol, startRow, startCol + currentSymbol.length() + 1),
                    LanguageManager.localize("lexan_error_char_literal_not_closed"));
        }

        return s;
    }

    private Symbol parseIdentifier() throws IOException {
        while (true) {
            currentSymbol.append((char) nextCharacter);
            nextCharacter = file.read();

            /**
             * Delimiters for identifiers are:
             * - whitespaces
             * - EOF
             * - operators
             * - single-quote
             * - doouble-quote
             */
            if (isOperator(nextCharacter) != null ||
                    isWhiteSpace(nextCharacter) || nextCharacter == -1 || nextCharacter == '\'') {
                skipNextCharacter = true;

                TokenType tokenType = TokenType.IDENTIFIER;

                // Check if currentSymbol is keyword
                if (keywordsMap.containsKey(currentSymbol.toString())) {
                    tokenType = keywordsMap.get(currentSymbol.toString());
                }

                // Check if currentSymbol is log const
                if (currentSymbol.toString().equals(Constants.trueKeyword) || currentSymbol.toString().equals(Constants.falseKeyword)) {
                    tokenType = TokenType.LOG_CONST;
                }

                return new Symbol(tokenType, currentSymbol.toString(), startRow, startCol, startRow, startCol + currentSymbol.length());
            }

            if (!isLegalIdentifier(nextCharacter)) {
                Logger.error(new Position(startRow, startCol, startRow, startCol + currentSymbol.length() + 1),
                        LanguageManager.localize("lexan_error_invalid_token", (char) nextCharacter));
            }
        }
    }

    private Symbol parseOperator(Symbol op) throws IOException {
        if (op.token == TokenType.NEWLINE) {
            // skip all whitespaces
            skipWhitespaces();

            skipNextCharacter = true;
            return op;
        }

        int tmpCh = file.read();
        Symbol op2 = isComposedOperator(nextCharacter, tmpCh);

        if (op2 != null) {
            startCol += 2;
            return op2;
        }

        skipNextCharacter = true;
        nextCharacter = tmpCh;
        startCol++;

        if (op.token == TokenType.NEWLINE) {
            startRow++;
            startCol = 1;
        }

        return op;
    }

	private Symbol isOperator(int ch) {
		if (ch == '+')
			return new Symbol(TokenType.ADD, "+", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '-')
			return new Symbol(TokenType.SUB, "-", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '*')
			return new Symbol(TokenType.MUL, "*", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '/')
			return new Symbol(TokenType.DIV, "/", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '%')
			return new Symbol(TokenType.MOD, "%", startRow, startCol, startRow,
					startCol + 1);

		if (ch == '!')
			return new Symbol(TokenType.NOT, "!", startRow, startCol, startRow,
					startCol + 1);

		if (ch == '(')
			return new Symbol(TokenType.LPARENT, "(", startRow, startCol, startRow,
					startCol + 1);
		if (ch == ')')
			return new Symbol(TokenType.RPARENT, ")", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '{')
			return new Symbol(TokenType.LBRACE, "{", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '}')
			return new Symbol(TokenType.RBRACE, "}", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '[')
			return new Symbol(TokenType.LBRACKET, "[", startRow, startCol,
					startRow, startCol + 1);
		if (ch == ']')
			return new Symbol(TokenType.RBRACKET, "]", startRow, startCol,
					startRow, startCol + 1);

		if (ch == '<')
			return new Symbol(TokenType.LTH, "<", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '>')
			return new Symbol(TokenType.GTH, ">", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '=')
			return new Symbol(TokenType.ASSIGN, "=", startRow, startCol, startRow,
					startCol + 1);

		if (ch == '.')
			return new Symbol(TokenType.DOT, ".", startRow, startCol, startRow,
					startCol + 1);
		if (ch == ':')
			return new Symbol(TokenType.COLON, ":", startRow, startCol, startRow,
					startCol + 1);
		if (ch == ';')
			return new Symbol(TokenType.SEMIC, ";", startRow, startCol, startRow,
					startCol + 1);
		if (ch == ',')
			return new Symbol(TokenType.COMMA, ",", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '|')
			return new Symbol(TokenType.IOR, "|", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '&')
			return new Symbol(TokenType.AND, "&", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '\n')
			return new Symbol(TokenType.NEWLINE, "\\n", startRow, startCol,
					startRow, startCol + 1);

		if (ch == '?')
			return new Symbol(TokenType.QMARK, "?", startRow, startCol, startRow,
					startCol + 1);
		if (ch == '!')
			return new Symbol(TokenType.EMARK, "?", startRow, startCol, startRow,
					startCol + 1);
		
		return null;
	}

	private Symbol isComposedOperator(int ch1, int ch2) {
		if (ch1 == '=' && ch2 == '=')
			return new Symbol(TokenType.EQU, "==", startRow, startCol, startRow,
					startCol + 2);
		if (ch1 == '!' && ch2 == '=')
			return new Symbol(TokenType.NEQ, "!=", startRow, startCol, startRow,
					startCol + 2);
		if (ch1 == '>' && ch2 == '=')
			return new Symbol(TokenType.GEQ, ">=", startRow, startCol, startRow,
					startCol + 2);
		if (ch1 == '<' && ch2 == '=')
			return new Symbol(TokenType.LEQ, "<=", startRow, startCol, startRow,
					startCol + 2);
		if (ch1 == '&' && ch1 == ch2)
			return new Symbol(TokenType.AND, "&", startRow, startCol, startRow,
					startCol + 1);
		if (ch1 == '|' && ch1 == ch2)
			return new Symbol(TokenType.IOR, "|", startRow, startCol, startRow,
					startCol + 1);
		if (ch1 == '-' && ch2 == '>')
			return new Symbol(TokenType.ARROW, "->", startRow, startCol, startRow,
					startCol + 1);
		return null;
	}

	private boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}

	private boolean isWhiteSpace(int ch) {
		return (ch == 32 || ch == 9 || ch == 13 || ch == 10);
	}

	private boolean isLegalIdentifier(int ch) {
		return isNumeric(nextCharacter) ||
                nextCharacter == '_' ||
				(nextCharacter >= 'a' && nextCharacter <= 'z') ||
				(nextCharacter >= 'A' && nextCharacter <= 'Z');
	}
	
	private void skipWhitespaces() throws IOException {
		do {
			if (nextCharacter == 10) {
				startCol = 1;
				startRow++;
			}
			else {
                startCol += nextCharacter == 9 ? 4 : 1;
            }

			nextCharacter = file.read();
		}
		while (isWhiteSpace(nextCharacter));
	}

	private void dump(Symbol symb) {
		if (!dump)
			return;
		if (Logger.dumpFile() == null)
			return;
		if (symb.token == TokenType.EOF)
			Logger.dumpFile().println(symb.toString());
		else
			Logger.dumpFile().println(
					"[" + symb.position.toString() + "] " + symb.toString());
	}
}
