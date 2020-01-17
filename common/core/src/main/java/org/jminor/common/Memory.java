/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

/**
 * A utility class for memory usage information.
 */
public final class Memory {

  private static final int K = 1024;

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
   * @return a String indicating the memory usage of this JVM
   */
  public static String getMemoryUsageString() {
    return getUsedMemory() + " KB";
  }
}
