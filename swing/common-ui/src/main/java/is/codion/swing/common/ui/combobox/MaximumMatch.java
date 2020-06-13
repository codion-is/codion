package is.codion.swing.common.ui.combobox;

import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 * Code originally from: http://www.orbital-computer.de/JComboBox
 *
 * Selects an item in a JComboBox based on values typed in on the keyboard.
 * @author Thomas Bierhance
 */
public final class MaximumMatch extends CompletionDocument {

  /**
   * Enables MaximumMatch on the given combobox
   * @param comboBox the combobox
   * @param normalize if YES then accented characters are normalized before matching
   */
  private MaximumMatch(final JComboBox<?> comboBox, final Normalize normalize) {
    super(comboBox, normalize);
  }

  /**
   * @param comboBox the combobox on which to enable maximum match
   * @param <T> the type
   * @return the combo box
   */
  public static <T> JComboBox<T> enable(final JComboBox<T> comboBox) {
    return enable(comboBox, Normalize.YES);
  }

  /**
   * @param comboBox the combobox on which to enable maximum match
   * @param normalize if YES then accented characters are normalized before matching
   * @param <T> the type
   * @return the combo box
   */
  public static <T> JComboBox<T> enable(final JComboBox<T> comboBox, final Normalize normalize) {
    comboBox.setEditable(true);
    new MaximumMatch(comboBox, normalize);

    return comboBox;
  }

  @Override
  public void insertString(final int offset, final String str, final AttributeSet a) throws BadLocationException {
    int offs = offset;
    if (isSelecting() || getModel().getSize() == 0) {
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
      item = getComboBox().getSelectedItem();
      offs = offs - str.length();
    }

    if (match) {
      offs = getMaximumMatchingOffset(getText(0, getLength()), item);
    }
    else {
      offs += str.length();
    }
    setTextAccordingToSelectedItem();
    highlightCompletedText(offs);
  }

  // calculates how many characters are predetermined by the given pattern.
  private int getMaximumMatchingOffset(final String pattern, final Object selectedItem) {
    final String selectedAsString = selectedItem.toString();
    int match = selectedAsString.length();
    // look for items that match the given pattern
    for (int i = 0; i < getModel().getSize(); i++) {
      final Object currentItem = getModel().getElementAt(i);
      final String itemAsString = currentItem == null ? "" : currentItem.toString();
      if (startsWithIgnoreCase(itemAsString, pattern, isNormalize())) {
        // current item matches the pattern
        // how many leading characters have the selected and the current item in common?
        final int tmpMatch = equalStartLength(itemAsString, selectedAsString);
        if (tmpMatch < match) {
          match = tmpMatch;
        }
      }
    }

    return match;
  }

  // returns how many leading characters two strings have in common?
  private int equalStartLength(final String str1, final String str2) {
    final String one = isNormalize() ? normalize(str1) : str1;
    final String two = isNormalize() ? normalize(str2) : str2;
    final char[] ch1 = one.toUpperCase().toCharArray();
    final char[] ch2 = two.toUpperCase().toCharArray();
    final int n = Math.min(ch1.length, ch2.length);
    for (int i = 0; i < n; i++) {
      if (ch1[i] != ch2[i]) {
        return i;
      }
    }

    return n;
  }
}