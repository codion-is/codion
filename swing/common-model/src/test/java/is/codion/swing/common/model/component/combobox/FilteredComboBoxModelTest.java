/*
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.combobox.FilteredComboBoxModel.ItemFinder;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class FilteredComboBoxModelTest {

  private FilteredComboBoxModel<String> testModel;

  private static final String NULL = "nullitem";
  private static final String ANNA = "anna";
  private static final String KALLI = "kalli";
  private static final String SIGGI = "siggi";
  private static final String TOMAS = "tomas";
  private static final String BJORN = "björn";

  private final ListDataListener listDataListener = new ListDataListener() {
    @Override
    public void intervalAdded(ListDataEvent e) {}
    @Override
    public void intervalRemoved(ListDataEvent e) {}
    @Override
    public void contentsChanged(ListDataEvent e) {}
  };

  @Test
  void testRefreshClear() {
    testModel.refresh();
    assertEquals(5, testModel.visibleItems().size());
    testModel.clear();
    assertEquals(0, testModel.getSize());
    assertTrue(testModel.cleared());
  }

  @Test
  void testDataListeners() {
    testModel.addListDataListener(listDataListener);
    testModel.removeListDataListener(listDataListener);
  }

  @Test
  void testSorting() {
    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(BJORN, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(SIGGI, testModel.getElementAt(4));
    assertEquals(TOMAS, testModel.getElementAt(5));

    Comparator<String> comparator = testModel.sortComparator().get();
    testModel.sortComparator().set(null);
    assertNull(testModel.sortComparator().get());
    List<String> names = new ArrayList<>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);
    testModel.setItems(names);

    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(KALLI, testModel.getElementAt(2));
    assertEquals(SIGGI, testModel.getElementAt(3));
    assertEquals(TOMAS, testModel.getElementAt(4));
    assertEquals(BJORN, testModel.getElementAt(5));

    testModel.sortComparator().set(comparator);
    names.remove(SIGGI);
    testModel.setItems(names);
    testModel.add(SIGGI);

    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(BJORN, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(SIGGI, testModel.getElementAt(4));
    assertEquals(TOMAS, testModel.getElementAt(5));

    testModel.sortComparator().set((o1, o2) -> {
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      return o2.compareTo(o1);
    });
    assertNotNull(testModel.sortComparator().get());

    assertEquals(TOMAS, testModel.getElementAt(1));
    assertEquals(SIGGI, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(BJORN, testModel.getElementAt(4));
    assertEquals(ANNA, testModel.getElementAt(5));
  }

  @Test
  void testSelection() {
    AtomicInteger selectionChangedCounter = new AtomicInteger();
    Consumer<String> selectionListener = selectedItem -> selectionChangedCounter.incrementAndGet();
    testModel.addSelectionListener(selectionListener);
    testModel.setSelectedItem(BJORN);
    assertEquals(1, selectionChangedCounter.get());
    testModel.setSelectedItem(null);
    assertEquals(2, selectionChangedCounter.get());
    testModel.setSelectedItem(NULL);
    assertEquals(2, selectionChangedCounter.get());
    testModel.setSelectedItem(BJORN);
    assertEquals(3, selectionChangedCounter.get());
    assertEquals(BJORN, testModel.getSelectedItem());
    assertEquals(BJORN, testModel.selectedValue());
    assertFalse(testModel.selectionEmpty().get());
    assertFalse(testModel.nullSelected());
    testModel.setSelectedItem(null);
    assertTrue(testModel.selectionEmpty().get());
    assertEquals(4, selectionChangedCounter.get());
    assertEquals(NULL, testModel.getSelectedItem());
    assertTrue(testModel.nullSelected());
    assertTrue(testModel.selectionEmpty().get());
    assertNull(testModel.selectedValue());
    testModel.setSelectedItem(SIGGI);
    testModel.clear();
    assertEquals(6, selectionChangedCounter.get());
    testModel.removeSelectionListener(selectionListener);
  }

  @Test
  void filterWithSelection() {
    testModel.setSelectedItem(BJORN);
    testModel.includeCondition().set(item -> !item.equals(BJORN));
    assertEquals(NULL, testModel.getSelectedItem());
    assertNull(testModel.selectedValue());

    testModel.includeCondition().set(null);
    testModel.filterSelectedItem().set(false);
    assertFalse(testModel.filterSelectedItem().get());
    testModel.setSelectedItem(BJORN);
    testModel.includeCondition().set(item -> !item.equals(BJORN));
    assertNotNull(testModel.getSelectedItem());
    assertEquals(BJORN, testModel.selectedValue());
  }

  @Test
  void includeCondition() {
    testModel.addListDataListener(listDataListener);

    testModel.includeCondition().set(item -> false);
    assertEquals(1, testModel.getSize());
    testModel.includeCondition().set(item -> true);
    assertEquals(6, testModel.getSize());
    testModel.includeCondition().set(item -> !item.equals(ANNA));
    assertEquals(5, testModel.getSize());
    assertFalse(testModel.visible(ANNA));
    assertTrue(testModel.filtered(ANNA));
    testModel.includeCondition().set(item -> item.equals(ANNA));
    assertEquals(2, testModel.getSize());
    assertTrue(testModel.visible(ANNA));

    assertEquals(4, testModel.filteredItems().size());
    assertEquals(1, testModel.visibleItems().size());
    assertEquals(4, testModel.filteredCount());
    assertEquals(2, testModel.visibleCount());
    assertEquals(5, testModel.items().size());

    testModel.add(BJORN);//already contained
    assertEquals(4, testModel.filteredCount());

    assertFalse(testModel.visible(BJORN));
    assertTrue(testModel.contains(BJORN));

    testModel.removeListDataListener(listDataListener);
  }

  @Test
  void remove() {
    //remove filtered item
    testModel.includeCondition().set(item -> !item.equals(BJORN));
    testModel.remove(BJORN);
    testModel.includeCondition().set(null);
    assertFalse(testModel.visible(BJORN));

    //remove visible item
    testModel.remove(KALLI);
    assertFalse(testModel.visible(KALLI));
  }

  @Test
  void add() {
    testModel.clear();
    //add filtered item
    testModel.includeCondition().set(item -> !item.equals(BJORN));
    testModel.add(BJORN);
    assertFalse(testModel.visible(BJORN));

    //add visible item
    testModel.add(KALLI);
    assertTrue(testModel.visible(KALLI));

    testModel.includeCondition().set(null);
    assertTrue(testModel.visible(BJORN));
  }

  @Test
  void setNullValueString() {
    assertTrue(testModel.visible(null));
    testModel.refresh();
    assertEquals(5, testModel.visibleItems().size());
    assertEquals(testModel.getElementAt(0), NULL);
    testModel.setSelectedItem(null);
    assertEquals(testModel.getSelectedItem(), NULL);
    assertTrue(testModel.nullSelected());
    assertNull(testModel.selectedValue());
    testModel.setSelectedItem(NULL);
    assertEquals(NULL, testModel.getElementAt(0));
    assertEquals(ANNA, testModel.getElementAt(1));
  }

  @Test
  void nullItem() {
    FilteredComboBoxModel<String> model = new FilteredComboBoxModel<>();
    assertFalse(model.contains(null));
    model.includeNull().set(true);
    assertTrue(model.contains(null));
    model.includeNull().set(false);
    assertFalse(model.contains(null));
    model.includeNull().set(true);
    model.nullItem().set("-");
    assertTrue(model.contains(null));
    assertEquals("-", model.getSelectedItem());
    model.setSelectedItem("-");
    assertTrue(model.nullSelected());
  }

  @Test
  void selectorValue() {
    Value<Character> selectorValue = testModel.createSelectorValue(new ItemFinder<String, Character>() {
      @Override
      public Character value(String item) {
        return item.charAt(0);
      }

      @Override
      public Predicate<String> createPredicate(Character value) {
        return item -> item.charAt(0) == value.charValue();
      }
    });
    assertNull(selectorValue.get());
    testModel.setSelectedItem(ANNA);
    assertEquals('a', selectorValue.get());
    selectorValue.set('k');
    assertEquals(KALLI, testModel.getSelectedItem());
    selectorValue.set(null);
    assertTrue(testModel.nullSelected());
    testModel.setSelectedItem(BJORN);
    assertEquals('b', selectorValue.get());
    testModel.setSelectedItem(null);
    assertNull(selectorValue.get());
  }

  @Test
  void setContentsSelectedItem() {
    class Data {
      final int id;
      final String data;

      Data(int id, String data) {
        this.id = id;
        this.data = data;
      }

      @Override
      public boolean equals(Object o) {
        if (this == o) {
          return true;
        }
        if (o == null || getClass() != o.getClass()) {
          return false;
        }

        return id == ((Data) o).id;
      }
    }
    List<Data> items = asList(new Data(1, "1"), new Data(2, "2"), new Data(3, "3"));

    FilteredComboBoxModel<Data> model = new FilteredComboBoxModel<>();
    model.setItems(items);
    model.setSelectedItem(items.get(1));
    assertEquals("2", model.selectedValue().data);

    items = asList(new Data(1, "1"), new Data(2, "22"), new Data(3, "3"));

    model.setItems(items);
    assertEquals("22", model.selectedValue().data);
  }

  @Test
  void includeNull() {
    FilteredComboBoxModel<Integer> model = new FilteredComboBoxModel<>();
    model.setItems(asList(1, 2, 3, 4, 5));
    model.includeNull().set(true);
    model.includeNull().set(true);
    assertTrue(model.includeNull().get());
    model.refresh();
  }

  @Test
  void itemValidator() {
    FilteredComboBoxModel<Integer> model = new FilteredComboBoxModel<>();
    model.itemValidator().set(item -> item > 0);
    assertThrows(IllegalArgumentException.class, () -> model.setItems(asList(1, 2, 3, 4, 5, 0)));
  }

  @Test
  void itemSupplier() {
    List<Integer> values = asList(0, 1, 2);
    FilteredComboBoxModel<Integer> model = new FilteredComboBoxModel<>();
    model.refresher().itemSupplier().set(() -> values);
    model.refresher().refresh();
    assertEquals(values, model.items());
  }

  @Test
  void allowSelectionPredicate() {
    FilteredComboBoxModel<Integer> model = new FilteredComboBoxModel<>();
    model.setItems(asList(0, 1, 2));
    model.setSelectedItem(0);
    assertThrows(IllegalArgumentException.class, () -> model.allowSelectionPredicate().set(item -> item > 0));
    model.setSelectedItem(1);
    model.allowSelectionPredicate().set(item -> item > 0);
    model.setSelectedItem(0);
    assertEquals(1, model.getSelectedItem());
  }

  @BeforeEach
  void setUp() {
    testModel = new FilteredComboBoxModel<>();
    testModel.includeNull().set(true);
    testModel.nullItem().set(NULL);
    List<String> names = new ArrayList<>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);
    testModel.setItems(names);
  }

  @AfterEach
  void tearDown() {
    testModel = null;
  }
}
