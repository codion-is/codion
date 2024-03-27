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
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.LayoutManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class DefaultPanelBuilder extends AbstractComponentBuilder<Void, JPanel, PanelBuilder> implements PanelBuilder {

	private final JPanel panel;
	private final List<ComponentConstraints> componentConstraints = new ArrayList<>();

	private LayoutManager layout;

	DefaultPanelBuilder(JPanel panel) {
		this.panel = requireNonNull(panel);
	}

	DefaultPanelBuilder(LayoutManager layout) {
		this.layout = layout;
		this.panel = null;
	}

	@Override
	public PanelBuilder layout(LayoutManager layoutManager) {
		layout = requireNonNull(layoutManager);
		return this;
	}

	@Override
	public PanelBuilder add(JComponent component) {
		componentConstraints.add(new ComponentConstraints(requireNonNull(component)));
		return this;
	}

	@Override
	public PanelBuilder add(JComponent component, Object constraints) {
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
		requireNonNull(components).forEach(component -> componentConstraints.add(new ComponentConstraints(requireNonNull(component))));
		return this;
	}

	@Override
	protected JPanel createComponent() {
		JPanel component = panel == null ? new JPanel() : panel;
		if (layout != null) {
			component.setLayout(layout);
		}
		componentConstraints.forEach(componentConstraint -> {
			if (componentConstraint.constraints != null) {
				component.add(componentConstraint.component, componentConstraint.constraints);
			}
			else {
				component.add(componentConstraint.component);
			}
		});

		return component;
	}

	@Override
	protected ComponentValue<Void, JPanel> createComponentValue(JPanel component) {
		throw new UnsupportedOperationException("A ComponentValue can not be based on a JPanel");
	}

	@Override
	protected void setInitialValue(JPanel component, Void initialValue) {}

	private static final class ComponentConstraints {

		private final JComponent component;
		private final Object constraints;

		private ComponentConstraints(JComponent component) {
			this(component, null);
		}

		private ComponentConstraints(JComponent component, Object constraints) {
			this.component = component;
			this.constraints = constraints;
		}
	}
}
