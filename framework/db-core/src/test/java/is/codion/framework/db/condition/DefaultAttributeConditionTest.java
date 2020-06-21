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

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DefaultAttributeConditionTest {

  private final TestDomain domain = new TestDomain();

  @Test
  void inClauseParenthesis() {
    final ColumnProperty<Integer> empIdProperty = domain.getEntities().getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_ID);

    final List<Integer> ids = new ArrayList<>();
    IntStream.range(0, 95).forEach(ids::add);
    DefaultAttributeCondition<Integer> condition = new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.EQUAL_TO, ids);
    String conditionString = condition.getConditionString(empIdProperty);
    assertTrue(conditionString.startsWith("empno in (?"));
    assertTrue(conditionString.endsWith("?, ?)"));

    ids.clear();
    IntStream.range(0, 105).forEach(ids::add);
    condition = new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.EQUAL_TO, ids);
    conditionString = condition.getConditionString(empIdProperty);
    assertTrue(conditionString.startsWith("(empno in (?"));
    assertTrue(conditionString.endsWith("?, ?))"));
  }

  @Test
  void incorrectProperty() {
    final ColumnProperty<String> nameProperty = domain.getEntities().getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_NAME);
    final DefaultAttributeCondition condition = new DefaultAttributeCondition(TestDomain.EMP_ID, Operator.EQUAL_TO, 1);
    assertThrows(IllegalArgumentException.class, () -> condition.getConditionString(nameProperty));
  }

  @Test
  void valueCount() {
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.GREATER_THAN, asList(1, 2)));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.GREATER_THAN, emptyList()));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.LESS_THAN, asList(1, 2)));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.LESS_THAN, emptyList()));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.WITHIN_RANGE, singletonList(1)));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.WITHIN_RANGE, asList(1, 2, 3)));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.WITHIN_RANGE, emptyList()));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.OUTSIDE_RANGE, singletonList(1)));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.OUTSIDE_RANGE, asList(1, 2, 3)));
    assertThrows(IllegalArgumentException.class, () -> new DefaultAttributeCondition<>(TestDomain.EMP_ID, Operator.OUTSIDE_RANGE, emptyList()));
  }
}
