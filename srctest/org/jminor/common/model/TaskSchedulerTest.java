/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class TaskSchedulerTest {

  private final Runnable runnable = new Runnable() {
    @Override
    public void run() {}
  };

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeInterval() {
    new TaskScheduler(runnable, -1, TimeUnit.SECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeInitialDelay() {
    new TaskScheduler(runnable, 1, -1, TimeUnit.SECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullTask() {
    new TaskScheduler(null, 1, 1, TimeUnit.SECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullTimUnit() {
    new TaskScheduler(runnable, 1, 1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullThreadFactory() {
    new TaskScheduler(runnable, 1, 1, TimeUnit.SECONDS, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setIntervalNegative() {
    new TaskScheduler(runnable, 1, TimeUnit.SECONDS).setInterval(-1);
  }

  @Test
  public void startStop() throws InterruptedException {
    final AtomicInteger counter = new AtomicInteger();
    final TaskScheduler scheduler = new TaskScheduler(new Runnable() {
      @Override
      public void run() {
        counter.incrementAndGet();
      }
    }, 5, TimeUnit.MILLISECONDS);
    assertFalse(scheduler.isRunning());
    scheduler.start();
    assertTrue(scheduler.isRunning());
    Thread.sleep(25);
    assertTrue(scheduler.isRunning());
    assertFalse(counter.get() == 0);
    scheduler.stop();
    final int currentCount = counter.get();
    assertFalse(scheduler.isRunning());
    Thread.sleep(25);
    assertEquals(currentCount, counter.get());
    final AtomicInteger intervalCounter = new AtomicInteger(0);
    scheduler.getIntervalObserver().addListener(new EventListener() {
      @Override
      public void eventOccurred() {
        intervalCounter.incrementAndGet();
      }
    });
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
