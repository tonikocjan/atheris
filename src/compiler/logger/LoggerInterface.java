package compiler.logger;

import compiler.Position;

import java.io.PrintStream;

/**
 * Created by Toni Kocjan on 16/09/2017.
 * Triglav Partner BE
 */

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
