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
  private int columns;
  private boolean upperCase = false;
  private boolean lowerCase = false;
  private boolean selectAllOnFocusGained;
  private Dimension textAreaSize;
  private int maximumLength;
  private String caption;
  private String dialogTitle;

  DefaultTextInputPanelBuilder(final Value<String> linkedValue) {
    super(linkedValue);
  }

  @Override
  public TextInputPanelBuilder updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return this;
  }

  @Override
  public TextInputPanelBuilder columns(final int columns) {
    this.columns = columns;
    return this;
  }

  @Override
  public TextInputPanelBuilder upperCase(final boolean upperCase) {
    if (upperCase && lowerCase) {
      throw new IllegalArgumentException("Field is already lowercase");
    }
    this.upperCase = upperCase;
    return this;
  }

  @Override
  public TextInputPanelBuilder lowerCase(final boolean lowerCase) {
    if (lowerCase && upperCase) {
      throw new IllegalArgumentException("Field is already uppercase");
    }
    this.lowerCase = lowerCase;
    return this;
  }

  @Override
  public TextInputPanelBuilder selectAllOnFocusGained(final boolean selectAllOnFocusGained) {
    this.selectAllOnFocusGained = selectAllOnFocusGained;
    return this;
  }

  @Override
  public TextInputPanelBuilder buttonFocusable(final boolean buttonFocusable) {
    this.buttonFocusable = buttonFocusable;
    return this;
  }

  @Override
  public TextInputPanelBuilder textAreaSize(final Dimension textAreaSize) {
    this.textAreaSize = requireNonNull(textAreaSize);
    return this;
  }

  @Override
  public TextInputPanelBuilder maximumLength(final int maximumLength) {
    this.maximumLength = maximumLength;
    return this;
  }

  @Override
  public TextInputPanelBuilder caption(final String caption) {
    this.caption = caption;
    return this;
  }

  @Override
  public TextInputPanelBuilder dialogTitle(final String dialogTitle) {
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
  protected ComponentValue<String, TextInputPanel> buildComponentValue(final TextInputPanel component) {
    return component.componentValue();
  }

  @Override
  protected void setInitialValue(final TextInputPanel component, final String initialValue) {
    component.setText(initialValue);
  }

  @Override
  protected void setTransferFocusOnEnter(final TextInputPanel component) {
    component.setTransferFocusOnEnter(true);
  }
}
