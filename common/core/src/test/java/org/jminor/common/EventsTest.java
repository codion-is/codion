/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventsTest {

  @Test
  public void test() throws Exception {
    final Event event = Events.event();
    final AtomicInteger counter = new AtomicInteger();
    final EventListener listener = counter::incrementAndGet;
    event.addListener(listener);
    event.fire();
    assertEquals(1, counter.get(), "EventListener should have been notified on .fire()");
    event.eventOccurred();
    assertEquals(2, counter.get(), "EventListener should have been notified on .eventOccurred");
    event.removeListener(listener);
    event.fire();
    assertEquals(2, counter.get(), "Removed EventListener should not have been notified");
    Events.listener(Events.dataListener(listener));
  }
}
