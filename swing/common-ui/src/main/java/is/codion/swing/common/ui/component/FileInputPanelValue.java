/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.model.component.text.DocumentAdapter;
import is.codion.swing.common.ui.component.text.FileInputPanel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

final class FileInputPanelValue extends AbstractComponentValue<byte[], FileInputPanel> {

  FileInputPanelValue(FileInputPanel fileInputPanel) {
    super(fileInputPanel);
    fileInputPanel.getFilePathField().getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
  }

  @Override
  protected byte[] getComponentValue(FileInputPanel component) {
    String filePath = component.getFilePathField().getText();
    if (filePath.isEmpty()) {
      return null;
    }
    try {
      return Files.readAllBytes(new File(filePath).toPath());
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void setComponentValue(FileInputPanel component, byte[] value) {
    throw new UnsupportedOperationException();
  }
}
