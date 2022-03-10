/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.textfield.TextInputPanel;

import javax.swing.JTextField;
import java.awt.Dimension;

import static java.util.Objects.requireNonNull;

final class DefaultTextInputPanelBuilder extends AbstractComponentBuilder<String, TextInputPanel, TextInputPanelBuilder>
        implements TextInputPanelBuilder {

  private UpdateOn updateOn = UpdateOn.KEYSTROKE;
  private boolean buttonFocusable;
  private int columns = TextFieldBuilder.DEFAULT_TEXT_FIELD_COLUMNS.get();
  private boolean upperCase = false;
  private boolean lowerCase = false;
  private boolean selectAllOnFocusGained;
  private Dimension textAreaSize;
  private int maximumLength;
  private String caption;
  private String dialogTitle;

  DefaultTextInputPanelBuilder(Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public TextInputPanelBuilder updateOn(UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public TextInputPanelBuilder columns(int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public TextInputPanelBuilder upperCase(boolean upperCase) {
    if (upperCase && lowerCase) {
      throw new IllegalArgumentException("Field is already lowercase");
    }
    this.upperCase = upperCase;
    return this;
  }

  @Override
  public TextInputPanelBuilder lowerCase(boolean lowerCase) {
    if (lowerCase && upperCase) {
      throw new IllegalArgumentException("Field is already uppercase");
    }
    this.lowerCase = lowerCase;
    return this;
  }

  @Override
  public TextInputPanelBuilder selectAllOnFocusGained(boolean selectAllOnFocusGained) {
    this.selectAllOnFocusGained = selectAllOnFocusGained;
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
  protected TextInputPanel buildComponent() {
    TextFieldBuilder<String, JTextField, ?> textFieldBuilder = new DefaultTextFieldBuilder<>(String.class, null)
            .selectAllOnFocusGained(selectAllOnFocusGained)
            .updateOn(updateOn)
            .columns(columns)
            .maximumLength(maximumLength)
            .upperCase(upperCase)
            .lowerCase(lowerCase);

    return TextInputPanel.builder(textFieldBuilder.build())
            .caption(caption)
            .maximumLength(maximumLength)
            .dialogTitle(dialogTitle)
            .textAreaSize(textAreaSize)
            .buttonFocusable(buttonFocusable)
            .build();
  }

  @Override
  protected ComponentValue<String, TextInputPanel> buildComponentValue(TextInputPanel component) {
    return component.componentValue();
  }

  @Override
  protected void setInitialValue(TextInputPanel component, String initialValue) {
    component.setText(initialValue);
  }

  @Override
  protected void setTransferFocusOnEnter(TextInputPanel component) {
    component.setTransferFocusOnEnter(true);
  }
}
