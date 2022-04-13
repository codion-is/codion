/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import java.awt.event.ActionListener;

abstract class AbstractButtonBuilder<T, C extends AbstractButton, B extends ButtonBuilder<T, C, B>>
        extends AbstractComponentBuilder<T, C, B> implements ButtonBuilder<T, C, B> {

  private String caption;
  private int mnemonic;
  private boolean includeCaption = true;
  private Icon icon;
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
  public final B icon(Icon icon) {
    this.icon = icon;
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
    if (mnemonic != 0) {
      button.setMnemonic(mnemonic);
    }
    if (icon != null) {
      button.setIcon(icon);
    }

    return button;
  }

  protected abstract C createButton();
}
