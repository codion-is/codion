/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.Configuration;
import is.codion.common.Text;
import is.codion.common.event.Event;
import is.codion.common.event.EventDataListener;
import is.codion.common.model.FilteredModel;
import is.codion.common.properties.PropertyValue;
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
 * @see #setIncludeCondition(Predicate)
 */
public class FilteredComboBoxModel<T> implements FilteredModel<T>, ComboBoxModel<T> {

  /**
   * Specifies the caption used by default to represent null in combo box models.
   * Value type: String<br>
   * Default value: -
   */
  public static final PropertyValue<String> COMBO_BOX_NULL_CAPTION = Configuration.stringValue("is.codion.common.model.combobox.nullCaption", "-");

  private final Event<T> selectionChangedEvent = Event.event();
  private final List<T> visibleItems = new ArrayList<>();
  private final List<T> filteredItems = new ArrayList<>();
  private final Refresher<T> refresher;

  private Predicate<T> itemValidator = new DefaultItemValidator<>();
  private Function<Object, T> selectedItemTranslator = new DefaultSelectedItemTranslator<>();
  private Predicate<T> allowSelectionPredicate = new DefaultAllowSelectionPredicate<>();

  /**
   * set during setItems()
   */
  private boolean cleared = true;

  private Comparator<T> sortComparator;
  private T selectedItem = null;
  private boolean includeNull;
  private T nullItem;
  private Predicate<T> includeCondition;
  private boolean filterSelectedItem = true;

  /**
   * Due to a java.util.ConcurrentModificationException in OSX
   */
  private final CopyOnWriteArrayList<ListDataListener> listDataListeners = new CopyOnWriteArrayList<>();

