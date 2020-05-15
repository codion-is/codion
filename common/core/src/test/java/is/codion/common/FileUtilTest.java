package is.codion.common;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileUtilTest {

  @Test
  public void countLines() throws IOException {
    final File file = File.createTempFile("FileUtilTest.countLines", ".txt");
    file.deleteOnExit();

    Files.write(file.toPath(), Arrays.asList("one", "two", "three", "--four", "five"));

    assertEquals(5, FileUtil.countLines(file.toString()));
    assertEquals(5, FileUtil.countLines(file));
    assertEquals(4, FileUtil.countLines(file, "--"));
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
