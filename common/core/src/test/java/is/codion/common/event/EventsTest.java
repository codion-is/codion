/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventsTest {

  @Test
  public void test() throws Exception {
    final Event<?> event = Event.event();
    final AtomicInteger counter = new AtomicInteger();
    final EventListener listener = counter::incrementAndGet;
    event.addListener(listener);
    event.onEvent();
    assertEquals(1, counter.get(), "EventListener should have been notified on onEvent()");
    event.onEvent();
    assertEquals(2, counter.get(), "EventListener should have been notified on onEvent()");
    event.removeListener(listener);
    event.onEvent();
    assertEquals(2, counter.get(), "Removed EventListener should not have been notified");
    Event.listener(Event.dataListener(listener));
  }
}
