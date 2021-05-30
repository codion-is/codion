package is.codion.common;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FileUtilTest {

  @Test
  void countLines() throws IOException {
    final File file = File.createTempFile("FileUtilTest.countLines", ".txt");
    file.deleteOnExit();

    Files.write(file.toPath(), Arrays.asList("one", "two", "three", "--four", "five"));

    assertEquals(5, FileUtil.countLines(file.toString()));
    assertEquals(5, FileUtil.countLines(file));
    assertEquals(4, FileUtil.countLines(file, "--"));
  }
}
