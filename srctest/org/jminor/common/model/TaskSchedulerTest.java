/*
 * Copyright (c) 2004 - 2012, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public class TaskSchedulerTest {

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeInterval() {
    new TaskScheduler(new Runnable() {
      @Override
      public void run() {}
    }, -1, TimeUnit.SECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNegativeInitialDelay() {
    new TaskScheduler(new Runnable() {
      @Override
      public void run() {}
    }, 1, -1, TimeUnit.SECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullTask() {
    new TaskScheduler(null, 1, 1, TimeUnit.SECONDS);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullTimUnit() {
    new TaskScheduler(new Runnable() {
      @Override
      public void run() {}
    }, 1, 1, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullThreadFactory() {
    new TaskScheduler(new Runnable() {
      @Override
      public void run() {}
    }, 1, 1, TimeUnit.SECONDS, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void setIntervalNegative() {
    new TaskScheduler(new Runnable() {
      @Override
      public void run() {}
    }, 1, TimeUnit.SECONDS).setInterval(-1);
  }
}
