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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.Action;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionListener;
import java.text.Format;

import static is.codion.common.utilities.Text.nullOrEmpty;
import static is.codion.swing.common.ui.component.text.SizedDocument.sizedDocument;
import static java.util.Objects.requireNonNull;

class DefaultTextFieldBuilder<C extends JTextField, T, B extends TextFieldBuilder<C, T, B>> extends AbstractTextComponentBuilder<C, T, B>
				implements TextFieldBuilder<C, T, B> {

	static final ValueClassStep VALUE_CLASS = new DefaultValueClassStep();

	private final Class<T> valueClass;

	private int columns = -1;
	private @Nullable Action action;
	private @Nullable ActionListener actionListener;
	private @Nullable Format format;
	private int horizontalAlignment = SwingConstants.LEADING;
	private @Nullable String hint;

	DefaultTextFieldBuilder(Class<T> valueClass) {
		this.valueClass = requireNonNull(valueClass);
		if (valueClass.equals(Character.class)) {
			maximumLength(1);
		}
		selectAllOnFocusGained(SELECT_ALL_ON_FOCUS_GAINED.getOrThrow());
	}

	@Override
	public final B columns(int columns) {
		this.columns = columns;
		return self();
	}

	@Override
	public final B action(Action action) {
		this.action = requireNonNull(action);

		return transferFocusOnEnter(false);
	}

	@Override
	public final B actionListener(ActionListener actionListener) {
		this.actionListener = actionListener;

		return transferFocusOnEnter(false);
	}

	@Override
	public final B format(@Nullable Format format) {
		this.format = format;
		return self();
	}

	@Override
	public final B horizontalAlignment(int horizontalAlignment) {
		this.horizontalAlignment = horizontalAlignment;
		return self();
	}

	@Override
	public final B hint(String hint) {
		if (nullOrEmpty(hint)) {
			throw new IllegalArgumentException("Hint is null or empty");
		}
		this.hint = hint;
		return self();
	}

	@Override
	protected final C createTextComponent() {
		C textField = createTextField();
		if (columns != -1) {
			textField.setColumns(columns);
		}
		textField.setHorizontalAlignment(horizontalAlignment);
		if (action != null) {
			textField.setAction(action);
		}
		if (actionListener != null) {
			textField.addActionListener(actionListener);
		}
		if (hint != null && textField instanceof HintTextField) {
			((HintTextField) textField).hint().set(hint);
		}

		return textField;
	}

	/**
	 * @return the {@link javax.swing.text.JTextField} built by this builder.
	 */
	protected C createTextField() {
		return (C) new HintTextField(sizedDocument());
	}

	@Override
	protected ComponentValue<C, T> createValue(C component) {
		requireNonNull(component);
		if (valueClass.equals(Character.class)) {
			return (ComponentValue<C, T>) new CharacterFieldValue(component, updateOn());
		}

		return new DefaultTextComponentValue<>(component, format, updateOn());
	}

	protected final @Nullable Format format() {
		return format;
	}

	private static final class DefaultValueClassStep implements ValueClassStep {

		@Override
		public <T, C extends JTextField, B extends TextFieldBuilder<C, T, B>> TextFieldBuilder<C, T, B> valueClass(Class<T> valueClass) {
			return new DefaultTextFieldBuilder<>(valueClass);
		}
	}
}
