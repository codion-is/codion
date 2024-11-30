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
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.combobox;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Normalizer;

import static java.awt.event.KeyEvent.VK_BACK_SPACE;
import static java.awt.event.KeyEvent.VK_DELETE;
import static java.util.Objects.requireNonNull;

/**
 * A base class for auto-completion and maximum match.<br>
 * Based on code originally from: <a href="http://www.orbital-computer.de/JComboBox">http://www.orbital-computer.de/JComboBox</a><br>
 * Included with permission.
 * @author Thomas Bierhance
 * @author Björn Darri Sigurðsson
 */
class CompletionDocument extends PlainDocument {

	private final JComboBox<?> comboBox;
	private final ComboBoxModel<?> comboBoxModel;
	private final boolean normalize;
	protected final StringBuilder searchString = new StringBuilder();
	// flag to indicate if setSelectedItem has been called
	// subsequent calls to remove/insertString should be ignored
	private boolean selecting = false;
	private boolean hitBackspace = false;
	private boolean hitBackspaceOnSelection;

	private JTextComponent editorComponent;

	private CompletionDocument(JComboBox<?> comboBox, boolean normalize) {
		this.comboBox = requireNonNull(comboBox);
		this.comboBox.setEditable(true);
		this.normalize = normalize;
		this.comboBoxModel = comboBox.getModel();
		setEditorComponent((JTextComponent) comboBox.getEditor().getEditorComponent());
		comboBox.addPropertyChangeListener("editor", new EditorChangedListener());
		comboBox.addActionListener(new HighlightCompletedOnActionPerformedListener());
		setTextAccordingToSelectedItem();
		highlightCompletedText(0);
	}

	@Override
	public void replace(int offset, int length, String string, AttributeSet attrs) throws BadLocationException {
		if (selecting) {
			super.replace(offset, length, string, attrs);
		}
		else if (string != null && string.length() > 0) {
			if (length > 0) {
				remove(offset, length);
			}
			insertString(offset, string, attrs);
		}
	}

	@Override
	public final void remove(int offs, int length) throws BadLocationException {
		if (selecting) {
			super.remove(offs, length);
		}
		else if (hitBackspace) {
			hitBackspace = false;
			boolean selectFirst = false;
			// user hit backspace => move the selection backwards
			// old item keeps being selected unless we've backspaced beyond the first character
			if (searchString.length() > 0) {
				searchString.replace(searchString.length() - 1, searchString.length(), "");
			}
			if (offs > 0) {
				if (hitBackspaceOnSelection) {
					offs--;
					if (offs == 0) {
						selectFirst = true;
					}
				}
			}
			else {
				selectFirst = true;
			}
			if (selectFirst) {
				setSelectedItem(comboBoxModel.getElementAt(0));
				setTextAccordingToSelectedItem();
			}
			highlightCompletedText(offs);
		}
	}

	protected final JComboBox<?> comboBox() {
		return comboBox;
	}

	protected final ComboBoxModel<?> comboBoxModel() {
		return comboBoxModel;
	}

	protected final boolean normalize() {
		return normalize;
	}

	protected final boolean selecting() {
		return selecting;
	}

	protected final void setTextAccordingToSelectedItem() {
		Object item = comboBox.getSelectedItem();
		String text = item == null ? "" : item.toString();
		try {
			// remove all text and insert the completed string
			super.remove(0, getLength());
			super.insertString(0, text, null);
		}
		catch (BadLocationException e) {
			throw new RuntimeException(e);
		}
	}

	protected final void trimSearchString(int offset) {
		searchString.replace(Math.min(searchString.length(), offset), searchString.length(), "");
	}

	protected final String searchPattern(String string) {
		return new StringBuilder(searchString).insert(searchString.length(), string).toString();
	}

	protected final void highlightCompletedText(int start) {
		editorComponent.setCaretPosition(getLength());
		editorComponent.moveCaretPosition(start);
	}

	protected final void setSelectedItem(Object item) {
		selecting = true;
		comboBoxModel.setSelectedItem(item);
		selecting = false;
	}

	protected final Object lookupItem(String startsWith) {
		Object selectedItem = comboBoxModel.getSelectedItem();
		// only search for a different item if the currently selected does not match
		if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), startsWith, normalize)) {
			return selectedItem;
		}
		for (int i = 0; i < comboBoxModel.getSize(); i++) {
			Object currentItem = comboBoxModel.getElementAt(i);
			if (currentItem != null && startsWithIgnoreCase(currentItem.toString(), startsWith, normalize)) {
				return currentItem;
			}
		}
		// no item starts with the pattern => return null
		return null;
	}

	protected static boolean startsWithIgnoreCase(String string, String startsWith, boolean normalize) {
		string = normalize ? normalize(string) : string;
		startsWith = normalize ? normalize(startsWith) : startsWith;

		return string.toUpperCase().startsWith(startsWith.toUpperCase());
	}

	protected static String normalize(String string) {
		//http://stackoverflow.com/a/4225698/317760
		return Normalizer.normalize(string, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
	}

	private void setEditorComponent(JTextComponent component) {
		editorComponent = component;
		if (editorComponent.getDocument() instanceof CompletionDocument) {
			throw new IllegalStateException("Completion has already been set for combo box");
		}
		editorComponent.setDocument(this);
		editorComponent.addKeyListener(new MatchKeyAdapter());
		editorComponent.addFocusListener(new HighlightCompletedOnFocusGainedListener());
	}

	static final class MaximumMatchDocument extends CompletionDocument {

		MaximumMatchDocument(JComboBox<?> comboBox, boolean normalize) {
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

	static final class AutoCompletionDocument extends CompletionDocument {

		AutoCompletionDocument(JComboBox<?> comboBox, boolean normalize) {
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

	private final class MatchKeyAdapter extends KeyAdapter {

		@Override
		public void keyPressed(KeyEvent e) {
			hitBackspace = false;
			switch (e.getKeyCode()) {
				// determine if the pressed key is backspace (needed by the remove method)
				case VK_BACK_SPACE:
					hitBackspace = true;
					hitBackspaceOnSelection = editorComponent.getSelectionStart() != editorComponent.getSelectionEnd();
					break;
				// ignore delete key
				case VK_DELETE:
					e.consume();
					break;
				default:
					break;
			}
		}
	}

	private final class EditorChangedListener implements PropertyChangeListener {
		@Override
		public void propertyChange(PropertyChangeEvent event) {
			ComboBoxEditor editor = (ComboBoxEditor) event.getNewValue();
			if (editor != null) {
				setEditorComponent((JTextComponent) editor.getEditorComponent());
			}
		}
	}

	private final class HighlightCompletedOnActionPerformedListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent e) {
			if (!selecting) {
				highlightCompletedText(0);
			}
		}
	}

	private final class HighlightCompletedOnFocusGainedListener extends FocusAdapter {
		@Override
		public void focusGained(FocusEvent e) {
			highlightCompletedText(0);
		}
	}

	/**
	 * Selects all when the field gains the focus while maintaining the cursor position at 0,
	 * selects none on focus lost.
	 */
	static final class CompletionFocusListener implements FocusListener {

		private final JTextComponent editor;

		CompletionFocusListener(JTextComponent editor) {
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
}
