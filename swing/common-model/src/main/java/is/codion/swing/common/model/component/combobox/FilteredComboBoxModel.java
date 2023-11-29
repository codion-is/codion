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

import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.model.FilteredModel;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.AbstractValue;
import is.codion.common.value.Value;
import is.codion.swing.common.model.component.AbstractFilteredModelRefresher;

import javax.swing.ComboBoxModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * A default combo box model implementation based on {@link FilteredModel}.
 * @param <T> the type of values in this combo box model
 * @see #includeCondition()
 */
public class FilteredComboBoxModel<T> implements FilteredModel<T>, ComboBoxModel<T> {

  /**
   * Specifies the caption used by default to represent null in combo box models.
   * Value type: String<br>
   * Default value: -
   */
  public static final PropertyValue<String> COMBO_BOX_NULL_CAPTION = Configuration.stringValue("is.codion.common.model.combobox.nullCaption", "-");

  private static final Predicate<?> DEFAULT_ITEM_VALIDATOR = new DefaultItemValidator<>();
  private static final Function<Object, ?> DEFAULT_SELECTED_ITEM_TRANSLATOR = new DefaultSelectedItemTranslator<>();
  private static final Predicate<?> DEFAULT_ALLOW_SELECTION_PREDICATE = new DefaultAllowSelectionPredicate<>();
  private static final Comparator<?> DEFAULT_COMPARATOR = new DefaultComparator<>();

  private final Event<T> selectionChangedEvent = Event.event();
  private final State selectionEmpty = State.state(true);
  private final State includeNull = State.state();
  private final Value<T> nullItem = Value.value();
  private final State filterSelectedItem = State.state(true);
  private final List<T> visibleItems = new ArrayList<>();
  private final List<T> filteredItems = new ArrayList<>();
  private final Refresher<T> refresher;
  private final Value<Predicate<T>> includeCondition = Value.value();
  private final Value<Predicate<T>> itemValidator =
          Value.value((Predicate<T>) DEFAULT_ITEM_VALIDATOR, (Predicate<T>) DEFAULT_ITEM_VALIDATOR);
  private final Value<Function<Object, T>> selectedItemTranslator =
          Value.value((Function<Object, T>) DEFAULT_SELECTED_ITEM_TRANSLATOR, (Function<Object, T>) DEFAULT_SELECTED_ITEM_TRANSLATOR);
  private final Value<Predicate<T>> allowSelectionPredicate =
          Value.value((Predicate<T>) DEFAULT_ALLOW_SELECTION_PREDICATE, (Predicate<T>) DEFAULT_ALLOW_SELECTION_PREDICATE);
  private final Value<Comparator<T>> comparator = Value.value((Comparator<T>) DEFAULT_COMPARATOR);

  /**
   * set during setItems()
   */
  private boolean cleared = true;
  private T selectedItem = null;

  /**
   * Due to a java.util.ConcurrentModificationException in OSX
   */
  private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

  /**
   * Instantiates a new FilteredComboBoxModel.
   * The model items are sorted automatically with a default collation based comparator.
   * To prevent sorting set the comparator to null via {@link #comparator()} before adding items.
   */
  public FilteredComboBoxModel() {
    this.refresher = new DefaultRefresher(new DefaultItemSupplier());
    includeCondition.addListener(this::filterItems);
    itemValidator.addValidator(validator -> items().stream()
            .filter(Objects::nonNull)
            .forEach(validator::test));
    comparator.addListener(this::sortVisibleItems);
    allowSelectionPredicate.addValidator(predicate -> {
      if (predicate != null && !predicate.test(selectedItem)){
        throw new IllegalArgumentException("The current selected item does not satisfy the allow selection predicate");
      }
    });
    includeNull.addDataListener(value -> {
      if (value && !visibleItems.contains(null)) {
        visibleItems.add(0, null);
      }
      else {
        visibleItems.remove(null);
      }
    });
    nullItem.addValidator(FilteredComboBoxModel.this::validate);
  }

  @Override
  public final Refresher<T> refresher() {
    return refresher;
  }

  @Override
  public final void refresh() {
    refreshThen(null);
  }

  @Override
  public final void refreshThen(Consumer<Collection<T>> afterRefresh) {
    refresher.refreshThen(afterRefresh);
  }

