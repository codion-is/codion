/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Builds a JPanel instance using a BorderLayout.
 */
public interface BorderLayoutPanelBuilder extends ComponentBuilder<Void, JPanel, BorderLayoutPanelBuilder> {

  /**
   * @param centerComponent the {@link BorderLayout#CENTER} component
   * @return this builder instance
   */
  BorderLayoutPanelBuilder centerComponent(JComponent centerComponent);

  /**
   * @param northComponent the {@link BorderLayout#NORTH} component
   * @return this builder instance
   */
  BorderLayoutPanelBuilder northComponent(JComponent northComponent);

  /**
   * @param southComponent the {@link BorderLayout#SOUTH} component
   * @return this builder instance
   */
  BorderLayoutPanelBuilder southComponent(JComponent southComponent);

  /**
   * @param eastComponent the {@link BorderLayout#EAST} component
   * @return this builder instance
   */
  BorderLayoutPanelBuilder eastComponent(JComponent eastComponent);

  /**
   * @param westComponent the {@link BorderLayout#WEST} component
   * @return this builder instance
   */
  BorderLayoutPanelBuilder westComponent(JComponent westComponent);

  /**
   * Creates a new {@link BorderLayoutPanelBuilder} instance using a new
   * {@link BorderLayout} instance with the default horizontal and vertical gap.
   * @return a border layout panel builder
   * @see Layouts#GAP
   */
  static BorderLayoutPanelBuilder builder() {
    return builder(Layouts.borderLayout());
  }

  /**
   * @param layout the BorderLayout to use
   * @return a border layout panel builder
   */
  static BorderLayoutPanelBuilder builder(BorderLayout layout) {
    return new DefaultBorderLayoutPanelBuilder(layout);
  }
}
