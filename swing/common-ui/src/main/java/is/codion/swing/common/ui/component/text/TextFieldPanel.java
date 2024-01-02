/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;
import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.key.KeyEvents;
import is.codion.swing.common.ui.key.KeyboardShortcuts;
import is.codion.swing.common.ui.key.TransferFocusOnEnter;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ResourceBundle;

import static is.codion.swing.common.ui.component.text.SizedDocument.sizedDocument;
import static is.codion.swing.common.ui.component.text.TextFieldPanel.KeyboardShortcut.DISPLAY_TEXT_AREA;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyboardShortcuts;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.util.Objects.requireNonNull;

/**
 * A panel that includes a JTextField in a {@link BorderLayout#CENTER} position and a button in {@link BorderLayout#EAST}
 * which opens a JTextArea for editing long strings.
 * @see #builder()
 */
public final class TextFieldPanel extends JPanel {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(TextFieldPanel.class.getName());

  /**
   * The default keyboard shortcut keyStrokes.
   */
  public static final KeyboardShortcuts<KeyboardShortcut> KEYBOARD_SHORTCUTS =
          keyboardShortcuts(KeyboardShortcut.class, TextFieldPanel::defaultKeyStroke);

  /**
   * The available keyboard shortcuts.
   */
  public enum KeyboardShortcut {
    DISPLAY_TEXT_AREA
  }

  private final JTextField textField;
  private final JButton button;
  private final String dialogTitle;
  private final String caption;
  private final Dimension textAreaSize;
  private final int maximumLength;

  private TextFieldPanel(DefaultBuilder builder) {
    this.textField = builder.textFieldBuilder.build();
    this.dialogTitle = builder.dialogTitle;
    this.textAreaSize = builder.textAreaSize;
    this.button = createButton(builder.buttonFocusable, builder.buttonIcon, builder.keyboardShortcuts);
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
   * A builder for {@link TextFieldPanel}.
   */
  public interface Builder extends ComponentBuilder<String, TextFieldPanel, Builder> {

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
     * Default false.
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

    /**
     * @param keyboardShortcut the keyboard shortcut key
     * @param keyStroke the keyStroke to assign to the given shortcut key, null resets to the default one
     * @return this builder instance
     */
    Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke);
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

  private JButton createButton(boolean buttonFocusable, ImageIcon buttonIcon, KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts) {
    Control buttonControl = Control.builder(this::inputFromUser)
            .name(buttonIcon == null ? "..." : "")
            .smallIcon(buttonIcon)
            .build();
    KeyEvents.builder(keyboardShortcuts.keyStroke(DISPLAY_TEXT_AREA).get())
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

  private static final class DefaultBuilder extends AbstractComponentBuilder<String, TextFieldPanel, Builder> implements Builder {

    private static final Dimension DEFAULT_TEXT_AREA_SIZE = new Dimension(500, 300);

    private final TextFieldBuilder<String, JTextField, ?> textFieldBuilder = new DefaultTextFieldBuilder<>(String.class, null);
    private final KeyboardShortcuts<KeyboardShortcut> keyboardShortcuts = KEYBOARD_SHORTCUTS.copy();

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
    public TextFieldPanel.Builder updateOn(UpdateOn updateOn) {
      textFieldBuilder.updateOn(updateOn);
      return this;
    }

    @Override
    public TextFieldPanel.Builder columns(int columns) {
      textFieldBuilder.columns(columns);
      return this;
    }

    @Override
    public TextFieldPanel.Builder upperCase(boolean upperCase) {
      textFieldBuilder.upperCase(upperCase);
      return this;
    }

    @Override
    public TextFieldPanel.Builder lowerCase(boolean lowerCase) {
      textFieldBuilder.lowerCase(lowerCase);
      return this;
    }

    @Override
    public TextFieldPanel.Builder selectAllOnFocusGained(boolean selectAllOnFocusGained) {
      textFieldBuilder.selectAllOnFocusGained(selectAllOnFocusGained);
      return this;
    }

    @Override
    public TextFieldPanel.Builder buttonFocusable(boolean buttonFocusable) {
      this.buttonFocusable = buttonFocusable;
      return this;
    }

    @Override
    public Builder buttonIcon(ImageIcon buttonIcon) {
      this.buttonIcon = buttonIcon;
      return this;
    }

    @Override
    public TextFieldPanel.Builder textAreaSize(Dimension textAreaSize) {
      this.textAreaSize = requireNonNull(textAreaSize);
      return this;
    }

    @Override
    public TextFieldPanel.Builder maximumLength(int maximumLength) {
      textFieldBuilder.maximumLength(maximumLength);
      this.maximumLength = maximumLength;
      return this;
    }

    @Override
    public TextFieldPanel.Builder caption(String caption) {
      this.caption = caption;
      return this;
    }

    @Override
    public TextFieldPanel.Builder dialogTitle(String dialogTitle) {
      this.dialogTitle = dialogTitle;
      return this;
    }

    @Override
    public TextFieldPanel.Builder keyStroke(KeyboardShortcut keyboardShortcut, KeyStroke keyStroke) {
      keyboardShortcuts.keyStroke(keyboardShortcut).set(keyStroke);
      return this;
    }

    @Override
    protected TextFieldPanel createComponent() {
      return new TextFieldPanel(this);
    }

    @Override
    protected ComponentValue<String, TextFieldPanel> createComponentValue(TextFieldPanel component) {
      return new TextFieldPanelValue(component);
    }

    @Override
    protected void setInitialValue(TextFieldPanel component, String initialValue) {
      component.setText(initialValue);
    }

    @Override
    protected void enableTransferFocusOnEnter(TextFieldPanel component) {
      component.setTransferFocusOnEnter(true);
    }
  }

  private static class TextFieldPanelValue extends AbstractComponentValue<String, TextFieldPanel> {

    private TextFieldPanelValue(TextFieldPanel textFieldPanel) {
      super(textFieldPanel);
      textFieldPanel.textField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyListeners());
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

  private static KeyStroke defaultKeyStroke(KeyboardShortcut shortcut) {
    if (shortcut == DISPLAY_TEXT_AREA) {
      return keyStroke(VK_INSERT, 0);
    }

    throw new IllegalArgumentException();
  }
}