/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JProgressBar;

final class IntegerProgressBarValue extends AbstractComponentValue<Integer, JProgressBar> {

  IntegerProgressBarValue(JProgressBar progressBar) {
    super(progressBar, 0);
    progressBar.getModel().addChangeListener(e -> notifyValueChange());
  }

  @Override
  protected Integer getComponentValue() {
    return getComponent().getValue();
  }

  @Override
  protected void setComponentValue(Integer value) {
    getComponent().setValue(value == null ? 0 : value);
  }
}
