/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.ValueObserver;
import is.codion.swing.common.ui.value.AbstractComponentValue;
import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import static java.util.Objects.requireNonNull;

final class DefaultLabelBuilder extends AbstractComponentBuilder<String, JLabel, LabelBuilder> implements LabelBuilder {

  private final String text;

  private Icon icon;
  private int horizontalAlignment = LABEL_TEXT_ALIGNMENT.get();
  private int displayedMnemonic = 0;
  private int iconTextGap = -1;
  private JComponent component;

  DefaultLabelBuilder(final Icon icon) {
    this(null, null);
    icon(requireNonNull(icon));
    horizontalAlignment(SwingConstants.CENTER);
  }

  DefaultLabelBuilder(final String text, final ValueObserver<String> linkedValueObserver) {
    this.text = text;
    focusable(false);
    linkedValueObserver(linkedValueObserver);
  }

  @Override
  public LabelBuilder horizontalAlignment(final int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return this;
  }

  @Override
  public LabelBuilder displayedMnemonic(final int displayedMnemonic) {
    this.displayedMnemonic = displayedMnemonic;
    return this;
  }

  @Override
  public LabelBuilder labelFor(final JComponent component) {
    this.component = component;
    return this;
  }

  @Override
  public LabelBuilder icon(final Icon icon) {
    this.icon = icon;
    return this;
  }

  @Override
  public LabelBuilder iconTextGap(final int iconTextGap) {
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
  protected ComponentValue<String, JLabel> buildComponentValue(final JLabel component) {
    return new AbstractComponentValue<String, JLabel>(component) {
      @Override
      protected String getComponentValue(final JLabel component) {
        return null;
      }

      @Override
      protected void setComponentValue(final JLabel component, final String value) {
        component.setText(value);
      }
    };
  }

  @Override
  protected void setInitialValue(final JLabel component, final String initialValue) {
    component.setText(initialValue);
  }
}
