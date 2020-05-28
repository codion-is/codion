/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

/**
 * Provides values associated with a {@link Property}.
 */
public interface PropertyValueProvider {

  /**
   * Returns the value associated with the given property.
   * @param property the property
   * @return the value
   */
  Object get(Property property);
}
