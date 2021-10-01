/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.TextInputPanel;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.common.ui.value.UpdateOn;

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
  protected TextInputPanel buildComponent() {
    final TextFieldBuilder<String, JTextField, ?> textFieldBuilder = new DefaultTextFieldBuilder<>(String.class)
            .selectAllOnFocusGained(selectAllOnFocusGained)
            .updateOn(updateOn)
            .columns(columns)
            .maximumLength(maximumLength)
            .upperCase(upperCase)
            .lowerCase(lowerCase);

    return TextInputPanel.builder(textFieldBuilder.build())
            .dialogTitle(caption)
            .textAreaSize(textAreaSize)
            .buttonFocusable(buttonFocusable)
            .build();
  }

  @Override
  protected ComponentValue<String, TextInputPanel> buildComponentValue(final TextInputPanel component) {
    return ComponentValues.textInputPanel(component);
  }

  @Override
  protected void setInitialValue(final TextInputPanel component, final String initialValue) {
    component.setText(initialValue);
  }

  @Override
  protected void setTransferFocusOnEnter(final TextInputPanel component) {
    Components.transferFocusOnEnter(component.getTextField());
    Components.transferFocusOnEnter(component.getButton());
  }
}
