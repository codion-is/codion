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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Component;
import java.awt.LayoutManager;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Builds a JPanel instance.
 */
public interface PanelBuilder extends ComponentBuilder<Void, JPanel, PanelBuilder> {

	/**
	 * @param panel the panel
	 * @return this builder instancwe
	 */
	PanelBuilder panel(@Nullable JPanel panel);

	/**
	 * @param layoutManager the layout manager
	 * @return this builder instance
	 * @see JPanel#setLayout(LayoutManager)
	 */
	PanelBuilder layout(@Nullable LayoutManager layoutManager);

	/**
	 * @param component the component to add
	 * @return this builder instance
	 * @see JPanel#add(Component)
	 */
	PanelBuilder add(JComponent component);

	/**
	 * @param component the component to add
	 * @return this builder instance
	 * @see JPanel#add(Component)
	 */
	PanelBuilder add(Supplier<? extends JComponent> component);

	/**
	 * @param component the component to add
	 * @param constraints the layout constraints
	 * @return this builder instance
	 * @see JPanel#add(Component, Object)
	 */
	PanelBuilder add(JComponent component, Object constraints);

	/**
	 * @param component the component to add
	 * @param constraints the layout constraints
	 * @return this builder instance
	 * @see JPanel#add(Component, Object)
	 */
	PanelBuilder add(Supplier<? extends JComponent> component, Object constraints);

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
		return new DefaultPanelBuilder();
	}
}