  /**
   * Instantiates a new FilteredComboBoxModel.
   * The model items are sorted automatically with a default collation based comparator.
   * To prevent sorting call {@link #setSortComparator(Comparator)} with a null argument before adding items.
   */
  public FilteredComboBoxModel() {
    this.sortComparator = new SortComparator<>();
    this.refresher = new DefaultRefresher(new DefaultItemSupplier());
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
  public final boolean isCleared() {
    return cleared;
  }

  /**
   * Resets the items of this model using the values found in {@code items},
   * if {@code items} is null then the model is cleared.
   * @param items the items to display in this combo box model
   * @throws IllegalArgumentException in case an item fails validation
   * @see #isCleared()
   * @see #setItemValidator(Predicate)
   */
  public final void setItems(Collection<T> items) {
    filteredItems.clear();
    visibleItems.clear();
    if (items != null) {
      items.forEach(this::validate);
      visibleItems.addAll(items);
      if (includeNull) {
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
    if (includeCondition != null) {
      for (Iterator<T> iterator = visibleItems.listIterator(); iterator.hasNext(); ) {
        T item = iterator.next();
        if (item != null && !includeCondition.test(item)) {
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
    if (selectedItem != null && !visibleItems.contains(selectedItem) && filterSelectedItem) {
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
    if (!includeNull) {
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
  public final void setIncludeCondition(Predicate<T> includeCondition) {
    this.includeCondition = includeCondition;
    filterItems();
  }

  @Override
  public final Predicate<T> getIncludeCondition() {
    return includeCondition;
  }

  @Override
  public final int filteredItemCount() {
    return filteredItems.size();
  }

  @Override
  public final int visibleItemCount() {
    return visibleItems.size();
  }

  @Override
  public final boolean isVisible(T item) {
    if (item == null) {
      return includeNull;
    }

    return visibleItems.contains(item);
  }

  @Override
  public final boolean isFiltered(T item) {
    return filteredItems.contains(item);
  }

  /**
   * Adds the given item to this model, respecting the sorting order if specified.
   * If this model already contains the item, calling this method has no effect.
   * Note that if the item does not satisfy the include condition, it will be filtered right away.
   * @param item the item to add
   * @throws IllegalArgumentException in case the item fails validation
   * @see #setIncludeCondition(Predicate)
   */
  public final void addItem(T item) {
    validate(item);
    if (includeCondition == null || includeCondition.test(item)) {
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
  public final void removeItem(T item) {
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
  public final void replaceItem(T item, T replacement) {
    validate(replacement);
    removeItem(item);
    addItem(replacement);
    if (Objects.equals(selectedItem, item)) {
      selectedItem = selectedItemTranslator.apply(null);
      setSelectedItem(replacement);
    }
  }

  @Override
  public final boolean containsItem(T item) {
    return visibleItems.contains(item) || filteredItems.contains(item);
  }

  /**
   * @return the Comparator used when sorting the items of this model
   */
  public final Comparator<T> getSortComparator() {
    return sortComparator;
  }

  /**
   * Sets the Comparator used when sorting the items visible in this model and sorts the model accordingly.
   * This Comparator must take into account the null value if a {@code nullValueString} is specified.
   * If a null {@code sortComparator} is provided no sorting will be performed.
   * @param sortComparator the Comparator, null if the items of this model should not be sorted
   */
  public final void setSortComparator(Comparator<T> sortComparator) {
    this.sortComparator = sortComparator;
    sortVisibleItems();
  }

  /**
   * @return the item supplier
   */
  public final Supplier<Collection<T>> getItemSupplier() {
    return refresher.getItemSupplier();
  }

  /**
   * Supplies the items when {@link #refresh()} is called.
   * @param itemSupplier the item supplier
   */
  public final void setItemSupplier(Supplier<Collection<T>> itemSupplier) {
    this.refresher.setItemSupplier(itemSupplier);
  }

  /**
   * @return the item validator
   */
  public final Predicate<T> getItemValidator() {
    return itemValidator;
  }

  /**
   * Provides a way for the model to prevent the addition of certain items.
   * Trying to add items that fail validation will result in an exception.
   * Note that any translation of the selected item is done before validation.
   * @param itemValidator the item validator
   * @throws IllegalArgumentException in case an item fails validation
   */
  public final void setItemValidator(Predicate<T> itemValidator) {
    requireNonNull(itemValidator);
    items().stream().filter(Objects::nonNull).forEach(itemValidator::test);
    this.itemValidator = itemValidator;
  }

  /**
   * Provides a way for the combo box model to translate an item when it is selected, such
   * as selecting the String "1" in a String based model when selected item is set to the number 1.
   * @return the selected item translator
   */
  public final Function<Object, T> getSelectedItemTranslator() {
    return selectedItemTranslator;
  }

  /**
   * @param selectedItemTranslator the selected item translator
   */
  public final void setSelectedItemTranslator(Function<Object, T> selectedItemTranslator) {
    this.selectedItemTranslator = requireNonNull(selectedItemTranslator);
  }

  /**
   * @return the allow selection predicate
   */
  public final Predicate<T> getAllowSelectionPredicate() {
    return allowSelectionPredicate;
  }

  /**
   * Provides a way for the combo box model to prevent the selection of certain items.
   * @param allowSelectionPredicate the allow selection predicate
   * @throws IllegalArgumentException in case the current selected item does not satisfy the allow selection predicate
   */
  public final void setAllowSelectionPredicate(Predicate<T> allowSelectionPredicate) {
    if (!requireNonNull(allowSelectionPredicate).test(selectedItem)) {
      throw new IllegalArgumentException("The current selected item does not satisfy the allow selection predicate");
    }
    this.allowSelectionPredicate = allowSelectionPredicate;
  }

  /**
   * @param includeNull if true then a null value is included as the first item
   * @see #setNullItem(Object)
   */
  public final void setIncludeNull(boolean includeNull) {
    this.includeNull = includeNull;
    if (includeNull && !visibleItems.contains(null)) {
      visibleItems.add(0, null);
    }
    else {
      visibleItems.remove(null);
    }
  }

  /**
   * @return true if a null value is included
   */
  public final boolean isIncludeNull() {
    return includeNull;
  }

  /**
   * Sets the item that should represent the null value in this model.
   * Note that {@link #setIncludeNull(boolean)} must be called as well to enable the null value.
   * @param nullItem the item representing null
   * @throws IllegalArgumentException in case the item fails validation
   * @see #setIncludeNull(boolean)
   */
  public final void setNullItem(T nullItem) {
    validate(nullItem);
    this.nullItem = nullItem;
  }

  /**
   * Returns true if this model contains null and it is selected.
   * @return true if this model contains null and it is selected, false otherwise
   * @see #isIncludeNull()
   */
  public final boolean isNullSelected() {
    return includeNull && selectedItem == null;
  }

  /**
   * @return true if no value is selected or if the value representing null is selected
   */
  public final boolean isSelectionEmpty() {
    return selectedValue() == null;
  }

  /**
   * @return the selected value, null in case the value representing null is selected
   * @see #isNullSelected()
   */
  public final T selectedValue() {
    if (isNullSelected()) {
      return null;
    }

    return selectedItem;
  }

  /**
   * @return the selected item, N.B. this can include the {@code nullItem} in case it has been set
   * via {@link #setNullItem(Object)}, {@link #selectedValue()} is usually what you want
   */
  @Override
  public final T getSelectedItem() {
    if (selectedItem == null && nullItem != null) {
      return nullItem;
    }

    return selectedItem;
  }

  /**
   * @param item the item to select
   */
  public final void setSelectedItem(Object item) {
    T toSelect = selectedItemTranslator.apply(Objects.equals(nullItem, item) ? null : item);
    if (!Objects.equals(selectedItem, toSelect) && allowSelectionPredicate.test(toSelect)) {
      selectedItem = toSelect;
      fireContentsChanged();
      selectionChangedEvent.onEvent(selectedItem);
    }
  }

  /**
   * Specifies whether filtering can change the selected item, if true then
   * the selected item is set to null when the currently selected item is filtered
   * from the model, otherwise the selected item can potentially represent a value
   * which is not currently visible in the model.
   * This is true by default.
   * @param filterSelectedItem if true then the selected item is changed when it is filtered out,
   */
  public final void setFilterSelectedItem(boolean filterSelectedItem) {
    this.filterSelectedItem = filterSelectedItem;
  }

  /**
   * Specifies whether filtering can change the selected item, if true then
   * the selected item is set to null when the currently selected item is filtered
   * from the model, otherwise the selected item can potentially represent a value
   * which is not currently visible in the model.
   * This is true by default.
   * @return true if the selected item is changed when it is filtered out
   */
  public final boolean isFilterSelectedItem() {
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
      return nullItem;
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
  public final void addSelectionListener(EventDataListener<T> listener) {
    selectionChangedEvent.addDataListener(listener);
  }

  /**
   * @param listener a selection listener to remove
   */
  public final void removeSelectionListener(EventDataListener<T> listener) {
    selectionChangedEvent.removeDataListener(listener);
  }

  private void fireContentsChanged() {
    ListDataEvent event = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, Integer.MAX_VALUE);
    for (ListDataListener dataListener : listDataListeners) {
      dataListener.contentsChanged(event);
    }
  }

  private void validate(T item) {
    requireNonNull(item);
    if (!itemValidator.test(item)) {
      throw new IllegalArgumentException("Invalid item: " + item);
    }
  }

  /**
   * Sorts the items visible in this model
   */
  private void sortVisibleItems() {
    if (sortComparator != null && !visibleItems.isEmpty()) {
      visibleItems.sort(sortComparator);
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
      addSelectionListener(selected -> notifyValueChange());
    }

    @Override
    public V get() {
      if (isSelectionEmpty()) {
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
//      return items();
      List<T> items = new ArrayList<>(visibleItems);
      if (includeNull) {
        items.remove(null);
      }
      items.addAll(filteredItems);

      return items;
    }
  }

  private static final class DefaultAllowSelectionPredicate<T> implements Predicate<T> {

    @Override
    public boolean test(T item) {
      return true;
    }
  }

  private static final class SortComparator<T> implements Comparator<T> {

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