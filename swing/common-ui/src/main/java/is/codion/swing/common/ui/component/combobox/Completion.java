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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static java.util.Objects.requireNonNull;

/**
 * Selects an item in a JComboBox based on values typed on the keyboard.
 * <p>
 * Based on code originally from: <a href="http://www.orbital-computer.de/JComboBox">http://www.orbital-computer.de/JComboBox</a><br>
 * Included with permission.
 * @author Thomas Bierhance
 * @author Björn Darri Sigurðsson
 */
public final class Completion {

	/**
	 * The available completion modes.
	 */
	public enum Mode {
		/**
		 * Identifies the completion mode MaximumMatch
		 */
		MAXIMUM_MATCH,
		/**
		 * Identifies the completion mode AutoCompletion
		 */
		AUTOCOMPLETE,
		/**
		 * Identifies the no completion mode
		 */
		NONE
	}

	/**
	 * Specifies whether to normalize accented characters in a String during autocomplete.
	 */
	public enum Normalize {
		/**
		 * The String should be normalized.
		 */
		YES,
		/**
		 * The String should not be normalized.
		 */
		NO
	}

	/**
	 * Specifies the default completion model used for comboboxes.
	 * <ul>
	 * <li>{@link Completion.Mode#MAXIMUM_MATCH} for maximum match,
	 * <li>{@link Completion.Mode#AUTOCOMPLETE} for auto-completion or
	 * <li>{@link Completion.Mode#NONE} for no completion.
	 * <li>Value type:String
	 * <li>Default value: {@link Completion.Mode#MAXIMUM_MATCH}
	 * </ul>
	 */
	public static final PropertyValue<Mode> COMBO_BOX_COMPLETION_MODE =
					Configuration.enumValue(Completion.class.getName() + ".completionMode", Mode.class, Mode.MAXIMUM_MATCH);

	/**
	 * Specifies whether strings are normalized during completion by default.
	 * <ul>
	 * <li>{@link Completion.Normalize#YES}
	 * <li>{@link Completion.Normalize#NO}
	 * <li>Value type:String
	 * <li>Default value: {@link Completion.Normalize#YES}
	 * </ul>
	 */
	public static final PropertyValue<Normalize> NORMALIZE =
					Configuration.enumValue(Completion.class.getName() + ".normalize", Normalize.class, Normalize.YES);

	private Completion() {}

	/**
	 * Enables maximum match on the given combobox
	 * @param comboBox the combobox on which to enable maximum match
	 * @param <C> the combobox type
	 * @param <T> the type
	 * @return the combo box
	 */
	public static <C extends JComboBox<T>, T> C maximumMatch(C comboBox) {
		return maximumMatch(comboBox, NORMALIZE.get());
	}

	/**
	 * Enables maximum match on the given combobox
	 * @param comboBox the combobox on which to enable maximum match
	 * @param normalize if YES then accented characters are normalized before matching
	 * @param <C> the combobox type
	 * @param <T> the type
	 * @return the combo box
	 */
	public static <C extends JComboBox<T>, T> C maximumMatch(C comboBox, Normalize normalize) {
		new MaximumMatchDocument(comboBox, normalize);

		return comboBox;
	}

	/**
	 * Enables auto-completion on the given combobox
	 * @param comboBox the combobox on which to enable autocompletion
	 * @param <C> the combobox type
	 * @param <T> the type
	 * @return the combo box
	 */
	public static <C extends JComboBox<T>, T> C autoComplete(C comboBox) {
		return autoComplete(comboBox, NORMALIZE.get());
	}

	/**
	 * Enables auto-completion on the given combobox
	 * @param comboBox the combobox on which to enable autocompletion
	 * @param normalize if YES then accented characters are normalized before matching
	 * @param <C> the combobox type
	 * @param <T> the type
	 * @return the combo box
	 */
	public static <C extends JComboBox<T>, T> C autoComplete(C comboBox, Normalize normalize) {
		new AutoCompletionDocument(comboBox, normalize);

		return comboBox;
	}

	/**
	 * Enables the default completion mode on the given combo box
	 * @param comboBox the combo box
	 * @param <C> the combo box type
	 * @param <T> the value type
	 * @return the combo box
	 * @see #COMBO_BOX_COMPLETION_MODE
	 */
	public static <C extends JComboBox<T>, T> C enable(C comboBox) {
		return enable(comboBox, COMBO_BOX_COMPLETION_MODE.get());
	}

