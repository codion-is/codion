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
package is.codion.swing.common.ui.component.splitpane;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JSplitPane;
import java.awt.Component;

/**
 * A builder for JSplitPane.
 */
public interface SplitPaneBuilder extends ComponentBuilder<Void, JSplitPane, SplitPaneBuilder> {

	/**
	 * @param orientation the orientation
	 * @return this builder instance
	 * @see JSplitPane#setOrientation(int)
	 */
	SplitPaneBuilder orientation(int orientation);

	/**
	 * @param oneTouchExpandable one touch expandable
	 * @return this builder instance
	 * @see JSplitPane#setOneTouchExpandable(boolean)
	 */
	SplitPaneBuilder oneTouchExpandable(boolean oneTouchExpandable);

	/**
	 * @param leftComponent the left component
	 * @return this builder instance
	 * @see JSplitPane#setLeftComponent(Component)
	 */
	SplitPaneBuilder leftComponent(JComponent leftComponent);

	/**
	 * @param rightComponent the right component
	 * @return this builder instance
	 * @see JSplitPane#setRightComponent(Component)
	 */
	SplitPaneBuilder rightComponent(JComponent rightComponent);

	/**
	 * @param topComponent the top component
	 * @return this builder instance
	 * @see JSplitPane#setTopComponent(Component)
	 */
	SplitPaneBuilder topComponent(JComponent topComponent);

	/**
	 * @param bottomComponent the bottom component
	 * @return this builder instance
	 * @see JSplitPane#setBottomComponent(Component)
	 */
	SplitPaneBuilder bottomComponent(JComponent bottomComponent);

	/**
	 * @param resizeWeight the resize weight
	 * @return this builder instance
	 * @see JSplitPane#setResizeWeight(double)
	 */
	SplitPaneBuilder resizeWeight(double resizeWeight);

	/**
	 * @param continuousLayout the value of the continuousLayout
	 * @return this builder instance
	 * @see JSplitPane#setContinuousLayout(boolean)
	 */
	SplitPaneBuilder continuousLayout(boolean continuousLayout);

	/**
	 * @param dividerSize the divider size
	 * @return this builder instance
	 * @see JSplitPane#setDividerSize(int)
	 */
	SplitPaneBuilder dividerSize(int dividerSize);

	/**
	 * @return a new {@link SplitPaneBuilder} instance
	 */
	static SplitPaneBuilder builder() {
		return new DefaultSplitPaneBuilder();
	}
}
