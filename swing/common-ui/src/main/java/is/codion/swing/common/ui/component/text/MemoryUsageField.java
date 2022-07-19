/*
 * Copyright (c) 2021 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.Memory;
import is.codion.common.scheduler.TaskScheduler;

import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.util.concurrent.TimeUnit;

/**
 * A text field containing information about the memory usage in KB.
 */
public final class MemoryUsageField extends JTextField {

  /**
   * @param updateIntervalMilliseconds the update interval
   */
  public MemoryUsageField(int updateIntervalMilliseconds) {
    super(8);
    setEditable(false);
    setHorizontalAlignment(SwingConstants.CENTER);
    TaskScheduler.builder(() -> SwingUtilities.invokeLater(() -> setText(Memory.getMemoryUsage())))
            .interval(updateIntervalMilliseconds)
            .timeUnit(TimeUnit.MILLISECONDS)
            .start();
  }
}
