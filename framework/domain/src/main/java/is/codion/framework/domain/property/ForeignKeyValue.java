/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

/**
 * A value associated with a {@link ForeignKeyProperty}.
 */
public interface ForeignKeyValue {

  /**
   * Returns the value associated with the given property.
   * @param property the property
   * @return the value
   */
  Object get(Property property);
}
