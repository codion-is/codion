/*
 * Copyright (c) 2012 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TaskSchedulerTest {

  private final Runnable runnable = () -> {};

  @Test
  void constructorNegativeInterval() {
    assertThrows(IllegalArgumentException.class, () -> TaskScheduler.builder(runnable).interval(-1, TimeUnit.SECONDS));
  }

  @Test
  void constructorNegativeInitialDelay() {
    assertThrows(IllegalArgumentException.class, () -> TaskScheduler.builder(runnable).initialDelay(-1));
  }

  @Test
  void constructorNullTask() {
    assertThrows(NullPointerException.class, () -> TaskScheduler.builder(null));
  }

  @Test
  void constructorNullTimUnit() {
    assertThrows(NullPointerException.class, () -> TaskScheduler.builder(runnable).interval(1, null));
  }

  @Test
  void constructorNullThreadFactory() {
    assertThrows(NullPointerException.class, () -> TaskScheduler.builder(runnable).threadFactory(null));
  }

  @Test
  void setIntervalNegative() {
    assertThrows(IllegalArgumentException.class, () -> TaskScheduler.builder(runnable).interval(1, TimeUnit.SECONDS).build().interval().set(-1));
  }

  @Test
  void startStop() throws InterruptedException {
    AtomicInteger counter = new AtomicInteger();
    TaskScheduler scheduler = TaskScheduler.builder(counter::incrementAndGet).interval(1, TimeUnit.MILLISECONDS).build();
    assertFalse(scheduler.running());
    assertEquals(TimeUnit.MILLISECONDS, scheduler.timeUnit());
    scheduler.start();
    assertTrue(scheduler.running());
    Thread.sleep(100);
    assertTrue(scheduler.running());
    assertNotEquals(0, counter.get());
    scheduler.stop();
    int currentCount = counter.get();
    assertFalse(scheduler.running());
    Thread.sleep(100);
    assertEquals(currentCount, counter.get());
    scheduler.interval().set(2);//still stopped
    assertFalse(scheduler.running());
    scheduler.start();
    assertTrue(scheduler.running());
    Thread.sleep(100);
    assertTrue(scheduler.running());
    assertTrue(counter.get() > currentCount);
    scheduler.stop();
    assertFalse(scheduler.running());
  }
}
