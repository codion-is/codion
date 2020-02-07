/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.condition;

import org.jminor.common.db.ConditionType;
import org.jminor.framework.db.TestDomain;
import org.jminor.framework.domain.property.ColumnProperty;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public final class DefaultPropertyConditionTest {

  @Test
  void inClauseParenthesis() {
    final ColumnProperty empIdProperty = new TestDomain().getDefinition(TestDomain.T_EMP).getColumnProperty(TestDomain.EMP_ID);

    final List<Integer> ids = new ArrayList<>();
    IntStream.range(0, 95).forEach(ids::add);
    DefaultPropertyCondition condition = new DefaultPropertyCondition(TestDomain.EMP_ID, ConditionType.LIKE, ids);
    String conditionString = condition.getConditionString(empIdProperty);
    assertTrue(conditionString.startsWith("empno in (?"));
    assertTrue(conditionString.endsWith("?, ?)"));

    ids.clear();
    IntStream.range(0, 105).forEach(ids::add);
    condition = new DefaultPropertyCondition(TestDomain.EMP_ID, ConditionType.LIKE, ids);
    conditionString = condition.getConditionString(empIdProperty);
    assertTrue(conditionString.startsWith("(empno in (?"));
    assertTrue(conditionString.endsWith("?, ?))"));
  }
}
