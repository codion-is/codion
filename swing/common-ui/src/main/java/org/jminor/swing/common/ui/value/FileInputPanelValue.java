/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.model.CancelException;
import org.jminor.swing.common.model.textfield.DocumentAdapter;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.common.ui.textfield.TextFields;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

final class FileInputPanelValue extends AbstractComponentValue<byte[], FileInputPanelValue.FileInputPanel> {

  FileInputPanelValue() {
    super(new FileInputPanel());
    getComponent().filePathField.getDocument().addDocumentListener((DocumentAdapter) e -> notifyValueChange());
  }

  @Override
  protected byte[] getComponentValue(final FileInputPanel component) {
    final String filePath = component.filePathField.getText();
    if (filePath.isEmpty()) {
      return null;
    }
    try {
      return Files.readAllBytes(new File(filePath).toPath());
    }
    catch (final IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void setComponentValue(final FileInputPanel component, final byte[] value) {
    throw new UnsupportedOperationException();
  }

  public static final class FileInputPanel extends JPanel {

    private final JTextField filePathField = new JTextField(20);

    private FileInputPanel() {
      filePathField.setEditable(false);
      filePathField.setFocusable(false);
      setLayout(Layouts.createBorderLayout());
      add(filePathField, BorderLayout.CENTER);
      final JButton browseButton = new JButton(new AbstractAction("...") {
        @Override
        public void actionPerformed(final ActionEvent e) {
          browseFile();
        }
      });
      browseButton.setPreferredSize(TextFields.DIMENSION_TEXT_FIELD_SQUARE);
      add(browseButton, BorderLayout.EAST);
    }

    public JTextField getFilePathField() {
      return filePathField;
    }

    private void browseFile() {
      try {
        final File file = Dialogs.selectFile(filePathField, null, "Select file");
        filePathField.setText(file.toString());
      }
      catch (final CancelException e) {
        filePathField.setText("");
        throw e;
      }
    }
  }
}
