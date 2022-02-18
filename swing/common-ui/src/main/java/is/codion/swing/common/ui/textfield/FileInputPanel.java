/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.io.File;

public final class FileInputPanel extends JPanel {

  private final JTextField filePathField = new JTextField(20);

  public FileInputPanel() {
    filePathField.setEditable(false);
    filePathField.setFocusable(false);
    setLayout(Layouts.borderLayout());
    add(filePathField, BorderLayout.CENTER);
    final JButton browseButton = new JButton(new AbstractAction("...") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        browseFile();
      }
    });
    browseButton.setPreferredSize(TextComponents.DIMENSION_TEXT_FIELD_SQUARE);
    add(browseButton, BorderLayout.EAST);
  }

  public JTextField getFilePathField() {
    return filePathField;
  }

  private void browseFile() {
    try {
      final File file = Dialogs.fileSelectionDialog()
              .owner(filePathField)
              .title("Select file")
              .selectFile();
      filePathField.setText(file.toString());
    }
    catch (final CancelException e) {
      filePathField.setText("");
      throw e;
    }
  }
}
