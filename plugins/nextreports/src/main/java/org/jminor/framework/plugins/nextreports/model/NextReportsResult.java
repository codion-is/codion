/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.Util;
import org.jminor.common.db.reports.ReportResult;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;

import static java.util.Objects.requireNonNull;

/**
 * A {@link ReportResult} implementation based on NextReports
 */
public final class NextReportsResult implements ReportResult<byte[]>, Serializable {

  private static final long serialVersionUID = 1;

  private final byte[] bytes;
  private final String format;

  /**
   * Instantiates a new {@link NextReportsResult}
   * @param bytes the bytes comprising the report result
   * @param format the report format
   */
  public NextReportsResult(final byte[] bytes, final String format) {
    this.bytes = requireNonNull(bytes, "bytes");
    this.format = requireNonNull(format, "format");
  }

  /** {@inheritDoc} */
  @Override
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
  public File writeResultToFile(final String parentDirectory, final String filename) throws IOException {
    requireNonNull(parentDirectory, "parentDirectory");
    requireNonNull(filename, "filename");
    final File file = new File(parentDirectory + Util.FILE_SEPARATOR + filename + "." + format.toLowerCase());
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
  public File writeResultToFile(final File file) throws IOException {
    Files.write(file.toPath(), getResult());

    return file;
  }
}
