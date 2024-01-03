/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
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
