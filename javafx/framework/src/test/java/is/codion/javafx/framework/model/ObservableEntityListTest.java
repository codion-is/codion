/*
 * Copyright (c) 2016 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Condition;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class ObservableEntityListTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domainClassName(TestDomain.class.getName())
          .user(UNIT_TEST_USER)
          .build();

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  void selectCondition() {
    ObservableEntityList list = new ObservableEntityList(Department.TYPE, CONNECTION_PROVIDER);
    list.refresh();
    assertEquals(4, list.size());
    list.setSelectCondition(Condition.where(Department.NAME).notEqualTo("SALES", "OPERATIONS"));
    list.refresh();
    assertEquals(2, list.size());
  }

  @Test
  void includeCondition() throws DatabaseException {
    AtomicInteger counter = new AtomicInteger();
    ObservableEntityList list = new ObservableEntityList(Department.TYPE, CONNECTION_PROVIDER);
    EventListener listener = counter::incrementAndGet;
    list.addFilterListener(listener);
    list.refresh();
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity operations = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "OPERATIONS");

    list.setIncludeCondition(item -> Objects.equals(item.get(Department.NAME), "SALES"));
    assertEquals(1, counter.get());
    assertNotNull(list.getIncludeCondition());
    assertEquals(3, list.filteredItemCount());
    assertEquals(1, list.visibleItemCount());
    assertEquals(3, list.filteredItems().size());
    assertFalse(list.isFiltered(sales));
    assertTrue(list.isVisible(sales));

    assertTrue(list.containsItem(operations));

    list.setIncludeCondition(null);
    assertEquals(2, counter.get());
    assertNull(list.getIncludeCondition());
    assertEquals(0, list.filteredItemCount());
    assertEquals(4, list.visibleItemCount());
    assertEquals(0, list.filteredItems().size());
    assertFalse(list.isFiltered(sales));
    assertTrue(list.isVisible(sales));
    list.removeFilterListener(listener);
  }

  @Test
  void selection() throws DatabaseException {
    ObservableEntityList list = new ObservableEntityList(Department.TYPE, CONNECTION_PROVIDER);
    ListView<Entity> listView = new ListView<>(list);
    list.setSelectionModel(listView.getSelectionModel());
    try {
      list.setSelectionModel(listView.getSelectionModel());
      fail();
    }
    catch (IllegalStateException ignored) {}
    list.refresh();
    Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "SALES");
    Entity operations = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME, "OPERATIONS");

    list.selectionModel().setSelectedItem(sales);
    assertFalse(list.selectionEmptyObserver().get());
    assertTrue(list.singleSelectionObserver().get());
    assertFalse(list.multipleSelectionObserver().get());

    list.selectionModel().setSelectedItems(asList(sales, operations));
    assertFalse(list.selectionEmptyObserver().get());
    assertFalse(list.singleSelectionObserver().get());
    assertTrue(list.multipleSelectionObserver().get());

    list.selectionModel().clearSelection();
    assertTrue(list.selectionEmptyObserver().get());
    assertFalse(list.singleSelectionObserver().get());
    assertFalse(list.multipleSelectionObserver().get());
  }

  @Test
  void validItems() {
    ObservableEntityList entityList = new ObservableEntityList(Employee.TYPE, CONNECTION_PROVIDER);
    Entity dept = CONNECTION_PROVIDER.entities().builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "dept")
            .build();
    assertThrows(IllegalArgumentException.class, () -> entityList.add(dept));
    assertThrows(IllegalArgumentException.class, () -> entityList.add(0, dept));
    assertThrows(IllegalArgumentException.class, () -> entityList.addAll(dept));
    assertThrows(IllegalArgumentException.class, () -> entityList.addAll(singletonList(dept)));
    assertThrows(IllegalArgumentException.class, () -> entityList.addAll(0, singletonList(dept)));

    assertThrows(IllegalArgumentException.class, () -> entityList.setAll(singletonList(dept)));
    assertThrows(IllegalArgumentException.class, () -> entityList.set(new ObservableEntityList(Department.TYPE, CONNECTION_PROVIDER)));

    assertThrows(NullPointerException.class, () -> entityList.add(null));
    assertThrows(NullPointerException.class, () -> entityList.add(0, null));
    assertThrows(NullPointerException.class, () -> entityList.addAll(singletonList(null)));
    assertThrows(NullPointerException.class, () -> entityList.addAll(0, singletonList(null)));
  }
}
