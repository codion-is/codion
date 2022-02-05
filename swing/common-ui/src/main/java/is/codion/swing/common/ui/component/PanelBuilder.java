/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JPanel;

/**
 * Builds a JPanel instance.
 */
public interface PanelBuilder extends ComponentBuilder<Void, JPanel, PanelBuilder> {

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
}
