/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

/**
 * Utility class for separators.
 */
public final class Separators {

  /**
   * The line separator for the current system, specified by the 'line.separator' system property
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * The file separator for the current system, specified by the 'file.separator' system property
   */
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");

  /**
   * The path separator for the current system, specified by the 'path.separator' system property
   */
  public static final String PATH_SEPARATOR = System.getProperty("path.separator");

  private Separators() {}
}
