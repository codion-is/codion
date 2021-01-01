/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.server.monitor;

import is.codion.common.TaskScheduler;
import is.codion.common.value.AbstractValue;

final class IntervalValue extends AbstractValue<Integer> {

  private final TaskScheduler scheduler;

  IntervalValue(final TaskScheduler scheduler) {
    super(0);
    this.scheduler = scheduler;
    this.scheduler.addIntervalListener(interval -> notifyValueChange());
  }

  @Override
  public Integer get() {
    return scheduler.getInterval();
  }

  @Override
  protected void doSet(final Integer value) {
    scheduler.setInterval(value);
  }
}
