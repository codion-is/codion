/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.db.Operator;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DefaultAttributeConditionTest {

  @Test
  void inClauseParenthesis() {
    final ColumnProperty<Integer> empIdProperty = new TestDomain().getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_ID);

    final List<Integer> ids = new ArrayList<>();
    IntStream.range(0, 95).forEach(ids::add);
    DefaultAttributeCondition condition = new DefaultAttributeCondition(TestDomain.EMP_ID, Operator.LIKE, ids);
    String conditionString = condition.getConditionString(empIdProperty);
    assertTrue(conditionString.startsWith("empno in (?"));
    assertTrue(conditionString.endsWith("?, ?)"));

    ids.clear();
    IntStream.range(0, 105).forEach(ids::add);
    condition = new DefaultAttributeCondition(TestDomain.EMP_ID, Operator.LIKE, ids);
    conditionString = condition.getConditionString(empIdProperty);
    assertTrue(conditionString.startsWith("(empno in (?"));
    assertTrue(conditionString.endsWith("?, ?))"));
  }

  @Test
  void incorrectProperty() {
    final ColumnProperty<String> nameProperty = new TestDomain().getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_NAME);
    final DefaultAttributeCondition condition = new DefaultAttributeCondition(TestDomain.EMP_ID, Operator.LIKE, 1);
    assertThrows(IllegalArgumentException.class, () -> condition.getConditionString(nameProperty));
  }
}
