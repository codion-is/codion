/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.ui.textfield.TextFieldPlus;

import com.toedter.calendar.JCalendar;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.TransferHandler;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.net.URI;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * A static utility class.
 */
public class UiUtil {

  public static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  public static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  /**
   * A square dimension which sides are the same as the preferred height of a JTextField.
   * This comes in handy when f.ex. adding "..." lookup buttons next to text fields.
   */
  public static final Dimension DIMENSION_TEXT_FIELD_SQUARE =
          new Dimension(getPreferredTextFieldHeight(), getPreferredTextFieldHeight());

  private static final Map<RootPaneContainer, Integer> waitCursorRequests = new HashMap<RootPaneContainer, Integer>();
  /**
   * Caching the file chooser since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  private static JFileChooser fileChooser;
  /**
   * A text field used by getPreferredTextFieldSize and getPreferredTextFieldHeight
   */
  private static JTextField textField;
  private static JScrollBar verticalScrollBar;

  private UiUtil() {}

  public static File selectDirectory(final JComponent dialogParent, final String startDir) throws CancelException {
    return selectDirectory(dialogParent, startDir, null);
  }

  public static File selectDirectory(final JComponent dialogParent, final String startDir,
                                     final String dialogTitle) throws CancelException {
    if (fileChooser == null) {
      try {
        setWaitCursor(true, dialogParent);
        fileChooser = new JFileChooser(new File(startDir == null ? System.getProperty("user.home") : startDir));
      }
      finally {
        setWaitCursor(false, dialogParent);
      }
    }
    fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    fileChooser.removeChoosableFileFilter(fileChooser.getFileFilter());
    fileChooser.setMultiSelectionEnabled(false);
    if (startDir != null && startDir.length() > 0)
      fileChooser.setCurrentDirectory(new File(startDir));
    if (dialogTitle != null)
      fileChooser.setDialogTitle(dialogTitle);
    final int ret = fileChooser.showOpenDialog(dialogParent);
    if (ret == JFileChooser.APPROVE_OPTION)
      return fileChooser.getSelectedFile();
    else
      throw new CancelException();
  }

  public static File selectFile(final JComponent dialogParent, final String startDir) throws CancelException {
    if (fileChooser == null) {
      try {
        setWaitCursor(true, dialogParent);
        fileChooser = new JFileChooser(new File(startDir == null ? System.getProperty("user.home") : startDir));
      }
      finally {
        setWaitCursor(false, dialogParent);
      }
    }
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.removeChoosableFileFilter(fileChooser.getFileFilter());
    fileChooser.setMultiSelectionEnabled(false);
    if (startDir != null && startDir.length() > 0)
      fileChooser.setCurrentDirectory(new File(startDir));
    final int option = fileChooser.showOpenDialog(dialogParent);
    if (option == JFileChooser.APPROVE_OPTION) {
      final File selectedFile = fileChooser.getSelectedFile();
      if (selectedFile.exists()) {
        return selectedFile;
      }
    }

    throw new CancelException();
  }

  public static File chooseFileToSave(final JComponent dialogParent, final String startDir, final String defaultFileName)
          throws CancelException {
    if (fileChooser == null) {
      try {
        setWaitCursor(true, dialogParent);
        fileChooser = new JFileChooser();
      }
      finally {
        setWaitCursor(false, dialogParent);
      }
    }
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.removeChoosableFileFilter(fileChooser.getFileFilter());
    fileChooser.setMultiSelectionEnabled(false);
    File selectedFile = new File((startDir == null ? System.getProperty("user.home") : startDir)
            + (defaultFileName != null ? (System.getProperty("file.separator") + defaultFileName) : ""));
    boolean fileChosen = false;
    while (!fileChosen) {
      if (selectedFile.isDirectory())
        fileChooser.setCurrentDirectory(selectedFile);
      else
        fileChooser.setSelectedFile(selectedFile);
      int option = fileChooser.showSaveDialog(dialogParent);
      if (option == JFileChooser.APPROVE_OPTION) {
        selectedFile = fileChooser.getSelectedFile();
        if (selectedFile.exists()) {
          option = JOptionPane.showConfirmDialog(dialogParent, Messages.get(Messages.OVERWRITE_FILE),
                  Messages.get(Messages.FILE_EXISTS), JOptionPane.YES_NO_CANCEL_OPTION);
          if (option == JOptionPane.YES_OPTION)
            fileChosen = true;
          else if (option == JOptionPane.CANCEL_OPTION)
            throw new CancelException();
        }
        else
          fileChosen = true;
      }
      else
        throw new CancelException();
    }

    return selectedFile;
  }

