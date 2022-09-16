/*
 * Copyright (c) 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.component.text.FileInputPanel;

import javax.swing.JTextField;

import static is.codion.swing.common.ui.component.text.FileInputPanel.fileInputPanel;
import static java.util.Objects.requireNonNull;

final class DefaultFileInputPanelBuilder extends AbstractComponentBuilder<byte[], FileInputPanel, FileInputPanelBuilder>
        implements FileInputPanelBuilder {

  private final JTextField filePathField;

  DefaultFileInputPanelBuilder(JTextField filePathField) {
    this.filePathField = requireNonNull(filePathField);
  }

  @Override
  protected FileInputPanel createComponent() {
    return fileInputPanel(filePathField);
  }

  @Override
  protected ComponentValue<byte[], FileInputPanel> createComponentValue(FileInputPanel component) {
    return new FileInputPanelValue(component);
  }

  @Override
  protected void setInitialValue(FileInputPanel component, byte[] initialValue) {}
}
