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
 * Copyright (c) 2023, Björn Darri Sigurðsson.
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
