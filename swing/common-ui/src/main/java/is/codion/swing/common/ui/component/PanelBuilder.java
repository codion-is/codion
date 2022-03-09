/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.Collection;

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
}
