/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.db.TestDomain;
import is.codion.framework.db.TestDomain.Employee;
import is.codion.framework.domain.entity.EntityDefinition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MultiValueAttributeConditionTest {

  private final TestDomain domain = new TestDomain();

  @Test
  void test() {
    assertThrows(IllegalStateException.class, () -> new MultiValueAttributeCondition<>(Employee.ID, Collections.emptyList(), Operator.BETWEEN));
    assertThrows(NullPointerException.class, () -> new MultiValueAttributeCondition<>(Employee.ID, Collections.singletonList(null), Operator.EQUAL));
  }

  @Test
  void inClauseParenthesis() {
    EntityDefinition definition = domain.entities().definition(Employee.TYPE);

    List<Integer> ids = new ArrayList<>();
    IntStream.range(0, 95).forEach(ids::add);
    MultiValueAttributeCondition<Integer> condition = new MultiValueAttributeCondition<>(Employee.ID, ids, Operator.EQUAL);
    String conditionString = condition.toString(definition);
    assertTrue(conditionString.startsWith("empno in (?"));
    assertTrue(conditionString.endsWith("?, ?)"));

    ids.clear();
    IntStream.range(0, 105).forEach(ids::add);
    condition = new MultiValueAttributeCondition<>(Employee.ID, ids, Operator.EQUAL);
    conditionString = condition.toString(definition);
    assertTrue(conditionString.startsWith("(empno in (?"));
    assertTrue(conditionString.endsWith("?, ?))"));
  }
}
