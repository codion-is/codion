/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.db.condition;

import is.codion.framework.db.TestDomain;
import is.codion.framework.domain.entity.EntityDefinition;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DefaultAttributeEqualConditionTest {

  private final TestDomain domain = new TestDomain();

  @Test
  void inClauseParenthesis() {
    EntityDefinition definition = domain.getEntities().getDefinition(TestDomain.T_EMP);

    List<Integer> ids = new ArrayList<>();
    IntStream.range(0, 95).forEach(ids::add);
    DefaultAttributeEqualCondition<Integer> condition = new DefaultAttributeEqualCondition<>(TestDomain.EMP_ID, ids);
    String conditionString = condition.getConditionString(definition);
    assertTrue(conditionString.startsWith("empno in (?"));
    assertTrue(conditionString.endsWith("?, ?)"));

    ids.clear();
    IntStream.range(0, 105).forEach(ids::add);
    condition = new DefaultAttributeEqualCondition<>(TestDomain.EMP_ID, ids);
    conditionString = condition.getConditionString(definition);
    assertTrue(conditionString.startsWith("(empno in (?"));
    assertTrue(conditionString.endsWith("?, ?))"));
  }
}
