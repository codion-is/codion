/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
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
 * @author Thomas Bierhance
 */
public class MaximumMatch extends PlainDocument {

  private final JComboBox comboBox;
  private ComboBoxModel model;
  private JTextComponent editor;
  // flag to indicate if setSelectedItem has been called
  // subsequent calls to remove/insertString should be ignored
  private boolean selecting = false;
  private boolean hidePopupOnFocusLoss;
  private boolean hitBackspace = false;
  private boolean hitBackspaceOnSelection;

  public MaximumMatch(final JComboBox comboBox) {
    this.comboBox = comboBox;
    model = comboBox.getModel();
    editor = (JTextComponent) comboBox.getEditor().getEditorComponent();
    editor.setDocument(this);
    comboBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (!selecting)
          highlightCompletedText(0);
      }
    });
    editor.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (comboBox.isDisplayable() && Character.isLetterOrDigit(e.getKeyChar()))
          comboBox.setPopupVisible(true);
        hitBackspace=false;
        switch (e.getKeyCode()) {
          // determine if the pressed key is backspace (needed by the remove method)
          case KeyEvent.VK_BACK_SPACE :
            hitBackspace=true;
            hitBackspaceOnSelection=editor.getSelectionStart()!=editor.getSelectionEnd();
            break;
            // ignore delete key
          case KeyEvent.VK_DELETE :
            e.consume();
            comboBox.getToolkit().beep();
            break;
        }
      }
    });
    // Bug 5100422 on Java 1.5: Editable JComboBox won't hide popup when tabbing out
    hidePopupOnFocusLoss=System.getProperty("java.version").startsWith("1.5");
    // Highlight whole text when gaining focus
    editor.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        highlightCompletedText(0);
      }
      @Override
      public void focusLost(FocusEvent e) {
        // Workaround for Bug 5100422 - Hide Popup on focus loss
        if (hidePopupOnFocusLoss)
          comboBox.setPopupVisible(false);
      }
    });
    // Handle initially selected object
    Object selected = comboBox.getSelectedItem();
    if (selected!=null)
      setText(selected.toString());
    highlightCompletedText(0);
  }

  public static void enable(JComboBox comboBox) {
    // has to be editable
    comboBox.setEditable(true);
    // change the editor's document
    new MaximumMatch(comboBox);
  }

  /** {@inheritDoc} */
  @Override
  public void remove(int offs, int len) throws BadLocationException {
    // return immediately when selecting an item
    if (selecting)
      return;
    if (hitBackspace) {
      // user hit backspace => move the selection backwards
      // old item keeps being selected
      if (offs>0) {
        if (hitBackspaceOnSelection)
          offs--;
      }
      else {
        // User hit backspace with the cursor positioned on the start => beep
        UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
      }
      highlightCompletedText(offs);
    }
    else {
      super.remove(offs, len);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
    // return immediately when selecting an item
    if (selecting || model.getSize() == 0)
      return;
    // insert the string into the document
    super.insertString(offs, str, a);
    // lookup and select a matching item
    boolean match=false;
    Object item = lookupItem(getText(0, getLength()));
    if (item != null) {
      match=true;
      setSelectedItem(item);
    }
    else {
      // keep old item selected if there is no match, possibly a null item
      item = comboBox.getSelectedItem();
      // imitate no insert (later on offs will be incremented by str.length(): selection won't move forward)
      offs = offs-str.length();
      // provide feedback to the user that his input has been received but can not be accepted
      UIManager.getLookAndFeel().provideErrorFeedback(comboBox);
    }

    if (match)
      offs = getMaximumMatchingOffset(getText(0, getLength()), item);
    else
      offs+=str.length();

    setText(item == null ? "" : item.toString());
    // select the completed part
    highlightCompletedText(offs);
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
      throw new RuntimeException(e.toString());
    }
  }

  private void highlightCompletedText(int start) {
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

  private Object lookupItem(String pattern) {
    Object selectedItem = model.getSelectedItem();
    // only search for a different item if the currently selected does not match
    if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern)) {
      return selectedItem;
    }
    else {
      // iterate over all items
      for (int i=0, n=model.getSize(); i < n; i++) {
        Object currentItem = model.getElementAt(i);
        // current item starts with the pattern?
        if (startsWithIgnoreCase(currentItem.toString(), pattern)) {
          return currentItem;
        }
      }
    }
    // no item starts with the pattern => return null
    return null;
  }

  // checks if str1 starts with str2 - ignores case
  private boolean startsWithIgnoreCase(String str1, String str2) {
    return str1.toUpperCase().startsWith(str2.toUpperCase());
  }

  // calculates how many characters are predetermined by the given pattern.
  private int getMaximumMatchingOffset(String pattern, Object selectedItem) {
    String selectedAsString=selectedItem.toString();
    int match=selectedAsString.length();
    // look for items that match the given pattern
    for (int i=0, n=model.getSize(); i < n; i++) {
      Object currentItem = model.getElementAt(i);
      String itemAsString = currentItem.toString();
      if (startsWithIgnoreCase(itemAsString, pattern)) {
        // current item matches the pattern
        // how many leading characters have the selected and the current item in common?
        int tmpMatch=equalStartLength(itemAsString, selectedAsString);
        if (tmpMatch < match)
          match=tmpMatch;
      }
    }
    return match;
  }

  // returns how many leading characters two strings have in common?
  private static int equalStartLength(String str1, String str2) {
    char[] ch1 = str1.toUpperCase().toCharArray();
    char[] ch2 = str2.toUpperCase().toCharArray();
    int n = ch1.length>ch2.length?ch2.length:ch1.length;
    for (int i=0; i<n; i++) {
      if (ch1[i]!=ch2[i])
        return i;
    }
    return n;
  }
}