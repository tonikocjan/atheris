package compiler.lexan;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import managers.LanguageManager;
import compiler.*;

/**
 * Leksikalni analizator.
 * 
 * @author sliva
 * @implementation Toni Kocjan
 */
public class LexAn {

	/** Ali se izpisujejo vmesni rezultati. */
	private boolean dump;

	/**
	 * File which is being parsed.
	 */
	private FileInputStream file = null;

	/**
	 * Buffer containg current word / symbol which is being processed.
	 */
	private StringBuilder word = null;

	/**
	 * Map containing all reserved keywords.
	 */
	private static Map<String, TokenType> keywordsMap = null;
	/**
	 * Reserved keywords.
	 */
	private static final String[] keywords = new String[] 
			{ 
			"Int", "String", "Double", "Bool", "Char", "Void", "else", 
			"for", "func", "if", "var", "while", "struct", "import", 
			"let", "nil", "class", "in", "return", "public", 
			"private", "continue", "break", "switch", "case", "default",
			"enum" 
			};

	/**
	 * Current caracter.
	 */
	private int nxtCh = -1;

	/**
	 * Position.
	 */
	private int startCol = 1, startRow = 1;

	/**
	 * 
	 */
	private boolean dontRead = false;

	/**
	 * ------------------------------
	 */

	/**
	 * Ustvari nov leksikalni analizator.
	 * 
	 * @param sourceFileName
	 *            Ime izvorne datoteke.
	 * @param dump
	 *            Ali se izpisujejo vmesni rezultati.
	 */
	public LexAn(String sourceFileName, boolean dump) {
		this.dump = dump;

		try {
			Path current = Paths.get("");
			System.out.println("Working dir: "
					+ current.toAbsolutePath().toString());
			System.out.println("  Opening file: " + sourceFileName);
			this.file = new FileInputStream(sourceFileName);
			word = new StringBuilder();

			/**
			 * Construct keyword map.
			 */
			keywordsMap = new HashMap<>();
			for (int i = 0; i < keywords.length; i++)
				keywordsMap.put(keywords[i],
						TokenType.values()[i + TokenType.INTEGER.ordinal()]);

		} catch (FileNotFoundException e) {
			Report.error(LanguageManager.localize("lexan_error_opening_file", sourceFileName));
		}
	}

	/**
	 * Vrne naslednji simbol iz izvorne datoteke. Preden vrne simbol, ga izpise
	 * v datoteko z vmesnimi rezultati.
	 * 
	 * @return Naslednji simbol iz izvorne datoteke.
	 */
	public Symbol lexAn() {
		if (file == null)
			return null;

		try {
			Symbol s = parseSymbol();
			if (s == null)
				s = new Symbol(TokenType.EOF, "$", startRow, startCol, startRow,
						startCol + 1);

			dump(s);

			return s;

		} catch (IOException e) {
			Report.error(LanguageManager.localize("lexan_error_parsing_file"));
		}

		return null;
	}

