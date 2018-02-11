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

import java.io.PrintStream;

public interface LoggerInterface {

    void report(String message);

    void report(Position pos, String message);

    void warning(String message);

    void warning(Position pos, String message);

    void warning(int line, int column, String message);

    void error(String message);

    void error(Position pos, String message);

    void error(int line, int column, String message);

    void openDumpFile(String sourceFileName);

    void closeDumpFile();

    PrintStream dumpFile();

    void dump(int indent, String line);

    void dump(int indent, String line, boolean newLine);

    void setFileName(String fileName);

    String getFileName();
}
