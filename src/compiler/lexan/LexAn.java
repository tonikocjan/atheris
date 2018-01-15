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

import compiler.logger.LoggerFactory;
import compiler.logger.LoggerInterface;
import utils.Constants;
import managers.LanguageManager;
import compiler.*;

public class LexAn implements LexicalAnalyzer {

    private static LoggerInterface logger = LoggerFactory.logger();

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
	private StringBuilder currentSymbol = new StringBuilder();
	private int nextCharacter = -1;
	private int startCol = 1, startRow = 1;
	private boolean skipNextCharacter = false;

	private LexAn(String sourceFileName, boolean dump) {
		this.dump = dump;

		openFile(sourceFileName);
        constructKeywordMap();
	}


	public static LexAn parseSourceFile(String sourceFileName, boolean dump) {
	    return new LexAn(sourceFileName, dump);
    }

	private void constructKeywordMap() {
	    if (keywordsMap != null) return;

	    int firstKeywordIndex = TokenType.KW_ELSE.ordinal();

        keywordsMap = new HashMap<>();
        for (int i = 0; i < reserverKeywords.length; i++) {
            keywordsMap.put(reserverKeywords[i], TokenType.values()[i + firstKeywordIndex]);
        }
    }

    private void openFile(String sourceFileName) {
        try {
            Path current = Paths.get("");
            System.out.println("Working dir: "
                    + current.toAbsolutePath().toString());
            System.out.println("    Opening file: " + sourceFileName);
            this.file = new FileInputStream(sourceFileName);
        } catch (FileNotFoundException e) {
            logger.error(LanguageManager.localize("lexan_error_opening_file", sourceFileName));
        }
    }

	public Symbol nextSymbol() {
		if (file == null) {
            return null;
        }

		try {
			Symbol symbol = parseSymbol();

			if (symbol == null) {
                symbol = Symbol.EOF()
                        .setStartRow(startRow)
                        .setStartCol(startCol)
                        .setEndRow(startRow)
                        .setEndCol(startCol + 1).build();
            }

			dump(symbol);
			return symbol;

		} catch (IOException e) {
            logger.error(LanguageManager.localize("lexan_error_parsing_file"));
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
                    // if next character is not *, return division tokenType
                    skipNextCharacter = true;
                    return new Symbol.Builder()
                            .setTokenType(TokenType.DIV)
                            .setLexeme("/")
                            .setStartRow(startRow)
                            .setStartCol(startCol)
                            .setEndRow(startRow)
                            .setEndCol(startCol + 1)
                            .build();
                }

				continue;
			}