  public static Date getDateFromUser(final Date startDate, final String message, final Container parent) {
    final Calendar cal = Calendar.getInstance();
    if (startDate != null)
      cal.setTime(startDate);

    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    final JCalendar calendar = new JCalendar(cal);

    showInDialog(getParentWindow(parent), calendar, true, message, true, true, null);

    return new Date(calendar.getCalendar().getTimeInMillis());
  }

  public static JFormattedTextField createFormattedField(final String mask) {
    return createFormattedField(mask, false);
  }

  public static JFormattedTextField createFormattedField(final String mask, final boolean valueContainsLiteralCharacter) {
    return createFormattedField(mask,valueContainsLiteralCharacter,false);
  }

  public static JFormattedTextField createFormattedField(final String mask, final boolean valueContainsLiteralCharacter,
                                                         final boolean charsAsUpper) {
    try {
      final MaskFormatter formatter = new MaskFormatter(mask) {
        @Override
        public Object stringToValue(final String value) throws ParseException {
          String ret = value;
          if (charsAsUpper)
            ret = ret.toUpperCase();

          return super.stringToValue(ret);
        }
      };
      formatter.setPlaceholderCharacter('_');
      formatter.setAllowsInvalid(false);
      formatter.setValueContainsLiteralCharacters(valueContainsLiteralCharacter);

      final JFormattedTextField formattedTextField = new JFormattedTextField(formatter);
      formattedTextField.setFocusLostBehavior(JFormattedTextField.COMMIT);
      moveCaretToStartOnFocusGained(formattedTextField);

      return formattedTextField;
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static Action linkToEnabledState(final State enabledState, final Action action) {
    if (enabledState != null) {
      action.setEnabled(enabledState.isActive());
      enabledState.eventStateChanged().addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          action.setEnabled(enabledState.isActive());
        }
      });
    }

