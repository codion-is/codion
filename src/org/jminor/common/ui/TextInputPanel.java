/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;
import javax.swing.text.PlainDocument;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

/**
 * A panel that includes a JTextComponent in a BorderLayout.CENTER position and a button in BorderLayout.EAST
 * which opens a JTextArea for editing long strings.
 */
public class TextInputPanel extends JPanel {

  private final JTextComponent textComponent;
  private final String dialogTitle;
  private final Dimension txtAreaSize;
  private int maxLength = 0;

  public TextInputPanel(final JTextComponent textComponent, final String dialogTitle) {
    this(textComponent, dialogTitle, null);
  }

  public TextInputPanel(final JTextComponent textComponent, final String dialogTitle,
                        final Dimension txtAreaSize) {
    this(textComponent, dialogTitle, txtAreaSize, true);
  }

  public TextInputPanel(final JTextComponent textComponent, final String dialogTitle,
                        final Dimension txtAreaSize, final boolean buttonFocusable) {
    this.dialogTitle = dialogTitle;
    this.textComponent = textComponent;
    this.txtAreaSize = txtAreaSize;
    initializeUI(textComponent, createButton(textComponent, buttonFocusable, UiUtil.DIMENSION_TEXT_FIELD_SQUARE));
  }

  /**
   * Sets the maximum length of the string allowed in the text area
   * @param maxLength the maximum length
   */
  public void setMaxLength(final int maxLength) {
    this.maxLength = maxLength;
  }

  public int getMaxLength() {
    return maxLength;
  }

  public void setText(final String text) {
    textComponent.setText(text);
  }

  public String getText() {
    return textComponent.getText();
  }

  public JTextComponent getTextComponent() {
    return textComponent;
  }

  protected void initializeUI(final JTextComponent textComponent, final JButton button) {
    setLayout(new BorderLayout());
    add(textComponent, BorderLayout.CENTER);
    add(button, BorderLayout.EAST);
  }

  private JButton createButton(final JTextComponent textComponent, final boolean buttonFocusable,
                               final Dimension buttonSize) {
    final AbstractAction action = new AbstractAction("...") {
      public void actionPerformed(final ActionEvent e) {
        final JTextArea txtArea = new JTextArea(textComponent.getText()) {
          /** {@inheritDoc} */
          @Override
          protected Document createDefaultModel() {
            return new PlainDocument() {
              @Override
              public void insertString(final int offset, final String string, final AttributeSet a) throws BadLocationException {
                if (getMaxLength() > 0 && getLength() + (string != null ? string.length() : 0) > getMaxLength()) {
                  return;
                }

                super.insertString(offset, string, a);
              }
            };
          }
        };
        txtArea.setPreferredSize(txtAreaSize == null ? UiUtil.getScreenSizeRatio(0.3) : txtAreaSize);
        txtArea.setLineWrap(true);
        txtArea.setWrapStyleWord(true);
        final JScrollPane scroller = new JScrollPane(txtArea);
        final AbstractAction okAction = new AbstractAction(Messages.get(Messages.OK)) {
          public void actionPerformed(final ActionEvent e) {
            textComponent.setText(txtArea.getText());
          }
        };
        okAction.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
        UiUtil.showInDialog(UiUtil.getParentWindow(textComponent), scroller, true, dialogTitle, true, true, okAction);
      }
    };

    final JButton button = new JButton(action);
    button.setFocusable(buttonFocusable);
    if (buttonSize != null) {
      button.setPreferredSize(buttonSize);
    }

    return button;
  }
}
