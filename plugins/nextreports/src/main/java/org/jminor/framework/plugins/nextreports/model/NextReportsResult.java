/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.plugins.nextreports.model;

import org.jminor.common.model.Util;
import org.jminor.common.model.reports.ReportResult;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Objects;

public final class NextReportsResult implements ReportResult<byte[]>, Serializable {

  private final byte[] bytes;
  private final String format;

  public NextReportsResult(final byte[] bytes, final String format) {
    this.bytes = Objects.requireNonNull(bytes, "bytes");
    this.format = Objects.requireNonNull(format, "format");
  }

  @Override
  public byte[] getResult() {
    return bytes;
  }

  public File writeResultToFile(final String parentDirectory, final String filename) throws IOException {
    Objects.requireNonNull(parentDirectory, "parentDirectory");
    Objects.requireNonNull(filename, "filename");
    final File file = new File(parentDirectory + Util.FILE_SEPARATOR + filename + "." + format.toLowerCase());
    if (file.exists()) {
      throw new IllegalArgumentException("File '" + file + "' already exists");
    }

    return writeResultToFile(file);
  }

  public File writeResultToFile(final File file) throws IOException {
    Files.write(file.toPath(), getResult());

    return file;
  }
}
