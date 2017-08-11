/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.Conjunction;
import org.jminor.common.EventListener;
import org.jminor.common.User;
import org.jminor.common.db.Databases;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.local.LocalEntityConnectionProvider;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.sql.Types;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class DefaultEntityTableConditionModelTest {

  private static final EntityConnectionProvider CONNECTION_PROVIDER = new LocalEntityConnectionProvider(new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger")), Databases.getInstance());

  private final EntityTableConditionModel conditionModel = new DefaultEntityTableConditionModel(TestDomain.T_EMP,
          CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(),
          new DefaultPropertyConditionModelProvider());

  static {
    TestDomain.init();
  }

  @Test
  public void test() {
    assertEquals(TestDomain.T_EMP, conditionModel.getEntityID());
    conditionModel.setConjunction(Conjunction.OR);
    assertEquals(Conjunction.OR, conditionModel.getConjunction());
    assertEquals(9, conditionModel.getPropertyFilterModels().size());
    assertEquals(8, conditionModel.getPropertyConditionModels().size());

    assertFalse(conditionModel.isFilterEnabled(TestDomain.EMP_DEPARTMENT_FK));
    assertFalse(conditionModel.isEnabled(TestDomain.EMP_DEPARTMENT_FK));

    assertFalse(conditionModel.isEnabled());
    conditionModel.setEnabled(TestDomain.EMP_DEPARTMENT_FK, true);
    assertTrue(conditionModel.isEnabled());
  }

  @Test(expected = IllegalStateException.class)
  public void noSearchPropertiesDefined() {
    final DefaultEntityTableConditionModel model = new DefaultEntityTableConditionModel(TestDomain.T_DETAIL,
            CONNECTION_PROVIDER, new DefaultPropertyFilterModelProvider(), new DefaultPropertyConditionModelProvider());
    //no search properties defined for master entity
    ((DefaultForeignKeyConditionModel) model.getPropertyConditionModel(TestDomain.DETAIL_MASTER_FK)).getEntityLookupModel().performQuery();
  }

  @Test
  public void getPropertyFilterModel() {
    assertNotNull(conditionModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION));
  }

  @Test
  public void getPropertyConditionModel() {
    assertNotNull(conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getPropertyConditionModelNonExisting() {
    assertNull(conditionModel.getPropertyConditionModel("bla bla"));
  }

  @Test
  public void setFilterValue() {
    conditionModel.setFilterValue(TestDomain.EMP_COMMISSION, 1400);
    final ColumnConditionModel<Property> propertyConditionModel = conditionModel.getPropertyFilterModel(TestDomain.EMP_COMMISSION);
    assertTrue(propertyConditionModel.isEnabled());
    assertEquals(Condition.Type.LIKE, propertyConditionModel.getConditionType());
    assertEquals(1400, propertyConditionModel.getUpperBound());
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
    conditionModel.getPropertyConditionModel(TestDomain.EMP_COMMISSION).setConditionType(Condition.Type.GREATER_THAN);
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
    final String wildcardValue = wildcard + "test" + wildcard;
    conditionModel.setSimpleConditionString(value);
    for (final PropertyConditionModel model : conditionModel.getPropertyConditionModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertEquals(wildcardValue, model.getUpperBound());
        assertTrue(model.isEnabled());
      }
    }
    conditionModel.setSimpleConditionString(null);
    for (final PropertyConditionModel model : conditionModel.getPropertyConditionModels()) {
      if (model.getType() == Types.VARCHAR) {
        assertNull(model.getUpperBound());
        assertFalse(model.isEnabled());
      }
    }
  }

  @Test
  public void testAdditionalFilterCondition() {
    conditionModel.setAdditionalTableFilterCondition(entity -> !Objects.equals(entity.get(TestDomain.EMP_ID), 1));
    assertNotNull(conditionModel.getAdditionalTableFilterCondition());

    final Entity emp = Entities.entity(TestDomain.T_EMP);
    emp.put(TestDomain.EMP_ID, 1);
    assertFalse(conditionModel.include(emp));

    emp.put(TestDomain.EMP_ID, 2);
    assertTrue(conditionModel.include(emp));
  }
}
