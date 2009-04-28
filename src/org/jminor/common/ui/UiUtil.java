/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Event;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;

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
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.RootPaneContainer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;

/**
 * A static utility class
 */
public class UiUtil {

  public final static Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  public final static Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  /**
   * A square dimension which sides are the same as the preferred height of a JTextField.
   * This comes in handy when f.ex. adding "..." lookup buttons next to text fields.
   */
  public final static Dimension DIMENSION_TEXT_FIELD_SQUARE =
          new Dimension(getPreferredTextFieldHeight(), getPreferredTextFieldHeight());

  private static int waitCursorRequests = 0;
  /**
   * Caching the file chooser since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  private static JFileChooser fileChooser;
  /**
   * A text field used by getPreferredTextFieldSize and getPreferredTextFieldHeight
   */
  private static JTextField textField;

  private UiUtil() {}

  public static File selectDirectory(final JComponent dialogParent, final String startDir) throws UserCancelException {
    return selectDirectory(dialogParent, startDir, null);
  }

  public static File selectDirectory(final JComponent dialogParent, final String startDir,
                                     final String dialogTitle) throws UserCancelException {
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
      throw new UserCancelException();
  }

  public static File selectFile(final JComponent dialogParent, final String startDir) throws UserCancelException {
    if (fileChooser == null) {
      try {
        setWaitCursor(true, dialogParent);
        fileChooser = new JFileChooser(new File(startDir == null ? System.getProperty("user.home") : startDir));
      }
      finally {
        setWaitCursor(false, dialogParent);
      }
    }
    if (startDir != null && startDir.length() > 0)
      fileChooser.setCurrentDirectory(new File(startDir));
    final int option = fileChooser.showOpenDialog(dialogParent);
    if (option == JFileChooser.APPROVE_OPTION) {
      final File selectedFile = fileChooser.getSelectedFile();
      if (selectedFile.exists()) {
        return selectedFile;
      }
    }

    throw new UserCancelException();
  }

