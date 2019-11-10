/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import org.jminor.common.Item;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;

final class DefaultValueListProperty extends DefaultColumnProperty implements ValueListProperty {

  private static final long serialVersionUID = 1;

  private final List<Item> items;

  /**
   * @param propertyId the property ID
   * @param type the data type of this property
   * @param caption the property caption
   * @param items the allowed values for this property
   */
  DefaultValueListProperty(final String propertyId, final int type, final String caption, final List<Item> items) {
    super(propertyId, type, caption);
    this.items = unmodifiableList(items);
  }

  @Override
  public boolean isValid(final Object value) {
    return findItem(value) != null;
  }

  @Override
  public List<Item> getValues() {
    return items;
  }

  @Override
  public String getCaption(final Object value) {
    final Item item = findItem(value);

    return item == null ? "" : item.getCaption();
  }

  private Item findItem(final Object value) {
    for (int i = 0; i < items.size(); i++) {
      final Item item = items.get(i);
      if (Objects.equals(item.getValue(), value)) {
        return item;
      }
    }

    return null;
  }
}
