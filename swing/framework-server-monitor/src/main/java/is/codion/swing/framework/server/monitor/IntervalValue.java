/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.scheduler.TaskScheduler;
import is.codion.common.value.AbstractValue;

final class IntervalValue extends AbstractValue<Integer> {

  private final TaskScheduler scheduler;

  IntervalValue(TaskScheduler scheduler) {
    super(0);
    this.scheduler = scheduler;
    this.scheduler.addIntervalListener(interval -> notifyValueChange());
  }

  @Override
  public Integer get() {
    return scheduler.getInterval();
  }

  @Override
  protected void setValue(Integer value) {
    scheduler.setInterval(value);
  }
}
