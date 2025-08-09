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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultInputPanelBuilder extends AbstractComponentBuilder<JPanel, InputPanelBuilder> implements InputPanelBuilder {

	private @Nullable JComponent component;
	private @Nullable JComponent label;

	private InputPanelLayout layout = InputPanelLayout
					.border()
					.build();

	@Override
	public InputPanelBuilder label(JComponent labelComponent) {
		this.label = requireNonNull(labelComponent);
		return this;
	}

	@Override
	public InputPanelBuilder label(Supplier<? extends JComponent> labelComponent) {
		return label(requireNonNull(labelComponent).get());
	}

	@Override
	public InputPanelBuilder component(JComponent component) {
		this.component = requireNonNull(component);
		return this;
	}

	@Override
	public InputPanelBuilder component(Supplier<? extends JComponent> component) {
		return component(requireNonNull(component).get());
	}

	@Override
	public InputPanelBuilder layout(InputPanelLayout layout) {
		this.layout = requireNonNull(layout);
		return this;
	}

	@Override
	protected JPanel createComponent() {
		if (label == null || component == null) {
			throw new IllegalStateException("You must set both label and component before building");
		}

		return layout.layout(label, component);
	}
}
