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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
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
	 * Only applies for {@link #selectDirectory()} and {@link #selectDirectories()}.
	 * @param selectStartDirectory if true and a start directory is specified it is selected by default initially, false by default
	 * @return this builder instance
	 */
	FileSelectionDialogBuilder selectStartDirectory(boolean selectStartDirectory);

	/**
	 * @param confirmOverwrite specifies whether overwriting a file should be confirmed, true by default
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
