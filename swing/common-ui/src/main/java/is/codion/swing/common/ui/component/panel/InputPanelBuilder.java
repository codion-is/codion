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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.function.Supplier;

/**
 * Builds an input panel with a label and input component.
 * <p>
 * The panel contains its label, so the {@code label()} methods below and the inherited
 * {@link ComponentBuilder#label(String)}, {@link ComponentBuilder#label(javax.swing.JLabel)} and
 * {@link ComponentBuilder#label(java.util.function.Consumer)} all specify the same thing: the label the panel
 * displays. They differ only in what they accept, the ones here lifting the restriction to a {@link javax.swing.JLabel}.
 * The last call wins, whichever was used, so a label supplied by a factory - the caption based one the entity
 * components come with, say - is replaced by specifying another.
 * <p>
 * Note that the panel itself is therefore never the target of {@link javax.swing.JLabel#setLabelFor(java.awt.Component)}:
 * a label carrying no association of its own is associated with the input component, which is what it labels.
 */
public interface InputPanelBuilder extends ComponentBuilder<JPanel, InputPanelBuilder> {

	/**
	 * Specifies the label the panel displays, lifting the {@link javax.swing.JLabel} restriction of
	 * {@link ComponentBuilder#label(javax.swing.JLabel)}. Overrides any previously specified label.
	 * @param labelComponent the label component
	 * @return this builder instance
	 */
	InputPanelBuilder label(JComponent labelComponent);

	/**
	 * Specifies the label the panel displays, lifting the {@link javax.swing.JLabel} restriction of
	 * {@link ComponentBuilder#label(javax.swing.JLabel)}. Overrides any previously specified label.
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
