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
public final class TextInputPanel extends JPanel {

  private static final double DEFAULT_TEXT_AREA_SCREEN_SIZE_RATIO = 1d/4d;

  private final JTextComponent textComponent;
  private final JButton button;
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
    this.txtAreaSize = txtAreaSize == null ? UiUtil.getScreenSizeRatio(DEFAULT_TEXT_AREA_SCREEN_SIZE_RATIO) : txtAreaSize;
    this.button = createButton(textComponent, buttonFocusable, UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
    initializeUI(textComponent, button);
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

  public JButton getButton() {
    return button;
  }

  private void initializeUI(final JTextComponent textComponent, final JButton button) {
    setLayout(new BorderLayout());
    add(textComponent, BorderLayout.CENTER);
    add(button, BorderLayout.EAST);
  }

  private JButton createButton(final JTextComponent textComponent, final boolean buttonFocusable,
                               final Dimension buttonSize) {
    final AbstractAction action = new AbstractAction("...") {
      public void actionPerformed(final ActionEvent e) {
        final JTextArea txtArea = new JTextArea(textComponent.getText()) {
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
          public void actionPerformed(final ActionEvent evt) {
            textComponent.setText(txtArea.getText());
          }
        };
        okAction.putValue(Action.MNEMONIC_KEY, Messages.get(Messages.OK_MNEMONIC).charAt(0));
        UiUtil.showInDialog(UiUtil.getParentWindow(textComponent), scroller, true, dialogTitle, true, true, okAction);
      }
    };

    final JButton jButton = new JButton(action);
    jButton.setFocusable(buttonFocusable);
    if (buttonSize != null) {
      jButton.setPreferredSize(buttonSize);
    }

    return jButton;
  }
}
