/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

/**
 * A property representing a column that should get its value automatically from a column in a referenced table
 */
public interface DenormalizedProperty extends ColumnProperty {

  /**
   * @return the id of the foreign key property from which this property should retrieve its value
   */
  String getForeignKeyPropertyId();

  /**
   * @return the property in the referenced entity from which this property gets its value
   */
  Property getDenormalizedProperty();
}