  /**
   * Clears all items from this combo box model, including the null item and sets the selected item to null
   */
  public final void clear() {
    setSelectedItem(null);
    setItems(null);
  }

  /**
   * @return true if the model data has been cleared and needs to be refreshed
   */
  public final boolean cleared() {
    return cleared;
  }

  /**
   * Resets the items of this model using the values found in {@code items},
   * if {@code items} is null then the model is cleared.
   * @param items the items to display in this combo box model
   * @throws IllegalArgumentException in case an item fails validation
   * @see #cleared()
   * @see #itemValidator()
   */
  public final void setItems(Collection<T> items) {
    filteredItems.clear();
    visibleItems.clear();
    if (items != null) {
      items.forEach(this::validate);
      visibleItems.addAll(items);
      if (includeNull.get()) {
        visibleItems.add(0, null);
      }
    }
    filterItems();
    cleared = items == null;
  }

  @Override
  public final void filterItems() {
    visibleItems.addAll(filteredItems);
    filteredItems.clear();
    if (includeCondition.isNotNull()) {
      for (Iterator<T> iterator = visibleItems.listIterator(); iterator.hasNext(); ) {
        T item = iterator.next();
        if (item != null && !includeCondition.get().test(item)) {
          filteredItems.add(item);
          iterator.remove();
        }
      }
    }
    sortVisibleItems();
    if (selectedItem != null && visibleItems.contains(selectedItem)) {
      //update the selected item since the underlying data could have changed
      selectedItem = visibleItems.get(visibleItems.indexOf(selectedItem));
    }
    if (selectedItem != null && !visibleItems.contains(selectedItem) && filterSelectedItem.get()) {
      setSelectedItem(null);
    }
    else {
      fireContentsChanged();
    }
  }

  @Override
  public final List<T> visibleItems() {
    if (visibleItems.isEmpty()) {
      return emptyList();
    }
    if (!includeNull.get()) {
      return unmodifiableList(visibleItems);
    }

    return unmodifiableList(visibleItems.subList(1, getSize()));
  }

  @Override
  public final Collection<T> filteredItems() {
    return unmodifiableList(filteredItems);
  }

  @Override
  public final Collection<T> items() {
    List<T> entities = new ArrayList<>(visibleItems());
    entities.addAll(filteredItems);

    return unmodifiableList(entities);
  }

  @Override
  public final Value<Predicate<T>> includeCondition() {
    return includeCondition;
  }

  @Override
  public final int filteredCount() {
    return filteredItems.size();
  }

  @Override
  public final int visibleCount() {
    return visibleItems.size();
  }

  @Override
  public final boolean visible(T item) {
    if (item == null) {
      return includeNull.get();
    }

    return visibleItems.contains(item);
  }

  @Override
  public final boolean filtered(T item) {
    return filteredItems.contains(item);
  }

  /**
   * Adds the given item to this model, respecting the sorting order if specified.
   * If this model already contains the item, calling this method has no effect.
   * Note that if the item does not satisfy the include condition, it will be filtered right away.
   * @param item the item to add
   * @throws IllegalArgumentException in case the item fails validation
   * @see #includeCondition()
   */
  public final void add(T item) {
    validate(item);
    if (includeCondition.isNull() || includeCondition.get().test(item)) {
      if (!visibleItems.contains(item)) {
        visibleItems.add(item);
        sortVisibleItems();
      }
    }
    else if (!filteredItems.contains(item)) {
      filteredItems.add(item);
    }
  }

  /**
   * Removes the given item from this model
   * @param item the item to remove
   */
  public final void remove(T item) {
    requireNonNull(item);
    filteredItems.remove(item);
    if (visibleItems.remove(item)) {
      fireContentsChanged();
    }
  }

  /**
   * Replaces the given item in this combo box model
   * @param item the item to replace
   * @param replacement the replacement item
   * @throws IllegalArgumentException in case the replacement item fails validation
   */
  public final void replace(T item, T replacement) {
    validate(replacement);
    remove(item);
    add(replacement);
    if (Objects.equals(selectedItem, item)) {
      selectedItem = selectedItemTranslator.get().apply(null);
      setSelectedItem(replacement);
    }
  }

