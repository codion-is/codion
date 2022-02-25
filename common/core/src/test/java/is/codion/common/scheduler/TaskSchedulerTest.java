/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    assertThrows(IllegalArgumentException.class, () -> TaskScheduler.builder(runnable).interval(-1));
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
    assertThrows(NullPointerException.class, () -> TaskScheduler.builder(runnable).timeUnit(null));
  }

  @Test
  void constructorNullThreadFactory() {
    assertThrows(NullPointerException.class, () -> TaskScheduler.builder(runnable).threadFactory(null));
  }

  @Test
  void setIntervalNegative() {
    assertThrows(IllegalArgumentException.class, () -> TaskScheduler.builder(runnable).interval(1).timeUnit(TimeUnit.SECONDS).build().setInterval(-1));
  }

  @Test
  void startStop() throws InterruptedException {
    AtomicInteger counter = new AtomicInteger();
    TaskScheduler scheduler = TaskScheduler.builder(counter::incrementAndGet).interval(1).timeUnit(TimeUnit.MILLISECONDS).build();
    assertFalse(scheduler.isRunning());
    scheduler.start();
    assertTrue(scheduler.isRunning());
    Thread.sleep(100);
    assertTrue(scheduler.isRunning());
    assertNotEquals(0, counter.get());
    scheduler.stop();
    int currentCount = counter.get();
    assertFalse(scheduler.isRunning());
    Thread.sleep(100);
    assertEquals(currentCount, counter.get());
    AtomicInteger intervalCounter = new AtomicInteger();
    scheduler.addIntervalListener(interval -> intervalCounter.incrementAndGet());
    scheduler.setInterval(2);//still stopped
    assertEquals(1, intervalCounter.get());
    assertFalse(scheduler.isRunning());
    scheduler.start();
    assertTrue(scheduler.isRunning());
    Thread.sleep(100);
    assertTrue(scheduler.isRunning());
    assertTrue(counter.get() > currentCount);
    scheduler.stop();
    assertFalse(scheduler.isRunning());
  }
}
