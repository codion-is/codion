/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

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
