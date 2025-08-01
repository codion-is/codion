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
 * Copyright (c) 2021 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.spinner;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerModel;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Objects;

import static java.util.Objects.requireNonNull;

abstract class AbstractSpinnerBuilder<T, B extends SpinnerBuilder<T, B>> extends AbstractComponentBuilder<T, JSpinner, B>
				implements SpinnerBuilder<T, B> {

	protected SpinnerModel spinnerModel;

	private boolean editable = true;
	private int columns = -1;
	private boolean mouseWheelScrolling = MOUSE_WHEEL_SCROLLING.getOrThrow();
	private boolean mouseWheelScrollingReversed = false;
	private int horizontalAlignment = -1;

	protected AbstractSpinnerBuilder(SpinnerModel model) {
		this.spinnerModel = requireNonNull(model);
	}

	@Override
	public B model(SpinnerModel model) {
		this.spinnerModel = requireNonNull(model);
		return self();
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
				editorField.addPropertyChangeListener("horizontalAlignment",
								new PreserveHorizontalAlignment(editorField, horizontalAlignment));
			}
		}
		if (mouseWheelScrolling) {
			spinner.addMouseWheelListener(new SpinnerMouseWheelListener(spinner, false));
		}
		if (mouseWheelScrollingReversed) {
			spinner.addMouseWheelListener(new SpinnerMouseWheelListener(spinner, true));
		}

		return spinner;
	}

	protected JSpinner createSpinner() {
		if (editable) {
			return new FocusEditorSpinner(spinnerModel);
		}

		return new JSpinner(spinnerModel);
	}

	/**
	 * Hack to preserve the horizontal alignment through UI changes, since the BasicSpinnerUI.createEditor()
	 * gets called, which updates the editor alignment screwing up any previously set alignment value.
	 * This of course renders the horizontal alignment effectively immutable for the component, which is not great.
	 */
	private static final class PreserveHorizontalAlignment implements PropertyChangeListener {

		private final JTextField editorField;
		private final int horizontalAlignment;

		private PreserveHorizontalAlignment(JTextField editorField, int horizontalAlignment) {
			this.editorField = editorField;
			this.horizontalAlignment = horizontalAlignment;
		}

		@Override
		public void propertyChange(PropertyChangeEvent event) {
			if (!Objects.equals(event.getNewValue(), horizontalAlignment)) {
				editorField.setHorizontalAlignment(horizontalAlignment);
			}
		}
	}

	private static final class FocusEditorSpinner extends JSpinner {

		private FocusEditorSpinner(SpinnerModel model) {
			super(model);
		}

		@Override
		public void requestFocus() {
			getEditor().requestFocus();
		}
	}
}
