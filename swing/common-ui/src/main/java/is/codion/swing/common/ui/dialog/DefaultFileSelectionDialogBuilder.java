/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.WaitCursor;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import java.awt.Window;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

final class DefaultFileSelectionDialogBuilder extends AbstractDialogBuilder<FileSelectionDialogBuilder>
        implements FileSelectionDialogBuilder {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(DefaultFileSelectionDialogBuilder.class.getName());

  /**
   * Caching the file chooser instances since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  private static JFileChooser fileChooserOpen;
  private static JFileChooser fileChooserSave;

  private final List<FileFilter> fileFilters = new ArrayList<>();
  private String startDirectory;
  private boolean confirmOverwrite = true;

  static {
    UIManager.addPropertyChangeListener(new LookAndFeelChangeListener());
  }

  @Override
  public FileSelectionDialogBuilder startDirectory(String startDirectory) {
    this.startDirectory = startDirectory;
    return this;
  }

  @Override
  public FileSelectionDialogBuilder confirmOverwrite(boolean confirmOverwrite) {
    this.confirmOverwrite = confirmOverwrite;
    return this;
  }

  @Override
  public FileSelectionDialogBuilder fileFilter(FileFilter fileFilter) {
    this.fileFilters.add(requireNonNull(fileFilter));
    return this;
  }

  @Override
  public File selectFile() {
    return selectFile(owner, startDirectory, titleProvider == null ? MESSAGES.getString("select_file") : titleProvider.get(), fileFilters);
  }

  @Override
  public List<File> selectFiles() {
    return selectFilesOrDirectories(owner, startDirectory, FilesOrDirectories.FILES, false,
            titleProvider == null ? MESSAGES.getString("select_files") : titleProvider.get(), fileFilters);
  }

  @Override
  public File selectDirectory() {
    return selectDirectory(owner, startDirectory, titleProvider == null ? MESSAGES.getString("select_directory") : titleProvider.get());
  }

  @Override
  public List<File> selectDirectories() {
    return selectFilesOrDirectories(owner, startDirectory, FilesOrDirectories.DIRECTORIES, false,
            titleProvider == null ? MESSAGES.getString("select_directories") : titleProvider.get(), emptyList());
  }

  @Override
  public File selectFileOrDirectory() {
    return selectFileOrDirectory(owner, startDirectory, FilesOrDirectories.BOTH,
            titleProvider == null ? MESSAGES.getString("select_file_or_directory") : titleProvider.get(), fileFilters);
  }

  @Override
  public List<File> selectFilesOrDirectories() {
    return selectFilesOrDirectories(owner, startDirectory, FilesOrDirectories.BOTH, false,
            titleProvider == null ? MESSAGES.getString("select_files_or_directories") : titleProvider.get(), fileFilters);
  }

  @Override
  public File selectFileToSave() {
    return selectFileToSave(null);
  }

  @Override
  public File selectFileToSave(String defaultFileName) {
    return selectFileToSave(owner, startDirectory, defaultFileName, confirmOverwrite);
  }

  /**
   * Specifies how a file selection dialog restricts its selection.
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

  static File selectDirectory(Window dialogParent, String startDir, String dialogTitle) {
    return selectFileOrDirectory(dialogParent, startDir, FilesOrDirectories.DIRECTORIES, dialogTitle, emptyList());
  }

  static File selectFile(Window dialogParent, String startDir, String dialogTitle,
                         List<FileFilter> fileFilters) {
    return selectFileOrDirectory(dialogParent, startDir, FilesOrDirectories.FILES, dialogTitle, fileFilters);
  }

  static File selectFileOrDirectory(Window dialogParent, String startDir,
                                    FilesOrDirectories filesOrDirectories, String dialogTitle,
                                    List<FileFilter> fileFilters) {
    return selectFilesOrDirectories(dialogParent, startDir, filesOrDirectories, false, dialogTitle, fileFilters).get(0);
  }

  static synchronized List<File> selectFilesOrDirectories(Window dialogParent, String startDir,
                                                          FilesOrDirectories filesOrDirectories,
                                                          boolean singleSelection,
                                                          String dialogTitle,
                                                          List<FileFilter> fileFilters) {
    if (fileChooserOpen == null) {
      try {
        WaitCursor.show(dialogParent);
        fileChooserOpen = new JFileChooser(new File(startDir == null ? System.getProperty("user.home") : startDir));
      }
      finally {
        WaitCursor.hide(dialogParent);
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
    fileChooserOpen.resetChoosableFileFilters();
    if (!fileFilters.isEmpty()) {
      fileChooserOpen.removeChoosableFileFilter(fileChooserOpen.getFileFilter());
    }
    fileFilters.forEach(fileChooserOpen::addChoosableFileFilter);
    fileChooserOpen.setMultiSelectionEnabled(!singleSelection);
    if (!nullOrEmpty(startDir) && new File(startDir).exists()) {
      fileChooserOpen.setCurrentDirectory(new File(startDir));
    }
    if (dialogTitle != null) {
      fileChooserOpen.setDialogTitle(dialogTitle);
    }
    int option = fileChooserOpen.showOpenDialog(dialogParent);
    if (option == JFileChooser.APPROVE_OPTION) {
      List<File> selectedFiles;
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
  static synchronized File selectFileToSave(Window dialogParent, String startDir,
                                            String defaultFileName, boolean confirmOverwrite) {
    if (fileChooserSave == null) {
      try {
        WaitCursor.show(dialogParent);
        fileChooserSave = new JFileChooser();
      }
      finally {
        WaitCursor.hide(dialogParent);
      }
    }
    fileChooserSave.setSelectedFiles(new File[] {new File("")});
    fileChooserSave.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooserSave.removeChoosableFileFilter(fileChooserSave.getFileFilter());
    fileChooserSave.setMultiSelectionEnabled(false);
    File startDirectory;
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

  private static final class LookAndFeelChangeListener implements PropertyChangeListener {
    private static final String LOOK_AND_FEEL_PROPERTY = "lookAndFeel";

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      if (LOOK_AND_FEEL_PROPERTY.equals(evt.getPropertyName())) {
        if (fileChooserOpen != null) {
          fileChooserOpen.updateUI();
        }
        if (fileChooserSave != null) {
          fileChooserSave.updateUI();
        }
      }
    }
  }
}
