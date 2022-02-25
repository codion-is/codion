/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.nextreports.model;

import is.codion.common.Util;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import static java.util.Objects.requireNonNull;

/**
 * A report result based on NextReports
 */
public final class NextReportsResult implements Serializable {

  private static final long serialVersionUID = 1;

  private final byte[] bytes;
  private final String format;

  /**
   * Instantiates a new {@link NextReportsResult}
   * @param bytes the bytes comprising the report result
   * @param format the report format
   */
  public NextReportsResult(byte[] bytes, String format) {
    this.bytes = requireNonNull(bytes, "bytes");
    this.format = requireNonNull(format, "format");
  }

  /**
   * @return the result as a byte array
   */
  public byte[] getResult() {
    return bytes;
  }

  /**
   * Writes this result to a file
   * @param parentDirectory the parent directory
   * @param filename the name of the file to write
   * @return the file
   * @throws IOException in case of an exception
   */
  public File writeResultToFile(String parentDirectory, String filename) throws IOException {
    requireNonNull(parentDirectory, "parentDirectory");
    requireNonNull(filename, "filename");
    File file = new File(parentDirectory + Util.FILE_SEPARATOR + filename + "." + format.toLowerCase());
    if (file.exists()) {
      throw new IllegalArgumentException("File '" + file + "' already exists");
    }

    return writeResultToFile(file);
  }

  /**
   * Writes this result to a file
   * @param file the file to write to
   * @return the file
   * @throws IOException in case of an exception
   */
  public File writeResultToFile(File file) throws IOException {
    Files.write(file.toPath(), bytes);

    return file;
  }
}
