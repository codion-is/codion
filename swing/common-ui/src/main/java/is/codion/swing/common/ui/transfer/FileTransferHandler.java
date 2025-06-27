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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.transfer;

import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;

/**
 * Handles the importing of files during drag'n drop operations.
 */
public abstract class FileTransferHandler extends TransferHandler {

	@Override
	public boolean canImport(TransferSupport transferSupport) {
		return fileDataFlavor(transferSupport);
	}

	@Override
	public boolean importData(TransferSupport transferSupport) {
		List<File> files = transferFiles(transferSupport);
		if (files.isEmpty()) {
			return false;
		}

		return importFiles(transferSupport.getComponent(), files);
	}

	/**
	 * Makes the text component accept files during drag and drop operations and
	 * insert the absolute path of the dropped file (the first file in a list if more
	 * than one file is dropped)
	 * @param textComponent the text component
	 */
	public static void addSingleFileDragAndDropSupport(JTextComponent textComponent) {
		requireNonNull(textComponent);
		textComponent.setDragEnabled(true);
		textComponent.setTransferHandler(new FileTransferHandler() {
			@Override
			protected boolean importFiles(Component component, List<File> files) {
				textComponent.setText(files.get(0).getAbsolutePath());
				textComponent.requestFocusInWindow();

				return true;
			}
		});
	}

	/**
	 * @param transferSupport a drag'n drop transfer support instance
	 * @return true if the given transfer support instance represents a file or a list of files
	 */
	public static boolean fileDataFlavor(TransferSupport transferSupport) {
		requireNonNull(transferSupport);
		try {
			DataFlavor nixFileDataFlavor = nixFileDataFlavor();

			return stream(transferSupport.getDataFlavors())
							.anyMatch(flavor -> flavor.isFlavorJavaFileListType() || flavor.equals(nixFileDataFlavor));
		}
		catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Returns the files described by the given transfer support object.
	 * An empty list is returned if no files are found.
	 * @param transferSupport the drag'n drop transfer support
	 * @return the files described by the given transfer support object
	 * @throws RuntimeException in case of an exception
	 */
	public static List<File> transferFiles(TransferSupport transferSupport) {
		requireNonNull(transferSupport);
		try {
			for (DataFlavor flavor : transferSupport.getDataFlavors()) {
				if (flavor.isFlavorJavaFileListType()) {
					List<File> files = (List<File>) transferSupport.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

					return files.isEmpty() ? emptyList() : files;
				}
			}
			//the code below is for handling unix/linux
			List<File> files = new ArrayList<>();
			DataFlavor nixFileDataFlavor = nixFileDataFlavor();
			String data = (String) transferSupport.getTransferable().getTransferData(nixFileDataFlavor);
			for (StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
				String token = st.nextToken().trim();
				if (token.startsWith("#") || token.isEmpty()) {// comment line, by RFC 2483
					continue;
				}

				files.add(new File(new URI(token)));
			}

			return files;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Called after a successful import
	 * @param component the component
	 * @param files the imported files, non-null and containing at least one item
	 * @return true if the files were inserted into the component, false otherwise
	 */
	protected abstract boolean importFiles(Component component, List<File> files);

	private static DataFlavor nixFileDataFlavor() throws ClassNotFoundException {
		return new DataFlavor("text/uri-list;class=java.lang.String");
	}
}
