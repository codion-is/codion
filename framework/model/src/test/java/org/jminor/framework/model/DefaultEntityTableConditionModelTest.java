/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.EventListener;
import org.jminor.common.User;
import org.jminor.common.db.ConditionType;
import org.jminor.common.db.Databases;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.Test;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.jminor.framework.db.condition.Conditions.entityCondition;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultEntityTableConditionModelTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(
          Databases.getInstance()).setDomainClassName(TestDomain.class.getName()).setUser(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray()));

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(),
          new DefaultPropertyConditionModelProvider());

  @Test
  public void test() {
    assertEquals(TestDomain.T_EMP, conditionModel.getEntityId());
    conditionModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, conditionModel.getConjunction());
    assertEquals(9, conditionModel.getPropertyFilterModels().size());
    assertEquals(8, conditionModel.getPropertyConditionModels().size());

    assertFalse(conditionModel.isFilterEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));

    assertFalse(conditionModel.isEnabled());
    conditionModel.setEnabled(TestDomain.EMP_DEPARTMENT_FK, true);
    assertTrue(conditionModel.isEnabled());

    conditionModel.clear();
    conditionModel.refresh();
  }

  @Test
  public void noSearchPropertiesDefined() {
    final DefaultEntityTableConditionModel model = new DefaultEntityTableConditionModel(TestDomain.T_DETAIL,
            CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(), new DefaultPropertyConditionModelProvider());
    //no search properties defined for master entity
    assertThrows(IllegalStateException.class, () ->
            ((DefaultForeignKeyConditionModel) model.getPropertyConditionModel(TestDomain.DETAIL_MASTER_FK)).getEntityLookupModel().performQuery());
  }

  @Test
  public void getPropertyFilterModel() {
    assertNotNull(conditionModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  public void getPropertyConditionModel() {
    assertNotNull(conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  public void getPropertyConditionModelNonExisting() {
    assertThrows(IllegalArgumentException.class, () -> assertNull(conditionModel.getPropertyConditionModel("bla bla")));
  }

  @Test
  public void setFilterValue() {
    conditionModel.setFilterValue(TestDomain.EMP_COMMISSION, 1400d);
    final ColumnConditionModel<Property> propertyConditionModel = conditionModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION);
    assertTrue(propertyConditionModel.isEnabled());
    assertTrue(conditionModel.isFilterEnabled(TestDomain.EMP_COMMISSION));
    assertEquals(ConditionType.LIKE, propertyConditionModel.getConditionType());
    assertEquals(1400d, propertyConditionModel.getUpperBound());
  }

  @Test
  public void setConditionValues() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    boolean searchStateChanged = conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(searchStateChanged);
    assertTrue(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertTrue(((ForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK)).getConditionEntities().contains(sales));
    assertTrue(((ForeignKeyConditionModel) conditionModel.getPropertyConditionModel(TestDomain.EMP_DEPARTMENT_FK)).getConditionEntities().contains(accounting));
    searchStateChanged = conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, null);
    assertTrue(searchStateChanged);
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  public void clearPropertyConditionModels() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    assertTrue(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.clearPropertyConditionModels();
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
  }

  @Test
  public void getCondition() throws DatabaseException {
    final Entity sales = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "SALES");
    final Entity accounting = CONNECTION_PROVIDER.getConnection().selectSingle(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME, "ACCOUNTING");
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));
    conditionModel.setConditionValues(TestDomain.EMP_DEPARTMENT_FK, asList(sales, accounting));
    final PropertyConditionModel nameConditionModel = conditionModel.getPropertyConditionModel(TestDomain.EMP_NAME);
    nameConditionModel.setLikeValue("SCOTT");
    assertEquals("(ename = ? and (deptno in (?, ?)))", entityCondition(TestDomain.T_EMP,
            conditionModel.getCondition()).getWhereClause(CONNECTION_PROVIDER.getDomain()));

    conditionModel.setAdditionalConditionProvider(() -> Conditions.customCondition(TestDomain.EMP_CONDITION_2_ID));
    assertNotNull(conditionModel.getAdditionalConditionProvider());
    assertEquals("(ename = ? and (deptno in (?, ?)) and 1 = 1)",
            entityCondition(TestDomain.T_EMP, conditionModel.getCondition()).getWhereClause(CONNECTION_PROVIDER.getDomain()));
  }

  @Test
  public void searchStateListener() {
    final AtomicInteger counter = new AtomicInteger();
    final EventListener listener = counter::incrementAndGet;
    conditionModel.addConditionStateListener(listener);
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setEnabled(true);
    assertEquals(1, counter.get());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setEnabled(false);
    assertEquals(2, counter.get());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setUpperBound(1200d);
    //automatically set enabled when upper bound is set
    assertEquals(4, counter.get());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setConditionType(ConditionType.GREATER_THAN);
    assertEquals(5, counter.get());
    conditionModel.removeConditionStateListener(listener);
  }

  @Test
  public void testSearchState() {
    assertFalse(conditionModel.hasConditionStateChanged());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_JOB).setLikeValue("job");
    assertTrue(conditionModel.hasConditionStateChanged());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_JOB).setEnabled(false);
    assertFalse(conditionModel.hasConditionStateChanged());
    conditionModel.getPropertyConditionModel(TestDomain.EMP_JOB).setEnabled(true);
    assertTrue(conditionModel.hasConditionStateChanged());
    conditionModel.rememberCurrentConditionState();
    assertFalse(conditionModel.hasConditionStateChanged());
  }

  @Test
  public void testSimpleSearchString() {
    final String value = "test";
    final String wildcard = Property.WILDCARD_CHARACTER.get();
    conditionModel.setSimpleConditionString(value);
    for (final PropertyConditionModel model : conditionModel.getPropertyConditionModels()) {
      if (model.getTypeClass().equals(String.class)) {
        assertEquals(wildcard + value + wildcard, model.getUpperBound());
        assertTrue(model.isEnabled());
      }
    }
    conditionModel.setSimpleConditionString(null);
    for (final PropertyConditionModel model : conditionModel.getPropertyConditionModels()) {
      if (model.getTypeClass().equals(String.class)) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }

  @Test
  public void testAdditionalFilterCondition() {
    conditionModel.setAdditionalFilterCondition(entity -> !Objects.equals(entity.get(TestDomain.EMP_ID), 1));
    assertNotNull(conditionModel.getAdditionalFilterCondition());

    final Entity emp = CONNECTION_PROVIDER.getDomain().entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_ID, 1);
    assertFalse(conditionModel.include(emp));

    emp.put(TestDomain.EMP_ID, 2);
    assertTrue(conditionModel.include(emp));
  }
}
