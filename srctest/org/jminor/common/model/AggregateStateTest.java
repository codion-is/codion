/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.*;
import org.junit.Test;

public class AggregateStateTest {

  @Test
  public void test() {
    AggregateState orState = new AggregateState(AggregateState.Type.OR);
    final State stateOne = new State();
    final State stateTwo = new State();
    final State stateThree = new State();
    orState.addState(stateOne);
    orState.addState(stateTwo);
    orState.addState(stateThree);

    AggregateState andState = new AggregateState(AggregateState.Type.AND, stateOne, stateTwo, stateThree);
    assertEquals(AggregateState.Type.AND, andState.getType());
    assertEquals("Aggregate AND inactive, inactive, inactive, inactive", andState.toString());

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
    catch (Exception e) {}

    stateOne.setActive(false);
    stateTwo.setActive(false);
    stateThree.setActive(false);
    orState = new AggregateState(AggregateState.Type.OR);
    orState.addState(stateOne.getLinkedState());
    orState.addState(stateTwo);
    orState.addState(stateThree.getLinkedState().getLinkedState());
    andState = new AggregateState(AggregateState.Type.AND, stateOne.getLinkedState(), stateTwo.getLinkedState(), stateThree.getLinkedState());
    assertEquals("Aggregate AND inactive, inactive linked, inactive linked, inactive linked", andState.toString());

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

    andState.removeState(stateOne.getLinkedState());
    assertTrue(andState.isActive());

    stateTwo.setActive(false);
    assertTrue("Or state should be active when one component state is active", orState.isActive());
    stateThree.setActive(false);
    assertFalse("Or state should be inactive when no component state is active", orState.isActive());
    stateTwo.setActive(true);
    assertTrue("Or state should be active when one component state is active", orState.isActive());

    try {
      andState.setActive(false);
      fail("Can not set active on an aggregate state");
    }
    catch (Exception e) {}
  }
}
