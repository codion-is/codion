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
    final JTextField field = new DefaultTextFieldBuilder<>(String.class)
            .updateOn(updateOn)
            .columns(columns)
            .maximumLength(maximumLength)
            .build();

    return TextInputPanel.builder(field)
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
  protected void setTransferFocusOnEnter(final TextInputPanel component) {
    Components.transferFocusOnEnter(component.getTextField());
    if (component.getButton() != null) {
      Components.transferFocusOnEnter(component.getButton());
    }
  }
}
