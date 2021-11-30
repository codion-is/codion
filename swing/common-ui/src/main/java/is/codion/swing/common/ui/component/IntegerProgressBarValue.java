/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JProgressBar;

final class IntegerProgressBarValue extends AbstractComponentValue<Integer, JProgressBar> {

  IntegerProgressBarValue(final JProgressBar progressBar) {
    super(progressBar, 0);
    progressBar.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected Integer getComponentValue(final JProgressBar component) {
    return component.getValue();
  }

  @Override
  protected void setComponentValue(final JProgressBar component, final Integer value) {
    component.setValue(value == null ? 0 : value);
  }
}
