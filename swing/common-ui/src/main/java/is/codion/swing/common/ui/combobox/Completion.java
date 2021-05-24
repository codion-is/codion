package is.codion.swing.common.ui.combobox;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;

import javax.swing.JComboBox;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import static java.util.Objects.requireNonNull;

/**
 * Code originally from: http://www.orbital-computer.de/JComboBox
 * Selects an item in a JComboBox based on values typed in on the keyboard.
 * @author Thomas Bierhance
 */
public final class Completion {

  /**
   * Identifies the completion mode MaximumMatch
   * @see Completion#maximumMatch(JComboBox)
   */
  public static final String COMPLETION_MODE_MAXIMUM_MATCH = "max";

  /**
   * Identifies the completion mode AutoCompletion
   * @see Completion#autoComplete(JComboBox)
   */
  public static final String COMPLETION_MODE_AUTOCOMPLETE = "auto";

  /**
   * No completion.
   * @see Completion#autoComplete(JComboBox)
   */
  public static final String COMPLETION_MODE_NONE = "none";

  /**
   * Specifies whether maximum match or autocomplete is used for comboboxes,
   * {@link Completion#COMPLETION_MODE_MAXIMUM_MATCH} for maximum match
   * and {@link Completion#COMPLETION_MODE_AUTOCOMPLETE} for auto completion.<br>
   * Value type:String<br>
   * Default value: {@link Completion#COMPLETION_MODE_MAXIMUM_MATCH}
   */
  public static final PropertyValue<String> COMBO_BOX_COMPLETION_MODE = Configuration.stringValue("codion.swing.comboBoxCompletionMode", COMPLETION_MODE_MAXIMUM_MATCH);

  /**
   * Enables maximum match on the given combobox
   * @param comboBox the combobox on which to enable maximum match
   * @param <C> the combobox type
   * @param <T> the type
   * @return the combo box
   */
  public static <C extends JComboBox<T>, T> C maximumMatch(final C comboBox) {
    return maximumMatch(comboBox, Normalize.YES);
  }

  /**
   * Enables maximum match on the given combobox
   * @param comboBox the combobox on which to enable maximum match
   * @param normalize if YES then accented characters are normalized before matching
   * @param <C> the combobox type
   * @param <T> the type
   * @return the combo box
   */
  public static <C extends JComboBox<T>, T> C maximumMatch(final C comboBox, final Normalize normalize) {
    new MaximumMatchDocument(comboBox, normalize);
    comboBox.setEditable(true);

    return comboBox;
  }

  /**
   * Enables auto completion on the given combobox
   * @param comboBox the combobox on which to enable autocompletion
   * @param <C> the combobox type
   * @param <T> the type
   * @return the combo box
   */
  public static <C extends JComboBox<T>, T> C autoComplete(final C comboBox) {
    return autoComplete(comboBox, Normalize.YES);
  }

  /**
   * Enables auto completion on the given combobox
   * @param comboBox the combobox on which to enable autocompletion
   * @param normalize if YES then accented characters are normalized before matching
   * @param <C> the combobox type
   * @param <T> the type
   * @return the combo box
   */
  public static <C extends JComboBox<T>, T> C autoComplete(final C comboBox, final Normalize normalize) {
    new AutoCompletionDocument(comboBox, normalize);
    comboBox.setEditable(true);

    return comboBox;
  }

  /**
   * Enables the default completion type on the given combo box
   * @param comboBox the combo box
   * @see #COMBO_BOX_COMPLETION_MODE
   */
  public static void enableComboBoxCompletion(final JComboBox<?> comboBox) {
    requireNonNull(comboBox);
    final String completionMode = COMBO_BOX_COMPLETION_MODE.get();
    switch (completionMode) {
      case COMPLETION_MODE_NONE:
        break;
      case COMPLETION_MODE_AUTOCOMPLETE:
        autoComplete(comboBox);
        break;
      case COMPLETION_MODE_MAXIMUM_MATCH:
        maximumMatch(comboBox);
        break;
      default:
        throw new IllegalArgumentException("Unknown completion mode: " + completionMode);
    }
  }

  /**
   * Specifies whether to normalize accented characters in a String.
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

  private static final class MaximumMatchDocument extends CompletionDocument {

    private MaximumMatchDocument(final JComboBox<?> comboBox, final Normalize normalize) {
      super(comboBox, normalize == Normalize.YES);
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

  private static final class AutoCompletionDocument extends CompletionDocument {

    private AutoCompletionDocument(final JComboBox<?> comboBox, final Normalize normalize) {
      super(comboBox, normalize == Normalize.YES);
    }

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
      }
      setTextAccordingToSelectedItem();
      highlightCompletedText(offs + str.length());
    }
  }
}
