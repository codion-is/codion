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
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.text.CaseDocumentFilter.DocumentCase;
import is.codion.swing.common.ui.key.KeyEvents;

import org.jspecify.annotations.Nullable;

import javax.swing.event.CaretListener;
import javax.swing.text.Caret;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.util.Objects.requireNonNull;

abstract class AbstractTextComponentBuilder<T, C extends JTextComponent, B extends TextComponentBuilder<T, C, B>>
				extends AbstractComponentBuilder<T, C, B> implements TextComponentBuilder<T, C, B> {

	private final List<CaretListener> caretListeners = new ArrayList<>();

	private UpdateOn updateOn = UpdateOn.VALUE_CHANGE;
	private boolean editable = true;
	private boolean upperCase;
	private boolean lowerCase;
	private int maximumLength = -1;
	private @Nullable Insets margin;
	private boolean controlDeleteWord = true;
	private @Nullable Color disabledTextColor;
	private @Nullable Color selectedTextColor;
	private @Nullable Color selectionColor;
	private boolean selectAllOnFocusGained;
	private boolean moveCaretToEndOnFocusGained;
	private boolean moveCaretToStartOnFocusGained;
	private @Nullable Consumer<String> onTextChanged;
	private boolean dragEnabled = false;
	private @Nullable Character focusAcceleratorKey;
	private CaretPosition caretPosition = CaretPosition.START;
	private int caretUpdatePolicy = DefaultCaret.UPDATE_WHEN_ON_EDT;

	protected AbstractTextComponentBuilder() {
		// Make sure this is done after value linking and setting the initial value
		onBuild(this::setCaretPosition);
	}

	@Override
	public final B editable(boolean editable) {
		this.editable = editable;
		return self();
	}

	@Override
	public final B updateOn(UpdateOn updateOn) {
		this.updateOn = requireNonNull(updateOn);
		return self();
	}

	@Override
	public final B upperCase(boolean upperCase) {
		if (upperCase && lowerCase) {
			throw new IllegalArgumentException("Field is already lowercase");
		}
		this.upperCase = upperCase;
		return self();
	}

	@Override
	public final B lowerCase(boolean lowerCase) {
		if (lowerCase && upperCase) {
			throw new IllegalArgumentException("Field is already uppercase");
		}
		this.lowerCase = lowerCase;
		return self();
	}

	@Override
	public final B maximumLength(int maximumLength) {
		this.maximumLength = maximumLength;
		return self();
	}

	@Override
	public final B margin(@Nullable Insets margin) {
		this.margin = margin;
		return self();
	}

	@Override
	public final B controlDeleteWord(boolean controlDeleteWord) {
		this.controlDeleteWord = controlDeleteWord;
		return self();
	}

	@Override
	public final B disabledTextColor(@Nullable Color disabledTextColor) {
		this.disabledTextColor = disabledTextColor;
		return self();
	}

	@Override
	public final B selectedTextColor(@Nullable Color selectedTextColor) {
		this.selectedTextColor = selectedTextColor;
		return self();
	}

	@Override
	public final B selectionColor(@Nullable Color selectionColor) {
		this.selectionColor = selectionColor;
		return self();
	}

	@Override
	public final B selectAllOnFocusGained(boolean selectAllOnFocusGained) {
		this.selectAllOnFocusGained = selectAllOnFocusGained;
		return self();
	}

	@Override
	public final B moveCaretToEndOnFocusGained(boolean moveCaretToEndOnFocusGained) {
		if (moveCaretToStartOnFocusGained) {
			throw new IllegalArgumentException("Caret is already set to move to start on focus gained");
		}
		this.moveCaretToEndOnFocusGained = moveCaretToEndOnFocusGained;
		return self();
	}

	@Override
	public final B moveCaretToStartOnFocusGained(boolean moveCaretToStartOnFocusGained) {
		if (moveCaretToEndOnFocusGained) {
			throw new IllegalArgumentException("Caret is already set to move to end on focus gained");
		}
		this.moveCaretToStartOnFocusGained = moveCaretToStartOnFocusGained;
		return self();
	}

	@Override
	public final B caretListener(CaretListener caretListener) {
		this.caretListeners.add(requireNonNull(caretListener));
		return self();
	}

	@Override
	public final B onTextChanged(Consumer<String> onTextChanged) {
		this.onTextChanged = requireNonNull(onTextChanged);
		return self();
	}

	@Override
	public final B dragEnabled(boolean dragEnabled) {
		this.dragEnabled = dragEnabled;
		return self();
	}

	@Override
	public final B focusAccelerator(char focusAcceleratorKey) {
		this.focusAcceleratorKey = focusAcceleratorKey;
		return self();
	}

	@Override
	public final B caretPosition(CaretPosition caretPosition) {
		this.caretPosition = requireNonNull(caretPosition);
		return self();
	}

	@Override
	public final B caretUpdatePolicy(int caretUpdatePolicy) {
		this.caretUpdatePolicy = caretUpdatePolicy;
		return self();
	}

	@Override
	protected final C createComponent() {
		C textComponent = createTextComponent();
		textComponent.setEditable(editable);
		textComponent.setDragEnabled(dragEnabled);
		caretListeners.forEach(new AddCaretListener(textComponent));
		if (focusAcceleratorKey != null) {
			textComponent.setFocusAccelerator(focusAcceleratorKey);
		}
		if (margin != null) {
			textComponent.setMargin(margin);
		}
		if (upperCase) {
			new TextFieldDocumentCase(textComponent.getDocument(), DocumentCase.UPPERCASE);
		}
		if (lowerCase) {
			new TextFieldDocumentCase(textComponent.getDocument(), DocumentCase.LOWERCASE);
		}
		if (maximumLength > 0) {
			new MaximumTextFieldLength(textComponent.getDocument(), maximumLength);
		}
		if (controlDeleteWord) {
			keyEvent(KeyEvents.builder()
							.keyCode(VK_DELETE)
							.modifiers(CTRL_DOWN_MASK)
							.action(new DeleteNextWordAction()));
			keyEvent(KeyEvents.builder()
							.keyCode(VK_BACK_SPACE)
							.modifiers(CTRL_DOWN_MASK)
							.action(new DeletePreviousWordAction()));
		}
		Caret caret = textComponent.getCaret();
		if (caret instanceof DefaultCaret) {
			((DefaultCaret) caret).setUpdatePolicy(caretUpdatePolicy);
		}
		if (disabledTextColor != null) {
			textComponent.setDisabledTextColor(disabledTextColor);
		}
		if (selectedTextColor != null) {
			textComponent.setSelectedTextColor(selectedTextColor);
		}
		if (selectionColor != null) {
			textComponent.setSelectionColor(selectionColor);
		}
		if (selectAllOnFocusGained) {
			textComponent.addFocusListener(new SelectAllFocusListener(textComponent));
		}
		if (moveCaretToStartOnFocusGained) {
			textComponent.addFocusListener(new MoveCaretToStartListener(textComponent));
		}
		if (moveCaretToEndOnFocusGained) {
			textComponent.addFocusListener(new MoveCaretToEndListener(textComponent));
		}
		if (onTextChanged != null) {
			textComponent.getDocument().addDocumentListener(new OnTextChangedListener(onTextChanged, textComponent));
		}

		return textComponent;
	}

	/**
	 * Creates the text component built by this builder.
	 * @return a JTextComponent or subclass
	 */
	protected abstract C createTextComponent();

	/**
	 * @return the {@link UpdateOn} policy set via {@link #updateOn(UpdateOn)}
	 */
	protected final UpdateOn updateOn() {
		return updateOn;
	}

	private void setCaretPosition(C component) {
		if (caretPosition != null) {
			component.setCaretPosition(caretPosition == CaretPosition.START ? 0 : component.getDocument().getLength());
		}
	}

	private static final class AddCaretListener implements Consumer<CaretListener> {

		private final JTextComponent textComponent;

		private AddCaretListener(JTextComponent textComponent) {
			this.textComponent = textComponent;
		}

		@Override
		public void accept(CaretListener listener) {
			textComponent.addCaretListener(listener);
		}
	}
}
