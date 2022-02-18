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

  DefaultLabelBuilder(final Icon icon) {
    this.text = null;
    icon(requireNonNull(icon));
    horizontalAlignment(SwingConstants.CENTER);
    focusable(false);
  }

  DefaultLabelBuilder(final String text) {
    this.text = text;
    focusable(false);
  }

  DefaultLabelBuilder(final ValueObserver<T> linkedValueObserver) {
    this.text = null;
    focusable(false);
    linkedValueObserver(requireNonNull(linkedValueObserver));
  }

  @Override
  public LabelBuilder<T> horizontalAlignment(final int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  public LabelBuilder<T> displayedMnemonic(final int displayedMnemonic) {
    this.displayedMnemonic = displayedMnemonic;
    return this;
  }

  @Override
  public LabelBuilder<T> labelFor(final JComponent component) {
    this.component = component;
    return this;
  }

  @Override
  public LabelBuilder<T> icon(final Icon icon) {
    this.icon = icon;
    return this;
  }

  @Override
  public LabelBuilder<T> iconTextGap(final int iconTextGap) {
    this.iconTextGap = iconTextGap;
    return this;
  }

  @Override
  protected JLabel buildComponent() {
    final JLabel label = new JLabel(text, icon, horizontalAlignment);
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
  protected ComponentValue<T, JLabel> buildComponentValue(final JLabel component) {
    return new AbstractComponentValue<T, JLabel>(component) {
      @Override
      protected T getComponentValue(final JLabel component) {
        return null;
      }

      @Override
      protected void setComponentValue(final JLabel component, final T value) {
        component.setText(value == null ? "" : value.toString());
      }
    };
  }

  @Override
  protected void setInitialValue(final JLabel component, final T initialValue) {
    component.setText(initialValue.toString());
  }
}
