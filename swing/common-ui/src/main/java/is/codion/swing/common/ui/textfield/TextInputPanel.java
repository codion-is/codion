/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.dialog.Dialogs;

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
 * @see #builder(JTextField)
 */
public final class TextInputPanel extends JPanel {

  private final JTextField textField;
  private final JButton button;
  private final String dialogTitle;
  private final Dimension textAreaSize;
  private int maximumLength = -1;

  private TextInputPanel(final JTextField textField, final String dialogTitle,
                         final Dimension textAreaSize, final boolean buttonFocusable) {
    this.dialogTitle = dialogTitle;
    this.textField = textField;
    this.textAreaSize = textAreaSize;
    this.button = createButton(buttonFocusable, TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    initializeUI();
  }

  /**
   * Sets the maximum length of the string allowed in the text area
   * @param maximumLength the maximum length
   */
  public void setMaximumLength(final int maximumLength) {
    this.maximumLength = maximumLength;
  }

  /**
   * @return the maximum length allowed for this text input panel
   */
  public int getMaximumLength() {
    return maximumLength;
  }

  /**
   * @param text the text to set
   * @throws IllegalArgumentException in case the text length exceeds maximum length
   * @see #getMaximumLength()
   */
  public void setText(final String text) {
    if (maximumLength > 0 && text.length() > maximumLength) {
      throw new IllegalArgumentException("Maximum allowed text length exceeded");
    }
    textField.setText(text == null ? "" : text);
  }

  /**
   * @param textField the text field
   * @return a new builder
   */
  public static Builder builder(final JTextField textField) {
    return new DefaultBuilder(requireNonNull(textField));
  }

  /**
   * @return the current input text value
   */
  public String getText() {
    final String text = textField.getText();

    return text.isEmpty() ? null : text;
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

  /**
   * A builder for {@link TextInputPanel}.
   */
  public interface Builder {

    /**
     * @param dialogTitle the input dialog title
     * @return this builder instance
     */
    Builder dialogTitle(String dialogTitle);

    /**
     * @param textAreaSize the input text area siz
     * @return this builder instance
     */
    Builder textAreaSize(Dimension textAreaSize);

    /**
     * @param buttonFocusable true if the input button should be focusable
     * @return this builder instance
     */
    Builder buttonFocusable(boolean buttonFocusable);

    /**
     * @return a new TextInputPanel
     */
    TextInputPanel build();
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

  private JButton createButton(final boolean buttonFocusable, final Dimension buttonSize) {
    final JButton actionButton = new JButton(new AbstractAction("...") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        getInputFromUser();
      }
    });
    actionButton.setFocusable(buttonFocusable);
    if (buttonSize != null) {
      actionButton.setPreferredSize(buttonSize);
    }

    return actionButton;
  }

  private void getInputFromUser() {
    final JTextArea textArea = new JTextArea(textField.getText()) {
      @Override
      protected Document createDefaultModel() {
        return new SizedDocument(getMaximumLength());
      }
    };
    textArea.setPreferredSize(textAreaSize);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(textField.isEditable());
    final Action okAction = new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        textField.setText(textArea.getText());
        Windows.getParentDialog(textArea).dispose();
      }
    };
    Dialogs.okCancelDialogBuilder(new JScrollPane(textArea))
            .owner(textField)
            .title(dialogTitle)
            .okAction(okAction)
            .show();
    textField.requestFocusInWindow();
  }

  private static final class DefaultBuilder implements Builder {

    private static final Dimension DEFAULT_TEXT_AREA_SIZE = new Dimension(500, 300);

    private final JTextField textField;

    private String dialogTitle;
    private Dimension textAreaSize = DEFAULT_TEXT_AREA_SIZE;
    private boolean buttonFocusable;

    private DefaultBuilder(final JTextField textField) {
      this.textField = textField;
    }

    @Override
    public Builder dialogTitle(final String dialogTitle) {
      this.dialogTitle = dialogTitle;
      return this;
    }

    @Override
    public Builder textAreaSize(final Dimension textAreaSize) {
      this.textAreaSize = textAreaSize == null ? DEFAULT_TEXT_AREA_SIZE : textAreaSize;
      return this;
    }

    @Override
    public Builder buttonFocusable(final boolean buttonFocusable) {
      this.buttonFocusable = buttonFocusable;
      return this;
    }

    @Override
    public TextInputPanel build() {
      return new TextInputPanel(textField, dialogTitle, textAreaSize, buttonFocusable);
    }
  }
}
