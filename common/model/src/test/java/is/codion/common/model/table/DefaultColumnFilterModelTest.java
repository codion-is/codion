/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.model.table;

import is.codion.common.Operator;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DefaultColumnFilterModelTest {

  @Test
  void include() {
    final DefaultColumnFilterModel<String, String, Integer> conditionModel = new DefaultColumnFilterModel<>("test", Integer.class, "%");
    conditionModel.setEqualValue(10);
    conditionModel.setOperator(Operator.EQUAL);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setOperator(Operator.NOT_EQUAL);
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setLowerBound(10);
    conditionModel.setOperator(Operator.GREATER_THAN_OR_EQUAL);
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    conditionModel.setOperator(Operator.GREATER_THAN);
    assertFalse(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));

    conditionModel.setUpperBound(10);
    conditionModel.setOperator(Operator.LESS_THAN_OR_EQUAL);
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    conditionModel.setOperator(Operator.LESS_THAN);
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertFalse(conditionModel.include(11));

    conditionModel.setLowerBound(6);
    conditionModel.setOperator(Operator.BETWEEN);
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));
    conditionModel.setOperator(Operator.BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
    assertTrue(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertFalse(conditionModel.include(11));
    assertFalse(conditionModel.include(5));

    conditionModel.setOperator(Operator.NOT_BETWEEN);
    assertTrue(conditionModel.include(6));
    assertFalse(conditionModel.include(7));
    assertFalse(conditionModel.include(9));
    assertTrue(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    assertTrue(conditionModel.include(5));
    conditionModel.setOperator(Operator.NOT_BETWEEN_EXCLUSIVE);
    assertFalse(conditionModel.include(6));
    assertFalse(conditionModel.include(7));
    assertFalse(conditionModel.include(9));
    assertFalse(conditionModel.include(10));
    assertTrue(conditionModel.include(11));
    assertTrue(conditionModel.include(5));

    conditionModel.setEnabled(false);
    assertTrue(conditionModel.include(5));
    assertTrue(conditionModel.include(6));
    assertTrue(conditionModel.include(7));
  }
}
