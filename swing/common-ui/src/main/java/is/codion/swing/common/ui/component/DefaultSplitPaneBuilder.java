/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JSplitPane;

final class DefaultSplitPaneBuilder extends AbstractComponentBuilder<Void, JSplitPane, SplitPaneBuilder> implements SplitPaneBuilder {

  private int orientation = JSplitPane.HORIZONTAL_SPLIT;
  private boolean oneTouchExpandable = false;
  private JComponent leftTopComponent;
  private JComponent rightBottomComponent;
  private double resizeWeight;
  private boolean continuousLayout;
  private int dividerSize;

  @Override
  public SplitPaneBuilder orientation(int orientation) {
    this.orientation = orientation;
    return this;
  }

  @Override
  public SplitPaneBuilder oneTouchExpandable(boolean oneTouchExpandable) {
    this.oneTouchExpandable = oneTouchExpandable;
    return this;
  }

  @Override
  public SplitPaneBuilder leftComponent(JComponent leftComponent) {
    this.leftTopComponent = leftComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder rightComponent(JComponent rightComponent) {
    this.rightBottomComponent = rightComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder topComponent(JComponent topComponent) {
    this.leftTopComponent = topComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder bottomComponent(JComponent bottomComponent) {
    this.rightBottomComponent = bottomComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder resizeWeight(double resizeWeight) {
    this.resizeWeight = resizeWeight;
    return this;
  }

  @Override
  public SplitPaneBuilder continuousLayout(boolean continuousLayout) {
    this.continuousLayout = continuousLayout;
    return this;
  }

  @Override
  public SplitPaneBuilder dividerSize(int dividerSize) {
    this.dividerSize = dividerSize;
    return this;
  }

  @Override
  protected JSplitPane createComponent() {
    JSplitPane splitPane = new JSplitPane(orientation);
    splitPane.setLeftComponent(leftTopComponent);
    splitPane.setRightComponent(rightBottomComponent);
    splitPane.setResizeWeight(resizeWeight);
    splitPane.setOneTouchExpandable(oneTouchExpandable);
    splitPane.setContinuousLayout(continuousLayout);
    if (dividerSize > 0) {
      splitPane.setDividerSize(dividerSize);
    }

    return splitPane;
  }

  @Override
  protected ComponentValue<Void, JSplitPane> createComponentValue(JSplitPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JSplitPane");
  }

  @Override
  protected void setInitialValue(JSplitPane component, Void initialValue) {}
}