	/**
	 * Enables the given completion mode on the given combo box
	 * @param comboBox the combo box
	 * @param completionMode the mode to enable
	 * @param <C> the combo box type
	 * @param <T> the value type
	 * @return the combo box
	 * @see #COMBO_BOX_COMPLETION_MODE
	 */
	public static <C extends JComboBox<T>, T> C enable(C comboBox, Mode completionMode) {
		requireNonNull(comboBox);
		requireNonNull(completionMode);
		switch (completionMode) {
			case NONE:
				break;
			case AUTOCOMPLETE:
				autoComplete(comboBox);
				break;
			case MAXIMUM_MATCH:
				maximumMatch(comboBox);
				break;
			default:
				throw new IllegalArgumentException("Unknown completion mode: " + completionMode);
		}
		comboBox.addFocusListener(new CompletionFocusListener((JTextComponent) comboBox.getEditor().getEditorComponent()));

		return comboBox;
	}

	/**
	 * Selects all when the field gains the focus while maintaining the cursor position at 0,
	 * selects none on focus lost.
	 */
	private static final class CompletionFocusListener implements FocusListener {

		private final JTextComponent editor;

		private CompletionFocusListener(JTextComponent editor) {
			this.editor = editor;
		}

		@Override
		public void focusGained(FocusEvent e) {
			int length = editor.getText().length();
			if (length > 0) {
				editor.setCaretPosition(length);
				editor.moveCaretPosition(0);
			}
		}

		@Override
		public void focusLost(FocusEvent e) {
			editor.select(0, 0);
		}
	}

	private static final class MaximumMatchDocument extends CompletionDocument {

		private MaximumMatchDocument(JComboBox<?> comboBox, Normalize normalize) {
			super(comboBox, normalize == Normalize.YES);
		}

		@Override
		public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
			int offs = offset;
			if (selecting() || comboBoxModel().getSize() == 0) {
				return;
			}
			super.insertString(offs, str, a);
			boolean match = false;
			Object item = lookupItem(getText(0, getLength()));
			if (item != null) {
				match = true;
				setSelectedItem(item);
			}
			else {
				item = comboBox().getSelectedItem();
				offs = offs - str.length();
			}

			if (match) {
				offs = maximumMatchingOffset(getText(0, getLength()), item);
			}
			else {
				offs += str.length();
			}
			setTextAccordingToSelectedItem();
			highlightCompletedText(offs);
		}

		// calculates how many characters are predetermined by the given pattern.
		private int maximumMatchingOffset(String pattern, Object selectedItem) {
			String selectedAsString = selectedItem.toString();
			int match = selectedAsString.length();
			// look for items that match the given pattern
			for (int i = 0; i < comboBoxModel().getSize(); i++) {
				Object currentItem = comboBoxModel().getElementAt(i);
				String itemAsString = currentItem == null ? "" : currentItem.toString();
				if (startsWithIgnoreCase(itemAsString, pattern, normalize())) {
					// current item matches the pattern
					// how many leading characters have the selected and the current item in common?
					int tmpMatch = equalStartLength(itemAsString, selectedAsString);
					if (tmpMatch < match) {
						match = tmpMatch;
					}
				}
			}

			return match;
		}

		// returns how many leading characters two strings have in common?
		private int equalStartLength(String str1, String str2) {
			String one = normalize() ? normalize(str1) : str1;
			String two = normalize() ? normalize(str2) : str2;
			char[] ch1 = one.toUpperCase().toCharArray();
			char[] ch2 = two.toUpperCase().toCharArray();
			int n = Math.min(ch1.length, ch2.length);
			for (int i = 0; i < n; i++) {
				if (ch1[i] != ch2[i]) {
					return i;
				}
			}

			return n;
		}
	}

	private static final class AutoCompletionDocument extends CompletionDocument {

		private AutoCompletionDocument(JComboBox<?> comboBox, Normalize normalize) {
			super(comboBox, normalize == Normalize.YES);
		}

		@Override
		public void insertString(int offset, String str, AttributeSet a) throws BadLocationException {
			int offs = offset;
			if (selecting()) {
				return;
			}
			super.insertString(offs, str, a);
			Object item = lookupItem(getText(0, getLength()));
			if (item != null) {
				setSelectedItem(item);
			}
			else {
				offs = offs - str.length();
			}
			setTextAccordingToSelectedItem();
			highlightCompletedText(offs + str.length());
		}
	}
}
