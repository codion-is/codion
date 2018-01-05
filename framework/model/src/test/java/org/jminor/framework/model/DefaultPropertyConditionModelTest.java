/*
 * Chinook.Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.condition.Condition;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class DefaultPropertyConditionModelTest {

  private static final Entities ENTITIES = new TestDomain();
  private static final EntityConditions ENTITY_CONDITIONS = new EntityConditions(ENTITIES);

  @Test
  public void propertyConditionModel() throws Exception {
    final Property.ColumnProperty property = (Property.ColumnProperty) ENTITIES.getProperty(TestDomain.T_DEPARTMENT, TestDomain.DEPARTMENT_NAME);
    final PropertyConditionModel model = new DefaultPropertyConditionModel(ENTITY_CONDITIONS, property);
    assertEquals(property, model.getColumnIdentifier());
    model.setConditionType(Condition.Type.LIKE);
    assertFalse(model.isLowerBoundRequired());
    model.setUpperBound("upper");
    assertEquals(property.getPropertyId() + " like ?", model.getCondition().getWhereClause());
    model.setConditionType(Condition.Type.NOT_LIKE);
    assertEquals(property.getPropertyId() + " not like ?", model.getCondition().getWhereClause());
    model.setConditionType(Condition.Type.GREATER_THAN);
    assertEquals(property.getPropertyId() + " >= ?", model.getCondition().getWhereClause());
    model.setConditionType(Condition.Type.LESS_THAN);
    assertEquals(property.getPropertyId() + " <= ?", model.getCondition().getWhereClause());

    model.setConditionType(Condition.Type.WITHIN_RANGE);
    assertTrue(model.isLowerBoundRequired());
    model.setLowerBound("lower");
    List values = model.getCondition().getValues();
    assertTrue(values.contains("upper"));
    assertTrue(values.contains("lower"));
    assertEquals("(" + property.getPropertyId() + " >= ? and " + property.getPropertyId() + " <= ?)", model.getCondition().getWhereClause());

    model.setConditionType(Condition.Type.LIKE);
    model.setAutomaticWildcard(true);
    values = model.getCondition().getValues();
    assertTrue(values.contains("%upper%"));
    assertEquals(property.getPropertyId() + " like ?", model.getCondition().getWhereClause());
    model.setConditionType(Condition.Type.NOT_LIKE);
    assertEquals(property.getPropertyId() + " not like ?", model.getCondition().getWhereClause());
  }
}
