/*
 * Copyright (c) 2022 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.ComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.Collection;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JPanel instance.
 */
public interface PanelBuilder extends ComponentBuilder<Void, JPanel, PanelBuilder> {

  /**
   * @param layoutManager the layout manager
   * @return this builder instance
   */
  PanelBuilder layout(LayoutManager layoutManager);

  /**
   * @param component the component to add
   * @return this builder instance
   */
  PanelBuilder add(JComponent component);

  /**
   * @param component the component to add
   * @param constraints the layout constraints
   * @return this builder instance
   */
  PanelBuilder add(JComponent component, Object constraints);

  /**
   * @param components the components to add
   * @return this builder instance
   */
  PanelBuilder addAll(JComponent... components);

  /**
   * @param components the components to add
   * @return this builder instance
   */
  PanelBuilder addAll(Collection<? extends JComponent> components);

  /**
   * @return a panel builder
   */
  static PanelBuilder builder() {
    return new DefaultPanelBuilder((LayoutManager) null);
  }

  /**
   * @param layout the panel layout manager
   * @return a panel builder
   */
  static PanelBuilder builder(LayoutManager layout) {
    return new DefaultPanelBuilder(requireNonNull(layout));
  }

  /**
   * @param panel the panel to configure
   * @return a panel builder
   */
  static PanelBuilder builder(JPanel panel) {
    return new DefaultPanelBuilder(panel);
  }
}
