/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.scheduler;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static is.codion.common.scheduler.TaskScheduler.taskScheduler;
import static org.junit.jupiter.api.Assertions.*;

public class TaskSchedulerTest {

  private final Runnable runnable = () -> {};

  @Test
  public void constructorNegativeInterval() {
    assertThrows(IllegalArgumentException.class, () -> taskScheduler(runnable, -1, TimeUnit.SECONDS));
  }

  @Test
  public void constructorNegativeInitialDelay() {
    assertThrows(IllegalArgumentException.class, () -> taskScheduler(runnable, 1, -1, TimeUnit.SECONDS));
  }

  @Test
  public void constructorNullTask() {
    assertThrows(NullPointerException.class, () -> taskScheduler(null, 1, 1, TimeUnit.SECONDS));
  }

  @Test
  public void constructorNullTimUnit() {
    assertThrows(NullPointerException.class, () -> taskScheduler(runnable, 1, 1, null));
  }

  @Test
  public void constructorNullThreadFactory() {
    assertThrows(NullPointerException.class, () -> taskScheduler(runnable, 1, 1, TimeUnit.SECONDS, null));
  }

  @Test
  public void setIntervalNegative() {
    assertThrows(IllegalArgumentException.class, () -> taskScheduler(runnable, 1, TimeUnit.SECONDS).setInterval(-1));
  }

  @Test
  public void startStop() throws InterruptedException {
    final AtomicInteger counter = new AtomicInteger();
    final TaskScheduler scheduler = taskScheduler(counter::incrementAndGet, 1, TimeUnit.MILLISECONDS);
    assertFalse(scheduler.isRunning());
    scheduler.start();
    assertTrue(scheduler.isRunning());
    Thread.sleep(100);
    assertTrue(scheduler.isRunning());
    assertNotEquals(0, counter.get());
    scheduler.stop();
    final int currentCount = counter.get();
    assertFalse(scheduler.isRunning());
    Thread.sleep(100);
    assertEquals(currentCount, counter.get());
    final AtomicInteger intervalCounter = new AtomicInteger();
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
