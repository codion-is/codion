/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

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

import static java.util.Objects.requireNonNull;

public final class FileInputPanel extends JPanel {

  private final JTextField filePathField;

  public FileInputPanel(JTextField filePathField) {
    this.filePathField = requireNonNull(filePathField);
    setLayout(Layouts.borderLayout());
    add(filePathField, BorderLayout.CENTER);
    JButton browseButton = new JButton(new AbstractAction("...") {
      @Override
      public void actionPerformed(ActionEvent e) {
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
      File file = Dialogs.fileSelectionDialog()
              .owner(filePathField)
              .title("Select file")
              .selectFile();
      filePathField.setText(file.toString());
    }
    catch (CancelException e) {
      filePathField.setText("");
      throw e;
    }
  }
}
