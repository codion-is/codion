/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.text.JTextComponent;

import static java.util.Objects.requireNonNull;

abstract class AbstractTextComponentBuilder<T, C extends JTextComponent, B extends TextComponentBuilder<T, C, B>>
        extends AbstractComponentBuilder<T, C, B> implements TextComponentBuilder<T, C, B> {

  protected boolean editable = true;
  protected UpdateOn updateOn = UpdateOn.KEYSTROKE;
  protected int columns;
  protected boolean upperCase;
  protected boolean lowerCase;
  protected int maximumLength;

  @Override
  public B editable(final boolean editable) {
    this.editable = editable;
    return (B) this;
  }

  @Override
  public B updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return (B) this;
  }

  @Override
  public B columns(final int columns) {
    this.columns = columns;
    return (B) this;
  }

  @Override
  public B upperCase() {
    this.upperCase = true;
    this.lowerCase = false;
    return (B) this;
  }

  @Override
  public B lowerCase() {
    this.lowerCase = true;
    this.upperCase = false;
    return (B) this;
  }

  @Override
  public B maximumLength(final int maximumLength) {
    this.maximumLength = maximumLength;
    return (B) this;
  }
}
