/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

public class EventImplTest {

  @Test
  public void test() throws Exception {
    final Event event = new Events.EventImpl();
    final List<Object> res = new ArrayList<Object>();
    final ActionListener listener = new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        res.add(new Object());
      }
    };
    event.addListener(listener);
    event.fire();
    assertTrue("ActionListener should have been notified on .fire()", res.size() == 1);
    event.actionPerformed(new ActionEvent(new Object(), -1, ""));
    assertTrue("ActionListener should have been notified on .actionPerformed", res.size() == 2);
    event.removeListener(listener);
    event.fire();
    assertTrue("Removed ActionListener should not have been notified", res.size() == 2);
  }
}
