/*
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static java.util.Objects.requireNonNull;

final class DefaultBorderLayoutPanelBuilder extends AbstractComponentBuilder<Void, JPanel, BorderLayoutPanelBuilder> implements BorderLayoutPanelBuilder {

  private final BorderLayout layout;

  private JComponent centerComponent;
  private JComponent northComponent;
  private JComponent southComponent;
  private JComponent eastComponent;
  private JComponent westComponent;

  DefaultBorderLayoutPanelBuilder(BorderLayout layout) {
    this.layout = requireNonNull(layout);
  }

  @Override
  public BorderLayoutPanelBuilder centerComponent(JComponent centerComponent) {
    this.centerComponent = requireNonNull(centerComponent);
    return this;
  }

  @Override
  public BorderLayoutPanelBuilder northComponent(JComponent northComponent) {
    this.northComponent = requireNonNull(northComponent);
    return this;
  }

  @Override
  public BorderLayoutPanelBuilder southComponent(JComponent southComponent) {
    this.southComponent = requireNonNull(southComponent);
    return this;
  }

  @Override
  public BorderLayoutPanelBuilder eastComponent(JComponent eastComponent) {
    this.eastComponent = requireNonNull(eastComponent);
    return this;
  }

  @Override
  public BorderLayoutPanelBuilder westComponent(JComponent westComponent) {
    this.westComponent = requireNonNull(westComponent);
    return this;
  }

  @Override
  protected JPanel createComponent() {
    JPanel component = new JPanel(layout);
    if (centerComponent != null) {
      component.add(centerComponent, BorderLayout.CENTER);
    }
    if (northComponent != null) {
      component.add(northComponent, BorderLayout.NORTH);
    }
    if (southComponent != null) {
      component.add(southComponent, BorderLayout.SOUTH);
    }
    if (eastComponent != null) {
      component.add(eastComponent, BorderLayout.EAST);
    }
    if (westComponent != null) {
      component.add(westComponent, BorderLayout.WEST);
    }

    return component;
  }

  @Override
  protected ComponentValue<Void, JPanel> createComponentValue(JPanel component) {
    throw new UnsupportedOperationException("A ComponentValue can not be based on a JPanel");
  }

  @Override
  protected void setInitialValue(JPanel component, Void initialValue) {}
}
