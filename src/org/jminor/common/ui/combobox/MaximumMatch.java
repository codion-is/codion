package org.jminor.common.ui.combobox;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.UIManager;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/**
 * Selects an item in a JComboBox based values typed in on the keyboard.
 * @author Thomas Bierhance
 */
public final class MaximumMatch extends PlainDocument {

  private final JComboBox comboBox;
  private final ComboBoxModel model;
  private final JTextComponent editor;
  // flag to indicate if setSelectedItem has been called
  // subsequent calls to remove/insertString should be ignored
  private boolean selecting = false;
  private final boolean hidePopupOnFocusLoss;
  private boolean hitBackspace = false;
  private boolean hitBackspaceOnSelection;

  /**
   * Activates the maximum match on the given combo box
   * @param comboBox the combo box
   */
  public MaximumMatch(final JComboBox comboBox) {
    this.comboBox = comboBox;
    model = comboBox.getModel();
    editor = (JTextComponent) comboBox.getEditor().getEditorComponent();
    editor.setDocument(this);
    comboBox.addActionListener(new ActionListener() {
      public void actionPerformed(final ActionEvent e) {
        if (!selecting) {
          highlightCompletedText(0);
        }
      }
    });
    editor.addKeyListener(new MatchKeyListener());
    // Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when tabbing out
    hidePopupOnFocusLoss=System.getProperty("java.version").startsWith("1.5");
    // Highlight whole text when gaining focus
    editor.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        highlightCompletedText(0);
      }
      @Override
      public void focusLost(final FocusEvent e) {
        // Workaround for Bug 5100422 - Hide Popup on focus loss
        if (hidePopupOnFocusLoss) {
          comboBox.setPopupVisible(false);
        }
      }
    });
    // Handle initially selected object
    final Object selected = comboBox.getSelectedItem();
    if (selected!=null) {
      setText(selected.toString());
    }
    highlightCompletedText(0);
  }

  /**
   * Enables maximum match for the given combo box.
   * A side effect of this method is that the combo box is made editable.
   * @param comboBox the combo box
   */
  public static void enable(final JComboBox comboBox) {
    // has to be editable
    comboBox.setEditable(true);
    // change the editor's document
    new MaximumMatch(comboBox);
  }

  /** {@inheritDoc} */
  @Override
  public void remove(final int offs, final int len) throws BadLocationException {
    int offset = offs;
    // return immediately when selecting an item
    if (selecting) {
      return;
    }
    if (hitBackspace) {
      // user hit backspace => move the selection backwards
      // old item keeps being selected
      if (offset > 0) {
        if (hitBackspaceOnSelection) {
          offset--;
        }
      }
      else {
        // User hit backspace with the cursor positioned on the start => beep
        UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
      }
      highlightCompletedText(offset);
    }
    else {
      super.remove(offset, len);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
    int offset = offs;
    // return immediately when selecting an item
    if (selecting || model.getSize() == 0) {
      return;
    }
    // insert the string into the document
    super.insertString(offset, str, a);
    // lookup and select a matching item
    boolean match = false;
    Object item = lookupItem(getText(0, getLength()));
    if (item != null) {
      match=true;
      setSelectedItem(item);
    }
    else {
      // keep old item selected if there is no match, possibly a null item
      item = comboBox.getSelectedItem();
      // imitate no insert (later on offs will be incremented by str.length(): selection won't move forward)
      offset = offset - str.length();
      // provide feedback to the user that his input has been received but can not be accepted
      UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
    }

    if (match) {
      offset = getMaximumMatchingOffset(getText(0, getLength()), item);
    }
    else {
      offset += str.length();
    }

    setText(item == null ? "" : item.toString());
    // select the completed part
    highlightCompletedText(offset);
  }

  /**
   * @param text the text to set
   */
  private void setText(final String text) {
    try {
      // remove all text and insert the completed string
      super.remove(0, getLength());
      super.insertString(0, text, null);
    }
    catch (BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  private void highlightCompletedText(final int start) {
    editor.setCaretPosition(getLength());
    editor.moveCaretPosition(start);
  }

  /**
   * @param item the item to select
   */
  private void setSelectedItem(final Object item) {
    selecting = true;
    model.setSelectedItem(item);
    selecting = false;
  }

  private Object lookupItem(final String pattern) {
    final Object selectedItem = model.getSelectedItem();
    // only search for a different item if the currently selected does not match
    if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern)) {
      return selectedItem;
    }
    // iterate over all items
    final int n = model.getSize();
    for (int i=0; i < n; i++) {
      final Object currentItem = model.getElementAt(i);
      // current item starts with the pattern?
      if (startsWithIgnoreCase(currentItem.toString(), pattern)) {
        return currentItem;
      }
    }
    // no item starts with the pattern => return null
    return null;
  }

  // checks if str1 starts with str2 - ignores case
  private boolean startsWithIgnoreCase(final String str1, final String str2) {
    return str1.toLowerCase().startsWith(str2.toLowerCase());
  }

  // calculates how many characters are predetermined by the given pattern.
  private int getMaximumMatchingOffset(final String pattern, final Object selectedItem) {
    final String selectedAsString=selectedItem.toString();
    int match=selectedAsString.length();
    // look for items that match the given pattern
    final int n = model.getSize();
    for (int i=0; i < n; i++) {
      final Object currentItem = model.getElementAt(i);
      if (currentItem == null) {
        return 0;
      }
      final String itemAsString = currentItem.toString();
      if (startsWithIgnoreCase(itemAsString, pattern)) {
        // current item matches the pattern
        // how many leading characters have the selected and the current item in common?
        final int tmpMatch=equalStartLength(itemAsString, selectedAsString);
        if (tmpMatch < match) {
          match = tmpMatch;
        }
      }
    }
    return match;
  }

  // returns how many leading characters two strings have in common?
  private static int equalStartLength(final String str1, final String str2) {
    final char[] ch1 = str1.toUpperCase().toCharArray();
    final char[] ch2 = str2.toUpperCase().toCharArray();
    final int n = ch1.length>ch2.length?ch2.length:ch1.length;
    for (int i=0; i<n; i++) {
      if (ch1[i]!=ch2[i]) {
        return i;
      }
    }
    return n;
  }

  private final class MatchKeyListener extends KeyAdapter {
    @Override
    public void keyPressed(final KeyEvent e) {
      if (comboBox.isDisplayable() && Character.isLetterOrDigit(e.getKeyChar())) {
        comboBox.setPopupVisible(true);
      }
      hitBackspace=false;
      switch (e.getKeyCode()) {
        // determine if the pressed key is backspace (needed by the remove method)
        case KeyEvent.VK_BACK_SPACE:
          hitBackspace=true;
          hitBackspaceOnSelection=editor.getSelectionStart()!=editor.getSelectionEnd();
          return;
        // ignore delete key
        case KeyEvent.VK_DELETE:
          e.consume();
          comboBox.getToolkit().beep();
          return;
      }
      editor.getParent().dispatchEvent(e);
    }
  }
}