/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import javax.swing.JComponent;

/**
 * A default InputProvider implementation.
 */
public abstract class AbstractInputProvider<T> implements InputProvider<T> {

  private final JComponent inputComponent;

  public AbstractInputProvider(final JComponent inputComponent) {
    this.inputComponent = inputComponent;
  }

  public final JComponent getInputComponent() {
    return this.inputComponent;
  }

  public abstract T getValue();
}
