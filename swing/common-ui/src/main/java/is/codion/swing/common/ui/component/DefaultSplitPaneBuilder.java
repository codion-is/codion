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
  public SplitPaneBuilder orientation(final int orientation) {
    this.orientation = orientation;
    return this;
  }

  @Override
  public SplitPaneBuilder oneTouchExpandable(final boolean oneTouchExpandable) {
    this.oneTouchExpandable = oneTouchExpandable;
    return this;
  }

  @Override
  public SplitPaneBuilder leftComponent(final JComponent leftComponent) {
    this.leftTopComponent = leftComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder rightComponent(final JComponent rightComponent) {
    this.rightBottomComponent = rightComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder topComponent(final JComponent topComponent) {
    this.leftTopComponent = topComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder bottomComponent(final JComponent bottomComponent) {
    this.rightBottomComponent = bottomComponent;
    return this;
  }

  @Override
  public SplitPaneBuilder resizeWeight(final double resizeWeight) {
    this.resizeWeight = resizeWeight;
    return this;
  }

  @Override
  protected JSplitPane buildComponent() {
    final JSplitPane splitPane = new JSplitPane(orientation);
    splitPane.setLeftComponent(leftTopComponent);
    splitPane.setRightComponent(rightBottomComponent);
    splitPane.setResizeWeight(resizeWeight);

    return splitPane;
  }

  @Override
  protected ComponentValue<Void, JSplitPane> buildComponentValue(final JSplitPane component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JSplitPane");
  }

  @Override
  protected void setInitialValue(final JSplitPane component, final Void initialValue) {}
}
