/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.common.item.Item;
import is.codion.framework.domain.entity.Attribute;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

final class DefaultItemProperty<T> extends DefaultColumnProperty<T> implements ItemProperty<T> {

  private static final long serialVersionUID = 1;

  private final List<Item<T>> items;

  /**
   * @param attribute the attribute
   * @param caption the property caption
   * @param items the allowed items for this property
   */
  DefaultItemProperty(Attribute<T> attribute, String caption, List<Item<T>> items) {
    super(attribute, caption);
    validateItems(items);
    this.items = unmodifiableList(items);
  }

  @Override
  public boolean isValid(T value) {
    return findItem(value) != null;
  }

  @Override
  public List<Item<T>> getItems() {
    return items;
  }

  private Item<T> findItem(T value) {
    for (int i = 0; i < items.size(); i++) {
      Item<T> item = items.get(i);
      if (Objects.equals(item.getValue(), value)) {
        return item;
      }
    }

    return null;
  }

  private static <T> void validateItems(List<Item<T>> items) {
    if (requireNonNull(items).size() != items.stream()
            .map(Item::getValue)
            .collect(Collectors.toSet())
            .size()) {
      throw new IllegalArgumentException("Item contains duplicate values: " + items);
    }
  }
}
