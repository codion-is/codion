/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

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
   * Responsible for providing values derived from other values
   * @param <T> the underlying type
   */
  interface Provider<T> extends Serializable {

    /**
     * @param sourceValues the source values, mapped to their respective attributes
     * @return the derived value
     */
    T getValue(Map<Attribute<?>, Object> sourceValues);
  }
}
