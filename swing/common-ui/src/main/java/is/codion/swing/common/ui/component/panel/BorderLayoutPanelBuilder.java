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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.JComponent;
import java.awt.BorderLayout;
import java.util.function.Supplier;

/**
 * Builds a {@link BorderLayout} based panel.
 */
public interface BorderLayoutPanelBuilder extends PanelBuilder<BorderLayout, BorderLayoutPanelBuilder> {

	/**
	 * @param centerComponent the {@link BorderLayout#CENTER} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder center(JComponent centerComponent);

	/**
	 * @param centerComponent the {@link BorderLayout#CENTER} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder center(Supplier<? extends JComponent> centerComponent);

	/**
	 * @param northComponent the {@link BorderLayout#NORTH} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder north(JComponent northComponent);

	/**
	 * @param northComponent the {@link BorderLayout#NORTH} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder north(Supplier<? extends JComponent> northComponent);

	/**
	 * @param southComponent the {@link BorderLayout#SOUTH} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder south(JComponent southComponent);

	/**
	 * @param southComponent the {@link BorderLayout#SOUTH} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder south(Supplier<? extends JComponent> southComponent);

	/**
	 * @param eastComponent the {@link BorderLayout#EAST} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder east(JComponent eastComponent);

	/**
	 * @param eastComponent the {@link BorderLayout#EAST} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder east(Supplier<? extends JComponent> eastComponent);

	/**
	 * @param westComponent the {@link BorderLayout#WEST} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder west(JComponent westComponent);

	/**
	 * @param westComponent the {@link BorderLayout#WEST} component
	 * @return this builder instance
	 */
	BorderLayoutPanelBuilder west(Supplier<? extends JComponent> westComponent);

	/**
	 * Creates a new {@link BorderLayoutPanelBuilder} instance using a new
	 * {@link BorderLayout} instance with the default horizontal and vertical gap.
	 * @return a border layout panel builder
	 * @see Layouts#GAP
	 */
	static BorderLayoutPanelBuilder builder() {
		return new DefaultBorderLayoutPanelBuilder();
	}
}
