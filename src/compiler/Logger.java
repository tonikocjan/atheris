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

package compiler;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Logger {

	public static boolean reporting = true;
    public static String fileName = null;
    private static PrintStream dumpFile = null;

	public static void report(String message) {
		if (reporting)
			System.err.println(":-) " + message);
	}

	public static void report(Position pos, String message) {
		report("[" + pos.toString() + "] " + message);
	}

	public static void warning(String message) {
		if (fileName != null)
			message = fileName + ":" + message;
		
		System.err.println(":-o " + message);
	}

	public static void warning(Position pos, String message) {
		warning("[" + pos.toString() + "] " + message);
	}

	public static void warning(int line, int column, String message) {
		warning(new Position(line, column), message);
	}

	public static void error(String message) {
		if (fileName != null)
			message = fileName + ": " + message;
		
		System.err.println(":-( " + message);
		System.err.flush();
		System.exit(1);
	}

	public static void error(Position pos, String message) {
		error("[" + pos.toString() + "] " + message);
	}

	public static void error(int line, int column, String message) {
		error(new Position(line, column), message);
	}

	public static void openDumpFile(String sourceFileName) {
		String dumpFileName = sourceFileName.replaceFirst("\\.prev$", "") + ".log";
		try {
			dumpFile = new PrintStream(dumpFileName);
		} catch (FileNotFoundException __) {
			Logger.warning("Cannot produce dump file '" + dumpFileName + "'.");
		}
	}

	public static void closeDumpFile() {
		dumpFile.close();
		dumpFile = null;
	}

	public static PrintStream dumpFile() {
		if (dumpFile == null)
			Logger.error ("Internal error: compiler.Logger.dumpFile().");
		return dumpFile;
	}

	public static void dump(int indent, String line) {
		dump(indent, line, true);
	}

    public static void dump(int indent, String line, boolean newLine) {
        for (int i = 0; i < indent; i++) dumpFile.print(" ");
        if (newLine)
            dumpFile.println(line);
        else
            dumpFile.print(line);
    }
}
