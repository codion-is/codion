/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.label;

import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;
import is.codion.swing.common.ui.component.ComponentValue;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

final class DefaultLabelBuilder<T> extends AbstractComponentBuilder<T, JLabel, LabelBuilder<T>> implements LabelBuilder<T> {

  private final String text;

  private Icon icon;
  private int horizontalAlignment = LABEL_TEXT_ALIGNMENT.get();
  private Integer displayedMnemonic;
  private Character displayedMnemonicChar;
  private int iconTextGap = -1;
  private JComponent component;

  DefaultLabelBuilder(Icon icon) {
    this.text = null;
    icon(requireNonNull(icon));
    horizontalAlignment(SwingConstants.CENTER);
  }

  DefaultLabelBuilder(String text) {
    this.text = text;
  }

  DefaultLabelBuilder(ValueObserver<T> linkedValueObserver) {
    this.text = null;
    linkedValueObserver(requireNonNull(linkedValueObserver));
  }

  @Override
  public LabelBuilder<T> horizontalAlignment(int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  public LabelBuilder<T> displayedMnemonic(int displayedMnemonic) {
    this.displayedMnemonic = displayedMnemonic;
    return this;
  }

  @Override
  public LabelBuilder<T> displayedMnemonic(char displayedMnemonic) {
    this.displayedMnemonicChar = displayedMnemonic;
    return this;
  }

  @Override
  public LabelBuilder<T> labelFor(JComponent component) {
    this.component = component;
    return this;
  }

  @Override
  public LabelBuilder<T> icon(Icon icon) {
    this.icon = icon;
    return this;
  }

  @Override
  public LabelBuilder<T> iconTextGap(int iconTextGap) {
    this.iconTextGap = iconTextGap;
    return this;
  }

  @Override
  protected JLabel createComponent() {
    JLabel label = new JLabel(text, icon, horizontalAlignment);
    if (displayedMnemonic != null) {
      label.setDisplayedMnemonic(displayedMnemonic);
    }
    if (displayedMnemonicChar != null) {
      label.setDisplayedMnemonic(displayedMnemonicChar);
    }
    if (component != null) {
      label.setLabelFor(component);
    }
    if (iconTextGap >= 0) {
      label.setIconTextGap(iconTextGap);
    }

    return label;
  }

  @Override
  protected ComponentValue<T, JLabel> createComponentValue(JLabel component) {
    return new LabelComponentValue<>(component);
  }

  @Override
  protected void setInitialValue(JLabel component, T initialValue) {
    component.setText(initialValue.toString());
  }
}