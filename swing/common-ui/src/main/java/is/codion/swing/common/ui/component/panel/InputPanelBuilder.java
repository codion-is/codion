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

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.function.Supplier;

/**
 * Builds an input panel with a label and input component.
 */
public interface InputPanelBuilder extends ComponentBuilder<JPanel, InputPanelBuilder> {

	/**
	 * @param labelComponent the label component
	 * @return this builder instance
	 */
	InputPanelBuilder label(JComponent labelComponent);

	/**
	 * @param labelComponent the label component
	 * @return this builder instance
	 */
	InputPanelBuilder label(Supplier<? extends JComponent> labelComponent);

	/**
	 * @param component the input component
	 * @return this builder instance
	 */
	InputPanelBuilder component(JComponent component);

	/**
	 * @param component the input component
	 * @return this builder instance
	 */
	InputPanelBuilder component(Supplier<? extends JComponent> component);

	/**
	 * @param layout the input panel layout
	 * @return this builder instance
	 */
	InputPanelBuilder layout(InputPanelLayout layout);

	/**
	 * @return a new {@link InputPanelBuilder}
	 */
	static InputPanelBuilder builder() {
		return new DefaultInputPanelBuilder();
	}
}
