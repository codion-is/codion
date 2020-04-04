package org.jminor.swing.framework.server.monitor;

import org.jminor.common.TaskScheduler;
import org.jminor.common.value.AbstractValue;

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
