/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventsTest {

  @Test
  void test() throws Exception {
    Event<Integer> event = Event.event();
    AtomicInteger counter = new AtomicInteger();
    EventListener listener = counter::incrementAndGet;
    EventDataListener<Integer> dataListener = value -> {};
    event.addListener(listener);
    event.addDataListener(dataListener);
    event.onEvent();
    assertEquals(1, counter.get(), "EventListener should have been notified on onEvent()");
    event.onEvent();
    assertEquals(2, counter.get(), "EventListener should have been notified on onEvent()");
    event.removeListener(listener);
    event.onEvent();
    assertEquals(2, counter.get(), "Removed EventListener should not have been notified");
    event.removeDataListener(dataListener);
    Event.listener(Event.dataListener(listener));
  }
}
