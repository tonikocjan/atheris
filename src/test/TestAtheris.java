package test;

import compiler.Main;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Created by Toni Kocjan on 31/07/2017.
 * Triglav Partner BE
 */
public class TestAtheris {

    @Test
    public void testBasics() {
        String[] args = new String[3];

        args[0] = "test/1.Basics.ar";
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
                // does not neccessarily read a whole array!
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