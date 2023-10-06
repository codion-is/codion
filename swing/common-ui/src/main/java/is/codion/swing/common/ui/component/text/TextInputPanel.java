/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.TransferFocusOnEnter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.text.SizedDocument.sizedDocument;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Objects.requireNonNull;

/**
 * A panel that includes a JTextField in a {@link BorderLayout#CENTER} position and a button in {@link BorderLayout#EAST}
 * which opens a JTextArea for editing long strings.
 * @see #builder()
 */
public final class TextInputPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TextInputPanel.class.getName());

  private final JTextField textField;
  private final JButton button;
  private final String dialogTitle;
  private final String caption;
  private final Dimension textAreaSize;
  private final int maximumLength;

  private TextInputPanel(DefaultBuilder builder) {
    this.textField = builder.textFieldBuilder.build();
    this.dialogTitle = builder.dialogTitle;
    this.textAreaSize = builder.textAreaSize;
    this.button = createButton(builder.buttonFocusable, builder.buttonIcon);
    this.caption = builder.caption;
    this.maximumLength = builder.maximumLength;
    initializeUI();
  }

  /**
   * @param text the text to set
   * @throws IllegalArgumentException in case the text length exceeds maximum length
   */
  public void setText(String text) {
    if (text != null && maximumLength > 0 && text.length() > maximumLength) {
      throw new IllegalArgumentException("Maximum allowed text length exceeded");
    }
    textField.setText(text == null ? "" : text);
  }

  /**
   * @return the current input text value
   */
  public String getText() {
    String text = textField.getText();

    return text.isEmpty() ? null : text;
  }

  /**
   * @return the text field
   */
  public JTextField textField() {
    return textField;
  }

  /**
   * @return the input dialog button
   */
  public JButton button() {
    return button;
  }

  @Override
  public void setEnabled(boolean enabled) {
    super.setEnabled(enabled);
    textField.setEnabled(enabled);
    button.setEnabled(enabled);
  }

  @Override
  public void setToolTipText(String text) {
    textField.setToolTipText(text);
  }

  /**
   * @param transferFocusOnEnter specifies whether focus should be transferred on Enter
   */
  public void setTransferFocusOnEnter(boolean transferFocusOnEnter) {
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
   * @return a new builder
   */
  public static Builder builder() {
    return new DefaultBuilder(null);
  }

  /**
   * @param linkedValue the linked value
   * @return a new builder
   */
  public static Builder builder(Value<String> linkedValue) {
    return new DefaultBuilder(requireNonNull(linkedValue));
  }

  /**
   * A builder for {@link TextInputPanel}.
   */
  public interface Builder extends ComponentBuilder<String, TextInputPanel, Builder> {

    /**
     * @param updateOn specifies when the underlying value should be updated
     * @return this builder instance
     */
    Builder updateOn(UpdateOn updateOn);

    /**
     * @param columns the number of colums in the text field
     * @return this builder instance
     */
    Builder columns(int columns);

    /**
     * @param upperCase if true the text component convert all lower case input to upper case
     * @return this builder instance
     */
    Builder upperCase(boolean upperCase);

    /**
     * @param lowerCase if true the text component convert all upper case input to lower case
     * @return this builder instance
     */
    Builder lowerCase(boolean lowerCase);

    /**
     * Makes the text field select all when it gains focus
     * @param selectAllOnFocusGained if true the component will select contents on focus gained
     * @return this builder instance
     */
    Builder selectAllOnFocusGained(boolean selectAllOnFocusGained);

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
     * @param buttonIcon the button icon
     * @return this builder instance
     */
    Builder buttonIcon(ImageIcon buttonIcon);

    /**
     * @param maximumLength the maximum text length
     * @return this builder instance
     */
    Builder maximumLength(int maximumLength);
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
      public void focusGained(FocusEvent e) {
        textField.requestFocusInWindow();
      }
    });
  }

  private JButton createButton(boolean buttonFocusable, ImageIcon buttonIcon) {
    Control buttonControl = Control.builder(this::inputFromUser)
            .name(buttonIcon == null ? "..." : "")
            .smallIcon(buttonIcon)
            .build();
    KeyEvents.builder(VK_INSERT)
            .action(buttonControl)
            .enable(textField);
    JButton actionButton = new JButton(buttonControl);
    actionButton.setFocusable(buttonFocusable);
    actionButton.setToolTipText(MESSAGES.getString("show_input_dialog"));
    actionButton.setPreferredSize(new Dimension(textField.getPreferredSize().height, textField.getPreferredSize().height));

    return actionButton;
  }

  private void inputFromUser() {
    JTextArea textArea = TextAreaBuilder.builder()
            .document(sizedDocument(maximumLength))
            .initialValue(textField.getText())
            .preferredSize(textAreaSize)
            .lineWrap(true)
            .wrapStyleWord(true)
            .editable(textField.isEditable())
            .moveCaretToEndOnFocusGained(true)
            .build();
    Dialogs.okCancelDialog(new JScrollPane(textArea))
            .owner(textField)
            .title(dialogTitle == null ? caption : dialogTitle)
            .onOk(() -> textField.setText(textArea.getText()))
            .show();
  }

  private static final class DefaultBuilder extends AbstractComponentBuilder<String, TextInputPanel, Builder> implements Builder {

    private static final Dimension DEFAULT_TEXT_AREA_SIZE = new Dimension(500, 300);

    private final TextFieldBuilder<String, JTextField, ?> textFieldBuilder = new DefaultTextFieldBuilder<>(String.class, null);

    private boolean buttonFocusable;
    private ImageIcon buttonIcon;
    private Dimension textAreaSize = DEFAULT_TEXT_AREA_SIZE;
    private int maximumLength;
    private String caption;
    private String dialogTitle;

    private DefaultBuilder(Value<String> linkedValue) {
      super(linkedValue);
    }

    @Override
    public TextInputPanel.Builder updateOn(UpdateOn updateOn) {
      textFieldBuilder.updateOn(updateOn);
      return this;
    }

    @Override
    public TextInputPanel.Builder columns(int columns) {
      textFieldBuilder.columns(columns);
      return this;
    }

    @Override
    public TextInputPanel.Builder upperCase(boolean upperCase) {
      textFieldBuilder.upperCase(upperCase);
      return this;
    }

    @Override
    public TextInputPanel.Builder lowerCase(boolean lowerCase) {
      textFieldBuilder.lowerCase(lowerCase);
      return this;
    }

    @Override
    public TextInputPanel.Builder selectAllOnFocusGained(boolean selectAllOnFocusGained) {
      textFieldBuilder.selectAllOnFocusGained(selectAllOnFocusGained);
      return this;
    }

    @Override
    public TextInputPanel.Builder buttonFocusable(boolean buttonFocusable) {
      this.buttonFocusable = buttonFocusable;
      return this;
    }

    @Override
    public Builder buttonIcon(ImageIcon buttonIcon) {
      this.buttonIcon = buttonIcon;
      return this;
    }

    @Override
    public TextInputPanel.Builder textAreaSize(Dimension textAreaSize) {
      this.textAreaSize = requireNonNull(textAreaSize);
      return this;
    }

    @Override
    public TextInputPanel.Builder maximumLength(int maximumLength) {
      textFieldBuilder.maximumLength(maximumLength);
      this.maximumLength = maximumLength;
      return this;
    }

    @Override
    public TextInputPanel.Builder caption(String caption) {
      this.caption = caption;
      return this;
    }

    @Override
    public TextInputPanel.Builder dialogTitle(String dialogTitle) {
      this.dialogTitle = dialogTitle;
      return this;
    }

    @Override
    protected TextInputPanel createComponent() {
      return new TextInputPanel(this);
    }

    @Override
    protected ComponentValue<String, TextInputPanel> createComponentValue(TextInputPanel component) {
      return new TextInputPanelValue(component);
    }

    @Override
    protected void setInitialValue(TextInputPanel component, String initialValue) {
      component.setText(initialValue);
    }

    @Override
    protected void enableTransferFocusOnEnter(TextInputPanel component) {
      component.setTransferFocusOnEnter(true);
    }
  }

  private static class TextInputPanelValue extends AbstractComponentValue<String, TextInputPanel> {

    private TextInputPanelValue(TextInputPanel textInputPanel) {
      super(textInputPanel);
      textInputPanel.textField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyListeners());
    }

    @Override
    protected String getComponentValue() {
      return component().getText();
    }

    @Override
    protected void setComponentValue(String value) {
      component().setText(value);
    }
  }
}