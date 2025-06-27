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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import javax.swing.JComponent;
import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

/**
 * Utility class for setting component sizes.
 */
public final class Sizes {

	private Sizes() {}

	/**
	 * Sets the preferred size of the given component to its current height and the given {@code preferredWidth}
	 * @param component the component
	 * @param preferredWidth the preferred width
	 * @param <T> the component type
	 * @return the component
	 */
	public static <T extends JComponent> T preferredWidth(T component, int preferredWidth) {
		requireNonNull(component).setPreferredSize(new Dimension(preferredWidth, component.getPreferredSize().height));

		return component;
	}

	/**
	 * Sets the preferred size of the given component to its current width and the given {@code preferredHeight}
	 * @param component the component
	 * @param preferredHeight the preferred height
	 * @param <T> the component type
	 * @return the component
	 */
	public static <T extends JComponent> T preferredHeight(T component, int preferredHeight) {
		requireNonNull(component).setPreferredSize(new Dimension(component.getPreferredSize().width, preferredHeight));

		return component;
	}

	/**
	 * Sets the minimum size of the given component to its current height and the given {@code minimumWidth}
	 * @param component the component
	 * @param minimumWidth the minimum width
	 * @param <T> the component type
	 * @return the component
	 */
	public static <T extends JComponent> T minimumWidth(T component, int minimumWidth) {
		requireNonNull(component).setMinimumSize(new Dimension(minimumWidth, component.getMinimumSize().height));

		return component;
	}

	/**
	 * Sets the minimum size of the given component to its current width and the given {@code minimumHeight}
	 * @param component the component
	 * @param minimumHeight the minimum height
	 * @param <T> the component type
	 * @return the component
	 */
	public static <T extends JComponent> T minimumHeight(T component, int minimumHeight) {
		requireNonNull(component).setMinimumSize(new Dimension(component.getMinimumSize().width, minimumHeight));

		return component;
	}

	/**
	 * Sets the maximum size of the given component to its current height and the given {@code maximumWidth}
	 * @param component the component
	 * @param maximumWidth the maximum width
	 * @param <T> the component type
	 * @return the component
	 */
	public static <T extends JComponent> T maximumWidth(T component, int maximumWidth) {
		requireNonNull(component).setMaximumSize(new Dimension(maximumWidth, component.getMaximumSize().height));

		return component;
	}

	/**
	 * Sets the maximum size of the given component to its current width and the given {@code maximumHeight}
	 * @param component the component
	 * @param maximumHeight the maximum height
	 * @param <T> the component type
	 * @return the component
	 */
	public static <T extends JComponent> T maximumHeight(T component, int maximumHeight) {
		requireNonNull(component).setMaximumSize(new Dimension(component.getMaximumSize().width, maximumHeight));

		return component;
	}
}
