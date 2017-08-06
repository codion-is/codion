package org.jminor.common;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FileUtilTest {

  @Test
  public void countLines() throws IOException {
    final File file = File.createTempFile("FileUtilTest.countLines", ".txt");
    file.deleteOnExit();

    FileUtil.writeFile("one\ntwo\nthree\n--four\nfive", file);

    assertEquals(5, FileUtil.countLines(file.toString()));
    assertEquals(5, FileUtil.countLines(file));
    assertEquals(4, FileUtil.countLines(file, "--"));
  }

  @Test
  public void writeDelimitedFile() throws IOException {
    final String[][] headers = new String[][] {{"h1", "h2", "h3"}};
    final String[][] data = new String[][] {{"one", "two", "three"}, {"1", "2", "3"}};
    final File file = File.createTempFile("FileUtilTest.writeDelimitedFile", ".txt");
    file.deleteOnExit();

    FileUtil.writeDelimitedFile(headers, data, "-", file);
    final String fileContents = "h1-h2-h3\none-two-three\n1-2-3";
    assertEquals(fileContents, TextUtil.getTextFileContents(file.toString(), Charset.forName("UTF-8")));

    file.delete();
  }

  @Test
  public void serialize() throws IOException, Serializer.SerializeException {
    final List<Integer> ints = Arrays.asList(1, 2, 3, 4);
    final File file = File.createTempFile("FileUtilTest.serialize", ".txt");
    file.deleteOnExit();

    FileUtil.serializeToFile(ints, file);

    final List<Integer> readInts = FileUtil.deserializeFromFile(file);

    assertEquals(ints, readInts);

    file.delete();
  }

  @Test
  public void getBytesFromFile() throws IOException {
    final File file = File.createTempFile("FileUtilTest.getBytesFromFile", ".txt");
    file.deleteOnExit();

    final String helloWorld = "hello world";
    FileUtil.writeFile(helloWorld, file);

    final byte[] expected = {(byte) 104, (byte) 101, (byte) 108, (byte) 108, (byte) 111, (byte) 32,
            (byte) 119, (byte) 111, (byte) 114, (byte) 108, (byte) 100};

    final byte[] bytes = FileUtil.getBytesFromFile(file);
    assertTrue(Arrays.equals(expected, bytes));

    file.delete();
  }
}
