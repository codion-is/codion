package org.jminor.common.ui.combobox;

import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 * Code originally from: http://www.orbital-computer.de/JComboBox
 * <p/>
 * Selects an item in a JComboBox based on values typed in on the keyboard.
 * @author Thomas Bierhance
 */
public final class AutoCompletion extends CompletionDocument {

  public AutoCompletion(final JComboBox comboBox, final boolean showPopupOnMatch, final boolean normalize) {
    super(comboBox, showPopupOnMatch, normalize);
  }

  /**
   * @param comboBox the combobox on which to enable autocompletion
   */
  public static void enable(final JComboBox comboBox) {
    enable(comboBox, false);
  }

  /**
   * @param comboBox the combobox on which to enable autocompletion
   */
  public static void enable(final JComboBox comboBox, final boolean showPopupOnMatch) {
    enable(comboBox, showPopupOnMatch, true);
  }

  /**
   * @param comboBox the combobox on which to enable autocompletion
   * @param normalize if true then accented characters are normalized before matching
   */
  public static void enable(final JComboBox comboBox, final boolean showPopupOnMatch, final boolean normalize) {
    comboBox.setEditable(true);
    new AutoCompletion(comboBox, showPopupOnMatch, normalize);
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