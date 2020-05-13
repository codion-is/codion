/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.textfield;

import dev.codion.common.i18n.Messages;
import dev.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.Document;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import static java.util.Objects.requireNonNull;

/**
 * A panel that includes a JTextField in a BorderLayout.CENTER position and a button in BorderLayout.EAST
 * which opens a JTextArea for editing long strings.
 */
public final class TextInputPanel extends JPanel {

  private static final Dimension DEFAULT_TEXT_AREA_SIZE = new Dimension(500, 300);

  /**
   * Specifies whether the edit button should be focusable.
   */
  public enum ButtonFocusable {
    /**
     * Button should be focusable.
     */
    YES,
    /**
     * Button should not be focusable.
     */
    NO
  }

  private final JTextField textField;
  private final JButton button;
  private final String dialogTitle;
  private final Dimension textAreaSize;
  private int maxLength = -1;

  /**
   * Instantiates a new TextInputPanel, with a focusable button.
   * @param textField the text field
   * @param dialogTitle the input dialog title
   * @throws NullPointerException in case textComponent is null
   */
  public TextInputPanel(final JTextField textField, final String dialogTitle) {
    this(textField, dialogTitle, null);
  }

  /**
   * Instantiates a new TextInputPanel, with a focusable button.
   * @param textField the text field
   * @param dialogTitle the input dialog title
   * @param textAreaSize the input text area size
   * @throws NullPointerException in case textComponent is null
   */
  public TextInputPanel(final JTextField textField, final String dialogTitle,
                        final Dimension textAreaSize) {
    this(textField, dialogTitle, textAreaSize, ButtonFocusable.YES);
  }

  /**
   * Instantiates a new TextInputPanel.
   * @param textField the text field
   * @param dialogTitle the input dialog title
   * @param textAreaSize the input text area size
   * @param buttonFocusable if yes then the input button is focusable
   * @throws NullPointerException in case textComponent is null
   */
  public TextInputPanel(final JTextField textField, final String dialogTitle,
                        final Dimension textAreaSize, final ButtonFocusable buttonFocusable) {
    requireNonNull(textField, "textComponent");
    this.dialogTitle = dialogTitle;
    this.textField = textField;
    this.textAreaSize = textAreaSize == null ? DEFAULT_TEXT_AREA_SIZE : textAreaSize;
    this.button = createButton(buttonFocusable, TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    initializeUI();
  }

  /**
   * Sets the maximum length of the string allowed in the text area
   * @param maxLength the maximum length
   */
  public void setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
  }

  /**
   * @return the maximum length allowed for this text input panel
   */
  public int getMaxLength() {
    return maxLength;
  }

  /**
   * @param text the text to set
   * @throws NullPointerException in case the text length exceeds maxLength
   * @see #getMaxLength()
   */
  public void setText(final String text) {
    if (maxLength > 0 && text.length() > maxLength) {
      throw new IllegalArgumentException("Maximum allowed text length exceeded");
    }
    textField.setText(text);
  }

  /**
   * @return the current input text value
   */
  public String getText() {
    return textField.getText();
  }

  /**
   * @return the text field
   */
  public JTextField getTextField() {
    return textField;
  }

  /**
   * @return the input dialog button
   */
  public JButton getButton() {
    return button;
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    add(textField, BorderLayout.CENTER);
    add(button, BorderLayout.EAST);
    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        textField.requestFocusInWindow();
      }
    });
  }

  private JButton createButton(final ButtonFocusable buttonFocusable, final Dimension buttonSize) {
    final JButton jButton = new JButton(new AbstractAction("...") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getInputFromUser();
      }
    });
    jButton.setFocusable(buttonFocusable == ButtonFocusable.YES);
    if (buttonSize != null) {
      jButton.setPreferredSize(buttonSize);
    }

    return jButton;
  }

  private void getInputFromUser() {
    final JTextArea textArea = new JTextArea(textField.getText()) {
      @Override
      protected Document createDefaultModel() {
        final SizedDocument document = new SizedDocument();
        document.setMaxLength(getMaxLength());

        return document;
      }
    };
    textArea.setPreferredSize(textAreaSize);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(textField.isEditable());
    final Action okAction = new AbstractAction(Messages.get(Messages.OK)) {
      @Override
      public void actionPerformed(final ActionEvent e) {
        textField.setText(textArea.getText());
      }
    };
    okAction.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
    Dialogs.displayInDialog(textField, new JScrollPane(textArea), dialogTitle, okAction);
    textField.requestFocusInWindow();
  }
}
