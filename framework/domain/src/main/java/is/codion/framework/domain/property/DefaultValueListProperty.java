/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;

import static java.util.Collections.unmodifiableList;

final class DefaultValueListProperty<T> extends DefaultColumnProperty<T> implements ValueListProperty<T> {

  private static final long serialVersionUID = 1;

  private final List<Item<T>> items;

  /**
   * @param attribute the attribute
   * @param caption the property caption
   * @param items the allowed values for this property
   */
  DefaultValueListProperty(final Attribute<T> attribute, final String caption, final List<Item<T>> items) {
    super(attribute, caption);
    this.items = unmodifiableList(items);
  }

  @Override
  public boolean isValid(final T value) {
    return findItem(value) != null;
  }

  @Override
  public List<Item<T>> getValues() {
    return items;
  }

  @Override
  public String getCaption(final T value) {
    final Item<T> item = findItem(value);

    return item == null ? "" : item.getCaption();
  }

  private Item<T> findItem(final T value) {
    for (int i = 0; i < items.size(); i++) {
      final Item<T> item = items.get(i);
      if (Objects.equals(item.getValue(), value)) {
        return item;
      }
    }

    return null;
  }
}
