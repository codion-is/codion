/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.swing.common.model.textfield.DocumentAdapter;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.AbstractComponentValue;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
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
  private final String caption;
  private final Dimension textAreaSize;
  private final int maximumLength;

  private TextInputPanel(final DefaultBuilder builder) {
    this.dialogTitle = builder.dialogTitle;
    this.textField = builder.textField;
    this.textAreaSize = builder.textAreaSize;
    this.button = createButton(builder.buttonFocusable, TextFields.DIMENSION_TEXT_FIELD_SQUARE);
    this.caption = builder.caption;
    this.maximumLength = builder.maximumLength;
    initializeUI();
  }

  /**
   * @param text the text to set
   * @throws IllegalArgumentException in case the text length exceeds maximum length
   */
  public void setText(final String text) {
    if (text != null && maximumLength > 0 && text.length() > maximumLength) {
      throw new IllegalArgumentException("Maximum allowed text length exceeded");
    }
    textField.setText(text == null ? "" : text);
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

  @Override
  public void setEnabled(final boolean enabled) {
    super.setEnabled(enabled);
    textField.setEnabled(enabled);
    button.setEnabled(enabled);
  }

  /**
   * @param transferFocusOnEnter specifies whether focus should be transferred on Enter
   */
  public void setTransferFocusOnEnter(final boolean transferFocusOnEnter) {
    if (transferFocusOnEnter) {
      TransferFocusOnEnter.enable(textField);
      TransferFocusOnEnter.enable(button);
    }
    else {
      TransferFocusOnEnter.disable(textField);
      TransferFocusOnEnter.disable(button);
    }
  }

  /**
   * Instantiates a new String based ComponentValue based on this input panel.
   * @return a String based ComponentValue
   */
  public ComponentValue<String, TextInputPanel> componentValue() {
    return new TextInputPanelValue(this);
  }

  /**
   * @param textField the text field
   * @return a new builder
   */
  public static Builder builder(final JTextField textField) {
    return new DefaultBuilder(requireNonNull(textField));
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
     * If specified a titled border with the given caption is added to the input field
     * @param caption the caption to display
     * @return this builder instance
     */
    Builder caption(String caption);

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
     * @param maximumLength the maximum text length
     * @return this builder instance
     */
    Builder maximumLength(int maximumLength);

    /**
     * @return a new TextInputPanel
     */
    TextInputPanel build();
  }

  private void initializeUI() {
    setLayout(new BorderLayout());
    add(textField, BorderLayout.CENTER);
    add(button, BorderLayout.EAST);
    if (caption != null) {
      setBorder(BorderFactory.createTitledBorder(caption));
    }
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
        return new SizedDocument(maximumLength);
      }
    };
    textArea.setCaretPosition(textArea.getText().length());
    textArea.setPreferredSize(textAreaSize);
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setEditable(textField.isEditable());
    TransferFocusOnEnter.enable(textArea);
    Dialogs.okCancelDialog(new JScrollPane(textArea))
            .owner(textField)
            .title(dialogTitle == null ? caption : dialogTitle)
            .onOk(() -> textField.setText(textArea.getText()))
            .show();
    textField.requestFocusInWindow();
  }

  private static final class DefaultBuilder implements Builder {

    private static final Dimension DEFAULT_TEXT_AREA_SIZE = new Dimension(500, 300);

    private final JTextField textField;

    private String dialogTitle;
    private String caption;
    private Dimension textAreaSize = DEFAULT_TEXT_AREA_SIZE;
    private boolean buttonFocusable;
    private int maximumLength = -1;

    private DefaultBuilder(final JTextField textField) {
      this.textField = textField;
    }

    @Override
    public Builder dialogTitle(final String dialogTitle) {
      this.dialogTitle = dialogTitle;
      return this;
    }

    @Override
    public Builder caption(final String caption) {
      this.caption = caption;
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
    public Builder maximumLength(final int maximumLength) {
      this.maximumLength = maximumLength;
      return this;
    }

    @Override
    public TextInputPanel build() {
      return new TextInputPanel(this);
    }
  }

  private static class TextInputPanelValue extends AbstractComponentValue<String, TextInputPanel> {

    private TextInputPanelValue(final TextInputPanel textInputPanel) {
      super(textInputPanel);
      textInputPanel.getTextField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
    }

    @Override
    protected String getComponentValue(final TextInputPanel component) {
      return component.getText();
    }

    @Override
    protected void setComponentValue(final TextInputPanel component, final String value) {
      component.setText(value);
    }
  }
}
