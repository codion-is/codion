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
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultBorderLayoutPanelBuilder extends AbstractComponentBuilder<Void, JPanel, BorderLayoutPanelBuilder> implements BorderLayoutPanelBuilder {

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
				return centerComponent(component);
			case BorderLayout.NORTH:
				return northComponent(component);
			case BorderLayout.SOUTH:
				return southComponent(component);
			case BorderLayout.EAST:
				return eastComponent(component);
			case BorderLayout.WEST:
				return westComponent(component);
			default:
				throw new IllegalArgumentException("Unknown BorderLayout constraints: " + constraints);
		}
	}

	@Override
	public BorderLayoutPanelBuilder centerComponent(JComponent centerComponent) {
		this.centerComponent = requireNonNull(centerComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder centerComponent(Supplier<? extends JComponent> centerComponent) {
		return centerComponent(requireNonNull(centerComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder northComponent(JComponent northComponent) {
		this.northComponent = requireNonNull(northComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder northComponent(Supplier<? extends JComponent> northComponent) {
		return northComponent(requireNonNull(northComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder southComponent(JComponent southComponent) {
		this.southComponent = requireNonNull(southComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder southComponent(Supplier<? extends JComponent> southComponent) {
		return southComponent(requireNonNull(southComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder eastComponent(JComponent eastComponent) {
		this.eastComponent = requireNonNull(eastComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder eastComponent(Supplier<? extends JComponent> eastComponent) {
		return eastComponent(requireNonNull(eastComponent).get());
	}

	@Override
	public BorderLayoutPanelBuilder westComponent(JComponent westComponent) {
		this.westComponent = requireNonNull(westComponent);
		return this;
	}

	@Override
	public BorderLayoutPanelBuilder westComponent(Supplier<? extends JComponent> westComponent) {
		return westComponent(requireNonNull(westComponent).get());
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

	@Override
	protected ComponentValue<Void, JPanel> createComponentValue(JPanel component) {
		return new PanelComponentValue(component);
	}

	private static final class PanelComponentValue extends AbstractComponentValue<Void, JPanel> {

		private PanelComponentValue(JPanel component) {
			super(component);
		}

		@Override
		protected Void getComponentValue() {
			throw new UnsupportedOperationException();
		}

		@Override
		protected void setComponentValue(Void value) {
			throw new UnsupportedOperationException();
		}
	}
}
