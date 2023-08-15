/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import java.io.Serializable;
import java.util.Optional;

public interface DerivedAttribute<T> extends Attribute<T> {

  /**
   * Provides the source values from which to derive the value.
   */
  interface SourceValues {

    /**
     * Returns the source value associated with the given attribute.
     * @param attribute the attribute which value to retrieve
     * @param <T> the value type
     * @return the value associated with attribute
     */
    <T> T get(Attribute<T> attribute);

    /**
     * Returns the source value associated with the given attribute.
     * @param attribute the attribute which value to retrieve
     * @param <T> the value type
     * @return the value associated with attribute, an empty Optional in case of null
     */
    default <T> Optional<T> optional(Attribute<T> attribute) {
      return Optional.ofNullable(get(attribute));
    }
  }

  /**
   * Responsible for providing values derived from other values
   * @param <T> the underlying type
   */
  interface Provider<T> extends Serializable {

    /**
     * @param sourceValues the source values, mapped to their respective attributes
     * @return the derived value
     */
    T get(SourceValues sourceValues);
  }
}