    return action;
  }

  public static JComponent linkToEnabledState(final State enabledState, final JComponent component) {
    if (enabledState != null) {
      component.setEnabled(enabledState.isActive());
      enabledState.eventStateChanged().addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          component.setEnabled(enabledState.isActive());
        }
      });
    }

    return component;
  }

  public static JFrame createFrame(final Image icon) {
    final JFrame frame = new JFrame();
    if (icon != null)
      frame.setIconImage(icon);

    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    return frame;
  }

  /**
   * @param ratio a ratio, 0.0 - 1.0
   * @return a Dimension which is the size of the available screen times ratio
   */
  public static Dimension getScreenSizeRatio(final double ratio) {
    final Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();

    return new Dimension((int) (screen.getWidth() * ratio), (int) (screen.getHeight() * ratio));
  }

  public static void setSizeWithinScreenBounds(final Window window) {
    final Dimension screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    final Dimension frameSize = window.getSize();
    if (frameSize.getHeight() > screenSize.getHeight() || frameSize.getWidth() > screenSize.getWidth()) {
      Dimension newFrameSize = new Dimension((int) Math.min(frameSize.getWidth(), screenSize.getWidth()),
              (int) Math.min(frameSize.getHeight(), screenSize.getHeight()));
      window.setSize(newFrameSize);
    }
  }

  public static void resizeWindow(final Window window, final double screenSizeRatio) {
    resizeWindow(window, screenSizeRatio, null);
  }

  public static void resizeWindow(final Window window, final double screenSizeRatio,
                                  final Dimension minimumSize) {
    final Dimension ratioSize = getScreenSizeRatio(screenSizeRatio);
    if (minimumSize != null)
      ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));

    window.setSize(ratioSize);
  }

  public static Window getParentWindow(final Container container) {
    final Window window = getParentDialog(container);

    return window == null ? getParentFrame(container) : window;
  }

  public static JFrame getParentFrame(Container container) {
    while (!(container instanceof JFrame) && (container != null))
      container = container.getParent();

    return (JFrame) container;
  }

  public static JDialog getParentDialog(Container container) {
    while (!(container instanceof JDialog) && (container != null))
      container = container.getParent();

    return (JDialog) container;
  }

  public static void centerWindow(final Window window) {
    final Dimension size = window.getSize();
    final Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    window.setLocation((int) (screen.getWidth() - size.getWidth()) / 2,
            (int) (screen.getHeight() - size.getHeight()) / 2);
  }

  public static void expandAll(final JTree tree, final TreePath parent, final boolean expand) {
    // Traverse children
    final TreeNode node = (TreeNode)parent.getLastPathComponent();
    if (node.getChildCount() >= 0) {
      final Enumeration e = node.children();
      while (e.hasMoreElements())
        expandAll(tree, parent.pathByAddingChild(e.nextElement()), expand);
    }
    // Expansion or collapse must be done bottom-up
    if (expand)
      tree.expandPath(parent);
    else
      tree.collapsePath(parent);
  }

  public static void setWaitCursor(final boolean on, final JComponent component) {
    RootPaneContainer root = getParentDialog(component);
    if (root == null)
      root = getParentFrame(component);
    if (root == null)
      return;

    synchronized (waitCursorRequests) {
      if (!waitCursorRequests.containsKey(root))
        waitCursorRequests.put(root, 0);

      int requests = waitCursorRequests.get(root);
      if (on)
        requests++;
      else
        requests--;

      if ((requests == 1 && on) || (requests == 0 && !on)) {
        root.getRootPane().setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
      }
      if (requests == 0)
        waitCursorRequests.remove(root);
      else
        waitCursorRequests.put(root, requests);
    }
  }

  public static int getPreferredScrollBarWidth() {
    if (verticalScrollBar == null)
      verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);

    return verticalScrollBar.getPreferredSize().width;
  }

  public static Dimension getPreferredTextFieldSize() {
    if (textField == null)
      textField = new JTextField();

    return textField.getPreferredSize();
  }

  public static int getPreferredTextFieldHeight() {
    if (textField == null)
      textField = new JTextField();

    return textField.getPreferredSize().height;
  }

  public static void showToolTip(final JComponent component) {
    final Action toolTipAction = component.getActionMap().get("postTip");
    if (toolTipAction != null)
      toolTipAction.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, ""));
  }

  /**
   * Makes <code>textField</code> convert all lower case input to upper case
   * @param textField the text field
   * @return the text field
   */
  public static JTextComponent makeUpperCase(final JTextComponent textField) {
    if (textField instanceof TextFieldPlus)
      return makeUpperCase((TextFieldPlus) textField);

    ((PlainDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
      @Override
      public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, text == null ? text : text.toUpperCase(), attr);
      }
      @Override
      public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
      }
      @Override
      public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        super.replace(fb, offset, length, text == null ? text : text.toUpperCase(), attrs);
      }
    });

    return textField;
  }

  public static TextFieldPlus makeUpperCase(final TextFieldPlus textField) {
    textField.setUpperCase(true);
    return textField;
  }

  /**
   * Makes <code>textField</code> convert all upper case input to lower case
   * @param textField the text field
   * @return the text field
   */
  public static JTextComponent makeLowerCase(final JTextComponent textField) {
    ((PlainDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
      @Override
      public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string.toLowerCase(), attr);
      }
      @Override
      public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
      }
      @Override
      public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        super.replace(fb, offset, length, text.toLowerCase(), attrs);
      }
    });

    return textField;
  }

  /**
   * Attaches a key listener to the component which transfers focus
   * on enter, and backwards if shift is down
   * @param component the component
   */
  public static void transferFocusOnEnter(final JComponent component) {
    component.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
          if (evt.isShiftDown())
            component.transferFocusBackward();
          else
            component.transferFocus();
        }
      }
    });
  }

  /**
   * Selects all text in the given component when it gains focus and clears
   * the selection when focus is lost
   * @param textComponent the text component
   */
  public static void selectAllOnFocusGained(final JTextComponent textComponent) {
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        textComponent.selectAll();
      }
      @Override
      public void focusLost(final FocusEvent e) {
        textComponent.select(0,0);
      }
    });
  }

  /**
   * Sets the caret position to 0 in the given text component when it gains focus
   * @param textComponent the text component
   */
  public static void moveCaretToStartOnFocusGained(final JTextComponent textComponent) {
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        textComponent.setCaretPosition(0);
      }
    });
  }

  /**
   * Sets the caret position to the right of the last character in the given text component when it gains focus
   * @param textComponent the text component
   */
  public static void moveCaretToEndOnFocusGained(final JTextComponent textComponent) {
    textComponent.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(final FocusEvent e) {
        textComponent.setCaretPosition(textComponent.getText().length());
      }
    });
  }

  public static JDialog showInDialog(final Window owner, final JComponent componentToShow, final boolean modal, final String title,
                                     final boolean includeButtonPanel, final boolean disposeOnOk, final Action okAction) {
    return showInDialog(owner, componentToShow, modal, title, includeButtonPanel,disposeOnOk, okAction, null);
  }

  public static JDialog showInDialog(final Window owner, final JComponent componentToShow, final boolean modal, final String title,
                                     final boolean includeButtonPanel, final boolean disposeOnOk, final Action okAction,
                                     final Dimension size) {
    return showInDialog(owner, componentToShow, modal, title, includeButtonPanel,disposeOnOk, okAction, size, null, null);
  }

  public static JDialog showInDialog(final Window owner, final JComponent componentToShow, final boolean modal, final String title,
                                     final boolean includeButtonPanel, final boolean disposeOnOk, final Action okAction,
                                     final Dimension size, final Point location, final Action closeAction) {
    final JDialog dialog = new JDialog(owner, title);
    dialog.setLayout(new BorderLayout());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    if (closeAction != null) {
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosing(WindowEvent we) {
          closeAction.actionPerformed(new ActionEvent(dialog, -1, null));
        }
      });
    }
    final String okCaption = okAction != null ? (String) okAction.getValue(Action.NAME) : Messages.get(Messages.OK);
    final Action ok = new AbstractAction(okCaption) {
      public void actionPerformed(ActionEvent e) {
        if (okAction != null)
          okAction.actionPerformed(e);
        if (disposeOnOk) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      }
    };
    if (includeButtonPanel) {
      final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
      final JButton okButton = new JButton(ok);
      Character okMnemonic;
      if (okAction != null && okAction.getValue(Action.MNEMONIC_KEY) != null)
        okMnemonic = (Character) okAction.getValue(Action.MNEMONIC_KEY);
      else
        okMnemonic = Messages.get(Messages.OK_MNEMONIC).charAt(0);

      if (okMnemonic != null)
        okButton.setMnemonic(okMnemonic);
      buttonPanel.add(okButton);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
    }
    dialog.add(componentToShow, BorderLayout.CENTER);
    if (size == null)
      dialog.pack();
    else
      dialog.setSize(size);
    if (location == null)
      dialog.setLocationRelativeTo(owner);
    else
      dialog.setLocation(location);
    dialog.setModal(modal);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  public static JDialog showInDialog(final Container owner, final JComponent componentToShow, final boolean modal,
                                     final String title, final Dimension size, final JButton defaultButton,
                                     final Event closeEvent) {
    final JDialog dialog = new JDialog(getParentWindow(owner), title);
    dialog.setLayout(new BorderLayout());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    if (defaultButton != null)
      dialog.getRootPane().setDefaultButton(defaultButton);

    final Action disposeActionListener = new AbstractAction() {
      public void actionPerformed(final ActionEvent e) {
        dialog.dispose();
      }
    };
    dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "closeDialog");
    dialog.getRootPane().getActionMap().put("closeDialog", disposeActionListener);
    if (closeEvent != null)
      closeEvent.addListener(disposeActionListener);

    dialog.add(componentToShow, BorderLayout.CENTER);
    if (size == null)
      dialog.pack();
    else
      dialog.setSize(size);

    dialog.setLocationRelativeTo(owner);
    dialog.setModal(modal);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  /**
   * Makes the text component accept files during drag and drop operations and
   * insert the absolute path of the dropped file (the first file in a list if more
   * than one file is dropped)
   * @param textComponent the text component
   */
  public static void addAcceptSingleFileDragAndDrop(final JTextComponent textComponent) {
    textComponent.setDragEnabled(true);
    textComponent.setTransferHandler(new TransferHandler() {
      @Override
      public boolean canImport(final TransferSupport support) {
        try {
          final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
          for (final DataFlavor flavor : support.getDataFlavors())
            if (flavor.isFlavorJavaFileListType() || flavor.equals(nixFileDataFlavor))
              return true;

          return false;
        }
        catch (ClassNotFoundException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public boolean importData(final TransferSupport support) {
        final String path = getFileDataFlavor(support);
        if (path != null) {
          textComponent.setText(path);
          return true;
        }
        else
          return false;
      }
    });
  }

  @SuppressWarnings({"unchecked"})
  private static String getFileDataFlavor(final TransferHandler.TransferSupport support) {
    try {
      for (final DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          final List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.size() > 0 ? files.get(0).getAbsolutePath() : null;
        }
      }
      //the code below is for handling unix/linux
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
      final String data = (String) support.getTransferable().getTransferData(nixFileDataFlavor);
      for (final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
        final String token = st.nextToken().trim();
        if (token.startsWith("#") || token.isEmpty()) // comment line, by RFC 2483
          continue;

        return new File(new URI(token)).getAbsolutePath();
      }
    }
    catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }

    return null;
  }
}