			/**
			 * Handle EOF.
			 */
			if (nextCharacter == -1) {
                return Symbol.EOF()
                        .setStartRow(startRow)
                        .setStartCol(startCol)
                        .setEndRow(startRow)
                        .setEndCol(startCol)
                        .build();
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
            logger.error(new Position(startRow, startCol, startRow, startCol
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

                logger.error(new Position(startRow, startCol, startRow, startCol + currentSymbol.length() + 1),
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
            logger.error(new Position(startRow, startCol, startRow,
                            startCol + currentSymbol.length()),
                    LanguageManager.localize("lexan_error_string_not_closed"));
        }

        return new Symbol.Builder()
                .setTokenType(TokenType.STR_CONST)
                .setLexeme(currentSymbol.toString())
                .setStartRow(startRow)
                .setStartCol(startCol)
                .setEndRow(startRow)
                .setEndCol(startCol + currentSymbol.length())
                .build();
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

        TokenType tokenType = didParseDouble ? TokenType.DOUBLE_CONST : TokenType.INT_CONST;
        return new Symbol.Builder()
                .setTokenType(tokenType)
                .setLexeme(currentSymbol.toString())
                .setStartRow(startRow)
                .setStartCol(startCol)
                .setEndRow(startRow)
                .setEndCol(startCol + currentSymbol.length())
                .build();
    }

    private Symbol parseCharConstant() throws IOException {
        nextCharacter = file.read();
        Symbol s = new Symbol.Builder()
                .setTokenType(TokenType.CHAR_CONST)
                .setLexeme("" + (char) nextCharacter)
                .setStartRow(startRow)
                .setStartCol(startCol)
                .setEndRow(startRow)
                .setEndCol(startCol + 2)
                .build();
        nextCharacter = file.read();

        if (nextCharacter != '\'') {
            logger.error(new Position(startRow, startCol, startRow, startCol + currentSymbol.length() + 1),
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

                return new Symbol.Builder()
                        .setTokenType(tokenType)
                        .setLexeme(currentSymbol.toString())
                        .setStartRow(startRow)
                        .setStartCol(startCol)
                        .setEndRow(startRow)
                        .setEndCol(startCol + currentSymbol.length())
                        .build();
            }

            if (!isLegalIdentifier(nextCharacter)) {
                logger.error(new Position(startRow, startCol, startRow, startCol + currentSymbol.length() + 1),
                        LanguageManager.localize("lexan_error_invalid_token", (char) nextCharacter));
            }
        }
    }

    private Symbol parseOperator(Symbol op) throws IOException {
        if (op.tokenType() == TokenType.NEWLINE) {
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

        if (op.tokenType() == TokenType.NEWLINE) {
            startRow++;
            startCol = 1;
        }

        return op;
    }

	private Symbol isOperator(int ch) {
	    Symbol.Builder builder = new Symbol.Builder();
	    builder.setStartRow(startRow)
                .setStartCol(startCol)
                .setEndRow(startRow)
                .setEndCol(startCol + 1)
                .setLexeme(String.valueOf((char) ch));

		if (ch == '+')
			builder.setTokenType(TokenType.ADD);
		if (ch == '-')
            builder.setTokenType(TokenType.SUB);
		if (ch == '*')
            builder.setTokenType(TokenType.MUL);
		if (ch == '/')
            builder.setTokenType(TokenType.DIV);
		if (ch == '%')
            builder.setTokenType(TokenType.MOD);
		if (ch == '!')
            builder.setTokenType(TokenType.NOT);
		if (ch == '(')
            builder.setTokenType(TokenType.LPARENT);
		if (ch == ')')
            builder.setTokenType(TokenType.RPARENT);
		if (ch == '{')
            builder.setTokenType(TokenType.LBRACE);
		if (ch == '}')
            builder.setTokenType(TokenType.RBRACE);
		if (ch == '[')
            builder.setTokenType(TokenType.LBRACKET);
		if (ch == ']')
            builder.setTokenType(TokenType.RBRACKET);
		if (ch == '<')
            builder.setTokenType(TokenType.LTH);
		if (ch == '>')
            builder.setTokenType(TokenType.GTH);
		if (ch == '=')
            builder.setTokenType(TokenType.ASSIGN);
		if (ch == '.')
            builder.setTokenType(TokenType.DOT);
		if (ch == ':')
            builder.setTokenType(TokenType.COLON);
		if (ch == ';')
            builder.setTokenType(TokenType.SEMIC);
		if (ch == ',')
            builder.setTokenType(TokenType.COMMA);
		if (ch == '|')
            builder.setTokenType(TokenType.IOR);
		if (ch == '&')
            builder.setTokenType(TokenType.AND);
//		if (ch == '?')
//            builder.setTokenType(TokenType.QMARK);
//		if (ch == '!')
//            builder.setTokenType(TokenType.EMARK);
        if (ch == '\n')
            builder.setTokenType(TokenType.NEWLINE)
                   .setLexeme("\\n");
		
		return builder.build();
	}

	private Symbol isComposedOperator(int ch1, int ch2) {
        Symbol.Builder builder = new Symbol.Builder();
        builder.setStartRow(startRow)
               .setStartCol(startCol)
               .setEndRow(startRow)
               .setEndCol(startCol + 1)
               .setLexeme(String.valueOf((char) ch1) + String.valueOf((char) ch2));

        if (ch1 == '=' && ch2 == '=')
            builder.setTokenType(TokenType.EQU);
		if (ch1 == '!' && ch2 == '=')
            builder.setTokenType(TokenType.NEQ);
		if (ch1 == '>' && ch2 == '=')
            builder.setTokenType(TokenType.GEQ);
		if (ch1 == '<' && ch2 == '=')
            builder.setTokenType(TokenType.LEQ);
		if (ch1 == '&' && ch1 == ch2)
            builder.setTokenType(TokenType.AND);
		if (ch1 == '|' && ch1 == ch2)
            builder.setTokenType(TokenType.IOR);
		if (ch1 == '-' && ch2 == '>')
            builder.setTokenType(TokenType.ARROW);

		return builder.build();
	}

	private boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}

	private boolean isWhiteSpace(int ch) {
		return (ch == 32 || ch == 9 || ch == 13 || ch == 10);
	}

	private boolean isLegalIdentifier(int ch) {
		return isNumeric(ch) || ch == '_' || (ch >= 'a' && ch <= 'z') || (ch >= 'A' && ch <= 'Z');
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
		if (logger.dumpFile() == null)
			return;
		if (symb.tokenType() == TokenType.EOF)
            logger.dumpFile().println(symb.toString());
		else
            logger.dumpFile().println(
					"[" + symb.position().toString() + "] " + symb.toString());
	}
}
