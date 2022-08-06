/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultColumnFilterModelTest {

  @Test
  void includeInteger() {
    DefaultColumnFilterModel<String, String, Integer> conditionModel = new DefaultColumnFilterModel<>("test", Integer.class, '%');
    conditionModel.setAutoEnable(false);
    conditionModel.setOperator(Operator.EQUAL);

    conditionModel.setEqualValue(null);
    conditionModel.setEnabled(true);
    assertTrue(conditionModel.include((Comparable<Integer>) null));
    assertFalse(conditionModel.include(1));

    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertTrue(conditionModel.include(1));

    conditionModel.setOperator(Operator.EQUAL);

    conditionModel.setEqualValue(10);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertTrue(conditionModel.include((Comparable<Integer>) null));
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setLowerBound(10);
    conditionModel.setOperator(Operator.GREATER_THAN_OR_EQUAL);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    conditionModel.setOperator(Operator.GREATER_THAN);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertFalse(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setUpperBound(10);
    conditionModel.setOperator(Operator.LESS_THAN_OR_EQUAL);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    conditionModel.setOperator(Operator.LESS_THAN);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setLowerBound(6);
    conditionModel.setOperator(Operator.BETWEEN);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));
    conditionModel.setOperator(Operator.BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertFalse(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));

    conditionModel.setOperator(Operator.NOT_BETWEEN);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertTrue(conditionModel.include(6));
    assertFalse(conditionModel.include(7));
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    assertTrue(conditionModel.include(5));
    conditionModel.setOperator(Operator.NOT_BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.include((Comparable<Integer>) null));
    assertFalse(conditionModel.include(6));
    assertFalse(conditionModel.include(7));
    assertFalse(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    assertTrue(conditionModel.include(5));

    conditionModel.setUpperBound(null);
    conditionModel.setLowerBound(null);
    conditionModel.setOperator(Operator.BETWEEN);
    assertTrue(conditionModel.include(1));
    assertTrue(conditionModel.include(8));
    assertTrue(conditionModel.include(11));
    conditionModel.setOperator(Operator.BETWEEN_EXCLUSIVE);
    assertTrue(conditionModel.include(1));
    assertTrue(conditionModel.include(8));
    assertTrue(conditionModel.include(11));
    conditionModel.setOperator(Operator.NOT_BETWEEN);
    assertTrue(conditionModel.include(1));
    assertTrue(conditionModel.include(8));
    assertTrue(conditionModel.include(11));
    conditionModel.setOperator(Operator.NOT_BETWEEN_EXCLUSIVE);
    assertTrue(conditionModel.include(1));
    assertTrue(conditionModel.include(8));
    assertTrue(conditionModel.include(11));

    conditionModel.setEnabled(false);
    assertTrue(conditionModel.include((Comparable<Integer>) null));
    assertTrue(conditionModel.include(5));
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
  }

  @Test
  void includeString() {
    DefaultColumnFilterModel<String, String, String> conditionModel = new DefaultColumnFilterModel<>("test", String.class, '%');
    conditionModel.setAutoEnable(false);
    conditionModel.setEnabled(true);

    conditionModel.setOperator(Operator.EQUAL);
    conditionModel.setEqualValue("hello");
    assertTrue(conditionModel.include("hello"));
    conditionModel.setEqualValue("hell%");
    assertTrue(conditionModel.include("hello"));
    assertFalse(conditionModel.include("helo"));
    conditionModel.setEqualValue("%ell%");
    assertTrue(conditionModel.include("hello"));
    assertFalse(conditionModel.include("helo"));

    conditionModel.caseSensitiveState().set(false);
    assertTrue(conditionModel.include("HELlo"));
    assertFalse(conditionModel.include("heLo"));
    assertFalse(conditionModel.include(null));

    conditionModel.setEqualValue("%");
    assertTrue(conditionModel.include("hello"));
    assertTrue(conditionModel.include("helo"));

    conditionModel.caseSensitiveState().set(true);
    conditionModel.setEqualValue("hello");
    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertFalse(conditionModel.include("hello"));
    conditionModel.setEqualValue("hell%");
    assertFalse(conditionModel.include("hello"));
    assertTrue(conditionModel.include("helo"));
    conditionModel.setEqualValue("%ell%");
    assertFalse(conditionModel.include("hello"));
    assertTrue(conditionModel.include("helo"));

    conditionModel.caseSensitiveState().set(false);
    assertFalse(conditionModel.include("HELlo"));
    assertTrue(conditionModel.include("heLo"));
    assertTrue(conditionModel.include(null));

    conditionModel.setEqualValue("%");
    assertFalse(conditionModel.include("hello"));
    assertFalse(conditionModel.include("helo"));
  }
}
