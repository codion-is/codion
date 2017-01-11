/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.i18n.Messages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.Objects;

/**
 * A panel that includes a JTextField in a BorderLayout.CENTER position and a button in BorderLayout.EAST
 * which opens a JTextArea for editing long strings.
 */
public final class TextInputPanel extends JPanel {

  private static final double DEFAULT_TEXT_AREA_SCREEN_SIZE_RATIO = 0.25;

  private final JTextField textField;
  private final JButton button;
  private final String dialogTitle;
  private final Dimension txtAreaSize;
  private int maxLength = 0;

  /**
   * Instantiates a new TextInputPanel.
   * @param textField the text field
   * @param dialogTitle the input dialog title
   * @throws NullPointerException in case textComponent is null
   */
  public TextInputPanel(final JTextField textField, final String dialogTitle) {
    this(textField, dialogTitle, null);
  }

  /**
   * Instantiates a new TextInputPanel.
   * @param textField the text field
   * @param dialogTitle the input dialog title
   * @param txtAreaSize the input text area size
   * @throws NullPointerException in case textComponent is null
   */
  public TextInputPanel(final JTextField textField, final String dialogTitle,
                        final Dimension txtAreaSize) {
    this(textField, dialogTitle, txtAreaSize, true);
  }

  /**
   * Instantiates a new TextInputPanel.
   * @param textField the text field
   * @param dialogTitle the input dialog title
   * @param txtAreaSize the input text area size
   * @param buttonFocusable if true then the input button is focusable
   * @throws NullPointerException in case textComponent is null
   */
  public TextInputPanel(final JTextField textField, final String dialogTitle,
                        final Dimension txtAreaSize, final boolean buttonFocusable) {
    Objects.requireNonNull(textField, "textComponent");
    this.dialogTitle = dialogTitle;
    this.textField = textField;
    this.txtAreaSize = txtAreaSize == null ? UiUtil.getScreenSizeRatio(DEFAULT_TEXT_AREA_SCREEN_SIZE_RATIO) : txtAreaSize;
    this.button = createButton(buttonFocusable, UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
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
  }

  private JButton createButton(final boolean buttonFocusable, final Dimension buttonSize) {
    final JButton jButton = new JButton(new InputAction());
    jButton.setFocusable(buttonFocusable);
    if (buttonSize != null) {
      jButton.setPreferredSize(buttonSize);
    }

    return jButton;
  }

  private final class InputAction extends AbstractAction {

    private InputAction() {
      super("...");
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final JTextArea txtArea = new JTextArea(textField.getText()) {
        @Override
        protected Document createDefaultModel() {
          return new PlainDocument() {
            @Override
            public void insertString(final int offs, final String str, final AttributeSet a) throws BadLocationException {
              if (getMaxLength() > 0 && getLength() + (str != null ? str.length() : 0) > getMaxLength()) {
                return;
              }

              super.insertString(offs, str, a);
            }
          };
        }
      };
      txtArea.setPreferredSize(txtAreaSize);
      txtArea.setLineWrap(true);
      txtArea.setWrapStyleWord(true);
      final JScrollPane scroller = new JScrollPane(txtArea);
      final AbstractAction okAction = new AbstractAction(Messages.get(Messages.OK)) {
        @Override
        public void actionPerformed(final ActionEvent evt) {
          textField.setText(txtArea.getText());
        }
      };
      okAction.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
      UiUtil.displayInDialog(textField, scroller, dialogTitle, okAction);
    }
  }
}
