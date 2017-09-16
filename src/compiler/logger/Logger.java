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

package compiler.logger;

import compiler.Position;

import java.io.FileNotFoundException;
import java.io.PrintStream;

public class LoggerImpl implements logger {

	public boolean reporting = true;
    public String fileName = null;
    private PrintStream dumpFile = null;

	@Override
    public void report(String message) {
		if (reporting)
			System.err.println(":-) " + message);
	}

	@Override
    public void report(Position pos, String message) {
		report("[" + pos.toString() + "] " + message);
	}

	@Override
    public void warning(String message) {
		if (fileName != null)
			message = fileName + ":" + message;
		
		System.err.println(":-o " + message);
	}

	@Override
    public void warning(Position pos, String message) {
		warning("[" + pos.toString() + "] " + message);
	}

	@Override
    public void warning(int line, int column, String message) {
		warning(new Position(line, column), message);
	}

	@Override
    public void error(String message) {
		if (fileName != null)
			message = fileName + ": " + message;
		
		System.err.println(":-( " + message);
		System.err.flush();
		System.exit(1);
	}

	@Override
    public void error(Position pos, String message) {
		error("[" + pos.toString() + "] " + message);
	}

	@Override
    public void error(int line, int column, String message) {
		error(new Position(line, column), message);
	}

	@Override
    public void openDumpFile(String sourceFileName) {
		String dumpFileName = sourceFileName.replaceFirst("\\.prev$", "") + ".log";
		try {
			dumpFile = new PrintStream(dumpFileName);
		} catch (FileNotFoundException __) {
			warning("Cannot produce dump file '" + dumpFileName + "'.");
		}
	}

	@Override
    public void closeDumpFile() {
		dumpFile.close();
		dumpFile = null;
	}

	@Override
    public PrintStream dumpFile() {
		if (dumpFile == null)
			error ("Internal error: compiler.logger.LoggerImpl.dumpFile().");
		return dumpFile;
	}

	@Override
    public void dump(int indent, String line) {
		dump(indent, line, true);
	}

    @Override
    public void dump(int indent, String line, boolean newLine) {
        for (int i = 0; i < indent; i++) dumpFile.print(" ");
        if (newLine)
            dumpFile.println(line);
        else
            dumpFile.print(line);
    }

    @Override
    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    @Override
    public String getFileName() {
        return fileName;
    }
}
