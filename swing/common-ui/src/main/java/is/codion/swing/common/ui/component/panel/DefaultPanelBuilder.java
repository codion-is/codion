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

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultPanelBuilder extends AbstractComponentBuilder<Void, JPanel, PanelBuilder> implements PanelBuilder {

	private final List<ComponentConstraints> componentConstraints = new ArrayList<>();

	private @Nullable JPanel panel;
	private @Nullable LayoutManager layout;

	DefaultPanelBuilder() {}

	@Override
	public PanelBuilder panel(@Nullable JPanel panel) {
		this.panel = panel;
		return this;
	}

	@Override
	public PanelBuilder layout(@Nullable LayoutManager layoutManager) {
		layout = layoutManager;
		return this;
	}

	@Override
	public PanelBuilder add(JComponent component) {
		return add(() -> component);
	}

	@Override
	public PanelBuilder add(Supplier<? extends JComponent> component) {
		componentConstraints.add(new ComponentConstraints(requireNonNull(component)));
		return this;
	}

	@Override
	public PanelBuilder add(JComponent component, Object constraints) {
		return add(() -> requireNonNull(component), constraints);
	}

	@Override
	public PanelBuilder add(Supplier<? extends JComponent> component, Object constraints) {
		if (constraints instanceof JComponent) {
			throw new IllegalArgumentException("Use addAll() when adding multiple components");
		}
		componentConstraints.add(new ComponentConstraints(requireNonNull(component), requireNonNull(constraints)));
		return this;
	}

	@Override
	public PanelBuilder addAll(JComponent... components) {
		addAll(Arrays.asList(components));
		return this;
	}

	@Override
	public PanelBuilder addAll(Collection<? extends JComponent> components) {
		requireNonNull(components).forEach(new AddComponents(componentConstraints));
		return this;
	}

	@Override
	protected JPanel createComponent() {
		JPanel component = panel == null ? new JPanel() : panel;
		if (layout != null) {
			component.setLayout(layout);
		}
		componentConstraints.forEach(new AddToPanel(component));

		return component;
	}

	@Override
	protected ComponentValue<Void, JPanel> createComponentValue(JPanel component) {
		return new PanelComponentValue(component);
	}

	private static final class ComponentConstraints {

		private final Supplier<? extends JComponent> component;
		private final @Nullable Object constraints;

		private ComponentConstraints(Supplier<? extends JComponent> component) {
			this(component, null);
		}

		private ComponentConstraints(Supplier<? extends JComponent> component, @Nullable Object constraints) {
			this.component = component;
			this.constraints = constraints;
		}
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

	private static final class AddComponents implements Consumer<JComponent> {

		private final List<ComponentConstraints> componentConstraints;

		private AddComponents(List<ComponentConstraints> componentConstraints) {
			this.componentConstraints = componentConstraints;
		}

		@Override
		public void accept(JComponent component) {
			componentConstraints.add(new ComponentConstraints(() -> requireNonNull(component)));
		}
	}

	private static final class AddToPanel implements Consumer<ComponentConstraints> {

		private final JPanel panel;

		private AddToPanel(JPanel panel) {
			this.panel = panel;
		}

		@Override
		public void accept(ComponentConstraints componentConstraint) {
			if (componentConstraint.constraints != null) {
				panel.add(componentConstraint.component.get(), componentConstraint.constraints);
			}
			else {
				panel.add(componentConstraint.component.get());
			}
		}
	}
}
