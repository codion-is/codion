/*
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.event;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EventsTest {

  @Test
  void test() {
    Event<Integer> event = Event.event();
    AtomicInteger counter = new AtomicInteger();
    Runnable listener = counter::incrementAndGet;
    Consumer<Integer> dataListener = value -> {};
    event.addListener(listener);
    event.addDataListener(dataListener);
    event.run();
    assertEquals(1, counter.get(), "Listener should have been notified on onEvent()");
    event.run();
    assertEquals(2, counter.get(), "Listener should have been notified on onEvent()");
    event.removeListener(listener);
    event.run();
    assertEquals(2, counter.get(), "Removed listener should not have been notified");
    event.removeDataListener(dataListener);
    Event.listener(Event.dataListener(listener));
  }

  @Test
  void weakListeners() {
    Event<Integer> event = Event.event();
    Runnable listener = () -> {};
    Consumer<Integer> dataListener = integer -> {};
    event.addWeakListener(listener);
    event.addWeakListener(listener);
    event.addWeakDataListener(dataListener);
    event.addWeakDataListener(dataListener);
    event.accept(1);
    event.removeWeakListener(listener);
    event.removeWeakDataListener(dataListener);
  }
}
