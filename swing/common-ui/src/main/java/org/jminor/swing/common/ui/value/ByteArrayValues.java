/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

public final class ByteArrayValues {

  /**
   * @return a blob based ComponentValue
   */
  public static ComponentValue<byte[], FileInputPanelValue.FileInputPanel> blobValue() {
    return new FileInputPanelValue();
  }
}
