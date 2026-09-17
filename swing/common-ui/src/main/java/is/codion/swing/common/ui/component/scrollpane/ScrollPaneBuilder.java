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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.scrollpane;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JScrollPane;
import java.awt.LayoutManager;
import java.util.function.Supplier;

/**
 * A builder for JScrollPane
 */
public interface ScrollPaneBuilder extends ComponentBuilder<JScrollPane, ScrollPaneBuilder> {

	/**
	 * @param view the view component
	 * @return this builder instance
	 */
	ScrollPaneBuilder view(JComponent view);

	/**
	 * @param view the view component
	 * @return this builder instance
	 */
	ScrollPaneBuilder view(Supplier<? extends JComponent> view);

	/**
	 * @param verticalScrollBarPolicy the vertical scroll bar policy
	 * @return this builder instance
	 * @see JScrollPane#setVerticalScrollBarPolicy(int)
	 */
	ScrollPaneBuilder verticalScrollBarPolicy(int verticalScrollBarPolicy);

	/**
	 * @param horizontalScrollBarPolicy the horizontal scroll bar policy
	 * @return this builder instance
	 * @see JScrollPane#setHorizontalScrollBarPolicy(int)
	 */
	ScrollPaneBuilder horizontalScrollBarPolicy(int horizontalScrollBarPolicy);

	/**
	 * @param verticalUnitIncrement the unit increment for the vertical scrollbar
	 * @return this builder instance
	 * @see javax.swing.JScrollBar#setUnitIncrement(int)
	 */
	ScrollPaneBuilder verticalUnitIncrement(int verticalUnitIncrement);

	/**
	 * @param horizontalUnitIncrement the unit increment for the horizontal scrollbar
	 * @return this builder instance
	 * @see javax.swing.JScrollBar#setUnitIncrement(int)
	 */
	ScrollPaneBuilder horizontalUnitIncrement(int horizontalUnitIncrement);

	/**
	 * @param verticalBlockIncrement the block increment for the vertical scrollbar
	 * @return this builder instance
	 * @see javax.swing.JScrollBar#setBlockIncrement(int)
	 */
	ScrollPaneBuilder verticalBlockIncrement(int verticalBlockIncrement);

	/**
	 * @param horizontalBlockIncrement the block increment for the horizontal scrollbar
	 * @return this builder instance
	 * @see javax.swing.JScrollBar#setBlockIncrement(int)
	 */
	ScrollPaneBuilder horizontalBlockIncrement(int horizontalBlockIncrement);

	/**
	 * @param wheelScrollingEnabled wheel scrolling enabled
	 * @return this builder instance
	 */
	ScrollPaneBuilder wheelScrollingEnable(boolean wheelScrollingEnabled);

	/**
	 * @param layout the layout manager
	 * @return this builder instance
	 * @see JScrollPane#setLayout(LayoutManager)
	 */
	ScrollPaneBuilder layout(@Nullable LayoutManager layout);

	/**
	 * The resulting scroll pane follows the horizontal scrolling of the given one, the way a filter
	 * or summary panel follows the columns of a table, typically without scroll bars of its own, see
	 * {@link javax.swing.ScrollPaneConstants#HORIZONTAL_SCROLLBAR_NEVER}.
	 * <p>Note that:
	 * <ul>
	 * <li>The following is one way, the given scroll pane leads. Scrolling the follower directly, such as via
	 * {@link JComponent#scrollRectToVisible(java.awt.Rectangle)} from within its view, is not reflected in the leader,
	 * the two ending up misaligned until the leader is next scrolled, so scroll the leader instead.
	 * <li>The view of the follower must be as wide as the view of the leader, plus the width of the vertical
	 * scroll bar of the leader, in case it is displayed, the follower being that much wider than the leader's
	 * viewport, the two otherwise ending up misaligned when scrolled to the end.
	 * <li>The leader holds on to the follower, the two are expected to share a lifetime.
	 * </ul>
	 * @param scrollPane the scroll pane which horizontal scrolling to follow, null for none
	 * @return this builder instance
	 */
	ScrollPaneBuilder followHorizontal(@Nullable JScrollPane scrollPane);

	/**
	 * @return a new {@link ScrollPaneBuilder} instance
	 */
	static ScrollPaneBuilder builder() {
		return new DefaultScrollPaneBuilder();
	}
}
