/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

/**
 * A utility class for working with files
 */
public final class FileUtil {

  private FileUtil() {}

  /**
   * @param filename the name of the file
   * @return the number of lines in the given file
   * @throws java.io.IOException in case the file can not be read
   */
  public static int countLines(final String filename) throws IOException {
    return countLines(new File(filename), null);
  }

  /**
   * @param file the file
   * @return the number of lines in the given file
   * @throws IOException in case the file can not be read
   */
  public static int countLines(final File file) throws IOException {
    return countLines(file, null);
  }

  /**
   * @param file the file
   * @param excludePrefix lines are excluded from the count if they start with this string
   * @return the number of lines in the given file
   * @throws IOException in case the file can not be read
   */
  public static int countLines(final File file, final String excludePrefix) throws IOException {
    try (final BufferedReader reader = new BufferedReader(new FileReader(file))) {
      int lines = 0;
      String line = reader.readLine();
      while (line != null) {
        if (excludePrefix == null || !line.startsWith(excludePrefix)) {
          lines++;
        }
        line = reader.readLine();
      }

      return lines;
    }
  }
}
