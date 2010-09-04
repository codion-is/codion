/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.ui.images.NavigableImagePanel;
import org.jminor.common.ui.textfield.TextFieldPlus;

import com.toedter.calendar.JCalendar;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.plaf.TabbedPaneUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;
import javax.swing.text.MaskFormatter;
import javax.swing.text.PlainDocument;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

/**
 * A static utility class.
 */
public final class UiUtil {

  public static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  public static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  private static final Collection<String> IMAGE_FILE_TYPES = Arrays.asList("gif", "tif", "jpg", "jpeg", "png", "bmp");

  /**
   * A square dimension which sides are the same as the preferred height of a JTextField.
   * This comes in handy when f.ex. adding "..." lookup buttons next to text fields.
   */
  public static final Dimension DIMENSION_TEXT_FIELD_SQUARE =
          new Dimension(getPreferredTextFieldHeight(), getPreferredTextFieldHeight());

  private static final Map<RootPaneContainer, Integer> WAIT_CURSOR_REQUESTS = new HashMap<RootPaneContainer, Integer>();
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

  public static JTextField createMemoryUsageField(final int updateInterval) {
    final JTextField txt = new JTextField();
    txt.setColumns(8);
    txt.setEditable(false);
    txt.setHorizontalAlignment(JTextField.CENTER);
    new Timer(true).schedule(new TimerTask() {
      @Override
      public void run() {
        txt.setText(Util.getMemoryUsageString());
      }
    }, new Date(), updateInterval);

    return txt;
  }

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
    if (!Util.nullOrEmpty(startDir)) {
      fileChooser.setCurrentDirectory(new File(startDir));
    }
    if (dialogTitle != null) {
      fileChooser.setDialogTitle(dialogTitle);
    }
    final int ret = fileChooser.showOpenDialog(dialogParent);
    if (ret == JFileChooser.APPROVE_OPTION) {
      return fileChooser.getSelectedFile();
    }
    else {
      throw new CancelException();
    }
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
    if (!Util.nullOrEmpty(startDir)) {
      fileChooser.setCurrentDirectory(new File(startDir));
    }
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
      if (selectedFile.isDirectory()) {
        fileChooser.setCurrentDirectory(selectedFile);
      }
      else {
        fileChooser.setSelectedFile(selectedFile);
      }
      int option = fileChooser.showSaveDialog(dialogParent);
      if (option == JFileChooser.APPROVE_OPTION) {
        selectedFile = fileChooser.getSelectedFile();
        if (selectedFile.exists()) {
          option = JOptionPane.showConfirmDialog(dialogParent, Messages.get(Messages.OVERWRITE_FILE),
                  Messages.get(Messages.FILE_EXISTS), JOptionPane.YES_NO_CANCEL_OPTION);
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

  public static Date getDateFromUser(final Date startDate, final String message, final Container parent) {
    final Calendar cal = Calendar.getInstance();
    if (startDate != null) {
      cal.setTime(startDate);
    }

    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    final JCalendar calendar = new JCalendar(cal);

    showInDialog(getParentWindow(parent), calendar, true, message, true, true, null);

    return new Date(calendar.getCalendar().getTimeInMillis());
  }

  public static JFormattedTextField createFormattedField(final SimpleDateFormat maskFormat, final Object initialValue) {
    final JFormattedTextField txtField = createFormattedField(DateUtil.getDateMask(maskFormat));
    if (initialValue != null) {
      txtField.setText(maskFormat.format(initialValue));
    }

    return txtField;
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
          if (charsAsUpper) {
            ret = ret.toUpperCase(Locale.getDefault());
          }

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

  public static Action linkToEnabledState(final StateObserver enabledState, final Action action) {
    if (enabledState != null) {
      action.setEnabled(enabledState.isActive());
      enabledState.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          action.setEnabled(enabledState.isActive());
        }
      });
    }

    return action;
  }

