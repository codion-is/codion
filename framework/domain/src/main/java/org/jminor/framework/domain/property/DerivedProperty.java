/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * A property which value is derived from the values of one or more properties.
 */
public interface DerivedProperty extends TransientProperty {

  /**
   * @return the ids of properties this property derives from.
   */
  List<String> getSourcePropertyIds();

  /**
   * @return the value provider, providing the derived value
   */
  Provider getValueProvider();

  /**
   * Responsible for providing values derived from other values
   */
  interface Provider extends Serializable {

    /**
     * @param sourceValues the source values, mapped to their respective propertyIds
     * @return the derived value
     */
    Object getValue(final Map<String, Object> sourceValues);
  }
}
