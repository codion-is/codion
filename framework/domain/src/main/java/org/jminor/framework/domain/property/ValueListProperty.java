/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.Item;

import java.util.List;

/**
 * A property based on a list of values, each with a displayable caption.
 */
public interface ValueListProperty extends ColumnProperty {

  /**
   * @param value the value to validate
   * @return true if the given value is valid for this property
   */
  boolean isValid(final Object value);

  /**
   * @return an unmodifiable view of the available values
   */
  List<Item> getValues();

  /**
   * @param value the value
   * @return the caption associated with the given value
   */
  String getCaption(final Object value);
}
