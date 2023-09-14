/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import is.codion.common.Operator;
import is.codion.framework.domain.TestDomain;
import is.codion.framework.domain.TestDomain.Employee;
import is.codion.framework.domain.entity.EntityDefinition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MultiValueColumnConditionTest {

  private final TestDomain domain = new TestDomain();

  @Test
  void test() {
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.GREATER_THAN));
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.GREATER_THAN_OR_EQUAL));
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.LESS_THAN));
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.LESS_THAN_OR_EQUAL));
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.BETWEEN_EXCLUSIVE));
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.NOT_BETWEEN));
    assertThrows(IllegalArgumentException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(1), Operator.NOT_BETWEEN_EXCLUSIVE));
    assertThrows(NullPointerException.class, () -> new MultiValueColumnCondition<>(Employee.ID, singletonList(null), Operator.EQUAL));
  }

  @Test
  void inClauseParenthesis() {
    EntityDefinition definition = domain.entities().definition(Employee.TYPE);

    List<Integer> ids = new ArrayList<>();
    IntStream.range(0, 95).forEach(ids::add);
    MultiValueColumnCondition<Integer> condition = new MultiValueColumnCondition<>(Employee.ID, ids, Operator.EQUAL);
    String conditionString = condition.toString(definition);
    assertTrue(conditionString.startsWith("empno in (?"));
    assertTrue(conditionString.endsWith("?, ?)"));

    ids.clear();
    IntStream.range(0, 105).forEach(ids::add);
    condition = new MultiValueColumnCondition<>(Employee.ID, ids, Operator.EQUAL);
    conditionString = condition.toString(definition);
    assertTrue(conditionString.startsWith("(empno in (?"));
    assertTrue(conditionString.endsWith("?, ?))"));
  }
}
