package org.jminor.swing.common.ui.combobox;

import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 * Code originally from: http://www.orbital-computer.de/JComboBox
 * Selects an item in a JComboBox based on values typed in on the keyboard.
 * @author Thomas Bierhance
 */
public final class AutoCompletion extends CompletionDocument {

  /**
   * @param comboBox the combo box to auto complete
   * @param normalize if true then accented characters are normalized before matching
   */
  public AutoCompletion(final JComboBox comboBox, final Normalize normalize) {
    super(comboBox, normalize);
  }

  /**
   * @param comboBox the combobox on which to enable autocompletion
   */
  public static void enable(final JComboBox comboBox) {
    enable(comboBox, Normalize.YES);
  }

  /**
   * @param comboBox the combobox on which to enable autocompletion
   * @param normalize if YES then accented characters are normalized before matching
   */
  public static void enable(final JComboBox comboBox, final Normalize normalize) {
    comboBox.setEditable(true);
    new AutoCompletion(comboBox, normalize);
  }

  /** {@inheritDoc} */
  @Override
  public void insertString(final int offset, final String str, final AttributeSet a) throws BadLocationException {
    int offs = offset;
    if (isSelecting()) {
      return;
    }
    super.insertString(offs, str, a);
    final Object item = lookupItem(getText(0, getLength()));
    if (item != null) {
      setSelectedItem(item);
    }
    else {
      offs = offs - str.length();
      getComboBox().getToolkit().beep();
    }
    setTextAccordingToSelectedItem();
    highlightCompletedText(offs + str.length());
  }
}