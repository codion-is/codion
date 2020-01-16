/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.TaskScheduler;
import org.jminor.common.Util;

import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;
import javax.swing.text.JTextComponent;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;

/**
 * A static utility class.
 */
public final class UiUtil {

  static {
    UiManagerDefaults.initialize();
  }

  private UiUtil() {}

  /**
   * Note that GTKLookAndFeel is overridden with MetalLookAndFeel, since JTabbedPane
   * does not respect the 'TabbedPane.contentBorderInsets' setting, making hierachical
   * tabbed panes look bad
   * @return the default look and feel for the platform we're running on
   */
  public static String getDefaultLookAndFeelClassName() {
    String systemLookAndFeel = UIManager.getSystemLookAndFeelClassName();
    if (systemLookAndFeel.endsWith("GTKLookAndFeel")) {
      systemLookAndFeel = "javax.swing.plaf.metal.MetalLookAndFeel";
    }

    return systemLookAndFeel;
  }

  /**
   * Creates a text field containing information about the memory usage
   * @param updateIntervalMilliseconds the interval between updating the memory usage info
   * @return a text field displaying the current VM memory usage
   */
  public static JTextField createMemoryUsageField(final int updateIntervalMilliseconds) {
    final JTextField textField = new JTextField(8);
    textField.setEditable(false);
    textField.setHorizontalAlignment(JTextField.CENTER);
    new TaskScheduler(() -> SwingUtilities.invokeLater(() ->
            textField.setText(Util.getMemoryUsageString())), updateIntervalMilliseconds, 0, TimeUnit.MILLISECONDS).start();

    return textField;
  }

  /**
   * Makes the text component accept files during drag and drop operations and
   * insert the absolute path of the dropped file (the first file in a list if more
   * than one file is dropped)
   * @param textComponent the text component
   */
  public static void addAcceptSingleFileDragAndDrop(final JTextComponent textComponent) {
    textComponent.setDragEnabled(true);
    textComponent.setTransferHandler(new FileTransferHandler(textComponent));
  }

  /**
   * @param transferSupport a drag'n drop transfer support instance
   * @return true if the given transfer support instance represents a file or a list of files
   */
  public static boolean isFileDataFlavor(final TransferHandler.TransferSupport transferSupport) {
    try {
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");

      return stream(transferSupport.getDataFlavors())
              .anyMatch(flavor -> flavor.isFlavorJavaFileListType() || flavor.equals(nixFileDataFlavor));
    }
    catch (final ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the files described by the given transfer support object.
   * An empty list is returned if no files are found.
   * @param support the drag'n drop transfer support
   * @return the files described by the given transfer support object
   * @throws RuntimeException in case of an exception
   */
  public static List<File> getTransferFiles(final TransferHandler.TransferSupport support) {
    try {
      for (final DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          final List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.isEmpty() ? emptyList() : files;
        }
      }
      //the code below is for handling unix/linux
      final List<File> files = new ArrayList<>();
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
      final String data = (String) support.getTransferable().getTransferData(nixFileDataFlavor);
      for (final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens(); ) {
        final String token = st.nextToken().trim();
        if (token.startsWith("#") || token.length() == 0) {// comment line, by RFC 2483
          continue;
        }

        files.add(new File(new URI(token)));
      }

      return files;
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Sets the given string as clipboard contents
   * @param string the string to put on the clipboard
   */
  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  /**
   * @param multiplier the font size multiplier
   */
  public static void setFontSize(final float multiplier) {
    final UIDefaults defaults = UIManager.getDefaults();
    final Enumeration enumeration = defaults.keys();
    while (enumeration.hasMoreElements()) {
      final Object key = enumeration.nextElement();
      final Object defaultValue = defaults.get(key);
      if (defaultValue instanceof Font) {
        final Font font = (Font) defaultValue;
        final int newSize = Math.round(font.getSize() * multiplier);
        if (defaultValue instanceof FontUIResource) {
          defaults.put(key, new FontUIResource(font.getName(), font.getStyle(), newSize));
        }
        else {
          defaults.put(key, new Font(font.getName(), font.getStyle(), newSize));
        }
      }
    }
  }

  private static final class FileTransferHandler extends TransferHandler {

    private final JTextComponent textComponent;

    private FileTransferHandler(final JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public boolean canImport(final TransferSupport transferSupport) {
      return UiUtil.isFileDataFlavor(transferSupport);
    }

    @Override
    public boolean importData(final TransferSupport transferSupport) {
      final List<File> files = UiUtil.getTransferFiles(transferSupport);
      if (files.isEmpty()) {
        return false;
      }

      textComponent.setText(files.get(0).getAbsolutePath());
      textComponent.requestFocusInWindow();
      return true;
    }
  }
}
