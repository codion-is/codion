/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.io.File;
import java.nio.file.FileSystems;

/**
 * Utility class for separators.
 */
public final class Separators {

  /**
   * The line separator for the current system, specified by the 'line.separator' system property
   */
  public static final String LINE_SEPARATOR = System.lineSeparator();

  /**
   * The file separator for the current system, specified by the 'file.separator' system property
   */
  public static final String FILE_SEPARATOR = FileSystems.getDefault().getSeparator();

  /**
   * The path separator for the current system, specified by the 'path.separator' system property
   */
  public static final String PATH_SEPARATOR = File.pathSeparator;

  private Separators() {}
}
