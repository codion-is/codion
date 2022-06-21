/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for mapping values to keys.
 */
public final class Mapper {

  private Mapper() {}

  /**
   * Maps the given values according to the keys provided by the given key provider,
   * keeping the iteration order of the given collection. Null keys are allowed.
   * <pre>
   * class Person {
   *   String name;
   *   Integer age;
   *
   *   public Integer getAge() {
   *     return age;
   *   }
   * }
   *
   * List&#60;Person&#62; persons = ...;
   *
   * Map&#60;Integer, List&#60;Person&#62;&#62; personsByAge = Mapper.map(persons, Person::getAge);
   * </pre>
   * @param values the values to map
   * @param keyProvider the object providing keys for values
   * @param <K> the key type
   * @param <T> the value type
   * @return a LinkedHashMap with the values mapped to their respective key values, respecting the iteration order of the given collection
   */
  public static <K, T> LinkedHashMap<K, List<T>> map(Collection<? extends T> values, Function<T, K> keyProvider) {
    requireNonNull(values, "values");
    requireNonNull(keyProvider, "keyProvider");
    LinkedHashMap<K, List<T>> map = new LinkedHashMap<>(values.size());
    for (T value : values) {
      map.computeIfAbsent(keyProvider.apply(value), k -> new ArrayList<>()).add(value);
    }

    return map;
  }
}
