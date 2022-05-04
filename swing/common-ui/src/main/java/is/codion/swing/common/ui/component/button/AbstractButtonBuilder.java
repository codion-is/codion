/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.AbstractComponentBuilder;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.SwingConstants;
import java.awt.Insets;
import java.awt.event.ActionListener;

import static java.util.Objects.requireNonNull;

abstract class AbstractButtonBuilder<T, C extends AbstractButton, B extends ButtonBuilder<T, C, B>>
        extends AbstractComponentBuilder<T, C, B> implements ButtonBuilder<T, C, B> {

  private String caption;
  private int mnemonic;
  private boolean includeCaption = true;
  private int horizontalAlignment = SwingConstants.CENTER;
  private Icon icon;
  private Insets insets;
  private Action action;
  private ActionListener actionListener;

  protected AbstractButtonBuilder(Value<T> linkedValue) {
    super(linkedValue);
  }

  @Override
  public final B caption(String caption) {
    this.caption = caption;
    return (B) this;
  }

  @Override
  public final B mnemonic(int mnemonic) {
    this.mnemonic = mnemonic;
    return (B) this;
  }

  @Override
  public final B includeCaption(boolean includeCaption) {
    this.includeCaption = includeCaption;
    return (B) this;
  }

  @Override
  public final B horizontalAlignment(int horizontalAlignment) {
    this.horizontalAlignment = horizontalAlignment;
    return (B) this;
  }

  @Override
  public final B icon(Icon icon) {
    this.icon = icon;
    return (B) this;
  }

  @Override
  public final B margin(Insets insets) {
    this.insets = requireNonNull(insets);
    return (B) this;
  }

  @Override
  public final B action(Action action) {
    this.action = action;
    return (B) this;
  }

  @Override
  public final B actionListener(ActionListener actionListener) {
    this.actionListener = actionListener;
    return (B) this;
  }

  @Override
  protected final C createComponent() {
    C button = createButton();
    if (action != null) {
      button.setAction(action);
    }
    if (actionListener != null) {
      button.addActionListener(actionListener);
    }
    if (!includeCaption) {
      button.setText(null);
    }
    else if (caption != null) {
      button.setText(caption);
    }
    button.setHorizontalAlignment(horizontalAlignment);
    if (mnemonic != 0) {
      button.setMnemonic(mnemonic);
    }
    if (icon != null) {
      button.setIcon(icon);
    }
    if (insets != null) {
      button.setMargin(insets);
    }

    return button;
  }

  protected abstract C createButton();
}
