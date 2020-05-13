/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

/**
 * Utility class for file based {@link ComponentValue} instances.
 */
public final class FileValues {

  private FileValues() {}

  /**
   * @return a file based ComponentValue
   */
  public static ComponentValue<byte[], FileInputPanelValue.FileInputPanel> fileInputValue() {
    return new FileInputPanelValue();
  }
}
