/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.condition.ConditionType;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.TestDomain;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DefaultPropertyConditionModelTest {

  @Test
  public void propertyConditionModel() throws Exception {
    TestDomain.init();
    final Property.ColumnProperty property = (Property.ColumnProperty) Entities.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    final PropertyConditionModel model = new DefaultPropertyConditionModel(property);
    assertEquals(property, model.getColumnIdentifier());
    model.setConditionType(ConditionType.LIKE);
    assertFalse(model.isLowerBoundRequired());
    model.setUpperBound("upper");
    assertEquals(property.getPropertyID() + " like ?", model.getCondition().getWhereClause());
    model.setConditionType(ConditionType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getCondition().getWhereClause());
    model.setConditionType(ConditionType.GREATER_THAN);
    assertEquals(property.getPropertyID() + " >= ?", model.getCondition().getWhereClause());
    model.setConditionType(ConditionType.LESS_THAN);
    assertEquals(property.getPropertyID() + " <= ?", model.getCondition().getWhereClause());

    model.setConditionType(ConditionType.WITHIN_RANGE);
    assertTrue(model.isLowerBoundRequired());
    model.setLowerBound("lower");
    List values = model.getCondition().getValues();
    assertTrue(values.contains("upper"));
    assertTrue(values.contains("lower"));
    assertEquals("(" + property.getPropertyID() + " >= ? and " + property.getPropertyID() + " <= ?)", model.getCondition().getWhereClause());

    model.setConditionType(ConditionType.LIKE);
    model.setAutomaticWildcard(true);
    values = model.getCondition().getValues();
    assertTrue(values.contains("%upper%"));
    assertEquals(property.getPropertyID() + " like ?", model.getCondition().getWhereClause());
    model.setConditionType(ConditionType.NOT_LIKE);
    assertEquals(property.getPropertyID() + " not like ?", model.getCondition().getWhereClause());
  }
}
