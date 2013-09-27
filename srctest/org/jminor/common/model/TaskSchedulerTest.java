/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

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
    final Collection<Object> counter = new ArrayList<>();
    final TaskScheduler scheduler = new TaskScheduler(new Runnable() {
      @Override
      public void run() {
        counter.add(new Object());
      }
    }, 5, TimeUnit.MILLISECONDS);
    assertFalse(scheduler.isRunning());
    scheduler.start();
    assertTrue(scheduler.isRunning());
    Thread.sleep(25);
    assertTrue(scheduler.isRunning());
    assertFalse(counter.isEmpty());
    scheduler.stop();
    final int currentSize = counter.size();
    assertFalse(scheduler.isRunning());
    Thread.sleep(25);
    assertEquals(currentSize, counter.size());
    scheduler.start();
    assertTrue(scheduler.isRunning());
    Thread.sleep(25);
    assertTrue(scheduler.isRunning());
    assertTrue(counter.size() > currentSize);
    scheduler.stop();
    assertFalse(scheduler.isRunning());
  }
}
