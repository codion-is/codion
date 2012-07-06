/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class EventImplTest {

  @Test
  public void test() throws Exception {
    final Event event = new Events.EventImpl();
    final List<Object> res = new ArrayList<Object>();
    final EventListener listener = new EventListener() {
      @Override
      public void eventOccurred(final ActionEvent e) {
        res.add(new Object());
      }
    };
    event.addListener(listener);
    event.fire();
    assertTrue("EventListener should have been notified on .fire()", res.size() == 1);
    event.eventOccurred(new ActionEvent(new Object(), -1, ""));
    assertTrue("EventListener should have been notified on .eventOccurred", res.size() == 2);
    event.removeListener(listener);
    event.fire();
    assertTrue("Removed EventListener should not have been notified", res.size() == 2);
  }
}
