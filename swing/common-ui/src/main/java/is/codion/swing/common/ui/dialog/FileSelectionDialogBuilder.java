/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.util.List;

/**
 * A builder for a file/directory selection dialog
 */
public interface FileSelectionDialogBuilder extends DialogBuilder<FileSelectionDialogBuilder> {

  /**
   * @param startDirectory the start directory
   * @return this builder instance
   */
  FileSelectionDialogBuilder startDirectory(String startDirectory);

  /**
   * @param confirmOverwrite specifies whether overwriting a file should be confirmed
   * @return this builder instance
   */
  FileSelectionDialogBuilder confirmOverwrite(boolean confirmOverwrite);

  /**
   * @param fileFilter the file filter to add, only applicable to file selection
   * @return this builder instance
   */
  FileSelectionDialogBuilder fileFilter(FileFilter fileFilter);

  /**
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  File selectFile();

  /**
   * @return the selected files
   * @throws CancelException in case the user cancels
   */
  List<File> selectFiles();

  /**
   * @return the selected directory
   * @throws CancelException in case the user cancels
   */
  File selectDirectory();

  /**
   * @return the selected directories
   * @throws CancelException in case the user cancels
   */
  List<File> selectDirectories();

  /**
   * @return the selected file or directory
   * @throws CancelException in case the user cancels
   */
  File selectFileOrDirectory();

  /**
   * @return a List containing the selected files, contains at least one file
   * @throws CancelException in case the user cancels or no files are selected
   */
  List<File> selectFilesOrDirectories();

  /**
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  File selectFileToSave();

  /**
   * @param defaultFileName the default file name
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  File selectFileToSave(String defaultFileName);
}
