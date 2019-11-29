/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.input;

import org.jminor.common.model.CancelException;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.Controls;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * A InputProvider implementation for Blob file based values.
 */
public class BlobInputProvider extends AbstractInputProvider<byte[], BlobInputProvider.FileInputPanel> {

  /**
   * Instantiates a new BlobInputProvider, providing blobs via files
   */
  public BlobInputProvider() {
    super(new FileInputPanel());
  }

  /** {@inheritDoc} */
  @Override
  public byte[] getValue() {
    final String filePath = getInputComponent().filePathField.getText();
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

  public static final class FileInputPanel extends JPanel {

    private final JTextField filePathField = new JTextField(20);

    public FileInputPanel() {
      filePathField.setEditable(false);
      filePathField.setFocusable(false);
      setLayout(new BorderLayout(5, 5));
      add(filePathField, BorderLayout.CENTER);
      final JButton browseButton = new JButton(Controls.control(this::browseFile, "..."));
      browseButton.setPreferredSize(UiUtil.DIMENSION_TEXT_FIELD_SQUARE);
      add(browseButton, BorderLayout.EAST);
    }

    private void browseFile() {
      try {
        final File file = UiUtil.selectFile(filePathField, null, "Select file");
        filePathField.setText(file.toString());
      }
      catch (final CancelException e) {
        filePathField.setText("");
        throw e;
      }
    }
  }
}
