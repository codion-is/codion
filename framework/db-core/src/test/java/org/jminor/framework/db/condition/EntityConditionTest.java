/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityConditionTest {

  private static final TestDomain entities = new TestDomain();
  private static final EntityConditions entityConditions = new EntityConditions(entities);

  @Test
  public void test() {
    final Condition.Set<Property.ColumnProperty> set1 = Conditions.conditionSet(
            Conjunction.AND,
            entityConditions.propertyCondition(Properties.columnProperty("stringProperty", Types.VARCHAR), Condition.Type.LIKE, "value"),
            entityConditions.propertyCondition(Properties.columnProperty("intProperty", Types.INTEGER), Condition.Type.LIKE, 666)
    );
    final EntityCondition condition = entityConditions.condition("entityId", set1);
    assertEquals("(stringProperty = ? and intProperty = ?)", condition.getWhereClause());
    assertEquals(set1, condition.getCondition());
    final Condition.Set<Property.ColumnProperty> set2 = Conditions.conditionSet(
            Conjunction.AND,
            entityConditions.propertyCondition(Properties.columnProperty("doubleProperty", Types.DOUBLE), Condition.Type.LIKE, 666.666),
            entityConditions.propertyCondition(Properties.columnProperty("stringProperty2", Types.VARCHAR), Condition.Type.LIKE, false, "valu%e2")
    );
    final Condition.Set<Property.ColumnProperty> set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty = ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            entityConditions.condition("entityId", set3).getWhereClause());
  }
}
