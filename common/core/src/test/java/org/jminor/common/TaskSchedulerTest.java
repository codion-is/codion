/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class TaskSchedulerTest {

  private final Runnable runnable = () -> {};

  @Test
  public void constructorNegativeInterval() {
    assertThrows(IllegalArgumentException.class, () -> new TaskScheduler(runnable, -1, TimeUnit.SECONDS));
  }

  @Test
  public void constructorNegativeInitialDelay() {
    assertThrows(IllegalArgumentException.class, () -> new TaskScheduler(runnable, 1, -1, TimeUnit.SECONDS));
  }

  @Test
  public void constructorNullTask() {
    assertThrows(NullPointerException.class, () -> new TaskScheduler(null, 1, 1, TimeUnit.SECONDS));
  }

  @Test
  public void constructorNullTimUnit() {
    assertThrows(NullPointerException.class, () -> new TaskScheduler(runnable, 1, 1, null));
  }

  @Test
  public void constructorNullThreadFactory() {
    assertThrows(NullPointerException.class, () -> new TaskScheduler(runnable, 1, 1, TimeUnit.SECONDS, null));
  }

  @Test
  public void setIntervalNegative() {
    assertThrows(IllegalArgumentException.class, () -> new TaskScheduler(runnable, 1, TimeUnit.SECONDS).setInterval(-1));
  }

  @Test
  public void startStop() throws InterruptedException {
    final AtomicInteger counter = new AtomicInteger();
    final TaskScheduler scheduler = new TaskScheduler(counter::incrementAndGet, 5, TimeUnit.MILLISECONDS);
    assertFalse(scheduler.isRunning());
    scheduler.start();
    assertTrue(scheduler.isRunning());
    Thread.sleep(25);
    assertTrue(scheduler.isRunning());
    assertNotEquals(0, counter.get());
    scheduler.stop();
    final int currentCount = counter.get();
    assertFalse(scheduler.isRunning());
    Thread.sleep(25);
    assertEquals(currentCount, counter.get());
    final AtomicInteger intervalCounter = new AtomicInteger(0);
    scheduler.getIntervalObserver().addListener(intervalCounter::incrementAndGet);
    scheduler.setInterval(4);//implicit start
    assertEquals(1, intervalCounter.get());
    assertTrue(scheduler.isRunning());
    Thread.sleep(25);
    assertTrue(scheduler.isRunning());
    assertTrue(counter.get() > currentCount);
    scheduler.stop();
    assertFalse(scheduler.isRunning());
  }
}
