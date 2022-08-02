/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.model.component.combobox;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventListener;
import is.codion.common.model.combobox.FilteredComboBoxModel.Finder;
import is.codion.common.value.Value;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class SwingFilteredComboBoxModelTest {

  private SwingFilteredComboBoxModel<String> testModel;

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
    assertEquals(5, testModel.getVisibleItems().size());
    testModel.clear();
    assertEquals(0, testModel.getSize());
    assertTrue(testModel.isCleared());
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

    Comparator<String> comparator = testModel.getSortComparator();
    testModel.setSortComparator(null);
    assertNull(testModel.getSortComparator());
    List<String> names = new ArrayList<>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);
    testModel.setContents(names);

    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(KALLI, testModel.getElementAt(2));
    assertEquals(SIGGI, testModel.getElementAt(3));
    assertEquals(TOMAS, testModel.getElementAt(4));
    assertEquals(BJORN, testModel.getElementAt(5));

    testModel.setSortComparator(comparator);
    names.remove(SIGGI);
    testModel.setContents(names);
    testModel.addItem(SIGGI);

    assertEquals(ANNA, testModel.getElementAt(1));
    assertEquals(BJORN, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(SIGGI, testModel.getElementAt(4));
    assertEquals(TOMAS, testModel.getElementAt(5));

    testModel.setSortComparator((o1, o2) -> {
      if (o1 == null) {
        return -1;
      }
      if (o2 == null) {
        return 1;
      }
      return o2.compareTo(o1);
    });
    assertNotNull(testModel.getSortComparator());

    assertEquals(TOMAS, testModel.getElementAt(1));
    assertEquals(SIGGI, testModel.getElementAt(2));
    assertEquals(KALLI, testModel.getElementAt(3));
    assertEquals(BJORN, testModel.getElementAt(4));
    assertEquals(ANNA, testModel.getElementAt(5));
  }

  @Test
  void testSelection() {
    AtomicInteger selectionChangedCounter = new AtomicInteger();
    EventDataListener<String> selectionListener = selectedItem -> selectionChangedCounter.incrementAndGet();
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
    assertEquals(BJORN, testModel.getSelectedValue());
    assertFalse(testModel.isSelectionEmpty());
    assertFalse(testModel.isNullSelected());
    testModel.setSelectedItem(null);
    assertTrue(testModel.isSelectionEmpty());
    assertEquals(4, selectionChangedCounter.get());
    assertEquals(NULL, testModel.getSelectedItem());
    assertTrue(testModel.isNullSelected());
    assertTrue(testModel.isSelectionEmpty());
    assertNull(testModel.getSelectedValue());
    testModel.setSelectedItem(SIGGI);
    testModel.clear();
    assertEquals(6, selectionChangedCounter.get());
    testModel.removeSelectionListener(selectionListener);
  }

  @Test
  void filterWithSelection() {
    testModel.setSelectedItem(BJORN);
    testModel.setIncludeCondition(item -> !item.equals(BJORN));
    assertEquals(NULL, testModel.getSelectedItem());
    assertNull(testModel.getSelectedValue());

    testModel.setIncludeCondition(null);
    testModel.setFilterSelectedItem(false);
    assertFalse(testModel.isFilterSelectedItem());
    testModel.setSelectedItem(BJORN);
    testModel.setIncludeCondition(item -> !item.equals(BJORN));
    assertNotNull(testModel.getSelectedItem());
    assertEquals(BJORN, testModel.getSelectedValue());
  }

  @Test
  void setIncludeCondition() {
    AtomicInteger filteringEndedCounter = new AtomicInteger();
    EventListener filteringEndedListener = filteringEndedCounter::incrementAndGet;
    testModel.addListDataListener(listDataListener);
    testModel.addFilterListener(filteringEndedListener);

    testModel.setIncludeCondition(item -> false);
    assertEquals(1, filteringEndedCounter.get());
    assertEquals(1, testModel.getSize());
    testModel.setIncludeCondition(item -> true);
    assertEquals(2, filteringEndedCounter.get());
    assertEquals(6, testModel.getSize());
    testModel.setIncludeCondition(item -> !item.equals(ANNA));
    assertEquals(5, testModel.getSize());
    assertFalse(testModel.isVisible(ANNA));
    assertTrue(testModel.isFiltered(ANNA));
    testModel.setIncludeCondition(item -> item.equals(ANNA));
    assertEquals(2, testModel.getSize());
    assertTrue(testModel.isVisible(ANNA));

    assertEquals(4, testModel.getFilteredItems().size());
    assertEquals(1, testModel.getVisibleItems().size());
    assertEquals(4, testModel.getFilteredItemCount());
    assertEquals(2, testModel.getVisibleItemCount());
    assertEquals(5, testModel.getItems().size());

    testModel.addItem(BJORN);//already contained
    assertEquals(4, testModel.getFilteredItemCount());

    assertFalse(testModel.isVisible(BJORN));
    assertTrue(testModel.containsItem(BJORN));

    testModel.removeListDataListener(listDataListener);
    testModel.removeFilterListener(filteringEndedListener);
  }

  @Test
  void removeItem() {
    //remove filtered item
    testModel.setIncludeCondition(item -> !item.equals(BJORN));
    testModel.removeItem(BJORN);
    testModel.setIncludeCondition(null);
    assertFalse(testModel.isVisible(BJORN));

    //remove visible item
    testModel.removeItem(KALLI);
    assertFalse(testModel.isVisible(KALLI));
  }

  @Test
  void addItem() {
    testModel.clear();
    //add filtered item
    testModel.setIncludeCondition(item -> !item.equals(BJORN));
    testModel.addItem(BJORN);
    assertFalse(testModel.isVisible(BJORN));

    //add visible item
    testModel.addItem(KALLI);
    assertTrue(testModel.isVisible(KALLI));

    testModel.setIncludeCondition(null);
    assertTrue(testModel.isVisible(BJORN));
  }

  @Test
  void setNullValueString() throws Exception {
    assertTrue(testModel.isVisible(null));
    testModel.refresh();
    assertEquals(5, testModel.getVisibleItems().size());
    assertEquals(testModel.getElementAt(0), NULL);
    testModel.setSelectedItem(null);
    assertEquals(testModel.getSelectedItem(), NULL);
    assertTrue(testModel.isNullSelected());
    assertNull(testModel.getSelectedValue());
    testModel.setSelectedItem(NULL);
    assertEquals(NULL, testModel.getElementAt(0));
    assertEquals(ANNA, testModel.getElementAt(1));
  }

  @Test
  void selectorValue() {
    Value<Character> selectorValue = testModel.selectorValue(new Finder<String, Character>() {
      @Override
      public Character getValue(String item) {
        return item.charAt(0);
      }

      @Override
      public Predicate<String> getPredicate(Character value) {
        return item -> item.charAt(0) == value.charValue();
      }
    });
    assertNull(selectorValue.get());
    testModel.setSelectedItem(ANNA);
    assertEquals('a', selectorValue.get());
    selectorValue.set('k');
    assertEquals(KALLI, testModel.getSelectedItem());
    selectorValue.set(null);
    assertTrue(testModel.isNullSelected());
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
    List<Data> contents = asList(new Data(1, "1"), new Data(2, "2"), new Data(3, "3"));

    SwingFilteredComboBoxModel<Data> model = new SwingFilteredComboBoxModel<>();
    model.setContents(contents);
    model.setSelectedItem(contents.get(1));
    assertEquals("2", model.getSelectedValue().data);

    contents = asList(new Data(1, "1"), new Data(2, "22"), new Data(3, "3"));

    model.setContents(contents);
    assertEquals("22", model.getSelectedValue().data);
  }

  @BeforeEach
  void setUp() throws Exception {
    testModel = new SwingFilteredComboBoxModel<>();
    testModel.setIncludeNull(true);
    testModel.setNullItem(NULL);
    List<String> names = new ArrayList<>();
    names.add(ANNA);
    names.add(KALLI);
    names.add(SIGGI);
    names.add(TOMAS);
    names.add(BJORN);
    testModel.setContents(names);
  }

  @AfterEach
  void tearDown() throws Exception {
    testModel = null;
  }
}