  @Override
  public final boolean containsItem(T item) {
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  /**
   * Controls the Comparator used when sorting the visible items in this model and sorts the model accordingly.
   * This Comparator must take into account the null value if a null item has been set via {@link #nullItem()}.
   * If a null {@code comparator} is provided no sorting will be performed.
   * @return the Value controlling the comparator used when sorting, value may be null if the items of this model should not be sorted
   */
  public final Value<Comparator<T>> comparator() {
    return comparator;
  }

  /**
   * Provides a way for the model to prevent the addition of certain items.
   * Trying to add items that fail validation will result in an exception.
   * Note that any translation of the selected item is done before validation.
   * @return the Value controlling the item validator
   */
  public final Value<Predicate<T>> itemValidator() {
    return itemValidator;
  }

  /**
   * Provides a way for the combo box model to translate an item when it is selected, such
   * as selecting the String "1" in a String based model when selected item is set to the number 1.
   * @return the Value controlling the selected item translator
   */
  public final Value<Function<Object, T>> selectedItemTranslator() {
    return selectedItemTranslator;
  }

  /**
   * Provides a way for the combo box model to prevent the selection of certain items.
   * @return the Value controlling the allow selection predicate
   * @throws IllegalArgumentException in case the current selected item does not satisfy the allow selection predicate
   */
  public final Value<Predicate<T>> allowSelectionPredicate() {
    return allowSelectionPredicate;
  }

  /**
   * @return the State controlling whether a null value is included as the first item
   * @see #nullItem()
   */
  public final State includeNull() {
    return includeNull;
  }

  /**
   * Controls the item that should represent the null value in this model.
   * Note that {@link #includeNull()} must be used as well to enable the null value.
   * @return the Value controlling the item representing null
   * @see #includeNull()
   */
  public final Value<T> nullItem() {
    return nullItem;
  }

  /**
   * Returns true if this model contains null and it is selected.
   * @return true if this model contains null and it is selected, false otherwise
   * @see #includeNull()
   */
  public final boolean nullSelected() {
    return includeNull.get() && selectedItem == null;
  }

  /**
   * @return a StateObserver indicating whether the selection is empty or the value representing null is selected
   */
  public final StateObserver selectionEmpty() {
    return selectionEmpty.observer();
  }

  /**
   * @return the selected value, null in case the value representing null is selected
   * @see #nullSelected()
   */
  public final T selectedValue() {
    if (nullSelected()) {
      return null;
    }

    return selectedItem;
  }

  /**
   * @return the selected item, N.B. this can include the {@code nullItem} in case it has been set
   * via {@link #nullItem()}, {@link #selectedValue()} is usually what you want
   */
  @Override
  public final T getSelectedItem() {
    if (selectedItem == null && nullItem.isNotNull()) {
      return nullItem.get();
    }

    return selectedItem;
  }

  /**
   * @param item the item to select
   */
  public final void setSelectedItem(Object item) {
    T toSelect = selectedItemTranslator.get().apply(Objects.equals(nullItem.get(), item) ? null : item);
    if (!Objects.equals(selectedItem, toSelect) && allowSelectionPredicate.get().test(toSelect)) {
      selectedItem = toSelect;
      fireContentsChanged();
      selectionEmpty.set(selectedValue() == null);
      selectionChangedEvent.accept(selectedItem);
    }
  }

  /**
   * Specifies whether filtering can change the selected item, if true then
   * the selected item is set to null when the currently selected item is filtered
   * from the model, otherwise the selected item can potentially represent a value
   * which is not currently visible in the model.
   * This is true by default.
   * @return the State controlling whether the selected item is changed when it is filtered
   */
  public final State filterSelectedItem() {
    return filterSelectedItem;
  }

  @Override
  public final void addListDataListener(ListDataListener listener) {
    requireNonNull(listener, "listener");
    listDataListeners.add(listener);
  }

  @Override
  public final void removeListDataListener(ListDataListener listener) {
    requireNonNull(listener, "listener");
    listDataListeners.remove(listener);
  }

  /**
   * @param index the index
   * @return the item at the given index
   */
  public final T getElementAt(int index) {
    T element = visibleItems.get(index);
    if (element == null) {
      return nullItem.get();
    }

    return element;
  }

  /**
   * @return the number of visible items in this model
   */
  public final int getSize() {
    return visibleItems.size();
  }

  /**
   * @param itemFinder responsible for finding the item to select
   * @param <V> the value type
   * @return a value linked to the selected item via the given finder instance
   */
  public final <V> Value<V> createSelectorValue(ItemFinder<T, V> itemFinder) {
    return new SelectorValue<>(itemFinder);
  }

  /**
   * @param listener a listener notified each time the selection changes
   */
  public final void addSelectionListener(Consumer<T> listener) {
    selectionChangedEvent.addDataListener(listener);
  }

  /**
   * @param listener a selection listener to remove
   */
  public final void removeSelectionListener(Consumer<T> listener) {
    selectionChangedEvent.removeDataListener(listener);
  }

  private void fireContentsChanged() {
    ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
    for (ListDataListener dataListener : listDataListeners) {
      dataListener.contentsChanged(event);
    }
  }

  private void validate(T item) {
    if (!itemValidator.get().test(item)) {
      throw new IllegalArgumentException("Invalid item: " + item);
    }
  }

  /**
   * Sorts the items visible in this model
   */
  private void sortVisibleItems() {
    if (comparator.isNotNull() && !visibleItems.isEmpty()) {
      visibleItems.sort(comparator.get());
      fireContentsChanged();
    }
  }

  /**
   * Responsible for finding an item of type {@link T} by a single value of type {@link V}.
   * @param <T> the combo box model item type
   * @param <V> the type of the value to search by
   */
  public interface ItemFinder<T, V> {

    /**
     * Returns the value from the given item to use when searching
     * @param item the item, never null
     * @return the value associated with the given item
     */
    V value(T item);

    /**
     * Returns the {@link Predicate} to use when searching for the given value
     * @param value the value to search for, never null
     * @return a {@link Predicate} based on the given value
     */
    Predicate<T> createPredicate(V value);

    /**
     * Returns the first item in the given collection containing the given {@code value}. Only called for non-null {@code value}s.
     * @param items the items to search
     * @param value the value to search for, never null
     * @return the first item in the given list containing the given value, null if none is found.
     */
    default T findItem(Collection<T> items, V value) {
      requireNonNull(value);

      return requireNonNull(items).stream()
              .filter(createPredicate(value))
              .findFirst()
              .orElse(null);
    }
  }

  private final class SelectorValue<V> extends AbstractValue<V> {

    private final ItemFinder<T, V> itemFinder;

    private SelectorValue(ItemFinder<T, V> itemFinder) {
      this.itemFinder = requireNonNull(itemFinder);
      addSelectionListener(selected -> notifyListeners());
    }

    @Override
    public V get() {
      if (selectionEmpty.get()) {
        return null;
      }

      return itemFinder.value(selectedValue());
    }

    @Override
    protected void setValue(V value) {
      setSelectedItem(value == null ? null : itemFinder.findItem(visibleItems(), value));
    }
  }

  private final class DefaultRefresher extends AbstractFilteredModelRefresher<T> {

    private DefaultRefresher(Supplier<Collection<T>> itemSupplier) {
      super(itemSupplier);
    }

    @Override
    protected void processResult(Collection<T> items) {
      setItems(items);
    }
  }

  private static final class DefaultSelectedItemTranslator<T> implements Function<Object, T> {

    @Override
    public T apply(Object item) {
      return (T) item;
    }
  }

  private static final class DefaultItemValidator<T> implements Predicate<T> {

    @Override
    public boolean test(T item) {
      return true;
    }
  }

  private final class DefaultItemSupplier implements Supplier<Collection<T>> {

    @Override
    public Collection<T> get() {
      return items();
    }
  }

  private static final class DefaultAllowSelectionPredicate<T> implements Predicate<T> {

    @Override
    public boolean test(T item) {
      return true;
    }
  }

  private static final class DefaultComparator<T> implements Comparator<T> {

    private final Comparator<T> comparator = Text.spaceAwareCollator();

    @Override
    public int compare(T o1, T o2) {
      if (o1 == null && o2 == null) {
        return 0;
      }
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      if (o1 instanceof Comparable && o2 instanceof Comparable) {
        return ((Comparable<T>) o1).compareTo(o2);
      }
      else {
        return comparator.compare(o1, o2);
      }
    }
  }
}