/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
  public static long getAllocatedMemory() {
    return Runtime.getRuntime().totalMemory() / K;
  }

  /**
   * @return the free memory available to this JVM in kilobytes
   */
  public static long getFreeMemory() {
    return Runtime.getRuntime().freeMemory() / K;
  }

  /**
   * @return the maximum memory available to this JVM in kilobytes
   */
  public static long getMaxMemory() {
    return Runtime.getRuntime().maxMemory() / K;
  }

  /**
   * @return the memory used by this JVM in kilobytes
   */
  public static long getUsedMemory() {
    return getAllocatedMemory() - getFreeMemory();
  }

  /**
   * @return a String containing the memory usage of this JVM in kilobytes.
   */
  public static String getMemoryUsage() {
    synchronized (FORMAT) {
      return FORMAT.format(getUsedMemory()) + " KB";
    }
  }
}
