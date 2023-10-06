/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.progressbar;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.BoundedRangeModel;
import javax.swing.JProgressBar;

final class DefaultProgressBarBuilder extends AbstractComponentBuilder<Integer, JProgressBar, ProgressBarBuilder> implements ProgressBarBuilder {

  private final BoundedRangeModel boundedRangeModel;

  private boolean borderPainted;
  private boolean stringPainted;
  private int orientation;
  private boolean indeterminate;
  private String string;

  DefaultProgressBarBuilder(BoundedRangeModel boundedRangeModel) {
    this.boundedRangeModel = boundedRangeModel;
    this.indeterminate = boundedRangeModel == null;
  }

  @Override
  public ProgressBarBuilder string(String string) {
    this.string = string;
    return this;
  }

  @Override
  public ProgressBarBuilder borderPainted(boolean borderPainted) {
    this.borderPainted = borderPainted;
    return this;
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
  public ProgressBarBuilder indeterminate(boolean indeterminate) {
    this.indeterminate = indeterminate;
    return this;
  }

  @Override
  protected JProgressBar createComponent() {
    JProgressBar progressBar = new JProgressBar(boundedRangeModel);
    progressBar.setBorderPainted(borderPainted);
    progressBar.setString(string);
    progressBar.setStringPainted(stringPainted);
    progressBar.setOrientation(orientation);
    progressBar.setIndeterminate(indeterminate);

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

  private static final class IntegerProgressBarValue extends AbstractComponentValue<Integer, JProgressBar> {

    private IntegerProgressBarValue(JProgressBar progressBar) {
      super(progressBar, 0);
      progressBar.getModel().addChangeListener(e -> notifyListeners());
    }

    @Override
    protected Integer getComponentValue() {
      return component().getValue();
    }

    @Override
    protected void setComponentValue(Integer value) {
      component().setValue(value == null ? 0 : value);
    }
  }
}
