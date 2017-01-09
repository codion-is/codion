package org.jminor.swing.common.ui.combobox;

import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.Normalizer;

/**
 * A base class for auto completion and maximum match
 * @author Thomas Bierhance
 */
class CompletionDocument extends PlainDocument {

  private final JComboBox comboBox;
  private final ComboBoxModel model;
  private final JTextComponent editor;
  private final boolean normalize;
  // flag to indicate if setSelectedItem has been called
  // subsequent calls to remove/insertString should be ignored
  private boolean selecting = false;
  private boolean hitBackspace = false;
  private boolean hitBackspaceOnSelection;

  public CompletionDocument(final JComboBox comboBox, final boolean showPopupOnMatch, final boolean normalize) {
    this.comboBox = comboBox;
    this.normalize = normalize;
    model = comboBox.getModel();
    editor = (JTextComponent) comboBox.getEditor().getEditorComponent();
    editor.setDocument(this);
    comboBox.addActionListener(e -> {
      if (!selecting) {
        highlightCompletedText(0);
      }
    });
    editor.addKeyListener(new MatchKeyAdapter(showPopupOnMatch));
    editor.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        highlightCompletedText(0);
      }
    });
    setTextAccordingToSelectedItem();
    highlightCompletedText(0);
  }

  /** {@inheritDoc} */
  @Override
  public final void remove(final int offset, final int len) throws BadLocationException {
    int offs = offset;
    if (selecting) {
      return;
    }
    if (hitBackspace) {
      hitBackspace = false;
      boolean selectFirst = false;
      // user hit backspace => move the selection backwards
      // old item keeps being selected unless we've backspaced beyond the first character
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
      if (selectFirst && model.getSize() > 0) {
        setSelectedItem(model.getElementAt(0));
        setTextAccordingToSelectedItem();
      }
      highlightCompletedText(offs);
    }
    else {
      super.remove(offs, len);
    }
  }

  protected final JComboBox getComboBox() {
    return comboBox;
  }

  protected final ComboBoxModel getModel() {
    return model;
  }

  protected final boolean isNormalize() {
    return normalize;
  }

  protected final boolean isSelecting() {
    return selecting;
  }

  protected final void setTextAccordingToSelectedItem() {
    final Object item = comboBox.getSelectedItem();
    final String text = item == null ? "" : item.toString();
    try {
      // remove all text and insert the completed string
      super.remove(0, getLength());
      super.insertString(0, text, null);
    }
    catch (final BadLocationException e) {
      throw new RuntimeException(e);
    }
  }

  protected final void highlightCompletedText(final int start) {
    editor.setCaretPosition(getLength());
    editor.moveCaretPosition(start);
  }

  /**
   * @param item Value to set for property 'selectedItem'.
   */
  protected final void setSelectedItem(final Object item) {
    selecting = true;
    model.setSelectedItem(item);
    selecting = false;
  }

  protected final Object lookupItem(final String pattern) {
    final Object selectedItem = model.getSelectedItem();
    // only search for a different item if the currently selected does not match
    if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern, normalize)) {
      return selectedItem;
    }
    else {
      for (int i = 0; i < model.getSize(); i++) {
        final Object currentItem = model.getElementAt(i);
        // current item starts with the pattern?
        if (currentItem != null && startsWithIgnoreCase(currentItem.toString(), pattern, normalize)) {
          return currentItem;
        }
      }
    }
    // no item starts with the pattern => return null
    return null;
  }

  protected static boolean startsWithIgnoreCase(final String str1, final String str2, final boolean normalize) {
    final String one = normalize ? normalize(str1) : str1;
    final String two = normalize ? normalize(str2) : str2;

    return one.toUpperCase().startsWith(two.toUpperCase());
  }

  protected static String normalize(final String str) {
    //http://stackoverflow.com/a/4225698/317760
    return Normalizer.normalize(str, Normalizer.Form.NFD).replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
  }

  private final class MatchKeyAdapter extends KeyAdapter {

    private final boolean showPopupOnMatch;

    public MatchKeyAdapter(final boolean showPopupOnMatch) {
      this.showPopupOnMatch = showPopupOnMatch;
    }

    @Override
    public void keyPressed(final KeyEvent e) {
      if (showPopupOnMatch && comboBox.isDisplayable()) {
        comboBox.setPopupVisible(true);
      }
      hitBackspace = false;
      switch (e.getKeyCode()) {
        // determine if the pressed key is backspace (needed by the remove method)
        case KeyEvent.VK_BACK_SPACE:
          hitBackspace = true;
          hitBackspaceOnSelection = editor.getSelectionStart() != editor.getSelectionEnd();
          break;
        // ignore delete key
        case KeyEvent.VK_DELETE:
          e.consume();
          comboBox.getToolkit().beep();
          break;
        default:
          break;
      }
    }
  }
}
