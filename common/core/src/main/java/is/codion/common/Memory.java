/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.text.NumberFormat;

/**
 * A utility class for memory usage information.
 */
public final class Memory {

  private static final NumberFormat FORMAT = NumberFormat.getIntegerInstance();
  private static final int K = 1024;

  private Memory() {}

  /**
   * @return the total memory allocated by this JVM in kilobytes
   */
  public static long allocatedMemory() {
    return Runtime.getRuntime().totalMemory() / K;
  }

  /**
   * @return the free memory available to this JVM in kilobytes
   */
  public static long freeMemory() {
    return Runtime.getRuntime().freeMemory() / K;
  }

  /**
   * @return the maximum memory available to this JVM in kilobytes
   */
  public static long maxMemory() {
    return Runtime.getRuntime().maxMemory() / K;
  }

  /**
   * @return the memory used by this JVM in kilobytes
   */
  public static long usedMemory() {
    return allocatedMemory() - freeMemory();
  }

  /**
   * @return a String containing the memory usage of this JVM in kilobytes.
   */
  public static String memoryUsage() {
    synchronized (FORMAT) {
      return FORMAT.format(usedMemory()) + " KB";
    }
  }
}
