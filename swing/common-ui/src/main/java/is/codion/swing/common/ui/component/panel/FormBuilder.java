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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.function.Supplier;

/**
 * Builds a form panel, laying out label/input pairs in rows, the label beside its input.
 * <p>A form is a sequence of rows; a row is either {@link #columns(int)} label/input pairs or one
 * full-width component, see {@link #span(JComponent)}; the label column of each pair column is as wide
 * as its widest label; the input columns absorb the horizontal slack, equally. Anything beyond that,
 * nesting, percentage widths, per-cell insets, row spans, is not a form but a hand-written {@link java.awt.GridBagLayout}.
 * <p>A label aligns with the baseline of its input, or with its top in case the input has no baseline, such as a panel,
 * and is hidden along with it. An input stretches to the height of its row, unless its baseline would move along
 * with its height, as with single line inputs, such as text fields, which keep their height.
 * <p>The form does not grow vertically, extra height staying below the last row, so a component which should
 * absorb the vertical slack, such as a text area, belongs outside the form, in the center of a
 * {@link java.awt.BorderLayout} with the form to the north.
 * <p>Focus traversal follows the geometry, right then down, which is correct for pair columns. For down then across,
 * lay out one form per column, side by side, each a focus traversal policy provider with a
 * {@link java.awt.ContainerOrderFocusTraversalPolicy}.
 * @param <B> the builder type
 * @see is.codion.swing.common.ui.component.Components#form()
 * @see DefaultFormBuilder
 */
public interface FormBuilder<B extends FormBuilder<B>> extends ComponentBuilder<JPanel, B> {

	/**
	 * @param columns the number of label/input pairs per row, default 1
	 * @return this builder instance
	 * @throws IllegalArgumentException in case columns is less than 1
	 */
	B columns(int columns);

	/**
	 * @param labelAlignment the horizontal alignment of the labels within the label column,
	 * {@link javax.swing.SwingConstants#LEADING} (default) or {@link javax.swing.SwingConstants#TRAILING}
	 * @return this builder instance
	 * @throws IllegalArgumentException in case of an alignment other than LEADING or TRAILING
	 */
	B labelAlignment(int labelAlignment);

	/**
	 * Adds a label/input pair, the label being the one associated with the component via
	 * {@link javax.swing.JLabel#setLabelFor(java.awt.Component)}, if any, otherwise the component
	 * occupies the input column alone, the label cell empty.
	 * @param component the input component
	 * @return this builder instance
	 */
	B add(JComponent component);

	/**
	 * Adds a label/input pair, the label being the one associated with the component via
	 * {@link javax.swing.JLabel#setLabelFor(java.awt.Component)}, if any, otherwise the component
	 * occupies the input column alone, the label cell empty.
	 * @param component the input component
	 * @return this builder instance
	 */
	B add(Supplier<? extends JComponent> component);

	/**
	 * Adds a label/input pair. A {@link javax.swing.JLabel} carrying no association of its own
	 * is associated with the component, so that its mnemonic focuses it.
	 * @param label the label
	 * @param component the input component
	 * @return this builder instance
	 */
	B add(JComponent label, JComponent component);

	/**
	 * Adds a label/input pair. A {@link javax.swing.JLabel} carrying no association of its own
	 * is associated with the component, so that its mnemonic focuses it.
	 * @param label the label
	 * @param component the input component
	 * @return this builder instance
	 */
	B add(Supplier<? extends JComponent> label, Supplier<? extends JComponent> component);

	/**
	 * Adds a component occupying a row of its own. A label associated with the component keeps the
	 * label column, the component spanning the rest of the row, otherwise the component spans the full width.
	 * @param component the component
	 * @return this builder instance
	 */
	B span(JComponent component);

	/**
	 * Adds a component occupying a row of its own. A label associated with the component keeps the
	 * label column, the component spanning the rest of the row, otherwise the component spans the full width.
	 * @param component the component
	 * @return this builder instance
	 */
	B span(Supplier<? extends JComponent> component);

	/**
	 * @param <B> the builder type
	 * @return a new {@link FormBuilder}
	 */
	static <B extends FormBuilder<B>> FormBuilder<B> builder() {
		return new DefaultFormBuilder<>();
	}
}
