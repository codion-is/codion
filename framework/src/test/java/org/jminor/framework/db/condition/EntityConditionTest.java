/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.db.condition.Conditions;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

public class EntityConditionTest {

  @Test
  public void test() {
    final Condition.Set<Property.ColumnProperty> set1 = Conditions.conditionSet(
            Conjunction.AND,
            EntityConditions.propertyCondition(Properties.columnProperty("stringProperty", Types.VARCHAR), Condition.Type.LIKE, "value"),
            EntityConditions.propertyCondition(Properties.columnProperty("intProperty", Types.INTEGER), Condition.Type.LIKE, 666)
    );
    final EntityCondition condition = EntityConditions.condition("entityID", set1);
    assertEquals("(stringProperty like ? and intProperty = ?)", condition.getWhereClause());
    assertEquals(set1, condition.getCondition());
    final Condition.Set<Property.ColumnProperty> set2 = Conditions.conditionSet(
            Conjunction.AND,
            EntityConditions.propertyCondition(Properties.columnProperty("doubleProperty", Types.DOUBLE), Condition.Type.LIKE, 666.666),
            EntityConditions.propertyCondition(Properties.columnProperty("stringProperty2", Types.VARCHAR), Condition.Type.LIKE, false, "value2")
    );
    final Condition.Set<Property.ColumnProperty> set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty like ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            EntityConditions.condition("entityID", set3).getWhereClause());
  }
}