  public static JComponent linkToEnabledState(final StateObserver enabledState, final JComponent component) {
    if (enabledState != null) {
      component.setEnabled(enabledState.isActive());
      enabledState.addListener(new ActionListener() {
        public void actionPerformed(final ActionEvent e) {
          component.setEnabled(enabledState.isActive());
        }
      });
    }

    return component;
  }

  public static JFrame createFrame(final Image icon) {
    final JFrame frame = new JFrame();
    if (icon != null) {
      frame.setIconImage(icon);
    }

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
      final Dimension newFrameSize = new Dimension((int) Math.min(frameSize.getWidth(), screenSize.getWidth()),
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
    if (minimumSize != null) {
      ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));
    }

    window.setSize(ratioSize);
  }

  public static Window getParentWindow(final Container container) {
    final Window window = getParentDialog(container);

    return window == null ? getParentFrame(container) : window;
  }

  public static JFrame getParentFrame(final Container container) {
    Container parent = container;
    while (!(parent instanceof JFrame) && (parent != null)) {
      parent = parent.getParent();
    }

    return (JFrame) parent;
  }

  public static JDialog getParentDialog(final Container container) {
    Container parent = container;
    while (!(parent instanceof JDialog) && (parent != null)) {
      parent = parent.getParent();
    }

    return (JDialog) parent;
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
      while (e.hasMoreElements()) {
        expandAll(tree, parent.pathByAddingChild(e.nextElement()), expand);
      }
    }
    // Expansion or collapse must be done bottom-up
    if (expand) {
      tree.expandPath(parent);
    }
    else {
      tree.collapsePath(parent);
    }
  }

  public static void setWaitCursor(final boolean on, final JComponent component) {
    RootPaneContainer root = getParentDialog(component);
    if (root == null) {
      root = getParentFrame(component);
    }
    if (root == null) {
      return;
    }

    synchronized (WAIT_CURSOR_REQUESTS) {
      if (!WAIT_CURSOR_REQUESTS.containsKey(root)) {
        WAIT_CURSOR_REQUESTS.put(root, 0);
      }

      int requests = WAIT_CURSOR_REQUESTS.get(root);
      if (on) {
        requests++;
      }
      else {
        requests--;
      }

      if ((requests == 1 && on) || (requests == 0 && !on)) {
        root.getRootPane().setCursor(on ? WAIT_CURSOR : DEFAULT_CURSOR);
      }
      if (requests == 0) {
        WAIT_CURSOR_REQUESTS.remove(root);
      }
      else {
        WAIT_CURSOR_REQUESTS.put(root, requests);
      }
    }
  }

  public static int getPreferredScrollBarWidth() {
    if (verticalScrollBar == null) {
      verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
    }

    return verticalScrollBar.getPreferredSize().width;
  }

  public static Dimension getPreferredTextFieldSize() {
    if (textField == null) {
      textField = new JTextField();
    }

    return textField.getPreferredSize();
  }

  public static int getPreferredTextFieldHeight() {
    if (textField == null) {
      textField = new JTextField();
    }

    return textField.getPreferredSize().height;
  }

  public static TabbedPaneUI getBorderlessTabbedPaneUI() {
    return new BasicTabbedPaneUI() {
      @Override
      protected Insets getContentBorderInsets(final int tabPlacement) {
        return new Insets(2,0,0,0);
      }
    };
  }

  public static JPanel northCenterPanel(final JComponent north, final JComponent center) {
    final JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.add(north, BorderLayout.NORTH);
    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  public static void showToolTip(final JComponent component) {
    final Action toolTipAction = component.getActionMap().get("postTip");
    if (toolTipAction != null) {
      toolTipAction.actionPerformed(new ActionEvent(component, ActionEvent.ACTION_PERFORMED, ""));
    }
  }

  /**
   * Makes <code>textField</code> convert all lower case input to upper case
   * @param textField the text field
   * @return the text field
   */
  public static JTextComponent makeUpperCase(final JTextComponent textField) {
    if (textField instanceof TextFieldPlus) {
      return makeUpperCase((TextFieldPlus) textField);
    }

    ((PlainDocument) textField.getDocument()).setDocumentFilter(new DocumentFilter() {
      @Override
      public void insertString(final FilterBypass fb, final int offset, final String string, final AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string == null ? null : string.toUpperCase(Locale.getDefault()), attr);
      }
      @Override
      public void remove(final FilterBypass fb, final int offset, final int length) throws BadLocationException {
        super.remove(fb, offset, length);
      }
      @Override
      public void replace(final FilterBypass fb, final int offset, final int length, final String text, final AttributeSet attrs) throws BadLocationException {
        super.replace(fb, offset, length, text == null ? null : text.toUpperCase(Locale.getDefault()), attrs);
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
      public void insertString(final FilterBypass fb, final int offset, final String string, final AttributeSet attr) throws BadLocationException {
        super.insertString(fb, offset, string.toLowerCase(), attr);
      }
      @Override
      public void remove(final FilterBypass fb, final int offset, final int length) throws BadLocationException {
        super.remove(fb, offset, length);
      }
      @Override
      public void replace(final FilterBypass fb, final int offset, final int length, final String text, final AttributeSet attrs) throws BadLocationException {
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
      public void keyPressed(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
          if (e.isShiftDown()) {
            component.transferFocusBackward();
          }
          else {
            component.transferFocus();
          }
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
        public void windowClosing(final WindowEvent e) {
          closeAction.actionPerformed(new ActionEvent(dialog, -1, null));
        }
      });
    }
    final String okCaption = okAction != null ? (String) okAction.getValue(Action.NAME) : Messages.get(Messages.OK);
    final Action ok = new AbstractAction(okCaption) {
      public void actionPerformed(final ActionEvent e) {
        if (okAction != null) {
          okAction.actionPerformed(e);
        }
        if (disposeOnOk) {
          dialog.setVisible(false);
          dialog.dispose();
        }
      }
    };
    addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, new DialogDisposeAction(dialog, "close"));
    if (includeButtonPanel) {
      final JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT,5,5));
      final JButton okButton = new JButton(ok);
      final Character okMnemonic;
      if (okAction != null && okAction.getValue(Action.MNEMONIC_KEY) != null) {
        okMnemonic = (Character) okAction.getValue(Action.MNEMONIC_KEY);
      }
      else {
        okMnemonic = Messages.get(Messages.OK_MNEMONIC).charAt(0);
      }

      if (okMnemonic != null) {
        okButton.setMnemonic(okMnemonic);
      }
      buttonPanel.add(okButton);
      dialog.getRootPane().setDefaultButton(okButton);
      dialog.add(buttonPanel, BorderLayout.SOUTH);
    }
    dialog.add(componentToShow, BorderLayout.CENTER);
    if (size == null) {
      dialog.pack();
    }
    else {
      dialog.setSize(size);
    }
    if (location == null) {
      dialog.setLocationRelativeTo(owner);
    }
    else {
      dialog.setLocation(location);
    }
    dialog.setModal(modal);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  public static JDialog showInDialog(final Container owner, final JComponent componentToShow, final boolean modal,
                                     final String title, final Dimension size, final JButton defaultButton,
                                     final EventObserver closeEvent) {
    final JDialog dialog = new JDialog(getParentWindow(owner), title);
    dialog.setLayout(new BorderLayout());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    if (defaultButton != null) {
      dialog.getRootPane().setDefaultButton(defaultButton);
    }

    final Action disposeActionListener = new DialogDisposeAction(dialog, null);
    addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, disposeActionListener);
    if (closeEvent != null) {
      closeEvent.addListener(disposeActionListener);
    }

    dialog.add(componentToShow, BorderLayout.CENTER);
    if (size == null) {
      dialog.pack();
    }
    else {
      dialog.setSize(size);
    }

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
    textComponent.setTransferHandler(new DnDTransferHandler(textComponent));
  }

  public static void addKeyEvent(final JComponent component, final int keyEvent, final Action action) {
    addKeyEvent(component, keyEvent, 0, action);
  }

  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers,
                                 final Action action) {
    addKeyEvent(component, keyEvent, modifiers, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, action);
  }

  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers, final int condition,
                                 final Action action) {
    addKeyEvent(component, keyEvent, modifiers, condition, false, action);
  }

  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers, final int condition,
                                 final boolean onKeyRelease, final Action action) {
    final Object name = action.getValue(Action.NAME);
    component.getInputMap(condition).put(KeyStroke.getKeyStroke(keyEvent, modifiers, onKeyRelease), name);
    component.getActionMap().put(name, action);
  }

  public static void addLookupDialog(final JTextField txtField, final ValueCollectionProvider valueListProvider) {
    addKeyEvent(txtField, KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK, 0, new AbstractAction("valueLookup") {
      public void actionPerformed(final ActionEvent e) {
        try {
          final Object value = selectValue(txtField, valueListProvider.getValues());
          if (value != null) {
            txtField.setText(value.toString());
          }
        }
        catch (Exception ex) {
          throw new RuntimeException(ex);
        }
      }
    });
  }

  public static Object selectValue(final JComponent dialogOwner, final Collection<?> values) {
    return selectValue(dialogOwner, values, Messages.get(Messages.SELECT_VALUE));
  }

  public static Object selectValue(final JComponent dialogOwner, final Collection<?> values, final String dialogTitle) {
    final DefaultListModel listModel = new DefaultListModel();
    for (final Object value : values) {
      listModel.addElement(value);
    }

    final JList list = new JList(values.toArray());
    final Window owner = getParentWindow(dialogOwner);
    final JDialog dialog = new JDialog(owner, dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    final Action okAction = new DialogDisposeAction(dialog, Messages.get(Messages.OK));
    final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
      public void actionPerformed(final ActionEvent e) {
        list.clearSelection();
        dialog.dispose();
      }
    };
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final JButton btnOk  = new JButton(okAction);
    final JButton btnCancel = new JButton(cancelAction);
    final String cancelMnemonic = Messages.get(Messages.CANCEL_MNEMONIC);
    final String okMnemonic = Messages.get(Messages.OK_MNEMONIC);
    btnOk.setMnemonic(okMnemonic.charAt(0));
    btnCancel.setMnemonic(cancelMnemonic.charAt(0));
    addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, cancelAction);
    list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          okAction.actionPerformed(null);
        }
      }
    });
    dialog.setLayout(new BorderLayout());
    final JScrollPane scroller = new JScrollPane(list);
    //scroller.setPreferredSize(new Dimension(400, 400)); //todo dialog can get extremely wide, fix please!
    dialog.add(scroller, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(new GridLayout(1,2,5,5));
    buttonPanel.add(btnOk);
    buttonPanel.add(btnCancel);
    final JPanel buttonBasePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(buttonPanel);
    dialog.getRootPane().setDefaultButton(btnOk);
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return list.getSelectedValue();
  }

  public static Action getBrowseAction(final JTextField txtFilename) {
    return new AbstractAction("...") {
      public void actionPerformed(final ActionEvent e) {
        try {
          final File file = selectFile(txtFilename, getStartDir(txtFilename.getText()));
          txtFilename.setText(file.getAbsolutePath());
        }
        catch (CancelException ex) {/**/}
      }
    };
  }

  public static String getStartDir(final String text) {
    if (Util.nullOrEmpty(text)) {
      return null;
    }
    try {
      return new File(text).getParentFile().getPath();
    }
    catch (Exception e) {
      return null;
    }
  }

  /**
   * @param imagePath the path to the image to show
   * @param dialogParent the component to use as dialog parent
   * @throws IOException in case of an IO exception
   */
  public static void showImage(final String imagePath, final JComponent dialogParent) throws IOException {
    showImage(imagePath, dialogParent, IMAGE_FILE_TYPES);
  }

  /**
   * @param imagePath the path to the image to show, if the file has a file type suffix it
   * is checked against the <code>acceptedFileTypes</code> collection.
   * @param dialogParent the component to use as dialog parent
   * @param acceptedFileTypes a collection of lower case file type suffixes, "gif", "jpeg"...
   * @throws IOException in case of an IO exception
   * @throws IllegalArgumentException in case the file type is not accepted
   */
  public static void showImage(final String imagePath, final JComponent dialogParent,
                               final Collection<String> acceptedFileTypes) throws IOException {
    Util.rejectNullValue(imagePath, "imagePath");
    if (imagePath.isEmpty()) {
      return;
    }

    final int lastDotIndex = imagePath.lastIndexOf('.');
    if (lastDotIndex != -1) {//if the type is specified check it
      final String type = imagePath.substring(lastDotIndex + 1, imagePath.length()).toLowerCase();
      if (!acceptedFileTypes.contains(type)) {
        throw new IllegalArgumentException(Messages.get(Messages.UNKNOWN_FILE_TYPE) + ": " + type);
      }
    }
    final NavigableImagePanel imagePanel = new NavigableImagePanel();
    final File imageFile = new File(imagePath);
    if (imageFile.exists()) {
      final BufferedImage bufferedImage = ImageIO.read(imageFile);
      imagePanel.setImage(bufferedImage);
      final JDialog dialog = initializeDialog(dialogParent, imagePanel);

      dialog.setTitle(imageFile.getName());

      if (!dialog.isShowing()) {
        dialog.setVisible(true);
      }
    }
    else {
      throw new RuntimeException(Messages.get(Messages.FILE_NOT_FOUND) + ": " + imagePath);
    }
  }

  private static JDialog initializeDialog(final JComponent parent, final NavigableImagePanel panel) {
    final JDialog dialog =  new JDialog(getParentWindow(parent));
    dialog.setLayout(new BorderLayout());
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, new DialogDisposeAction(dialog, "close"));
    dialog.add(panel, BorderLayout.CENTER);
    dialog.setSize(getScreenSizeRatio(0.5));
    dialog.setLocationRelativeTo(parent);
    dialog.setModal(false);

    return dialog;
  }

  public static final class DialogDisposeAction extends AbstractAction {
    private final JDialog dialog;

    public DialogDisposeAction(final JDialog dialog, final String name) {
      super(name);
      this.dialog = dialog;
    }

    public void actionPerformed(final ActionEvent e) {
      dialog.dispose();
    }
  }

  private static final class DnDTransferHandler extends TransferHandler {

    private final JTextComponent textComponent;

    private DnDTransferHandler(final JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public boolean canImport(final TransferSupport support) {
      try {
        final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
        for (final DataFlavor flavor : support.getDataFlavors()) {
          if (flavor.isFlavorJavaFileListType() || flavor.equals(nixFileDataFlavor)) {
            return true;
          }
        }

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
      else {
        return false;
      }
    }

    @SuppressWarnings({"unchecked"})
    private static String getFileDataFlavor(final TransferHandler.TransferSupport support) {
      try {
        for (final DataFlavor flavor : support.getDataFlavors()) {
          if (flavor.isFlavorJavaFileListType()) {
            final List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

            return !files.isEmpty() ? files.get(0).getAbsolutePath() : null;
          }
        }
        //the code below is for handling unix/linux
        final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
        final String data = (String) support.getTransferable().getTransferData(nixFileDataFlavor);
        for (final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
          final String token = st.nextToken().trim();
          if (token.startsWith("#") || token.isEmpty()) {// comment line, by RFC 2483
            continue;
          }

          return new File(new URI(token)).getAbsolutePath();
        }
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }

      return null;
    }
  }
}
