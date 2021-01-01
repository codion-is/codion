/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

import java.io.Serializable;
import java.util.List;

/**
 * A property which value is derived from the values of one or more properties.
 * @param <T> the underlying type
 */
public interface DerivedProperty<T> extends TransientProperty<T> {

  /**
   * @return the attributes this property derives from.
   */
  List<Attribute<?>> getSourceAttributes();

  /**
   * @return the value provider, providing the derived value
   */
  Provider<T> getValueProvider();

  /**
   * Provides the source values from which to derive the property value.
   */
  interface SourceValues {

    /**
     * Returns the source value associated with the given attribute.
     * @param attribute the attribute which value to retrieve
     * @param <T> the value type
     * @return the value associated with attribute
     */
    <T> T get(Attribute<T> attribute);
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
