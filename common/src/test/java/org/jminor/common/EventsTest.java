/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;

public class EventsTest {

  @Test
  public void test() throws Exception {
    final Event event = Events.event();
    final AtomicInteger counter = new AtomicInteger();
    final EventListener listener = counter::incrementAndGet;
    event.addListener(listener);
    event.fire();
    assertTrue("EventListener should have been notified on .fire()", counter.get() == 1);
    event.eventOccurred();
    assertTrue("EventListener should have been notified on .eventOccurred", counter.get() == 2);
    event.removeListener(listener);
    event.fire();
    assertTrue("Removed EventListener should not have been notified", counter.get() == 2);
    Events.listener(Events.infoListener(listener));
  }
}
