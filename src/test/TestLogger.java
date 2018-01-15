package test;

import compiler.Position;
import compiler.logger.LoggerInterface;

import java.io.PrintStream;

/**
 * Created by Toni Kocjan on 15/01/2018.
 * Triglav Partner BE
 */

public class TestLogger implements LoggerInterface {

    PrintStream outputStream = System.out;

    @Override
    public void report(String message) {
        outputStream.println(message);
    }

    @Override
    public void report(Position pos, String message) {
        report(message);
    }

    @Override
    public void warning(String message) {
        report(message);
    }

    @Override
    public void warning(Position pos, String message) {
        report(message);
    }

    @Override
    public void warning(int line, int column, String message) {
        report(message);
    }

    @Override
    public void error(String message) {
        report(message);
    }

    @Override
    public void error(Position pos, String message) {
        report(message);
    }

    @Override
    public void error(int line, int column, String message) {
        report(message);
    }

    @Override
    public void openDumpFile(String sourceFileName) {

    }

    @Override
    public void closeDumpFile() {

    }

    @Override
    public PrintStream dumpFile() {
        return null;
    }

    @Override
    public void dump(int indent, String line) {

    }

    @Override
    public void dump(int indent, String line, boolean newLine) {

    }

    @Override
    public void setFileName(String fileName) {

    }

    @Override
    public String getFileName() {
        return null;
    }
}