	/**
	 * Parse next symbol in file. If symbol is not lexically correct, report
	 * error.
	 * 
	 * @return next symbol in file or null, if error detected
	 * @throws IOException
	 */
	private Symbol parseSymbol() throws IOException {
		while (true) {
			startCol += word.length();
			word = new StringBuilder();

			if (!dontRead)
				nxtCh = file.read();
			else
				dontRead = false;

			/**
			 * Skip characters after '#'
			 */
			if (nxtCh == '#') {
				while (nxtCh != -1 && nxtCh != 10)
					nxtCh = file.read();
				continue;
			}
			/**
			 * Handle multi-line comments.
			 */
			if (nxtCh == '/') {
				nxtCh = file.read();
				// if next character is not *, return DIV token
				if (nxtCh != '*') {
					dontRead = true;
					return new Symbol(TokenType.DIV, "/", startRow, startCol,
							startRow, startCol + 1);
				}
				// else skip characters until */
				do {
					nxtCh = file.read();
					startCol++;
					if (nxtCh == '*' && file.read() == '/') {
						nxtCh = file.read();
						startCol++;
						break;
					}
					if (nxtCh == '\n') {
						startRow++;
						startCol = 1;
					}
				} while (true);
			}

			/**
			 * Handle EOF.
			 */
			if (nxtCh == -1)
				return new Symbol(TokenType.EOF, "$", startRow, startCol, startRow,
						startCol);

			/**
			 * Parse string.
			 */
			if (nxtCh == '\"') {
				word.append((char) nxtCh);
				boolean strClosed = false;
				while (true) {
					nxtCh = file.read();
					if (nxtCh < 32 || nxtCh > 126) {
						if (isWhiteSpace(nxtCh) || nxtCh == -1)
							break;
						Report.error(new Position(startRow, startCol, startRow,
								startCol + word.length() + 1),
								LanguageManager.localize("lexan_error_invalid_token_in_string_constant"));
					}

					word.append((char) nxtCh);

					if (nxtCh == '\"') {
						nxtCh = file.read();
						if (nxtCh == '\"')
							;
						else {
							dontRead = true;
							strClosed = true;
							break;
						}
					}
				}
				// if last character of the word isn't single-quote, report
				// error
				if (!strClosed) {
					Report.error(new Position(startRow, startCol, startRow,
							startCol + word.length()),
							LanguageManager.localize("lexan_error_string_not_closed"));
				}

				return new Symbol(TokenType.STR_CONST, word.toString(), startRow,
						startCol, startRow, startCol + word.length());
			}

			/**
			 * Parse numeric const.
			 */
			if (isNumeric(nxtCh)) {
				boolean didParseDouble = false;
				while (true) {
					while (isNumeric(nxtCh)) {
						word.append((char) nxtCh);
						nxtCh = file.read();
					}
					if (!didParseDouble && nxtCh == '.') {
						didParseDouble = true;
						word.append((char) nxtCh);
						nxtCh = file.read();
					} else
						break;
				}
				dontRead = true;

				TokenType t = didParseDouble ? TokenType.DOUBLE_CONST : TokenType.INT_CONST;
				return new Symbol(t, word.toString(), startRow, startCol,
						startRow, startCol + word.length());
			}

			/**
			 * Parse char const.
			 */
			if (nxtCh == '\'') {
				nxtCh = file.read();
				Symbol s = new Symbol(TokenType.CHAR_CONST, "" + (char) nxtCh,
						startRow, startCol, startRow, startCol + 2);
				nxtCh = file.read();
				if (nxtCh != '\'')
					Report.error(new Position(startRow, startCol, startRow,
							startCol + word.length() + 1),
							LanguageManager.localize("lexan_error_char_literal_not_closed"));
				return s;
			}

			/**
			 * Parse identifier.
			 */
			if (isLegalId(nxtCh)) {
				while (true) {
					word.append((char) nxtCh);
					nxtCh = file.read();

					/**
					 * Delemiters for identifier are: 
					 * - whitespaces 
					 * - EOF
					 * - operators 
					 * - single-quote
					 * - doouble-quote
					 */
					if (isOperator(nxtCh) != null || isWhiteSpace(nxtCh)
							|| nxtCh == -1 || nxtCh == '\'') {
						dontRead = true;
						TokenType tokenType = TokenType.IDENTIFIER;

						// Check if word is keyword
						if (keywordsMap.containsKey(word.toString()))
							tokenType = keywordsMap.get(word.toString());
						// Check if word is log const
						if (word.toString().equals("true")
								|| word.toString().equals("false"))
							tokenType = TokenType.LOG_CONST;

						return new Symbol(tokenType, word.toString(), startRow,
								startCol, startRow, startCol + word.length());
					}
					/**
					 * If this is not legal identifier character, report error.
					 */
					if (!isLegalId(nxtCh))
						Report.error(new Position(startRow, startCol, startRow,
								startCol + word.length() + 1),
								LanguageManager.localize("lexan_error_invalid_token", (char)nxtCh));
				}
			}

			/**
			 * Parse operators.
			 */
			Symbol op = isOperator(nxtCh);
			if (op != null) {
				/**
				 * Handle newline
				 */
				if (op.token == TokenType.NEWLINE) {
					// skip all whitespaces
					do {
						if (nxtCh == 10) {
							startCol = 1;
							startRow++;
						}
						else
							startCol += nxtCh == 9 ? 4 : 1;
						nxtCh = file.read();
					}
					while (isWhiteSpace(nxtCh));
					
					dontRead = true;
					return op;
				}
				
				/**
				 * Also check if this character + next character is an operator.
				 */
				int tmpCh = file.read();
				Symbol op2 = isOperator2(nxtCh, tmpCh);
				if (op2 != null) {
					startCol += 2;
					return op2;
				}

				dontRead = true;
				nxtCh = tmpCh;
				startCol++;

				if (op.token == TokenType.NEWLINE) {
					startRow++;
					startCol = 1;
				}

				return op;
			}

			/**
			 * Handle whitespaces.
			 */
			if (isWhiteSpace(nxtCh)) {
				// update counters
				if (nxtCh == 32 || nxtCh == 9)
					startCol += (nxtCh == 32) ? 1 : 4;
//				else if (nxtCh == 10) {
//					startCol = 1;
//					startRow++;
//				}
				continue;
			}


			/**
			 * Unknown character. Report error.
			 */
			Report.error(new Position(startRow, startCol, startRow, startCol
					+ word.length() + 1), LanguageManager.localize("lexan_error_unknown_token", 
																	(char) nxtCh));
		}
	}

	/**
	 * Check if character is an operator.
	 * 
	 * @param ch
	 * @return detected operator or null, if no operator is detected
	 */
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

		return null;
	}

	/**
	 * Check if this two characters are an operator.
	 * 
	 * @param ch1
	 * @param ch2
	 * @return detected operator or null, if no operator is detected
	 */
	private Symbol isOperator2(int ch1, int ch2) {
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

	/**
	 * @param ch
	 *            character to be checked
	 * @return true if character is a number; false otherwise
	 */
	private boolean isNumeric(int ch) {
		return (ch >= '0' && ch <= '9');
	}

	/**
	 * @param ch
	 *            character to be checked
	 * @return true if character is whitespace; false otherwise
	 */
	private boolean isWhiteSpace(int ch) {
		return (ch == 32 || ch == 9 || ch == 13 || ch == 10);
	}

	/**
	 * @param ch
	 *            character to be checked
	 * @return true if character is legal identifier character; false otherwise
	 */
	private boolean isLegalId(int ch) {
		return isNumeric(nxtCh) || nxtCh == '_'
				|| (nxtCh >= 'a' && nxtCh <= 'z')
				|| (nxtCh >= 'A' && nxtCh <= 'Z');
	}

	/**
	 * Izpise simbol v datoteko z vmesnimi rezultati.
	 * 
	 * @param symb
	 *            Simbol, ki naj bo izpisan.
	 */
	private void dump(Symbol symb) {
		if (!dump)
			return;
		if (Report.dumpFile() == null)
			return;
		if (symb.token == TokenType.EOF)
			Report.dumpFile().println(symb.toString());
		else
			Report.dumpFile().println(
					"[" + symb.position.toString() + "] " + symb.toString());
	}
}
