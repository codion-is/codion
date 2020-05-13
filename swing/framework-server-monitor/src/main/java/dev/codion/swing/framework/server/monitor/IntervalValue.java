/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.framework.server.monitor;

import dev.codion.common.TaskScheduler;
import dev.codion.common.value.AbstractValue;

final class IntervalValue extends AbstractValue<Integer> {

  private final TaskScheduler scheduler;

  IntervalValue(final TaskScheduler scheduler) {
    this.scheduler = scheduler;
    this.scheduler.addIntervalListener(interval -> notifyValueChange());
  }

  @Override
  public void set(final Integer value) {
    scheduler.setInterval(value);
  }

  @Override
  public Integer get() {
    return scheduler.getInterval();
  }

  @Override
  public boolean isNullable() {
    return false;
  }
}
