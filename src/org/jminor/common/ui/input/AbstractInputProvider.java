/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import javax.swing.JComponent;

/**
 * A default InputProvider implementation.
 */
public abstract class AbstractInputProvider<T, K extends JComponent> implements InputProvider<T, K> {

  private final K inputComponent;

  public AbstractInputProvider(final K inputComponent) {
    this.inputComponent = inputComponent;
  }

  public final K getInputComponent() {
    return this.inputComponent;
  }

  public abstract T getValue();
}
