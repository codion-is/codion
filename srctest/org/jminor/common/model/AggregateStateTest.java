/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class AggregateStateTest {

  @Test
  public void test() {
    final AggregateState orState = new AggregateState(AggregateState.Type.OR);
    final AggregateState andState = new AggregateState(AggregateState.Type.AND);
    final State stateOne = new State();
    final State stateTwo = new State();
    final State stateThree = new State();
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);
    andState.addState(stateOne);
    andState.addState(stateTwo);
    andState.addState(stateThree);
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
  }
}
