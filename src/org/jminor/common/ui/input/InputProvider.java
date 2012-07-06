/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.input;

import javax.swing.JComponent;

/**
 * Specifies a generic class for providing a value via a UI component.
 * @param <T> the value type
 * @param <K> the input component type
 */
public interface InputProvider<T, K extends JComponent> {

  /**
   * @return the input component
   */
  K getInputComponent();

  /**
   * @return the value according to the input component
   */
  T getValue();
}
