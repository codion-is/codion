/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.model;

import org.jminor.common.EventListener;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.model.TestDomain;

import javafx.scene.control.ListView;
import org.junit.Test;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ObservableEntityListTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.createInstance());

  @Test
  public void selectCondition() {
    final ObservableEntityList list = new ObservableEntityList(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    list.refresh();
    assertEquals(4, list.size());
    list.setSelectCondition(EntityConditions.propertyCondition(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME,
            Condition.Type.NOT_LIKE, Arrays.asList("SALES", "OPERATIONS")));
    list.refresh();
    assertEquals(2, list.size());
  }

  @Test
  public void filterCondition() throws DatabaseException {
    final AtomicInteger counter = new AtomicInteger(0);
    final ObservableEntityList list = new ObservableEntityList(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER);
    final EventListener listener = counter::incrementAndGet;
    list.addFilteringListener(listener);
    list.refresh();
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity operations = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "OPERATIONS");

    list.setFilterCondition(item -> Objects.equals(item.get(TestDomain.DEPARTMENT_NAME), "SALES"));
    assertEquals(1, counter.get());
    assertNotNull(list.getFilterCondition());
    assertEquals(3, list.getFilteredItemCount());
    assertEquals(1, list.getVisibleItemCount());
    assertEquals(3, list.getFilteredItems().size());
    assertTrue(list.isFiltered(sales));
    assertTrue(list.isVisible(sales));

    assertTrue(list.contains(operations, true));

    list.setFilterCondition(null);
    assertEquals(2, counter.get());
    assertNull(list.getFilterCondition());
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
    catch (final IllegalStateException e) {}
    list.refresh();
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity operations = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT,
            TestDomain.DEPARTMENT_NAME, "OPERATIONS");

    list.getSelectionModel().setSelectedItem(sales);
    assertFalse(list.getSelectionEmptyObserver().isActive());
    assertTrue(list.getSingleSelectionObserver().isActive());
    assertFalse(list.getMultipleSelectionObserver().isActive());

    list.getSelectionModel().setSelectedItems(Arrays.asList(sales, operations));
    assertFalse(list.getSelectionEmptyObserver().isActive());
    assertFalse(list.getSingleSelectionObserver().isActive());
    assertTrue(list.getMultipleSelectionObserver().isActive());

    list.getSelectionModel().clearSelection();
    assertTrue(list.getSelectionEmptyObserver().isActive());
    assertFalse(list.getSingleSelectionObserver().isActive());
    assertFalse(list.getMultipleSelectionObserver().isActive());
  }
}
