/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.text.JTextComponent;
import java.awt.Insets;

import static java.util.Objects.requireNonNull;

abstract class AbstractTextComponentBuilder<T, C extends JTextComponent, B extends TextComponentBuilder<T, C, B>>
        extends AbstractComponentBuilder<T, C, B> implements TextComponentBuilder<T, C, B> {

  protected boolean editable = true;
  protected UpdateOn updateOn = UpdateOn.KEYSTROKE;
  protected int columns;
  protected boolean upperCase;
  protected boolean lowerCase;
  protected int maximumLength = -1;
  protected Insets margin;

  protected AbstractTextComponentBuilder(final Value<T> linkedValue) {
    super(linkedValue);
  }

  @Override
  public final B editable(final boolean editable) {
    this.editable = editable;
    return (B) this;
  }

  @Override
  public final B updateOn(final UpdateOn updateOn) {
    this.updateOn = requireNonNull(updateOn);
    return (B) this;
  }

  @Override
  public final B columns(final int columns) {
    this.columns = columns;
    return (B) this;
  }

  @Override
  public final B upperCase(final boolean upperCase) {
    if (upperCase && lowerCase) {
      throw new IllegalArgumentException("Field is already lowercase");
    }
    this.upperCase = upperCase;
    return (B) this;
  }

  @Override
  public final B lowerCase(final boolean lowerCase) {
    if (lowerCase && upperCase) {
      throw new IllegalArgumentException("Field is already uppercase");
    }
    this.lowerCase = lowerCase;
    return (B) this;
  }

  @Override
  public final B maximumLength(final int maximumLength) {
    this.maximumLength = maximumLength;
    return (B) this;
  }

  @Override
  public final B margin(final Insets margin) {
    this.margin = margin;
    return (B) this;
  }
}
