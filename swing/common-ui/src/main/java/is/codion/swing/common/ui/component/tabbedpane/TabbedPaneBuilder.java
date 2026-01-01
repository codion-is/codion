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
package is.codion.swing.common.ui.component.tabbedpane;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.event.ChangeListener;
import java.awt.Component;
import java.util.function.Supplier;

/**
 * A builder for a JTabbedPane.
 * {@snippet :
 * Components.tabbedPane()
 *         .tab("First Tab", new JLabel("First"))
 *         .tab("Second Tab", new JLabel("Second"))
 *         .build();
 *
 * Components.tabbedPane()
 *         .tabPlacement(SwingConstants.TOP)
 *         .tabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT)
 *         .tab("First Tab")
 *         .component(new JLabel("First"))
 *         .mnemonic(KeyEvent.VK_1)
 *         .toolTipText("This is the first tab")
 *         .icon(firstTabIcon)
 *         .add()
 *         .tab("Second Tab")
 *         .component(new JLabel("Second"))
 *         .mnemonic(KeyEvent.VK_2)
 *         .toolTipText("This is the second tab")
 *         .icon(secondTabIcon)
 *         .add()
 *         .build();
 *}
 */
public interface TabbedPaneBuilder extends ComponentBuilder<JTabbedPane, TabbedPaneBuilder> {

	/**
	 * @param tabPlacement the tab placement
	 * @return this builder instance
	 * @see JTabbedPane#setTabPlacement(int)
	 */
	TabbedPaneBuilder tabPlacement(int tabPlacement);

	/**
	 * @param tabLayoutPolicy the tab layout policy
	 * @return this builder instance
	 * @see JTabbedPane#setTabLayoutPolicy(int)
	 */
	TabbedPaneBuilder tabLayoutPolicy(int tabLayoutPolicy);

	/**
	 * @param changeListener the change listener
	 * @return this builder instance
	 * @see JTabbedPane#addChangeListener(ChangeListener)
	 */
	TabbedPaneBuilder changeListener(ChangeListener changeListener);

	/**
	 * Adds a tab to this tabbed pane builder.
	 * For further tab configuration use {@link #tab(String)}.
	 * @param title the tab title
	 * @param component the component to display in the tab
	 * @return this builder instance
	 * @see JTabbedPane#addTab(String, Component)
	 */
	TabbedPaneBuilder tab(String title, JComponent component);

	/**
	 * Adds a tab to this tabbed pane builder.
	 * For further tab configuration use {@link #tab(String)}.
	 * @param title the tab title
	 * @param component the component to display in the tab
	 * @return this builder instance
	 * @see JTabbedPane#addTab(String, Component)
	 */
	TabbedPaneBuilder tab(String title, Supplier<? extends JComponent> component);

	/**
	 * Returns a new {@link TabBuilder} for adding a tab
	 * @param title the tab title
	 * @return a new {@link TabBuilder} instance
	 */
	TabBuilder tab(String title);

	/**
	 * Builds a Tab for a {@link TabbedPaneBuilder}.
	 */
	interface TabBuilder {

		/**
		 * @param component the component
		 * @return this builder instance
		 */
		TabBuilder component(@Nullable JComponent component);

		/**
		 * @param component the component
		 * @return this builder instance
		 */
		TabBuilder component(Supplier<? extends JComponent> component);

		/**
		 * @param mnemonic the tab mnemonic
		 * @return this builder instance
		 */
		TabBuilder mnemonic(int mnemonic);

		/**
		 * @param toolTipText the tab tool tip text
		 * @return this builder instance
		 */
		TabBuilder toolTipText(@Nullable String toolTipText);

		/**
		 * @param icon the tab icon
		 * @return this builder instance
		 */
		TabBuilder icon(@Nullable Icon icon);

		/**
		 * @param tabComponent the tab component
		 * @return this builder instance
		 * @see JTabbedPane#setTabComponentAt(int, Component)
		 */
		TabBuilder tabComponent(@Nullable JComponent tabComponent);

		/**
		 * Adds this tab and returns the {@link TabbedPaneBuilder}
		 * @return the {@link TabbedPaneBuilder} instance
		 */
		TabbedPaneBuilder add();
	}

	/**
	 * @return a new {@link TabbedPaneBuilder} instance
	 */
	static TabbedPaneBuilder builder() {
		return new DefaultTabbedPaneBuilder();
	}
}
