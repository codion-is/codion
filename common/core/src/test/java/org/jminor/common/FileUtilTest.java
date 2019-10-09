package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
    final String newline = System.getProperty("line.separator");
    final String fileContents = "h1-h2-h3" + newline + "one-two-three" + newline + "1-2-3";
    assertEquals(fileContents, TextUtil.getTextFileContents(file.toString(), StandardCharsets.UTF_8));

    file.delete();
  }

  @Test
  public void serialize() throws IOException, ClassNotFoundException {
    final List<Integer> ints = asList(1, 2, 3, 4);
    final File file = File.createTempFile("FileUtilTest.serialize", ".txt");
    file.deleteOnExit();

    FileUtil.serializeToFile(ints, file);

    final List<Integer> readInts = FileUtil.deserializeFromFile(file);

    assertEquals(ints, readInts);

    file.delete();
  }
}
