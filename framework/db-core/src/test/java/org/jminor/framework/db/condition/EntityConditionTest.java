/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.Conjunction;
import org.jminor.common.db.ConditionType;
import org.jminor.framework.domain.Properties;

import org.junit.jupiter.api.Test;

import java.sql.Types;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EntityConditionTest {

  @Test
  public void test() {
    final Condition.Set set1 = Conditions.conditionSet(
            Conjunction.AND,
            Conditions.propertyCondition(Properties.columnProperty("stringProperty", Types.VARCHAR), ConditionType.LIKE, "value"),
            Conditions.propertyCondition(Properties.columnProperty("intProperty", Types.INTEGER), ConditionType.LIKE, 666)
    );
    final EntityCondition condition = EntityConditions.condition("entityId", set1);
    assertEquals("(stringProperty = ? and intProperty = ?)", condition.getWhereClause());
    assertEquals(set1, condition.getCondition());
    final Condition.Set set2 = Conditions.conditionSet(
            Conjunction.AND,
            Conditions.propertyCondition(Properties.columnProperty("doubleProperty", Types.DOUBLE), ConditionType.LIKE, 666.666),
            Conditions.propertyCondition(Properties.columnProperty("stringProperty2", Types.VARCHAR), ConditionType.LIKE, false, "valu%e2")
    );
    final Condition.Set set3 = Conditions.conditionSet(Conjunction.OR, set1, set2);
    assertEquals("((stringProperty = ? and intProperty = ?) or (doubleProperty = ? and upper(stringProperty2) like upper(?)))",
            EntityConditions.condition("entityId", set3).getWhereClause());
  }
}
