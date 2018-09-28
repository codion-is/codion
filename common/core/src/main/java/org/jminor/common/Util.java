/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Misc. utilities.
 */
public class Util {

  private static final Logger LOG = LoggerFactory.getLogger(Util.class);

  private static final int K = 1024;
  private static final int TEN = 10;
  private static final byte[] EMPTY_BYTE_ARRAY = new byte[0];


  /**
   * The line separator for the current system
   */
  public static final String LINE_SEPARATOR = System.getProperty("line.separator");

  /**
   * The file separator for the current system
   */
  public static final String FILE_SEPARATOR = System.getProperty("file.separator");

  private Util() {}

  /**
   * @param strings the strings to check
   * @return true if one of the given strings is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final String... strings) {
    if (strings == null || strings.length == 0) {
      return true;
    }
    for (int i = 0; i < strings.length; i++) {
      final String string = strings[i];
      if (string == null || string.length() == 0) {
        return true;
      }
    }

    return false;
  }

  /**
   * Checks if any of the given objects is null
   * @param objects the objects to check
   * @return true if none of the given objects is null
   */
  public static boolean notNull(final Object... objects) {
    if (objects == null) {
      return false;
    }

    return Arrays.stream(objects).noneMatch(Objects::isNull);
  }

  /**
   * Maps the given values according to the keys provided by the given key provider,
   * keeping the iteration order of the given collection.
   * {@code
   * class Person {
   *   String name;
   *   Integer age;
   *   ...
   * }
   *
   * List&#60;Person&#62; persons = ...;
   * MapKeyProvider ageKeyProvider = new MapKeyProvider&#60;Integer, Person&#62;() {
   *   public Integer getKey(Person person) {
   *     return person.getAge();
   *   }
   * };
   * Map&#60;Integer, List&#60;Person&#62;&#62; personsByAge = Util.map(persons, ageKeyProvider);
   * }
   * @param values the values to map
   * @param keyProvider the object providing keys for values
   * @param <K> the key type
   * @param <V> the value type
   * @return a LinkedHashMap with the values mapped to their respective key values, respecting the iteration order of the given collection
   */
  public static <K, V> LinkedHashMap<K, List<V>> map(final Collection<V> values, final MapKeyProvider<K, V> keyProvider) {
    Objects.requireNonNull(values, "values");
    Objects.requireNonNull(keyProvider, "keyProvider");
    final LinkedHashMap<K, List<V>> map = new LinkedHashMap<>(values.size());
    for (final V value : values) {
      map.computeIfAbsent(keyProvider.getKey(value), k -> new ArrayList<>()).add(value);
    }

    return map;
  }

  /**
   * @param className the name of the class to search for
   * @return true if the given class is found on the classpath
   */
  public static boolean onClasspath(final String className) {
    try {
      Class.forName(Objects.requireNonNull(className, "className"));
      return true;
    }
    catch (final ClassNotFoundException e) {
      return false;
    }
  }

  /**
   * @param maps the maps to check
   * @return true if one of the given maps is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final Map... maps) {
    if (maps == null) {
      return true;
    }

    return Arrays.stream(maps).anyMatch(Util::nullOrEmpty);
  }

  /**
   * @param map the map to check
   * @return true if the given map is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final Map map) {
    return map == null || map.isEmpty();
  }

  /**
   * @param collections the collections to check
   * @return true if one of the given collections is null or empty or if no arguments are provided, false otherwise
   */
  public static boolean nullOrEmpty(final Collection... collections) {
    if (collections == null) {
      return true;
    }

    return Arrays.stream(collections).anyMatch(Util::nullOrEmpty);
  }

  /**
   * @param collection the collection to check
   * @return true if the given collection is null or empty, false otherwise
   */
  public static boolean nullOrEmpty(final Collection collection) {
    return collection == null || collection.isEmpty();
  }

  /**
   * Throws an IllegalArgumentException if the given string value is null or empty
   * @param value the string value
   * @param valueName the name of the value to include in the error message
   * @return the string value
   */
  public static String rejectNullOrEmpty(final String value, final String valueName) {
    if (value == null || value.isEmpty()) {
      throw new IllegalArgumentException(valueName + " is null or empty");
    }

    return value;
  }

  /**
   * Rounds the given double to {@code places} decimal places
   * @param d the double to round
   * @param places the number of decimal places
   * @return the rounded value
   */
  public static double roundDouble(final double d, final int places) {
    return Math.round(d * Math.pow(TEN, (double) places)) / Math.pow(TEN, (double) places);
  }

  /**
   * Closes the given Closeable instances, swallowing any Exceptions that occur
   * @param closeables the closeables to close
   */
  public static void closeSilently(final Closeable... closeables) {
    if (closeables == null) {
      return;
    }
    for (final Closeable closeable : closeables) {
      try {
        if (closeable != null) {
          closeable.close();
        }
      }
      catch (final Exception ignored) {/*ignored*/}
    }
  }

