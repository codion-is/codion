/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.condition.ConditionSet;
import org.jminor.common.db.condition.Conditions;
import org.jminor.common.model.ConditionType;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;

import org.junit.Test;

import java.sql.Types;

import static org.junit.Assert.assertEquals;

public class EntityConditionTest {

  @Test
  public void test() {
    final ConditionSet<Property.ColumnProperty> set1 = Conditions.conditionSet(
            Conjunction.AND,
            EntityConditions.propertyCondition(Properties.columnProperty("stringProperty", Types.VARCHAR), ConditionType.LIKE, "value"),
            EntityConditions.propertyCondition(Properties.columnProperty("intProperty", Types.INTEGER), ConditionType.LIKE, 666)
    );
    final EntityCondition condition = EntityConditions.condition("entityID", set1);
    assertEquals("(stringProperty like ? and intProperty = ?)", condition.getWhereClause());
    assertEquals(set1, condition.getCondition());
    final ConditionSet<Property.ColumnProperty> set2 = Conditions.conditionSet(
            Conjunction.AND,
            EntityConditions.propertyCondition(Properties.columnProperty("doubleProperty", Types.DOUBLE), ConditionType.LIKE, 666.666),
            EntityConditions.propertyCondition(Properties.columnProperty("stringProperty2", Types.VARCHAR), ConditionType.LIKE, false, "value2")
    );
    final ConditionSet<Property.ColumnProperty> set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty like ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            EntityConditions.condition("entityID", set3).getWhereClause());
  }
}
