/*
 * Copyright (c) 2020 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.common.Operator;
import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.EntityDefinition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class MultiValueAttributeConditionTest {

  private final TestDomain domain = new TestDomain();

  @Test
  void inClauseParenthesis() {
    EntityDefinition definition = domain.entities().getDefinition(TestDomain.T_EMP);

    List<Integer> ids = new ArrayList<>();
    IntStream.range(0, 95).forEach(ids::add);
    MultiValueAttributeCondition<Integer> condition = new MultiValueAttributeCondition<>(TestDomain.EMP_ID, ids, Operator.EQUAL);
    String conditionString = condition.toString(definition);
    assertTrue(conditionString.startsWith("empno in (?"));
    assertTrue(conditionString.endsWith("?, ?)"));

    ids.clear();
    IntStream.range(0, 105).forEach(ids::add);
    condition = new MultiValueAttributeCondition<>(TestDomain.EMP_ID, ids, Operator.EQUAL);
    conditionString = condition.toString(definition);
    assertTrue(conditionString.startsWith("(empno in (?"));
    assertTrue(conditionString.endsWith("?, ?))"));
  }
}
