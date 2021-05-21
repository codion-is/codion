/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.builder;

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
  public TextInputPanel build() {
    final TextInputPanel inputPanel = createTextInputPanel();
    setPreferredSize(inputPanel);
    onBuild(inputPanel);
    inputPanel.getTextField().setColumns(columns);
    if (transferFocusOnEnter) {
      Components.transferFocusOnEnter(inputPanel.getTextField());
      if (inputPanel.getButton() != null) {
        Components.transferFocusOnEnter(inputPanel.getButton());
      }
    }

    return inputPanel;
  }

  private TextInputPanel createTextInputPanel() {
    final JTextField field = new DefaultTextFieldBuilder<>(property, value)
            .updateOn(updateOn)
            .build();
    final TextInputPanel panel = new TextInputPanel(field, property.getCaption(), textAreaSize,
            buttonFocusable ? TextInputPanel.ButtonFocusable.YES : TextInputPanel.ButtonFocusable.NO);
    panel.setMaximumLength(property.getMaximumLength());

    return panel;
  }
}
