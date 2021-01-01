/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.model;

import is.codion.common.db.database.Databases;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventListener;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.tests.TestDomain;

import javafx.scene.control.ListView;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testfx.api.FxToolkit;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class ObservableEntityListTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(UNIT_TEST_USER);

  @BeforeAll
  public static void setUp() throws Exception {
    FxToolkit.registerPrimaryStage();
  }

  @Test
  public void selectCondition() {
    final ObservableEntityList list = new ObservableEntityList(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    list.refresh();
    assertEquals(4, list.size());
    list.setSelectCondition(Conditions.condition(TestDomain.DEPARTMENT_NAME).notEqualTo("SALES", "OPERATIONS"));
    list.refresh();
    assertEquals(2, list.size());
  }

  @Test
  public void includeCondition() throws DatabaseException {
    final AtomicInteger counter = new AtomicInteger();
    final ObservableEntityList list = new ObservableEntityList(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final EventListener listener = counter::incrementAndGet;
    list.addFilteringListener(listener);
    list.refresh();
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity operations = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "OPERATIONS");

    list.setIncludeCondition(item -> Objects.equals(item.get(TestDomain.DEPARTMENT_NAME), "SALES"));
    assertEquals(1, counter.get());
    assertNotNull(list.getIncludeCondition());
    assertEquals(3, list.getFilteredItemCount());
    assertEquals(1, list.getVisibleItemCount());
    assertEquals(3, list.getFilteredItems().size());
    assertFalse(list.isFiltered(sales));
    assertTrue(list.isVisible(sales));

    assertTrue(list.containsItem(operations));

    list.setIncludeCondition(null);
    assertEquals(2, counter.get());
    assertNull(list.getIncludeCondition());
    assertEquals(0, list.getFilteredItemCount());
    assertEquals(4, list.getVisibleItemCount());
    assertEquals(0, list.getFilteredItems().size());
    assertFalse(list.isFiltered(sales));
    assertTrue(list.isVisible(sales));
    list.removeFilteringListener(listener);
  }

  @Test
  public void selection() throws DatabaseException {
    final ObservableEntityList list = new ObservableEntityList(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final ListView<Entity> listView = new ListView<>(list);
    list.setSelectionModel(listView.getSelectionModel());
    try {
      list.setSelectionModel(listView.getSelectionModel());
      fail();
    }
    catch (final IllegalStateException ignored) {}
    list.refresh();
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity operations = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.DEPARTMENT_NAME, "OPERATIONS");

    list.getSelectionModel().setSelectedItem(sales);
    assertFalse(list.getSelectionEmptyObserver().get());
    assertTrue(list.getSingleSelectionObserver().get());
    assertFalse(list.getMultipleSelectionObserver().get());

    list.getSelectionModel().setSelectedItems(asList(sales, operations));
    assertFalse(list.getSelectionEmptyObserver().get());
    assertFalse(list.getSingleSelectionObserver().get());
    assertTrue(list.getMultipleSelectionObserver().get());

    list.getSelectionModel().clearSelection();
    assertTrue(list.getSelectionEmptyObserver().get());
    assertFalse(list.getSingleSelectionObserver().get());
    assertFalse(list.getMultipleSelectionObserver().get());
  }
}
