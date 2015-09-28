/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui.input;

import javax.swing.JComponent;

/**
 * A default InputProvider implementation.
 * @param <T> the value type
 * @param <K> the input component type
 */
public abstract class AbstractInputProvider<T, K extends JComponent> implements InputProvider<T, K> {

  private final K inputComponent;

  /**
   * Instantiates a new AbstractInputProvider.
   * @param inputComponent the input component
   */
  public AbstractInputProvider(final K inputComponent) {
    this.inputComponent = inputComponent;
  }

  /** {@inheritDoc} */
  @Override
  public final K getInputComponent() {
    return this.inputComponent;
  }

  /** {@inheritDoc} */
  @Override
  public abstract T getValue();
}
