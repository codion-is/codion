/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.ValueObserver;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

final class DefaultLabelBuilder<T> extends AbstractComponentBuilder<T, JLabel, LabelBuilder<T>> implements LabelBuilder<T> {

  private final String text;

  private Icon icon;
  private int horizontalAlignment = LABEL_TEXT_ALIGNMENT.get();
  private int displayedMnemonic = 0;
  private int iconTextGap = -1;
  private JComponent component;

  DefaultLabelBuilder(Icon icon) {
    this.text = null;
    icon(requireNonNull(icon));
    horizontalAlignment(SwingConstants.CENTER);
    focusable(false);
  }

  DefaultLabelBuilder(String text) {
    this.text = text;
    focusable(false);
  }

  DefaultLabelBuilder(ValueObserver<T> linkedValueObserver) {
    this.text = null;
    focusable(false);
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
    if (displayedMnemonic != 0) {
      label.setDisplayedMnemonic(displayedMnemonic);
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
