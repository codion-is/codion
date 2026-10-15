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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.button.ButtonPanelBuilder.Separator;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * Lays out a row or a column of buttons, each separated by a gap. With fixed button sizes, the buttons are all
 * sized like the largest one, anything else, separators and nested controls, keeping its own size along the axis.
 * Everything is stretched across the axis, and any extra space along it is shared by the buttons.
 * <p>Nested controls are panels laid out by this layout as well, the outermost one sizing the buttons of all of
 * them, so that the buttons of nested controls are sized like the others, and sharing the extra space by the button.
 */
final class ButtonPanelLayout implements LayoutManager {

	// how far a separator line stops short of the ends of the buttons
	private static final int LINE_INSET = 3;

	private final boolean horizontal;
	private final int gap;
	private final boolean fixedButtonSize;

	ButtonPanelLayout(int orientation, int gap, boolean fixedButtonSize) {
		this.horizontal = orientation == SwingConstants.HORIZONTAL;
		this.gap = gap;
		this.fixedButtonSize = fixedButtonSize;
	}

	/**
	 * A separator for a panel with the given orientation, its space on each side the default gap, rather than the
	 * button gap, which may be zero.
	 * @param separator the separator type
	 * @param orientation the panel orientation
	 * @return a new separator component
	 */
	static JComponent separator(Separator separator, int orientation) {
		requireNonNull(separator);
		boolean horizontal = orientation == SwingConstants.HORIZONTAL;
		int space = Layouts.GAP.getOrThrow();
		SeparatorPanel panel = new SeparatorPanel();
		if (separator == Separator.LINE) {
			panel.setBorder(horizontal ?
							createEmptyBorder(LINE_INSET, space, LINE_INSET, space) :
							createEmptyBorder(space, LINE_INSET, space, LINE_INSET));
			panel.add(new JSeparator(horizontal ? SwingConstants.VERTICAL : SwingConstants.HORIZONTAL), BorderLayout.CENTER);
		}
		else {
			panel.setBorder(horizontal ?
							createEmptyBorder(0, space, 0, space) :
							createEmptyBorder(space, 0, space, 0));
		}

		return panel;
	}

	@Override
	public void addLayoutComponent(String name, Component component) {}

	@Override
	public void removeLayoutComponent(Component component) {}

