/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * A utility class for working with primitives.
 */
public final class PrimitiveTypes {

  private static final Map<Class<?>, Class<?>> PRIMITIVE_BOXED_TYPE_MAP;
  private static final Map<Class<?>, Object> DEFAULT_PRIMITIVE_VALUES;

  private static boolean defaultBoolean;
  private static byte defaultByte;
  private static short defaultShort;
  private static int defaultInt;
  private static long defaultLong;
  private static float defaultFloat;
  private static double defaultDouble;

  static {
    PRIMITIVE_BOXED_TYPE_MAP = new HashMap<>();
    PRIMITIVE_BOXED_TYPE_MAP.put(Boolean.TYPE, Boolean.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Byte.TYPE, Byte.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Short.TYPE, Short.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Integer.TYPE, Integer.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Long.TYPE, Long.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Float.TYPE, Float.class);
    PRIMITIVE_BOXED_TYPE_MAP.put(Double.TYPE, Double.class);

    DEFAULT_PRIMITIVE_VALUES = new HashMap<>();
    DEFAULT_PRIMITIVE_VALUES.put(Boolean.TYPE, defaultBoolean);
    DEFAULT_PRIMITIVE_VALUES.put(Byte.TYPE, defaultByte);
    DEFAULT_PRIMITIVE_VALUES.put(Short.TYPE, defaultShort);
    DEFAULT_PRIMITIVE_VALUES.put(Integer.TYPE, defaultInt);
    DEFAULT_PRIMITIVE_VALUES.put(Long.TYPE, defaultLong);
    DEFAULT_PRIMITIVE_VALUES.put(Float.TYPE, defaultFloat);
    DEFAULT_PRIMITIVE_VALUES.put(Double.TYPE, defaultDouble);
  }

  private PrimitiveTypes() {}

  /**
   * @param <T> the type
   * @param primitiveType the primitive type
   * @return the default value for the given type
   * @throws IllegalArgumentException in case primitiveType is not a primitive type
   */
  public static <T> T getDefaultValue(Class<T> primitiveType) {
    if (!requireNonNull(primitiveType).isPrimitive()) {
      throw new IllegalArgumentException("Not a primitive type: " + primitiveType);
    }

    return (T) DEFAULT_PRIMITIVE_VALUES.get(primitiveType);
  }

  /**
   * @param <T> the type
   * @param primitiveType the primitive type
   * @return the boxed type
   * @throws IllegalArgumentException in case primitiveType is not a primitive type
   */
  public static <T> Class<T> getBoxedType(Class<T> primitiveType) {
    if (!requireNonNull(primitiveType).isPrimitive()) {
      throw new IllegalArgumentException("Not a primitive type: " + primitiveType);
    }

    return (Class<T>) PRIMITIVE_BOXED_TYPE_MAP.get(primitiveType);
  }
}
