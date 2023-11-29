/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel implementation based on the {@link Item} class.
 * @param <T> the type of value wrapped by this combo box models items
 * @see #itemComboBoxModel()
 * @see #sortedItemComboBoxModel()
 * @see #booleanItemComboBoxModel()
 */
public final class ItemComboBoxModel<T> extends FilteredComboBoxModel<Item<T>> {

  private ItemComboBoxModel(List<Item<T>> items) {
    selectedItemTranslator().set(new SelectedItemTranslator());
    setItems(items);
  }

  private ItemComboBoxModel(Comparator<Item<T>> comparator, Collection<Item<T>> items) {
    selectedItemTranslator().set(new SelectedItemTranslator());
    comparator().set(comparator);
    setItems(items);
    if (containsItem(Item.item(null))) {
      setSelectedItem(null);
    }
  }

  /**
   * Returns the index of the Item representing the given value, -1 if this model does not contain such an Item.
   * @param value the value
   * @return the index of the Item representing the given value, -1 if not found
   */
  public int indexOf(T value) {
    for (int i = 0; i < getSize(); i++) {
      if (Objects.equals(getElementAt(i).get(), value)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> itemComboBoxModel() {
    return new ItemComboBoxModel<>(null, null);
  }

  /**
   * @param items the items
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> itemComboBoxModel(List<Item<T>> items) {
    return new ItemComboBoxModel<>(null, requireNonNull(items));
  }

  /**
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> sortedItemComboBoxModel() {
    return sortedItemComboBoxModel((List<Item<T>>) null);
  }

  /**
   * @param items the items
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> sortedItemComboBoxModel(List<Item<T>> items) {
    return new ItemComboBoxModel<>(items);
  }

  /**
   * @param comparator the comparator to use when sorting
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> sortedItemComboBoxModel(Comparator<Item<T>> comparator) {
    return new ItemComboBoxModel<>(requireNonNull(comparator), null);
  }

  /**
   * @param items the items
   * @param comparator the comparator to use when sorting
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> sortedItemComboBoxModel(List<Item<T>> items, Comparator<Item<T>> comparator) {
    requireNonNull(items);
    requireNonNull(comparator);

    return new ItemComboBoxModel<>(comparator, items);
  }

  /**
   * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
   * @return a Boolean based ItemComboBoxModel
   */
  public static ItemComboBoxModel<Boolean> booleanItemComboBoxModel() {
    return booleanItemComboBoxModel("-");
  }

  /**
   * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
   * @param nullCaption the string representing a null value
   * @return a Boolean based ItemComboBoxModel
   */
  public static ItemComboBoxModel<Boolean> booleanItemComboBoxModel(String nullCaption) {
    return booleanItemComboBoxModel(nullCaption, Messages.yes(), Messages.no());
  }

  /**
   * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
   * @param nullCaption the string representing a null value
   * @param trueCaption the string representing the boolean value 'true'
   * @param falseCaption the string representing the boolean value 'false'
   * @return a Boolean based ItemComboBoxModel
   */
  public static ItemComboBoxModel<Boolean> booleanItemComboBoxModel(String nullCaption, String trueCaption, String falseCaption) {
    return new ItemComboBoxModel<>(null, asList(item(null, nullCaption), item(true, trueCaption), item(false, falseCaption)));
  }

  private final class SelectedItemTranslator implements Function<Object, Item<T>> {

    @Override
    public Item<T> apply(Object item) {
      if (item instanceof Item) {
        return findItem((Item<T>) item);
      }

      return findItem((T) item);
    }

    private Item<T> findItem(Item<T> item) {
      int index = visibleItems().indexOf(item);
      if (index >= 0) {
        return getElementAt(index);
      }

      return null;
    }

    private Item<T> findItem(T value) {
      int index = indexOf(value);
      if (index >= 0) {
        return getElementAt(index);
      }

      return null;
    }
  }
}