	@Override
	public Dimension preferredLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			return size(parent, Component::getPreferredSize, buttonSize(parent, Component::getPreferredSize));
		}
	}

	@Override
	public Dimension minimumLayoutSize(Container parent) {
		synchronized (parent.getTreeLock()) {
			return size(parent, Component::getMinimumSize, buttonSize(parent, Component::getMinimumSize));
		}
	}

	@Override
	public void layoutContainer(Container parent) {
		synchronized (parent.getTreeLock()) {
			List<Component> components = visibleComponents(parent);
			if (components.isEmpty()) {
				return;
			}
			int buttonSize = buttonSize(parent, Component::getPreferredSize);
			int[] sizes = components.stream()
							.mapToInt(component -> along(size(component, Component::getPreferredSize, buttonSize)))
							.toArray();
			Insets insets = parent.getInsets();
			int along = horizontal ?
							parent.getWidth() - insets.left - insets.right :
							parent.getHeight() - insets.top - insets.bottom;
			distributeExtraSpace(components, sizes, along - total(sizes));
			int across = horizontal ?
							parent.getHeight() - insets.top - insets.bottom :
							parent.getWidth() - insets.left - insets.right;
			int position = horizontal ? insets.left : insets.top;
			for (int i = 0; i < components.size(); i++) {
				if (horizontal) {
					components.get(i).setBounds(position, insets.top, sizes[i], across);
				}
				else {
					components.get(i).setBounds(insets.left, position, across, sizes[i]);
				}
				position += sizes[i] + gap;
			}
		}
	}

	/**
	 * The size of a panel laid out by this layout, computed from its components rather than taken from the panel,
	 * since a panel caches its preferred size until it is invalidated, which a change outside it does not do.
	 */
	private Dimension size(Container container, Function<Component, Dimension> dimension, int buttonSize) {
		List<Component> components = visibleComponents(container);
		int[] sizes = new int[components.size()];
		int across = 0;
		for (int i = 0; i < components.size(); i++) {
			Dimension size = size(components.get(i), dimension, buttonSize);
			sizes[i] = along(size);
			across = Math.max(across, across(size));
		}
		int along = total(sizes);
		Insets insets = container.getInsets();

		return horizontal ?
						new Dimension(along + insets.left + insets.right, across + insets.top + insets.bottom) :
						new Dimension(across + insets.left + insets.right, along + insets.top + insets.bottom);
	}

	private Dimension size(Component component, Function<Component, Dimension> dimension, int buttonSize) {
		ButtonPanelLayout nested = layout(component);
		if (nested != null) {
			return nested.size((Container) component, dimension, buttonSize);
		}
		Dimension size = dimension.apply(component);
		if (fixedButtonSize && component instanceof AbstractButton) {
			return horizontal ? new Dimension(buttonSize, size.height) : new Dimension(size.width, buttonSize);
		}

		return size;
	}

	/**
	 * @return the size of the largest button along the axis, of the outermost panel and the nested ones
	 */
	private static int buttonSize(Container parent, Function<Component, Dimension> dimension) {
		Container outermost = parent;
		while (outermost.getParent() != null && layout(outermost.getParent()) != null) {
			outermost = outermost.getParent();
		}

		return requireNonNull(layout(outermost)).largestButton(outermost, dimension);
	}

	private int largestButton(Container container, Function<Component, Dimension> dimension) {
		int largest = 0;
		for (Component component : visibleComponents(container)) {
			ButtonPanelLayout nested = layout(component);
			if (nested != null) {
				largest = Math.max(largest, nested.largestButton((Container) component, dimension));
			}
			else if (component instanceof AbstractButton) {
				largest = Math.max(largest, along(dimension.apply(component)));
			}
		}

		return largest;
	}

	private int total(int[] sizes) {
		int total = 0;
		for (int size : sizes) {
			total += size;
		}

		return total + Math.max(0, sizes.length - 1) * gap;
	}

	/**
	 * Shares the extra space, or the lack of it, by the button: each button gets a share, nested controls one for each
	 * of their buttons, anything else, separators included, none, unless there are no buttons, when all but the
	 * separators share it evenly.
	 */
	private static void distributeExtraSpace(List<Component> components, int[] sizes, int extraSpace) {
		if (extraSpace == 0) {
			return;
		}
		int[] shares = new int[components.size()];
		int totalShares = 0;
		for (int i = 0; i < components.size(); i++) {
			shares[i] = buttons(components.get(i));
			totalShares += shares[i];
		}
		if (totalShares == 0) {
			for (int i = 0; i < components.size(); i++) {
				shares[i] = components.get(i) instanceof SeparatorPanel ? 0 : 1;
				totalShares += shares[i];
			}
		}
		if (totalShares == 0) {
			return;
		}
		int distributed = 0;
		int sharesSoFar = 0;
		for (int i = 0; i < components.size(); i++) {
			if (shares[i] > 0) {
				sharesSoFar += shares[i];
				// rounded so that the shares add up to the whole
				int upToHere = (int) ((long) extraSpace * sharesSoFar / totalShares);
				sizes[i] = Math.max(0, sizes[i] + upToHere - distributed);
				distributed = upToHere;
			}
		}
	}

	/**
	 * @return 1 for a button, the number of buttons in nested controls, 0 for anything else
	 */
	private static int buttons(Component component) {
		if (component instanceof AbstractButton) {
			return 1;
		}
		if (layout(component) != null) {
			return visibleComponents((Container) component).stream()
							.mapToInt(ButtonPanelLayout::buttons)
							.sum();
		}

		return 0;
	}

	private int along(Dimension size) {
		return horizontal ? size.width : size.height;
	}

	private int across(Dimension size) {
		return horizontal ? size.height : size.width;
	}

	private static @Nullable ButtonPanelLayout layout(Component component) {
		if (component instanceof Container && ((Container) component).getLayout() instanceof ButtonPanelLayout) {
			return (ButtonPanelLayout) ((Container) component).getLayout();
		}

		return null;
	}

	private static List<Component> visibleComponents(Container parent) {
		List<Component> components = new ArrayList<>(parent.getComponentCount());
		for (Component component : parent.getComponents()) {
			if (component.isVisible()) {
				components.add(component);
			}
		}

		return components;
	}

	private static final class SeparatorPanel extends JPanel {

		private SeparatorPanel() {
			super(new BorderLayout());
			setOpaque(false);
		}
	}
}
