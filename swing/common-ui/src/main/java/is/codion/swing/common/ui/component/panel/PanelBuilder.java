/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
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
   * @see JPanel#setLayout(LayoutManager)
   */
  PanelBuilder layout(LayoutManager layoutManager);

  /**
   * @param component the component to add
   * @return this builder instance
   * @see JPanel#add(Component)
   */
  PanelBuilder add(JComponent component);

  /**
   * @param component the component to add
   * @param constraints the layout constraints
   * @return this builder instance
   * @see JPanel#add(Component, Object)
   */
  PanelBuilder add(JComponent component, Object constraints);

  /**
   * @param components the components to add
   * @return this builder instance
   * @see JPanel#add(Component)
   */
  PanelBuilder addAll(JComponent... components);

  /**
   * @param components the components to add
   * @return this builder instance
   * @see JPanel#add(Component)
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
