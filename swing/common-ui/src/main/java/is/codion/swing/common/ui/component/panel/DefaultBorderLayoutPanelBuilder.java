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

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultBorderLayoutPanelBuilder extends AbstractComponentBuilder<JPanel, BorderLayoutPanelBuilder> implements BorderLayoutPanelBuilder {

	private BorderLayout layout = Layouts.borderLayout();
	private @Nullable JComponent centerComponent;
	private @Nullable JComponent northComponent;
	private @Nullable JComponent southComponent;
	private @Nullable JComponent eastComponent;
	private @Nullable JComponent westComponent;

	DefaultBorderLayoutPanelBuilder() {}

	@Override
	public BorderLayoutPanelBuilder layout(BorderLayout layout) {
		this.layout = requireNonNull(layout);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder add(JComponent component, String constraints) {
		return add(() -> requireNonNull(component), constraints);
	}

	@Override
	public BorderLayoutPanelBuilder add(Supplier<? extends JComponent> component, String constraints) {
		switch (requireNonNull(constraints)) {
			case BorderLayout.CENTER:
				return center(component);
			case BorderLayout.NORTH:
				return north(component);
			case BorderLayout.SOUTH:
				return south(component);
			case BorderLayout.EAST:
				return east(component);
			case BorderLayout.WEST:
				return west(component);
			default:
				throw new IllegalArgumentException("Unknown BorderLayout constraints: " + constraints);
		}
	}

	@Override
	public BorderLayoutPanelBuilder center(JComponent centerComponent) {
		this.centerComponent = requireNonNull(centerComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder center(Supplier<? extends JComponent> centerComponent) {
		return center(requireNonNull(centerComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder north(JComponent northComponent) {
		this.northComponent = requireNonNull(northComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder north(Supplier<? extends JComponent> northComponent) {
		return north(requireNonNull(northComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder south(JComponent southComponent) {
		this.southComponent = requireNonNull(southComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder south(Supplier<? extends JComponent> southComponent) {
		return south(requireNonNull(southComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder east(JComponent eastComponent) {
		this.eastComponent = requireNonNull(eastComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder east(Supplier<? extends JComponent> eastComponent) {
		return east(requireNonNull(eastComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder west(JComponent westComponent) {
		this.westComponent = requireNonNull(westComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder west(Supplier<? extends JComponent> westComponent) {
		return west(requireNonNull(westComponent).get());
	}

	@Override
	protected JPanel createComponent() {
		JPanel component = new JPanel(layout);
		if (centerComponent != null) {
			component.add(centerComponent, BorderLayout.CENTER);
		}
		if (northComponent != null) {
			component.add(northComponent, BorderLayout.NORTH);
		}
		if (southComponent != null) {
			component.add(southComponent, BorderLayout.SOUTH);
		}
		if (eastComponent != null) {
			component.add(eastComponent, BorderLayout.EAST);
		}
		if (westComponent != null) {
			component.add(westComponent, BorderLayout.WEST);
		}

		return component;
	}
}