  /**
   * @return a String containing all system properties, one per line
   */
  public static String getSystemProperties() {
    try {
      final SecurityManager manager = System.getSecurityManager();
      if (manager != null) {
        manager.checkPropertiesAccess();
      }
    }
    catch (final SecurityException e) {
      LOG.error(e.getMessage(), e);
      return "";
    }
    final Properties props = System.getProperties();
    final Enumeration propNames = props.propertyNames();
    final List<String> propertyNames = new ArrayList<>(props.size());
    while (propNames.hasMoreElements()) {
      propertyNames.add((String) propNames.nextElement());
    }

    Collections.sort(propertyNames);

    return propertyNames.stream().map(key -> key + ": " + props.getProperty(key)).collect(Collectors.joining("\n"));
  }

  /**
   * Initializes a proxy instance for the given class, using the class loader of that class
   * @param clazz the class to proxy
   * @param invocationHandler the invocation handler to use
   * @param <T> the type
   * @return a proxy for the given class
   */
  @SuppressWarnings({"unchecked"})
  public static <T> T initializeProxy(final Class<T> clazz, final InvocationHandler invocationHandler) {
    return (T) Proxy.newProxyInstance(clazz.getClassLoader(), new Class[] {clazz}, invocationHandler);
  }

  /**
   * Serializes the given Object, null object results in an empty byte array
   * @param object the object
   * @return a byte array representing the serialized object, an empty byte array in case of null
   * @throws IOException in case of an exception
   */
  public static byte[] serialize(final Object object) throws IOException {
    if (object != null) {
      final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      new ObjectOutputStream(byteArrayOutputStream).writeObject(object);

      return byteArrayOutputStream.toByteArray();
    }

    return EMPTY_BYTE_ARRAY;
  }

  /**
   * Deserializes the given byte array into a T, null or an empty byte array result in a null return value
   * @param bytes a byte array representing the serialized object
   * @param <T> the type of the object represented in the byte array
   * @return the deserialized object
   * @throws IOException in case of an exception
   * @throws ClassNotFoundException in case the deserialized class is not found
   */
  public static <T> T deserialize(final byte[] bytes) throws IOException, ClassNotFoundException {
    if (bytes != null && bytes.length > 0) {
      return (T) new ObjectInputStream(new ByteArrayInputStream(bytes)).readObject();
    }

    return null;
  }

  /**
   * Provides objects of type K, derived from a value of type V, for hashing said value via .hashCode().
   * @param <K> the type of the object to use for key generation via .hashCode()
   * @param <V> the value type
   * @see Util#map(java.util.Collection, MapKeyProvider)
   */
  public interface MapKeyProvider<K, V> {
    /**
     * @param value the value being mapped
     * @return a map key for the given value
     */
    K getKey(final V value);
  }

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

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the set method
   * @param valueOwner a bean instance
   * @return the method used to set the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getSetMethod(final Class valueType, final String property, final Object valueOwner) throws NoSuchMethodException {
    return getSetMethod(valueType, property, Objects.requireNonNull(valueOwner, "valueOwner").getClass());
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the set method
   * @param ownerClass the bean class
   * @return the method used to set the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getSetMethod(final Class valueType, final String property, final Class<?> ownerClass) throws NoSuchMethodException {
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(ownerClass, "ownerClass");
    if (property.length() == 0) {
      throw new IllegalArgumentException("Property must be specified");
    }
    final String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    return ownerClass.getMethod("set" + propertyName, valueType);
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the get method
   * @param valueOwner a bean instance
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getGetMethod(final Class valueType, final String property, final Object valueOwner) throws NoSuchMethodException {
    Objects.requireNonNull(valueOwner, "valueOwner");
    return getGetMethod(valueType, property, valueOwner.getClass());
  }

  /**
   * @param valueType the class of the value for the given bean property
   * @param property the name of the bean property for which to retrieve the get method
   * @param ownerClass the bean class
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  public static Method getGetMethod(final Class valueType, final String property, final Class<?> ownerClass) throws NoSuchMethodException {
    Objects.requireNonNull(valueType, "valueType");
    Objects.requireNonNull(property, "property");
    Objects.requireNonNull(ownerClass, "ownerClass");
    if (property.length() == 0) {
      throw new IllegalArgumentException("Property must be specified");
    }
    final String propertyName = Character.toUpperCase(property.charAt(0)) + property.substring(1);
    if (valueType.equals(boolean.class) || valueType.equals(Boolean.class)) {
      try {
        return ownerClass.getMethod("is" + propertyName);
      }
      catch (final NoSuchMethodException ignored) {/*ignored*/}
      try {
        return ownerClass.getMethod(propertyName.substring(0, 1).toLowerCase() + propertyName.substring(1));
      }
      catch (final NoSuchMethodException ignored) {/*ignored*/}
    }

    return ownerClass.getMethod("get" + propertyName);
  }
}
