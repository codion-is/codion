/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.common.value.Value;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.textfield.TextInputPanel;
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

  DefaultTextInputPanelBuilder(final Property<String> attribute, final Value<String> value) {
    super(attribute, value);
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
  protected TextInputPanel buildComponent() {
    final JTextField field = new DefaultTextFieldBuilder<>(property, value)
            .updateOn(updateOn)
            .columns(columns)
            .build();
    final TextInputPanel panel = TextInputPanel.builder(field)
            .dialogTitle(property.getCaption())
            .textAreaSize(textAreaSize)
            .buttonFocusable(buttonFocusable)
            .build();
    panel.setMaximumLength(property.getMaximumLength());

    return panel;
  }

  @Override
  protected void setTransferFocusOnEnter(final TextInputPanel component) {
    Components.transferFocusOnEnter(component.getTextField());
    if (component.getButton() != null) {
      Components.transferFocusOnEnter(component.getButton());
    }
  }
}
