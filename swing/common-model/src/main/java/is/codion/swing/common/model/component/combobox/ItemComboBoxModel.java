/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static is.codion.common.item.Item.item;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;

/**
 * A ComboBoxModel implementation based on the {@link Item} class.
 * @param <T> the type of value wrapped by this combo box models items
 * @see #createModel()
 * @see #createSortedModel()
 * @see #createBooleanModel()
 */
public final class ItemComboBoxModel<T> extends SwingFilteredComboBoxModel<Item<T>> {

  private ItemComboBoxModel(List<Item<T>> items) {
    super(null);
    setContents(items);
  }

  private ItemComboBoxModel(Comparator<Item<T>> sortComparator, Collection<Item<T>> items) {
    super(null, sortComparator);
    setContents(items);
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
      if (Objects.equals(getElementAt(i).getValue(), value)) {
        return i;
      }
    }

    return -1;
  }

  /**
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> createModel() {
    return new ItemComboBoxModel<>(null, null);
  }

  /**
   * @param items the items
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> createModel(List<Item<T>> items) {
    return new ItemComboBoxModel<>(null, requireNonNull(items));
  }

  /**
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> createSortedModel() {
    return createSortedModel((List<Item<T>>) null);
  }

  /**
   * @param items the items
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> createSortedModel(List<Item<T>> items) {
    return new ItemComboBoxModel<>(items);
  }

  /**
   * @param sortComparator the sort comparator to use
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> createSortedModel(Comparator<Item<T>> sortComparator) {
    return new ItemComboBoxModel<>(requireNonNull(sortComparator), null);
  }

  /**
   * @param items the items
   * @param sortComparator the sort comparator to use
   * @param <T> the Item value type
   * @return a new combo box model
   */
  public static <T> ItemComboBoxModel<T> createSortedModel(List<Item<T>> items, Comparator<Item<T>> sortComparator) {
    requireNonNull(items);
    requireNonNull(sortComparator);

    return new ItemComboBoxModel<>(sortComparator, items);
  }

  /**
   * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
   * @return a Boolean based ItemComboBoxModel
   */
  public static ItemComboBoxModel<Boolean> createBooleanModel() {
    return createBooleanModel("-");
  }

  /**
   * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
   * @param nullCaption the string representing a null value
   * @return a Boolean based ItemComboBoxModel
   */
  public static ItemComboBoxModel<Boolean> createBooleanModel(String nullCaption) {
    return createBooleanModel(nullCaption, Messages.get(Messages.YES), Messages.get(Messages.NO));
  }

  /**
   * Constructs a new Boolean based ItemComboBoxModel with null as the initially selected value.
   * @param nullCaption the string representing a null value
   * @param trueCaption the string representing the boolean value 'true'
   * @param falseCaption the string representing the boolean value 'false'
   * @return a Boolean based ItemComboBoxModel
   */
  public static ItemComboBoxModel<Boolean> createBooleanModel(String nullCaption, String trueCaption, String falseCaption) {
    return new ItemComboBoxModel<>(null, asList(item(null, nullCaption), item(true, trueCaption), item(false, falseCaption)));
  }

  @Override
  protected Item<T> translateSelectionItem(Object item) {
    if (item instanceof Item) {
      return (Item<T>) item;
    }

    int index = indexOf((T) item);
    if (index >= 0) {
      return getElementAt(index);
    }

    return null;
  }
}
