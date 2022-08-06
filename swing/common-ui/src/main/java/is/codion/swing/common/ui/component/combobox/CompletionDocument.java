/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.BadLocationException;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.Normalizer;

import static java.util.Objects.requireNonNull;

/**
 * A base class for auto-completion and maximum match
 * @author Thomas Bierhance
 */
class CompletionDocument extends PlainDocument {

  private final JComboBox<?> comboBox;
  private final ComboBoxModel<?> model;
  private final boolean normalize;
  // flag to indicate if setSelectedItem has been called
  // subsequent calls to remove/insertString should be ignored
  private boolean selecting = false;
  private boolean hitBackspace = false;
  private boolean hitBackspaceOnSelection;

  private JTextComponent editorComponent;

  protected CompletionDocument(JComboBox<?> comboBox, boolean normalize) {
    this.comboBox = requireNonNull(comboBox);
    this.comboBox.setEditable(true);
    this.normalize = normalize;
    this.model = comboBox.getModel();
    setEditorComponent((JTextComponent) comboBox.getEditor().getEditorComponent());
    comboBox.addPropertyChangeListener("editor", new EditorChangedListener());
    comboBox.addActionListener(new HighlightCompletedOnActionPerformedListener());
    setTextAccordingToSelectedItem();
    highlightCompletedText(0);
  }

  @Override
  public final void remove(int offset, int len) throws BadLocationException {
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

  protected final JComboBox<?> comboBox() {
    return comboBox;
  }

  protected final ComboBoxModel<?> model() {
    return model;
  }

  protected final boolean isNormalize() {
    return normalize;
  }

  protected final boolean isSelecting() {
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

  protected final void highlightCompletedText(int start) {
    editorComponent.setCaretPosition(getLength());
    editorComponent.moveCaretPosition(start);
  }

  /**
   * @param item Value to set for property 'selectedItem'.
   */
  protected final void setSelectedItem(Object item) {
    selecting = true;
    model.setSelectedItem(item);
    selecting = false;
  }

  protected final Object lookupItem(String pattern) {
    Object selectedItem = model.getSelectedItem();
    // only search for a different item if the currently selected does not match
    if (selectedItem != null && startsWithIgnoreCase(selectedItem.toString(), pattern, normalize)) {
      return selectedItem;
    }
    else {
      for (int i = 0; i < model.getSize(); i++) {
        Object currentItem = model.getElementAt(i);
        // current item starts with the pattern?
        if (currentItem != null && startsWithIgnoreCase(currentItem.toString(), pattern, normalize)) {
          return currentItem;
        }
      }
    }
    // no item starts with the pattern => return null
    return null;
  }

  protected static boolean startsWithIgnoreCase(String str1, String str2, boolean normalize) {
    String one = normalize ? normalize(str1) : str1;
    String two = normalize ? normalize(str2) : str2;

    return one.toUpperCase().startsWith(two.toUpperCase());
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

  private final class MatchKeyAdapter extends KeyAdapter {

    @Override
    public void keyPressed(KeyEvent e) {
      hitBackspace = false;
      switch (e.getKeyCode()) {
        // determine if the pressed key is backspace (needed by the remove method)
        case KeyEvent.VK_BACK_SPACE:
          hitBackspace = true;
          hitBackspaceOnSelection = editorComponent.getSelectionStart() != editorComponent.getSelectionEnd();
          break;
        // ignore delete key
        case KeyEvent.VK_DELETE:
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
}
