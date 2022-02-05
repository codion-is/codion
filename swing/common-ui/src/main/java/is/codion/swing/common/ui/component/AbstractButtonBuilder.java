/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;
import is.codion.common.value.ValueObserver;

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

  protected AbstractButtonBuilder(final Value<T> linkedValue) {
    super(linkedValue);
  }

  protected AbstractButtonBuilder(final ValueObserver<T> linkedValueObserver) {
    super(linkedValueObserver);
  }

  @Override
  public final B caption(final String caption) {
    this.caption = caption;
    return (B) this;
  }

  @Override
  public final B mnemonic(final int mnemonic) {
    this.mnemonic = mnemonic;
    return (B) this;
  }

  @Override
  public final B includeCaption(final boolean includeCaption) {
    this.includeCaption = includeCaption;
    return (B) this;
  }

  @Override
  public final B icon(final Icon icon) {
    this.icon = icon;
    return (B) this;
  }

  @Override
  public final B action(final Action action) {
    this.action = action;
    return (B) this;
  }

  @Override
  public final B actionListener(final ActionListener actionListener) {
    this.actionListener = actionListener;
    return (B) this;
  }

  @Override
  protected final C buildComponent() {
    final C button = createButton();
    if (action != null) {
      button.setAction(action);
    }
    if (actionListener != null) {
      button.addActionListener(actionListener);
    }
    if (includeCaption) {
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
