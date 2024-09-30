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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;

import static java.util.Objects.requireNonNull;

abstract class AbstractSpinnerBuilder<T, B extends SpinnerBuilder<T, B>> extends AbstractComponentBuilder<T, JSpinner, B>
				implements SpinnerBuilder<T, B> {

	protected final SpinnerModel spinnerModel;

	private boolean editable = true;
	private int columns = -1;
	private boolean mouseWheelScrolling = true;
	private boolean mouseWheelScrollingReversed = false;
	private int horizontalAlignment = -1;

	protected AbstractSpinnerBuilder(SpinnerModel spinnerModel, Value<T> linkedValue) {
		super(linkedValue);
		this.spinnerModel = requireNonNull(spinnerModel);
	}

	@Override
	public final B editable(boolean editable) {
		this.editable = editable;
		return self();
	}

	@Override
	public final B columns(int columns) {
		this.columns = columns;
		return self();
	}

	@Override
	public final B mouseWheelScrolling(boolean mouseWheelScrolling) {
		this.mouseWheelScrolling = mouseWheelScrolling;
		if (mouseWheelScrolling) {
			this.mouseWheelScrollingReversed = false;
		}
		return self();
	}

	@Override
	public B mouseWheelScrollingReversed(boolean mouseWheelScrollingReversed) {
		this.mouseWheelScrollingReversed = mouseWheelScrollingReversed;
		if (mouseWheelScrollingReversed) {
			this.mouseWheelScrolling = false;
		}
		return self();
	}

	@Override
	public final B horizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		return self();
	}

	@Override
	protected final JSpinner createComponent() {
		JSpinner spinner = createSpinner();
		JComponent editor = spinner.getEditor();
		if (editor instanceof JSpinner.DefaultEditor) {
			JTextField editorField = ((JSpinner.DefaultEditor) editor).getTextField();
			if (editable) {
				spinner.setFocusable(false);
			}
			else {
				editorField.setEditable(false);
				editorField.setFocusable(false);
			}
			if (columns != -1) {
				editorField.setColumns(columns);
			}
			if (horizontalAlignment != -1) {
				editorField.setHorizontalAlignment(horizontalAlignment);
			}
		}
		if (mouseWheelScrolling) {
			spinner.addMouseWheelListener(new SpinnerMouseWheelListener(spinnerModel, false));
		}
		if (mouseWheelScrollingReversed) {
			spinner.addMouseWheelListener(new SpinnerMouseWheelListener(spinnerModel, true));
		}

		return spinner;
	}

	protected JSpinner createSpinner() {
		return new JSpinner(spinnerModel);
	}
}
