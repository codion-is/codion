/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;
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
 * Handles the importing of files.
 */
public abstract class FileTransferHandler extends TransferHandler {

  @Override
  public boolean canImport(TransferSupport transferSupport) {
    return isFileDataFlavor(transferSupport);
  }

  @Override
  public boolean importData(TransferSupport transferSupport) {
    List<File> files = getTransferFiles(transferSupport);
    if (files.isEmpty()) {
      return false;
    }

    onImport(files);
    return true;
  }

  /**
   * Makes the text component accept files during drag and drop operations and
   * insert the absolute path of the dropped file (the first file in a list if more
   * than one file is dropped)
   * @param textComponent the text component
   */
  public static void addSingleFileDragAndDropSupport(JTextComponent textComponent) {
    requireNonNull(textComponent, "textComponent");
    textComponent.setDragEnabled(true);
    textComponent.setTransferHandler(new FileTransferHandler() {
      @Override
      protected void onImport(List<File> importedFiles) {
        textComponent.setText(importedFiles.get(0).getAbsolutePath());
        textComponent.requestFocusInWindow();
      }
    });
  }

  /**
   * @param transferSupport a drag'n drop transfer support instance
   * @return true if the given transfer support instance represents a file or a list of files
   */
  public static boolean isFileDataFlavor(TransferSupport transferSupport) {
    requireNonNull(transferSupport, "transferSupport");
    try {
      DataFlavor nixFileDataFlavor = getNixFileDataFlavor();

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
  public static List<File> getTransferFiles(TransferSupport transferSupport) {
    requireNonNull(transferSupport, "transferSupport");
    try {
      for (DataFlavor flavor : transferSupport.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          List<File> files = (List<File>) transferSupport.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.isEmpty() ? emptyList() : files;
        }
      }
      //the code below is for handling unix/linux
      List<File> files = new ArrayList<>();
      DataFlavor nixFileDataFlavor = getNixFileDataFlavor();
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
   * @param importedFiles the imported files
   */
  protected abstract void onImport(List<File> importedFiles);

  private static DataFlavor getNixFileDataFlavor() throws ClassNotFoundException {
    return new DataFlavor("text/uri-list;class=java.lang.String");
  }
}
