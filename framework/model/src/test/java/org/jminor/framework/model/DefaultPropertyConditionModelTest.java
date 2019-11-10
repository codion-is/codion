/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.jminor.framework.db.condition.Conditions.entityCondition;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultPropertyConditionModelTest {

  private static final Domain DOMAIN = new TestDomain();

  @Test
  public void propertyConditionModel() throws Exception {
    final ColumnProperty property = (ColumnProperty) DOMAIN.getDefinition(TestDomain.T_DEPARTMENT).getProperty(TestDomain.DEPARTMENT_NAME);
    final PropertyConditionModel model = new DefaultPropertyConditionModel(property);
    assertEquals(property, model.getColumnIdentifier());
    model.setConditionType(ConditionType.LIKE);
    assertFalse(model.isLowerBoundRequired());
    model.setUpperBound("upper%");
    assertEquals(property.getPropertyId() + " like ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));
    model.setUpperBound("upper");
    assertEquals(property.getPropertyId() + " = ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));
    model.setConditionType(ConditionType.NOT_LIKE);
    model.setUpperBound("upper%");
    assertEquals(property.getPropertyId() + " not like ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));
    model.setUpperBound("upper");
    assertEquals(property.getPropertyId() + " <> ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));
    model.setConditionType(ConditionType.GREATER_THAN);
    assertEquals(property.getPropertyId() + " >= ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));
    model.setConditionType(ConditionType.LESS_THAN);
    assertEquals(property.getPropertyId() + " <= ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));

    model.setConditionType(ConditionType.WITHIN_RANGE);
    assertTrue(model.isLowerBoundRequired());
    model.setLowerBound("lower");
    List values = model.getCondition().getValues();
    assertTrue(values.contains("upper"));
    assertTrue(values.contains("lower"));
    assertEquals("(" + property.getPropertyId() + " >= ? and " + property.getPropertyId() + " <= ?)",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));

    model.setConditionType(ConditionType.LIKE);
    model.setAutomaticWildcard(ColumnConditionModel.AutomaticWildcard.PREFIX_AND_POSTFIX);
    values = model.getCondition().getValues();
    assertTrue(values.contains("%upper%"));
    assertEquals(property.getPropertyId() + " like ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));
    model.setConditionType(ConditionType.NOT_LIKE);
    assertEquals(property.getPropertyId() + " not like ?",
            entityCondition(TestDomain.T_DEPARTMENT, model.getCondition()).getWhereClause(DOMAIN));
  }
}
