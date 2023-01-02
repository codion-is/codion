/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.BoundedRangeModel;
import javax.swing.JProgressBar;

import static java.util.Objects.requireNonNull;

final class DefaultProgressBarBuilder extends AbstractComponentBuilder<Integer, JProgressBar, ProgressBarBuilder> implements ProgressBarBuilder {

  private final BoundedRangeModel boundedRangeModel;

  private boolean stringPainted;
  private int orientation;

  DefaultProgressBarBuilder(BoundedRangeModel boundedRangeModel) {
    this.boundedRangeModel = requireNonNull(boundedRangeModel);
  }

  @Override
  public ProgressBarBuilder stringPainted(boolean stringPainted) {
    this.stringPainted = stringPainted;
    return this;
  }

  @Override
  public ProgressBarBuilder orientation(int orientation) {
    this.orientation = orientation;
    return this;
  }

  @Override
  protected JProgressBar createComponent() {
    JProgressBar progressBar = new JProgressBar(boundedRangeModel);
    progressBar.setStringPainted(stringPainted);
    progressBar.setOrientation(orientation);

    return progressBar;
  }

  @Override
  protected ComponentValue<Integer, JProgressBar> createComponentValue(JProgressBar component) {
    return new IntegerProgressBarValue(component);
  }

  @Override
  protected void setInitialValue(JProgressBar component, Integer initialValue) {
    component.setValue(initialValue);
  }
}
