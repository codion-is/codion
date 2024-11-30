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
 * <pre>
 * {@code
 *   Completion.builder()
 *       .mode(Mode.AUTOCOMPLETE)
 *       .normalize(false)
 *       .enable(comboBox);
 * }
 *</pre>
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
	 * Specifies the default completion model used for comboboxes.
	 * <ul>
	 * <li>{@link Completion.Mode#MAXIMUM_MATCH} for maximum match,
	 * <li>{@link Completion.Mode#AUTOCOMPLETE} for auto-completion or
	 * <li>{@link Completion.Mode#NONE} for no completion.
	 * <li>Value type:String
	 * <li>Default value: {@link Completion.Mode#MAXIMUM_MATCH}
	 * </ul>
	 */
	public static final PropertyValue<Mode> COMPLETION_MODE =
					Configuration.enumValue(Completion.class.getName() + ".completionMode", Mode.class, Mode.MAXIMUM_MATCH);

	/**
	 * Specifies whether strings are normalized during completion by default.
	 * <ul>
	 * <li>Value type:Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> NORMALIZE =
					Configuration.booleanValue(Completion.class.getName() + ".normalize", true);

	private Completion() {}

	/**
	 * @return a new {@link Builder} instance.
	 */
	public static Builder builder() {
		return new DefaultBuilder();
	}

	/**
	 * A builder for combo box completion.
	 */
	public interface Builder {

		/**
		 * @param mode the mode to enable
		 * @return this builder
		 */
		Builder mode(Mode mode);

		/**
		 * @param normalize true if accented characters should be normalized before matching
		 * @return this builder
		 */
		Builder normalize(boolean normalize);

		/**
		 * @param comboBox the combo box on which to enable completion
		 */
		void enable(JComboBox<?> comboBox);
	}

	private static final class DefaultBuilder implements Builder {

		private Mode mode = COMPLETION_MODE.get();
		private boolean normalize = NORMALIZE.get();

		@Override
		public Builder mode(Mode mode) {
			this.mode = requireNonNull(mode);
			return this;
		}

		@Override
		public Builder normalize(boolean normalize) {
			this.normalize = normalize;
			return this;
		}

		@Override
		public void enable(JComboBox<?> comboBox) {
			requireNonNull(comboBox);
			switch (mode) {
				case NONE:
					break;
				case AUTOCOMPLETE:
					new AutoCompletionDocument(comboBox, normalize);
					break;
				case MAXIMUM_MATCH:
					new MaximumMatchDocument(comboBox, normalize);
					break;
				default:
					throw new IllegalArgumentException("Unknown completion mode: " + mode);
			}
			comboBox.addFocusListener(new CompletionFocusListener((JTextComponent) comboBox.getEditor().getEditorComponent()));
		}
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

		private MaximumMatchDocument(JComboBox<?> comboBox, boolean normalize) {
			super(comboBox, normalize);
		}

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			if (selecting()) {
				super.insertString(offs, str, a);
			}
			else {
				trimSearchString(offs);
				Object item = lookupItem(searchPattern(str));
				boolean match = false;
				if (item != null) {
					searchString.insert(Math.min(searchString.length(), offs), str);
					match = true;
					setSelectedItem(item);
				}
				else {
					item = comboBox().getSelectedItem();
					offs = offs - str.length();
				}
				setTextAccordingToSelectedItem();
				if (match) {
					offs = maximumMatchingOffset(searchString.toString(), item);
					searchString.replace(0, searchString.length(), getText(0, offs));
				}
				else {
					offs += str.length();
				}
				highlightCompletedText(offs);
			}
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

		private AutoCompletionDocument(JComboBox<?> comboBox, boolean normalize) {
			super(comboBox, normalize);
		}

		@Override
		public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
			if (selecting()) {
				super.insertString(offs, str, a);
			}
			else {
				trimSearchString(offs);
				Object item = lookupItem(searchPattern(str));
				if (item != null) {
					searchString.insert(offs, str);
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
}
