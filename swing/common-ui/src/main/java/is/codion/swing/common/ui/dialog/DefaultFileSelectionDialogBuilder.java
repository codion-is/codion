/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.dialog.Dialogs.FileSelectionDialogBuilder;

import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import java.awt.Window;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.singletonList;

final class DefaultFileSelectionDialogBuilder implements FileSelectionDialogBuilder {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(Dialogs.class.getName());

  /**
   * Caching the file chooser instances since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  private static JFileChooser fileChooserOpen;
  private static JFileChooser fileChooserSave;
  private Window owner;
  private String title;
  private String startDirectory;
  private boolean confirmOverwrite = true;

  @Override
  public FileSelectionDialogBuilder owner(final Window owner) {
    this.owner = owner;
    return this;
  }

  @Override
  public FileSelectionDialogBuilder dialogParent(final JComponent dialogParent) {
    if (owner != null) {
      throw new IllegalStateException("owner has alrady been set");
    }
    this.owner = dialogParent == null ? null : Windows.getParentWindow(dialogParent);
    return this;
  }

  @Override
  public FileSelectionDialogBuilder title(final String title) {
    this.title = title;
    return this;
  }

  @Override
  public FileSelectionDialogBuilder startDirectory(final String startDirectory) {
    this.startDirectory = startDirectory;
    return this;
  }

  @Override
  public FileSelectionDialogBuilder confirmOverwrite(final boolean confirmOverwrite) {
    this.confirmOverwrite = confirmOverwrite;
    return this;
  }

  @Override
  public File selectFile() {
    return selectFile(owner, startDirectory, title);
  }

  @Override
  public File selectDirectory() {
    return selectDirectory(owner, startDirectory, title);
  }

  @Override
  public List<File> selectFilesOrDirectories() {
    return selectFilesOrDirectories(owner, startDirectory, FilesOrDirectories.BOTH, false, title);
  }

  @Override
  public File selectFileToSave() {
    return selectFileToSave(null);
  }

  @Override
  public File selectFileToSave(final String defaultFileName) {
    return selectFileToSave(owner, startDirectory, defaultFileName, confirmOverwrite);
  }

  /**
   * Specifies how a file selection dialog restricts it's selection.
   */
  private enum FilesOrDirectories {
    /**
     * Only files.
     */
    FILES,
    /**
     * Only directories.
     */
    DIRECTORIES,
    /**
     * Both files and directories.
     */
    BOTH
  }

  static File selectDirectory(final Window dialogParent, final String startDir, final String dialogTitle) {
    return selectFileOrDirectory(dialogParent, startDir, FilesOrDirectories.DIRECTORIES, dialogTitle);
  }

  static File selectFile(final Window dialogParent, final String startDir, final String dialogTitle) {
    return selectFileOrDirectory(dialogParent, startDir, FilesOrDirectories.FILES, dialogTitle);
  }

  static File selectFileOrDirectory(final Window dialogParent, final String startDir,
                                    final FilesOrDirectories filesOrDirectories, final String dialogTitle) {
    return selectFilesOrDirectories(dialogParent, startDir, filesOrDirectories, false, dialogTitle).get(0);
  }

  static synchronized List<File> selectFilesOrDirectories(final Window dialogParent, final String startDir,
                                                          final FilesOrDirectories filesOrDirectories,
                                                          final boolean singleSelection,
                                                          final String dialogTitle) {
    if (fileChooserOpen == null) {
      try {
        Components.showWaitCursor(dialogParent);
        fileChooserOpen = new JFileChooser(new File(startDir == null ? System.getProperty("user.home") : startDir));
      }
      finally {
        Components.hideWaitCursor(dialogParent);
      }
    }
    switch (filesOrDirectories) {
      case FILES:
        fileChooserOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
        break;
      case DIRECTORIES:
        fileChooserOpen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        break;
      case BOTH:
        fileChooserOpen.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        break;
    }
    fileChooserOpen.setSelectedFiles(new File[] {new File("")});
    fileChooserOpen.removeChoosableFileFilter(fileChooserOpen.getFileFilter());
    fileChooserOpen.setMultiSelectionEnabled(!singleSelection);
    if (!nullOrEmpty(startDir) && new File(startDir).exists()) {
      fileChooserOpen.setCurrentDirectory(new File(startDir));
    }
    if (dialogTitle != null) {
      fileChooserOpen.setDialogTitle(dialogTitle);
    }
    final int option = fileChooserOpen.showOpenDialog(dialogParent);
    if (option == JFileChooser.APPROVE_OPTION) {
      final List<File> selectedFiles;
      if (singleSelection) {
        selectedFiles = singletonList(fileChooserOpen.getSelectedFile());
      }
      else {
        selectedFiles = Arrays.asList(fileChooserOpen.getSelectedFiles());
      }
      if (!selectedFiles.isEmpty()) {
        return selectedFiles;
      }
    }

    throw new CancelException();
  }

  /**
   * Displays a save file dialog for creating a new file
   * @param dialogParent the dialog parent
   * @param startDir the start dir, user.dir if not specified
   * @param defaultFileName the default file name to suggest
   * @param confirmOverwrite specifies whether overwriting a file should be confirmed
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  static synchronized File selectFileToSave(final Window dialogParent, final String startDir,
                                            final String defaultFileName, final boolean confirmOverwrite) {
    if (fileChooserSave == null) {
      try {
        Components.showWaitCursor(dialogParent);
        fileChooserSave = new JFileChooser();
      }
      finally {
        Components.hideWaitCursor(dialogParent);
      }
    }
    fileChooserSave.setSelectedFiles(new File[] {new File("")});
    fileChooserSave.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooserSave.removeChoosableFileFilter(fileChooserSave.getFileFilter());
    fileChooserSave.setMultiSelectionEnabled(false);
    final File startDirectory;
    if (!nullOrEmpty(startDir) && new File(startDir).exists()) {
      startDirectory = new File(startDir);
    }
    else {
      startDirectory = fileChooserSave.getCurrentDirectory();
    }
    File selectedFile = new File(startDirectory.getAbsolutePath() + (defaultFileName != null ? File.separator + defaultFileName : ""));
    boolean fileChosen = false;
    while (!fileChosen) {
      if (selectedFile.isDirectory()) {
        fileChooserSave.setCurrentDirectory(selectedFile);
      }
      else {
        fileChooserSave.setSelectedFile(selectedFile);
      }
      int option = fileChooserSave.showSaveDialog(dialogParent);
      if (option == JFileChooser.APPROVE_OPTION) {
        selectedFile = fileChooserSave.getSelectedFile();
        if (selectedFile.exists() && confirmOverwrite) {
          option = JOptionPane.showConfirmDialog(dialogParent, MESSAGES.getString("overwrite_file"),
                  MESSAGES.getString("file_exists"), JOptionPane.YES_NO_CANCEL_OPTION);
          if (option == JOptionPane.YES_OPTION) {
            fileChosen = true;
          }
          else if (option == JOptionPane.CANCEL_OPTION) {
            throw new CancelException();
          }
        }
        else {
          fileChosen = true;
        }
      }
      else {
        throw new CancelException();
      }
    }

    return selectedFile;
  }
}
