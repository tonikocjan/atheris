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

package test;

import compiler.Main;
import compiler.logger.LoggerFactory;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertTrue;

public class TestAtheris {

    public TestAtheris() {
        LoggerFactory.setLoggerImpl(TestLogger.class);
    }

    @Test
    public void testBasics() {
        test("1.Basics.ar");
    }

    @Test
    public void testControlFlow() {
        test("2.ControlFlow.ar");
    }

    @Test
    public void testFunctions() {
        test("4.Functions.ar");
    }

    @Test
    public void testTuples() {
        test("5.Tuples.ar");
    }

    @Test
    public void testEnums() {
        test("7.Enums.ar");
    }

    @Test
    public void testInheritance() {
        test("10.Inheritance.ar");
    }

    @Test
    public void testNestedClasses() {
        test("11.NestedClasses.ar");
    }

    @Test
    public void testQuickSort() {
        test("13.QuickSort.ar");
    }

    @Test
    public void testAbstractClasses() {
        test("14.AbstractClasses.ar");
    }

    private void test(String sourceFileName) {
        String[] args = new String[3];

        args[0] = "test/demo/" + sourceFileName;
        args[1] = "--phase=interpret";
        args[2] = "--testing=true";

        Main.main(args);

        assertTrue(compareFiles(args[0] + ".out", args[0] + ".test"));
    }

    private boolean compareFiles(String fileA, String fileB) {
        try {
            Path file1 = Paths.get(fileA);
            Path file2 = Paths.get(fileB);

            final long size;
            size = Files.size(file1);

            if (size != Files.size(file2))
                return false;

            if (size < 4096)
                return Arrays.equals(Files.readAllBytes(file1), Files.readAllBytes(file2));

            try (InputStream is1 = Files.newInputStream(file1);
                 InputStream is2 = Files.newInputStream(file2)) {
                // Compare byte-by-byte.
                // Note that this can be sped up drastically by reading large chunks
                // (e.g. 16 KBs) but care must be taken as InputStream.read(byte[])
                // does not necessarily read a whole array!
                int data;
                while ((data = is1.read()) != -1)
                    if (data != is2.read())
                        return false;
            }

            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return false;
    }
}