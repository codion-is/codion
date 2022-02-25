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
  protected JSplitPane buildComponent() {
    JSplitPane splitPane = new JSplitPane(orientation);
    splitPane.setLeftComponent(leftTopComponent);
    splitPane.setRightComponent(rightBottomComponent);
    splitPane.setResizeWeight(resizeWeight);
    splitPane.setOneTouchExpandable(oneTouchExpandable);

    return splitPane;
  }

  @Override
  protected ComponentValue<Void, JSplitPane> buildComponentValue(JSplitPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JSplitPane");
  }

  @Override
  protected void setInitialValue(JSplitPane component, Void initialValue) {}
}
