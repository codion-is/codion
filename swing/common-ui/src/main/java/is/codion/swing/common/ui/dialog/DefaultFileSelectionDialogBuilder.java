/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.swing.common.ui.Cursors;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.filechooser.FileFilter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

import static is.codion.common.NullOrEmpty.nullOrEmpty;
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
  private boolean selectStartDirectory = false;

  static {
    UIManager.addPropertyChangeListener(new LookAndFeelChangeListener());
  }

  @Override
  public FileSelectionDialogBuilder startDirectory(String startDirectory) {
    this.startDirectory = startDirectory;
    return this;
  }

  @Override
  public FileSelectionDialogBuilder selectStartDirectory(boolean selectStartDirectory) {
    this.selectStartDirectory = selectStartDirectory;
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
    return selectFile(MESSAGES.getString("select_file"));
  }

  @Override
  public List<File> selectFiles() {
    return selectFilesOrDirectories(FilesOrDirectories.FILES, MESSAGES.getString("select_files"), false);
  }

  @Override
  public File selectDirectory() {
    return selectDirectory(MESSAGES.getString("select_directory"));
  }

  @Override
  public List<File> selectDirectories() {
    return selectFilesOrDirectories(FilesOrDirectories.DIRECTORIES, MESSAGES.getString("select_directories"), false);
  }

  @Override
  public File selectFileOrDirectory() {
    return selectFileOrDirectory(FilesOrDirectories.BOTH, MESSAGES.getString("select_file_or_directory"));
  }

  @Override
  public List<File> selectFilesOrDirectories() {
    return selectFilesOrDirectories(FilesOrDirectories.BOTH, MESSAGES.getString("select_files_or_directories"), false);
  }

  @Override
  public File selectFileToSave() {
    return selectFileToSave(null);
  }

  @Override
  public File selectFileToSave(String defaultFileName) {
    synchronized (DefaultSelectionDialogBuilder.class) {
      if (fileChooserSave == null) {
        if (owner != null) {
          owner.setCursor(Cursors.WAIT);
        }
        try {
          fileChooserSave = new JFileChooser();
        }
        finally {
          if (owner != null) {
            owner.setCursor(Cursors.DEFAULT);
          }
        }
      }
      fileChooserSave.setSelectedFiles(new File[]{new File("")});
      fileChooserSave.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooserSave.removeChoosableFileFilter(fileChooserSave.getFileFilter());
      fileChooserSave.setMultiSelectionEnabled(false);
      File startDirectory;
      if (!nullOrEmpty(this.startDirectory) && new File(this.startDirectory).exists()) {
        startDirectory = new File(this.startDirectory);
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
        int option = fileChooserSave.showSaveDialog(owner);
        if (option == JFileChooser.APPROVE_OPTION) {
          selectedFile = fileChooserSave.getSelectedFile();
          if (selectedFile.exists() && confirmOverwrite) {
            option = JOptionPane.showConfirmDialog(owner, MESSAGES.getString("overwrite_file"),
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

  private File selectDirectory(String defaultDialogTitle) {
    return selectFileOrDirectory(FilesOrDirectories.DIRECTORIES, defaultDialogTitle);
  }

  private File selectFile(String defaultDialogTitle) {
    return selectFileOrDirectory(FilesOrDirectories.FILES, defaultDialogTitle);
  }

  private File selectFileOrDirectory(FilesOrDirectories filesOrDirectories, String defaultDialogTitle) {
    return selectFilesOrDirectories(filesOrDirectories, defaultDialogTitle, false).get(0);
  }

  private List<File> selectFilesOrDirectories(FilesOrDirectories filesOrDirectories, String defaultDialogTitle, boolean singleSelection) {
    synchronized (DefaultSelectionDialogBuilder.class) {
      if (fileChooserOpen == null) {
        if (owner != null) {
          owner.setCursor(Cursors.WAIT);
        }
        try {
          fileChooserOpen = new JFileChooser(new File(startDirectory == null ? System.getProperty("user.home") : startDirectory));
        }
        finally {
          if (owner != null) {
            owner.setCursor(Cursors.DEFAULT);
          }
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
      fileChooserOpen.setSelectedFiles(new File[] {initialSelection(filesOrDirectories)});
      fileChooserOpen.resetChoosableFileFilters();
      if (!fileFilters.isEmpty()) {
        fileChooserOpen.removeChoosableFileFilter(fileChooserOpen.getFileFilter());
      }
      fileFilters.forEach(fileChooserOpen::addChoosableFileFilter);
      fileChooserOpen.setMultiSelectionEnabled(!singleSelection);
      if (!nullOrEmpty(startDirectory) && new File(startDirectory).exists()) {
        fileChooserOpen.setCurrentDirectory(new File(startDirectory));
      }
      String dialogTitle = titleProvider == null ? defaultDialogTitle : titleProvider.get();
      if (dialogTitle != null) {
        fileChooserOpen.setDialogTitle(dialogTitle);
      }
      int option = fileChooserOpen.showOpenDialog(owner);
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
  }

  private File initialSelection(FilesOrDirectories filesOrDirectories) {
    if (filesOrDirectories == FilesOrDirectories.DIRECTORIES && selectStartDirectory && !nullOrEmpty(startDirectory)) {
      return new File(startDirectory);
    }

    return new File("");
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
