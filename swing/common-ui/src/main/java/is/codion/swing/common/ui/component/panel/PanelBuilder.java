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
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.layout.FlexibleGridLayout;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.LayoutManager;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Builds a JPanel instance.
 */
public interface PanelBuilder<L extends LayoutManager, B extends PanelBuilder<L, B>> extends ComponentBuilder<JPanel, B> {

	/**
	 * @param panel the panel
	 * @return this builder instancwe
	 */
	B panel(@Nullable JPanel panel);

	/**
	 * @param layoutManager the layout manager
	 * @return this builder instance
	 * @see JPanel#setLayout(LayoutManager)
	 */
	B layout(@Nullable L layoutManager);

	/**
	 * @param component the component to add
	 * @return this builder instance
	 * @see JPanel#add(Component)
	 */
	B add(JComponent component);

	/**
	 * @param component the component to add
	 * @return this builder instance
	 * @see JPanel#add(Component)
	 */
	B add(Supplier<? extends JComponent> component);

	/**
	 * @param component the component to add
	 * @param constraints the layout constraints
	 * @return this builder instance
	 * @see JPanel#add(Component, Object)
	 */
	B add(JComponent component, Object constraints);

	/**
	 * @param component the component to add
	 * @param constraints the layout constraints
	 * @return this builder instance
	 * @see JPanel#add(Component, Object)
	 */
	B add(Supplier<? extends JComponent> component, Object constraints);

	/**
	 * @param components the components to add
	 * @return this builder instance
	 * @see JPanel#add(Component)
	 */
	B addAll(JComponent... components);

	/**
	 * @param components the components to add
	 * @return this builder instance
	 * @see JPanel#add(Component)
	 */
	B addAll(Collection<? extends JComponent> components);

	/**
	 * Provides panel builders.
	 */
	interface PanelBuilderFactory {

		/**
		 * @param layout the layout
		 * @param <L> the layout type
		 * @param <B> the builder type
		 * @return a new panel builder
		 */
		<L extends LayoutManager, B extends PanelBuilder<L, B>> PanelBuilder<L, B> layout(L layout);

		/**
		 * Uses the default vertical and horizontal gap value
		 * @return a new panel builder
		 * @see Layouts#GAP
		 */
		BorderLayoutPanelBuilder borderLayout();

		/**
		 * @param layout the layout
		 * @return a new panel builder
		 */
		BorderLayoutPanelBuilder borderLayout(BorderLayout layout);

		/**
		 * Uses the default vertical and horizontal gap value
		 * @param align the layout alignment
		 * @return a new panel builder
		 * @see Layouts#GAP
		 */
		FlowLayoutPanelBuilder flowLayout(int align);

		/**
		 * @param layout the layout
		 * @return a new panel builder
		 */
		FlowLayoutPanelBuilder flowLayout(FlowLayout layout);

		/**
		 * Uses the default vertical and horizontal gap value
		 * @param rows the number of rows
		 * @param columns the number of columns
		 * @return a new panel builder
		 * @see Layouts#GAP
		 */
		GridLayoutPanelBuilder gridLayout(int rows, int columns);

		/**
		 * @param layout the layout
		 * @return a new panel builder
		 */
		GridLayoutPanelBuilder gridLayout(GridLayout layout);

		/**
		 * Uses the default vertical and horizontal gap value
		 * @param rows the number of rows
		 * @param columns the number of columns
		 * @return a new panel builder
		 * @see Layouts#GAP
		 */
		FlexibleGridLayoutPanelBuilder flexibleGridLayout(int rows, int columns);

		/**
		 * @param layout the layout
		 * @return a new panel builder
		 */
		FlexibleGridLayoutPanelBuilder flexibleGridLayout(FlexibleGridLayout layout);
	}

	/**
	 * @return a panel builder factory
	 */
	static PanelBuilderFactory builder() {
		return DefaultPanelBuilder.FACTORY;
	}
}
