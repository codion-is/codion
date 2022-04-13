/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.model.component.textfield.DocumentAdapter;
import is.codion.swing.common.ui.component.textfield.TextInputPanel;

import javax.swing.JTextField;
import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

final class DefaultTextInputPanelBuilder extends AbstractComponentBuilder<String, TextInputPanel, TextInputPanelBuilder> implements TextInputPanelBuilder {

  private final TextFieldBuilder<String, JTextField, ?> textFieldBuilder = new DefaultTextFieldBuilder<>(String.class, null);

  private boolean buttonFocusable;
  private Dimension textAreaSize;
  private int maximumLength;
  private String caption;
  private String dialogTitle;

  DefaultTextInputPanelBuilder(Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public TextInputPanelBuilder updateOn(UpdateOn updateOn) {
    textFieldBuilder.updateOn(updateOn);
    return this;
  }

  @Override
  public TextInputPanelBuilder columns(int columns) {
    textFieldBuilder.columns(columns);
    return this;
  }

  @Override
  public TextInputPanelBuilder upperCase(boolean upperCase) {
    textFieldBuilder.upperCase(upperCase);
    return this;
  }

  @Override
  public TextInputPanelBuilder lowerCase(boolean lowerCase) {
    textFieldBuilder.lowerCase(lowerCase);
    return this;
  }

  @Override
  public TextInputPanelBuilder selectAllOnFocusGained(boolean selectAllOnFocusGained) {
    textFieldBuilder.selectAllOnFocusGained(selectAllOnFocusGained);
    return this;
  }

  @Override
  public TextInputPanelBuilder buttonFocusable(boolean buttonFocusable) {
    this.buttonFocusable = buttonFocusable;
    return this;
  }

  @Override
  public TextInputPanelBuilder textAreaSize(Dimension textAreaSize) {
    this.textAreaSize = requireNonNull(textAreaSize);
    return this;
  }

  @Override
  public TextInputPanelBuilder maximumLength(int maximumLength) {
    textFieldBuilder.maximumLength(maximumLength);
    this.maximumLength = maximumLength;
    return this;
  }

  @Override
  public TextInputPanelBuilder caption(String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public TextInputPanelBuilder dialogTitle(String dialogTitle) {
    this.dialogTitle = dialogTitle;
    return this;
  }

  @Override
  protected TextInputPanel createComponent() {
    return TextInputPanel.builder(textFieldBuilder.build())
            .caption(caption)
            .maximumLength(maximumLength)
            .dialogTitle(dialogTitle)
            .textAreaSize(textAreaSize)
            .buttonFocusable(buttonFocusable)
            .build();
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
  protected void setTransferFocusOnEnter(TextInputPanel component) {
    component.setTransferFocusOnEnter(true);
  }

  private static class TextInputPanelValue extends AbstractComponentValue<String, TextInputPanel> {

    private TextInputPanelValue(TextInputPanel textInputPanel) {
      super(textInputPanel);
      textInputPanel.getTextField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
    }

    @Override
    protected String getComponentValue(TextInputPanel component) {
      return component.getText();
    }

    @Override
    protected void setComponentValue(TextInputPanel component, String value) {
      component.setText(value);
    }
  }
}