  public static File chooseFileToSave(final JComponent dialogParent, final String startDir, final String defaultFileName)
          throws UserCancelException {
    if (fileChooser == null) {
      try {
        setWaitCursor(true, dialogParent);
        fileChooser = new JFileChooser();
      }
      finally {
        setWaitCursor(false, dialogParent);
      }
    }
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
          option = JOptionPane.showConfirmDialog(dialogParent, Messages.get(Messages.FILE_EXISTS),
                  Messages.get(Messages.OVERWRITE_FILE), JOptionPane.YES_NO_CANCEL_OPTION);
          if (option == JOptionPane.YES_OPTION)
            fileChosen = true;
          else if (option == JOptionPane.CANCEL_OPTION)
            throw new UserCancelException();
        }
        else
          fileChosen = true;
      }
      else
        throw new UserCancelException();
    }

    return selectedFile;
  }

  public static Date getDateFromUser(final Date startDate, final String message,
                                     final JComponent parent) throws UserCancelException {
    final String[] options = new String[] {
            Messages.get(Messages.OK), Messages.get(Messages.CANCEL)};
    final Calendar calendar = Calendar.getInstance();
    final JCalendar jCalendar = new JCalendar();
    if (startDate != null)
      calendar.setTime(startDate);
    jCalendar.setCalendar(calendar);
    final int res = JOptionPane.showOptionDialog(parent, jCalendar, message, -1, -1, null, options, null);
    if (res == 0) {
      jCalendar.getCalendar().set(Calendar.HOUR_OF_DAY, 0);
      jCalendar.getCalendar().set(Calendar.MINUTE, 0);
      jCalendar.getCalendar().set(Calendar.SECOND, 0);
      jCalendar.getCalendar().set(Calendar.MILLISECOND, 0);

      return new Date(jCalendar.getCalendar().getTimeInMillis());
    }

    throw new UserCancelException();
  }

  public static JFormattedTextField createFormattedField(final String mask) {
    return createFormattedField(mask, false);
  }

  public static JFormattedTextField createFormattedField(final String mask,
                                                         final boolean valueContainsLiteralCharacter) {
    return createFormattedField(mask,valueContainsLiteralCharacter,false);
  }

  public static JFormattedTextField createFormattedField(final String mask,
                                                         final boolean valueContainsLiteralCharacter,
                                                         final boolean charsAsUpper) {
    try {
      final MaskFormatter formatter = new MaskFormatter(mask) {
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

      final JFormattedTextField ret = new JFormattedTextField(formatter);
      ret.setFocusLostBehavior(JFormattedTextField.COMMIT);

      return ret;
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static void bindColumnAndPanelSizes(final TableColumnModel columnModel, final List<JPanel> panelList) {
    if (columnModel.getColumnCount() != panelList.size())
      throw new IllegalArgumentException("An equal number of columns and panels is required when binding sizes");

    for (int columnIndex = 0; columnIndex < columnModel.getColumnCount(); columnIndex++) {
      final JPanel panel = panelList.get(columnIndex);
      final TableColumn column = columnModel.getColumn(columnIndex);
      panel.setPreferredSize(new Dimension(column.getWidth(), panel.getPreferredSize().height));
      column.addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals("width")) {
            panel.setPreferredSize(new Dimension(column.getWidth(), panel.getPreferredSize().height));
            panel.revalidate();
          }
        }
      });
    }
  }

  public static Action linkToEnabledState(final State enabledState, final Action action) {
    if (enabledState != null) {
      action.setEnabled(enabledState.isActive());
      enabledState.evtStateChanged.addListener(new ActionListener() {
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
      enabledState.evtStateChanged.addListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          component.setEnabled(enabledState.isActive());
        }
      });
    }

    return component;
  }

  public static JFrame createFrame(final Image icon) {
    final JFrame ret = new JFrame();
    if (icon != null)
      ret.setIconImage(icon);

    ret.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

    return ret;
  }

  public static Dimension getSize(final double ratio) {
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
    final Dimension ratioSize = getSize(screenSizeRatio);
    if (minimumSize != null)
      ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));

    window.setSize(ratioSize);
  }

  public static Window getParentWindow(Container container) {
    Window ret = getParentDialog(container);

    return ret == null ? getParentFrame(container) : ret;
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

  /**
   * Sets the popup menu as the table popup menu and adds a key listener which
   * shows the popup menu on SHIFT-space
   * @param table the table
   * @param popupMenu the popup menu
   */
  public static void setTablePopup(final JTable table, final JPopupMenu popupMenu) {
    table.setComponentPopupMenu(popupMenu);
    table.getTableHeader().setComponentPopupMenu(popupMenu);
    table.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {//shift-space shows popup menu
        if (e.isShiftDown() && e.getKeyChar() == ' ')
          popupMenu.show(table, 100, table.getSelectedRow() * table.getRowHeight());
      }
    });
  }

  public static void setWaitCursor(final boolean on, final JComponent component) {
    if (on)
      waitCursorRequests++;
    else
      waitCursorRequests--;
    if ((waitCursorRequests == 1 && on) || (waitCursorRequests == 0 && !on)) {
      RootPaneContainer root = getParentDialog(component);
      if (root != null) {
        root.getRootPane().setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
      }
      root = getParentFrame(component);
      if (root != null) {
        root.getRootPane().setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
      }
    }
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
  public static JTextField makeUpperCase(final JTextField textField) {
    ((PlainDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
      public void insertString(FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, text == null ? text : text.toUpperCase(), attr);
      }
      public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
      }
      public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        super.replace(fb, offset, length, text == null ? text : text.toUpperCase(), attrs);
      }
    });

    return textField;
  }

  /**
   * Makes <code>textField</code> convert all upper case input to lower case
   * @param textField the text field
   * @return the text field
   */
  public static JTextField makeLowerCase(final JTextField textField) {
    ((PlainDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
      public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string.toLowerCase(), attr);
      }
      public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
      }
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
   * Selects all text in the given component when it gains focus
   * @param textComponent the text component
   */
  public static void selectAllOnFocusGained(final JTextComponent textComponent) {
    textComponent.addFocusListener(new FocusAdapter() {
      public void focusGained(final FocusEvent e) {
        textComponent.selectAll();
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
      final Character mnemonic = okAction == null || okAction.getValue(Action.MNEMONIC_KEY) == null ?
              okCaption.charAt(0) : ((Character) okAction.getValue(Action.MNEMONIC_KEY));
      okButton.setMnemonic(mnemonic);
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

  public static JDialog showInDialog(final Container owner, final JComponent componentToShow, final boolean modal, final String title,
                                     final Dimension size, final JButton defaultButton, final Event closeEvent) {
    final JDialog dialog = new JDialog(getParentWindow(owner), title);
    dialog.setLayout(new BorderLayout());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    if (closeEvent != null) {
      closeEvent.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          dialog.dispose();
        }
      });
    }
    if (defaultButton != null)
      dialog.getRootPane().setDefaultButton(defaultButton);
    dialog.getRootPane().getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close");

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
}
