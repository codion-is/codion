/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

public final class AggregateStateImplTest {

  @Test
  public void test() {
    State.AggregateState orState = States.aggregateState(Conjunction.OR);
    final State stateOne = States.state();
    final State stateTwo = States.state();
    final State stateThree = States.state();
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);

    State.AggregateState andState = States.aggregateState(Conjunction.AND, stateOne, stateTwo, stateThree);
    assertEquals(Conjunction.AND, andState.getConjunction());
    assertEquals("Aggregate  and inactive, inactive, inactive, inactive", andState.toString());

    assertFalse("Or state should be inactive", orState.isActive());
    assertFalse("And state should be inactive", andState.isActive());

    stateOne.setActive(true);

    assertTrue("Or state should be active when one component state is active", orState.isActive());
    assertFalse("And state should be inactive when only one component state is active", andState.isActive());

    stateTwo.setActive(true);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    stateThree.setActive(true);

    assertTrue("Or state should be active when all component states are active", orState.isActive());
    assertTrue("And state should be active when all component states are active", andState.isActive());

    stateOne.setActive(false);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    andState.removeState(stateOne);
    assertTrue(andState.isActive());

    try {
      andState.setActive(false);
      fail("Can not set active on an aggregate state");
    }
    catch (UnsupportedOperationException e) {}

    stateOne.setActive(false);
    stateTwo.setActive(false);
    stateThree.setActive(false);
    orState = States.aggregateState(Conjunction.OR);
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);
    andState = States.aggregateState(Conjunction.AND, stateOne, stateTwo, stateThree);
    assertEquals("Aggregate  and inactive, inactive, inactive, inactive", andState.toString());

    assertFalse("Or state should be inactive", orState.isActive());
    assertFalse("And state should be inactive", andState.isActive());

    stateOne.setActive(true);

    assertTrue("Or state should be active when one component state is active", orState.isActive());
    assertFalse("And state should be inactive when only one component state is active", andState.isActive());

    stateTwo.setActive(true);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    stateThree.setActive(true);

    assertTrue("Or state should be active when all component states are active", orState.isActive());
    assertTrue("And state should be active when all component states are active", andState.isActive());

    stateOne.setActive(false);

    assertTrue("Or state should be active when two component states are active", orState.isActive());
    assertFalse("And state should be inactive when only two of three component states is active", andState.isActive());

    andState.removeState(stateOne);
    assertTrue(andState.isActive());

    stateTwo.setActive(false);
    assertTrue("Or state should be active when one component state is active", orState.isActive());
    stateThree.setActive(false);
    assertFalse("Or state should be inactive when no component state is active", orState.isActive());
    stateTwo.setActive(true);
    assertTrue("Or state should be active when one component state is active", orState.isActive());
  }
}
