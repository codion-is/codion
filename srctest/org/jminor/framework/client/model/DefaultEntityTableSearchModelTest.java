/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model;

import org.jminor.common.model.Conjunction;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.table.ColumnSearchModel;
import org.jminor.framework.Configuration;
import org.jminor.framework.db.local.DefaultEntityConnectionTest;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.sql.Types;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultEntityTableSearchModelTest {

  private final EntityTableModel tableModel = new DefaultEntityTableModel(EmpDept.T_EMPLOYEE, DefaultEntityConnectionTest.CONNECTION_PROVIDER);
  private final EntityTableSearchModel searchModel = tableModel.getSearchModel();

  public DefaultEntityTableSearchModelTest() {
    EmpDept.init();
  }

  @Test
  public void test() {
    assertEquals(EmpDept.T_EMPLOYEE, searchModel.getEntityID());
    searchModel.setSearchConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, searchModel.getSearchConjunction());
    assertEquals(9, searchModel.getPropertyFilterModels().size());
    assertEquals(8, searchModel.getPropertySearchModels().size());

    searchModel.refresh();
    assertTrue(((ForeignKeySearchModel) searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() > 1);
    searchModel.clear();
    assertTrue(((ForeignKeySearchModel) searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_DEPARTMENT_FK)).getEntityComboBoxModel().getSize() == 0);

    assertFalse(searchModel.isFilterEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));
    assertFalse(searchModel.isSearchEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK));

    assertFalse(searchModel.isSearchEnabled());
    searchModel.setSearchEnabled(EmpDept.EMPLOYEE_DEPARTMENT_FK, true);
    assertTrue(searchModel.isSearchEnabled());
  }

  @Test
  public void getPropertyFilterModel() {
    assertNotNull(searchModel.getPropertyFilterModel(EmpDept.EMPLOYEE_COMMISSION));
    assertNull(searchModel.getPropertyFilterModel("bla bla"));
  }

  @Test
  public void getPropertySearchModel() {
    assertNotNull(searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_COMMISSION));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPropertySearchModelNonExisting() {
    searchModel.getPropertySearchModel("bla bla");
  }

  @Test
  public void setFilterValue() {
    searchModel.setFilterValue(EmpDept.EMPLOYEE_COMMISSION, 1400);
    searchModel.setFilterValue("bla bla", "bla");
    final ColumnSearchModel<Property> propertySearchModel = searchModel.getPropertyFilterModel(EmpDept.EMPLOYEE_COMMISSION);
    assertTrue(propertySearchModel.isEnabled());
    assertEquals(SearchType.LIKE, propertySearchModel.getSearchType());
    assertEquals(1400, propertySearchModel.getUpperBound());
  }

  @Test
  public void searchStateListener() {
    final AtomicInteger counter = new AtomicInteger();
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred() {
        counter.incrementAndGet();
      }
    };
    searchModel.addSearchStateListener(listener);
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_COMMISSION).setEnabled(true);
    assertEquals(1, counter.get());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_COMMISSION).setEnabled(false);
    assertEquals(2, counter.get());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_COMMISSION).setUpperBound(1200d);
    //automatically set enabled when upper bound is set
    assertEquals(4, counter.get());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_COMMISSION).setSearchType(SearchType.GREATER_THAN);
    assertEquals(5, counter.get());
    searchModel.removeSearchStateListener(listener);
  }

  @Test
  public void testSearchState() {
    assertFalse(searchModel.hasSearchStateChanged());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_JOB).setLikeValue("job");
    assertTrue(searchModel.hasSearchStateChanged());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_JOB).setEnabled(false);
    assertFalse(searchModel.hasSearchStateChanged());
    searchModel.getPropertySearchModel(EmpDept.EMPLOYEE_JOB).setEnabled(true);
    assertTrue(searchModel.hasSearchStateChanged());
    searchModel.rememberCurrentSearchState();
    assertFalse(searchModel.hasSearchStateChanged());
  }

  @Test
  public void testSimpleSearchString() {
    final String value = "test";
    final String wildcard = (String) Configuration.getValue(Configuration.WILDCARD_CHARACTER);
    final String wildcardValue = wildcard + "test" + wildcard;
    searchModel.setSimpleSearchString(value);
    for (final PropertySearchModel model : searchModel.getPropertySearchModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertEquals(wildcardValue, model.getUpperBound());
        assertTrue(model.isEnabled());
      }
    }
    searchModel.setSimpleSearchString(null);
    for (final PropertySearchModel model : searchModel.getPropertySearchModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }
}
