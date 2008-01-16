/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.db.User;
import org.jminor.common.db.UserAccessException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.State;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.model.UserException;
import org.jminor.common.model.formats.AbstractDateMaskFormat;

import com.toedter.calendar.JCalendar;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
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
import javax.swing.RootPaneContainer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.MaskFormatter;
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;

/**
 * A static utility class
 */
public class UiUtil {

  public final static Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  public final static Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);
  public static int waitCursorRequests = 0;
  /**
   * Caching the file chooser since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  public static JFileChooser fileChooser;
  public static JTextField textField;

  private UiUtil() {}

  public static File chooseFileToSave(final Component dialogParent, final String startDir) throws UserCancelException {
    return chooseFileToSave(dialogParent, startDir, Messages.get(Messages.OVERWRITE_FILE), Messages.get(Messages.FILE_EXISTS));
  }

  public static File chooseFileToSave(final Component dialogParent, final String startDir,
                                      final String overwriteDialogText, final String overwriteDialogTitle)
          throws UserCancelException {
    final JFileChooser fileChooser = new JFileChooser();
    File selectedFile = new File(startDir == null ? "C://" : startDir);
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
          option = JOptionPane.showConfirmDialog(dialogParent, overwriteDialogText,
                  overwriteDialogTitle, JOptionPane.YES_NO_CANCEL_OPTION);
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
    return createFormattedField(mask, valueContainsLiteralCharacter, charsAsUpper, false);
  }

  public static JFormattedTextField createFormattedField(final String mask,
                                                         final boolean valueContainsLiteralCharacter,
                                                         final boolean charsAsUpper,
                                                         final boolean transferFocusOnEnter) {
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
      if (transferFocusOnEnter)
        UiUtil.transferFocusOnEnter(ret);

      return ret;
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  public static void bindColumnSizesAndPanelSizes(final JTable table, final JPanel[] pnlColumns) {
    table.addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        syncSearchPanelSize(table, pnlColumns);
      }

      public void componentResized(ComponentEvent e) {
        syncSearchPanelSize(table, pnlColumns);
      }
    });

    final TableColumnModel cm = table.getColumnModel();
    for (int i = 0; i < cm.getColumnCount(); i++) {
      cm.getColumn(i).addPropertyChangeListener(new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals("width"))
            syncSearchPanelSize(table, pnlColumns);
        }
      });
    }
  }

  private static void syncSearchPanelSize(final JTable table, final JPanel[] pnlColumns) {
    final TableColumnModel cm = table.getColumnModel();
    for (int i = 0; i < cm.getColumnCount(); i++)
      syncColumnSize(cm.getColumn(i), pnlColumns[i]);
  }

  private static void syncColumnSize(final TableColumn tableColumn, final JPanel pnlColumn) {
    final int tableColumnWidth = tableColumn.getWidth();
    final int searchPanePreferredlHeight = pnlColumn.getPreferredSize().height;
    final Dimension newDim = new Dimension(tableColumnWidth, searchPanePreferredlHeight);
    pnlColumn.setPreferredSize(newDim);
    pnlColumn.getParent().addNotify();
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

  /*UIManager.setLookAndFeel("org.jvnet.substance.SubstanceLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceAutumnLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceBusinessLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceChallengerDeepLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceCremeLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceEmeraldDuskLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceFieldOfWheatLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceGreenMagicLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceMagmaLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceMangoLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceMistAquaLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceMistSilverLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceModerateLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceNebulaBrickWallLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceNebulaLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceOfficeBlue2007LookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceOfficeSilver2007LookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceRavenLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceRavenGraphiteGlassLookAndFeel");
    UIManager.setLookAndFeel("org.jvnet.substance.skin.SubstanceSaharaLookAndFeel); */

  public static void setLookAndFeel(final Window owner) throws IllegalAccessException, UnsupportedLookAndFeelException,
          InstantiationException, ClassNotFoundException {
    final JComboBox box = new JComboBox(new String[] {
            "org.jvnet.substance.SubstanceLookAndFeel",
            "org.jvnet.substance.skin.SubstanceAutumnLookAndFeel",
            "org.jvnet.substance.skin.SubstanceBusinessLookAndFeel",
            "org.jvnet.substance.skin.SubstanceChallengerDeepLookAndFeel",
            "org.jvnet.substance.skin.SubstanceCremeLookAndFeel",
            "org.jvnet.substance.skin.SubstanceEmeraldDuskLookAndFeel",
            "org.jvnet.substance.skin.SubstanceFieldOfWheatLookAndFeel",
            "org.jvnet.substance.skin.SubstanceGreenMagicLookAndFeel",
            "org.jvnet.substance.skin.SubstanceMagmaLookAndFeel",
            "org.jvnet.substance.skin.SubstanceMangoLookAndFeel",
            "org.jvnet.substance.skin.SubstanceMistAquaLookAndFeel",
            "org.jvnet.substance.skin.SubstanceMistSilverLookAndFeel",
            "org.jvnet.substance.skin.SubstanceModerateLookAndFeel",
            "org.jvnet.substance.skin.SubstanceNebulaBrickWallLookAndFeel",
            "org.jvnet.substance.skin.SubstanceNebulaLookAndFeel",
            "org.jvnet.substance.skin.SubstanceOfficeBlue2007LookAndFeel",
            "org.jvnet.substance.skin.SubstanceOfficeSilver2007LookAndFeel",
            "org.jvnet.substance.skin.SubstanceRavenLookAndFeel",
            "org.jvnet.substance.skin.SubstanceRavenGraphiteLookAndFeel",
            "org.jvnet.substance.skin.SubstanceRavenGraphiteGlassLookAndFeel",
            "org.jvnet.substance.skin.SubstanceSaharaLookAndFeel"
    });

    final int ret = JOptionPane.showConfirmDialog(owner, box, "Select look and feel", JOptionPane.OK_CANCEL_OPTION);
    if (ret == JOptionPane.OK_OPTION)
      UIManager.setLookAndFeel((String) box.getSelectedItem());
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

  public static User getUser(final JComponent parent, final User defaultUser) throws UserCancelException {
    return LoginPanel.showLoginPanel(parent, defaultUser);
  }

  public static void handleException(final Throwable throwable, final Container dialogParent) {
    if (throwable instanceof UserCancelException)
      return;

    if (throwable instanceof UserException && throwable.getCause() instanceof UserAccessException)
      handleException(throwable.getCause(), dialogParent);
    else {
      if (!(throwable instanceof UserAccessException))
        throwable.printStackTrace();
      ExceptionDialog.showExceptionDialog(getParentWindow(dialogParent),
              Messages.get(Messages.EXCEPTION), throwable.getMessage(), throwable);
    }
  }

  public static JTable setTablePopup(final JTable table, final JPopupMenu popupMenu) {
    if (popupMenu == null)
      return table;

    table.add(popupMenu);
    final MouseListener listener = new MouseAdapter() {
      public void mouseClicked(final MouseEvent e) {
        if (e.isPopupTrigger() || e.getButton() == MouseEvent.BUTTON3) {//for linux :|
          if (e.getComponent() == table.getTableHeader())
            popupMenu.show(table.getTableHeader(), e.getX(), e.getY());
          else
            popupMenu.show(table, e.getX(), e.getY());
        }
      }
    };
    table.addMouseListener(listener);
    table.getTableHeader().addMouseListener(listener);
    table.addKeyListener(new KeyAdapter() {
      public void keyReleased(KeyEvent e) {//shift-space shows popup menu
        if (e.isShiftDown()) {
          if (e.getKeyChar() == ' ') {
            popupMenu.show(table, 100, table.getSelectedRow() * table.getRowHeight());
          }
        }
      }
    });

    return table;
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
      public void insertString(FilterBypass fb, int offset, String string, AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string.toUpperCase(), attr);
      }
      public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
        super.remove(fb, offset, length);
      }
      public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrs) throws BadLocationException {
        super.replace(fb, offset, length, text.toUpperCase(), attrs);
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

  public static void transferFocusOnEnter(final JTextField txtField) {
    txtField.addKeyListener(new KeyAdapter() {
      public void keyPressed(final KeyEvent evt) {
        if (evt.getKeyCode() == KeyEvent.VK_ENTER)
          txtField.transferFocus();
      }
    });
  }

  public static class DateInputPanel extends JPanel {

    public final JFormattedTextField inputField;
    public final AbstractDateMaskFormat maskFormat;

    public DateInputPanel(final JFormattedTextField inputField, final AbstractDateMaskFormat maskFormat,
                          final Action buttonAction, final State enabledState) {
      super(new BorderLayout());
      this.inputField = inputField;
      this.maskFormat = maskFormat;
      add(inputField, BorderLayout.CENTER);
      if (buttonAction != null) {
        final JButton btnChooser = new JButton(buttonAction);
        btnChooser.setPreferredSize(new Dimension(18,18));
        if (enabledState != null)
          linkToEnabledState(enabledState, btnChooser);
        add(btnChooser, BorderLayout.EAST);
      }
    }
  }
}
