/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Event;
import org.jminor.common.model.EventInfoListener;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueCollectionProvider;
import org.jminor.common.ui.images.NavigableImagePanel;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.common.ui.textfield.SizedDocument;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.RootPaneContainer;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.WindowConstants;
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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.text.DateFormat;
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
import java.util.Vector;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A static utility class.
 */
public final class UiUtil {

  /**
   * A wait cursor
   */
  public static final Cursor WAIT_CURSOR = new Cursor(Cursor.WAIT_CURSOR);
  /**
   * The default cursor
   */
  public static final Cursor DEFAULT_CURSOR = new Cursor(Cursor.DEFAULT_CURSOR);

  private static final Collection<String> IMAGE_FILE_TYPES = Arrays.asList("gif", "tif", "jpg", "jpeg", "png", "bmp");

  /**
   * A square dimension which sides are the same as the preferred height of a JTextField.
   * This comes in handy when f.ex. adding "..." lookup buttons next to text fields.
   */
  public static final Dimension DIMENSION_TEXT_FIELD_SQUARE = new Dimension(getPreferredTextFieldHeight(), getPreferredTextFieldHeight());
  private static final int DEFAULT_HOR_VERT_GAP = 5;
  private static final int DEFAULT_DATE_FIELD_COLUMNS = 12;
  private static final int MAX_SELECT_VALUE_DIALOG_WIDTH = 500;
  private static final int DEFAULT_PROGRESS_BAR_WIDTH = 400;
  private static final double DEFAULT_IMAGE_PANEL_SCREEN_SIZE_RATIO = 0.5;
  private static final Map<RootPaneContainer, Integer> WAIT_CURSOR_REQUESTS = new HashMap<>();
  /**
   * Caching the file chooser since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  private static JFileChooser fileChooser;
  /**
   * A text field used by getPreferredTextFieldSize and getPreferredTextFieldHeight
   */
  private static JTextField textField;
  private static JScrollBar verticalScrollBar;

  private static int horizontalVerticalComponentGap = DEFAULT_HOR_VERT_GAP;

  private UiUtil() {}

  /**
   * Sets the default horizontal and vertical component gap, used by the layout factory methods, by default this is 5
   * @param gap the default horizontal and vertical gap
   * @see #createBorderLayout()
   * @see #createFlowLayout(int)
   * @see #createGridLayout(int, int)
   * @see #createFlexibleGridLayout(int, int, boolean, boolean)
   */
  public static void setHorizontalVerticalComponentGap(final int gap) {
    horizontalVerticalComponentGap = gap;
  }

  /**
   * Creates a BorderLayout using the default vertical and horizontal gap value
   * @return a BorderLayout
   * @see #setHorizontalVerticalComponentGap(int)
   */
  public static BorderLayout createBorderLayout() {
    return new BorderLayout(horizontalVerticalComponentGap, horizontalVerticalComponentGap);
  }

  /**
   * Creates a FlowLayout using the default vertical and horizontal gap value
   * @param alignment the alignment
   * @return a FlowLayout
   * @see #setHorizontalVerticalComponentGap(int)
   */
  public static FlowLayout createFlowLayout(final int alignment) {
    return new FlowLayout(alignment, horizontalVerticalComponentGap, horizontalVerticalComponentGap);
  }

  /**
   * Creates a GridLayout using the default vertical and horizontal gap value
   * @param rows the number of rows
   * @param columns the number of columns
   * @return a GridLayout
   * @see #setHorizontalVerticalComponentGap(int)
   */
  public static GridLayout createGridLayout(final int rows, final int columns) {
    return new GridLayout(rows, columns, horizontalVerticalComponentGap, horizontalVerticalComponentGap);
  }

  /**
   * Creates a FlexibleGridLayout using the default vertical and horizontal gap value
   * @param rows the number of rows
   * @param columns the number of columns
   * @param fixRowHeights if true then the height of the rows is fixed as the largest value
   * @param fixColumnWidths if true then the width of the columns is fixed as the largest value
   * @return a FlexibleGridLayout
   * @see #setHorizontalVerticalComponentGap(int)
   */
  public static FlexibleGridLayout createFlexibleGridLayout(final int rows, final int columns,
                                                            final boolean fixRowHeights, final boolean fixColumnWidths) {
    return new FlexibleGridLayout(rows, columns, horizontalVerticalComponentGap,
            horizontalVerticalComponentGap, fixRowHeights, fixColumnWidths);
  }

  /**
   * Creates a text field containing information about the memory usage
   * @param updateIntervalMilliseconds the interval between updating the memory usage info
   * @return the text field
   */
  public static JTextField createMemoryUsageField(final int updateIntervalMilliseconds) {
    final JTextField txt = new JTextField(8);
    txt.setEditable(false);
    txt.setHorizontalAlignment(JTextField.CENTER);
    Executors.newSingleThreadScheduledExecutor(new Util.DaemonThreadFactory()).scheduleWithFixedDelay(new Runnable() {
      @Override
      public void run() {
        txt.setText(Util.getMemoryUsageString());
      }
    }, 0, updateIntervalMilliseconds, TimeUnit.MILLISECONDS);

    return txt;
  }

  /**
   * Displays a file selection dialog for selecting an existing directory
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectDirectory(final JComponent dialogParent, final String startDir) {
    return selectDirectory(dialogParent, startDir, Messages.get(Messages.SELECT_DIRECTORY));
  }

  /**
   * Displays a file selection dialog for selecting an existing directory
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @param dialogTitle the dialog title
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectDirectory(final JComponent dialogParent, final String startDir, final String dialogTitle) {
    return selectFileOrDirectory(dialogParent, startDir, false, dialogTitle);
  }

  /**
   * Displays a file selection dialog for selecting an existing file
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectFile(final JComponent dialogParent, final String startDir) {
    return selectFile(dialogParent, startDir, Messages.get(Messages.SELECT_FILE));
  }

  /**
   * Displays a file selection dialog for selecting an existing file
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @param dialogTitle the dialog title
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectFile(final JComponent dialogParent, final String startDir, final String dialogTitle) {
    return selectFileOrDirectory(dialogParent, startDir, true, dialogTitle);
  }

  /**
   * Displays a file selection dialog for selecting an existing file or directory
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @param files if true then files are displayed, otherwise only directories
   * @param dialogTitle the dialog title
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectFileOrDirectory(final JComponent dialogParent, final String startDir, final boolean files,
                                           final String dialogTitle) {
    return selectFilesOrDirectories(dialogParent, startDir, files, false, dialogTitle).get(0);
  }

  /**
   * Displays a file selection dialog for selecting files or directories
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @param files if true then files are displayed, otherwise only directories
   * @param multiSelection if true then the dialog will allow selection of multiple items
   * @param dialogTitle the dialog title
   * @return a List containing the selected files, contains at least one file
   * @throws CancelException in case the user cancels or no files are selected
   */
  public static List<File> selectFilesOrDirectories(final JComponent dialogParent, final String startDir,
                                                    final boolean files, final boolean multiSelection,
                                                    final String dialogTitle) {
    if (fileChooser == null) {
      try {
        setWaitCursor(true, dialogParent);
        fileChooser = new JFileChooser(new File(startDir == null ? System.getProperty("user.home") : startDir));
      }
      finally {
        setWaitCursor(false, dialogParent);
      }
    }
    if (files) {
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }
    else {
      fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }
    fileChooser.removeChoosableFileFilter(fileChooser.getFileFilter());
    fileChooser.setMultiSelectionEnabled(multiSelection);
    if (!Util.nullOrEmpty(startDir)) {
      fileChooser.setCurrentDirectory(new File(startDir));
    }
    if (dialogTitle != null) {
      fileChooser.setDialogTitle(dialogTitle);
    }
    final int option = fileChooser.showOpenDialog(dialogParent);
    if (option == JFileChooser.APPROVE_OPTION) {
      final List<File> selectedFiles;
      if (multiSelection) {
        selectedFiles = Arrays.asList(fileChooser.getSelectedFiles());
      }
      else {
        selectedFiles = Arrays.asList(fileChooser.getSelectedFile());
      }
      if (!selectedFiles.isEmpty()) {
        return selectedFiles;
      }
    }

    throw new CancelException();
  }

  /**
   * Displays a save file dialog for creating a new file
   * @param dialogParent the dialog parent
   * @param startDir the start dir, user.dir if not specified
   * @param defaultFileName the default file name to suggest
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File chooseFileToSave(final JComponent dialogParent, final String startDir, final String defaultFileName) {
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
    final File startDirectory;
    if (!Util.nullOrEmpty(startDir)) {
      startDirectory = new File(startDir);
    }
    else {
      startDirectory = fileChooser.getCurrentDirectory();
    }
    File selectedFile = new File(startDirectory.getAbsolutePath() + (defaultFileName != null ? File.separator + defaultFileName : ""));
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

  /**
   * Retrieves a date from the user. If JCalendar is available on the classpath a JCalendar panel is shown,
   * otherwise a simple formatted text field is used
   * @param startDate the starting date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a Date from the user with all time base fields set to zero, null if the action was cancelled
   */
  public static Date getDateFromUser(final Date startDate, final String message, final Container parent) {
    final String jCalendarClassName = "com.toedter.calendar.JCalendar";
    if (!Util.onClasspath(jCalendarClassName)) {
      return getDateFromUserAsText(startDate, message, parent);
    }

    try {
      final Calendar cal = Calendar.getInstance();
      if (startDate != null) {
        cal.setTime(startDate);
      }
      cal.setTime(DateUtil.floorDate(cal.getTime()));

      final Class<?> jCalendarClass = Class.forName(jCalendarClassName);
      final Method getCalendar = jCalendarClass.getMethod("getCalendar");
      final Constructor constructor = jCalendarClass.getConstructor(Calendar.class);
      final JPanel calendarPanel = (JPanel) constructor.newInstance(cal);
      final JPanel datePanel = new JPanel(createBorderLayout());
      datePanel.add(calendarPanel, BorderLayout.NORTH);

      final Event closeEvent = Events.event();
      final State cancel = States.state();
      final Calendar returnTime = Calendar.getInstance();
      returnTime.setTime(cal.getTime());
      final JButton okBtn = new JButton(new AbstractAction(Messages.get(Messages.OK)) {
        @Override
        public void actionPerformed(final ActionEvent e) {
          try {
            returnTime.setTimeInMillis(((Calendar) getCalendar.invoke(calendarPanel)).getTimeInMillis());
            closeEvent.fire();
          }
          catch (Exception ex) {
            throw new RuntimeException("Exception while using JCalendar", ex);
          }
        }
      });
      final Action cancelAction = new AbstractAction(Messages.get(Messages.CANCEL)) {
        @Override
        public void actionPerformed(final ActionEvent e) {
          cancel.setActive(true);
          closeEvent.fire();
        }
      };
      final JButton cancelBtn = new JButton(cancelAction);
      final JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
      buttonPanel.add(okBtn);
      buttonPanel.add(cancelBtn);

      datePanel.add(buttonPanel, BorderLayout.SOUTH);

      addKeyEvent(datePanel, KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelAction);
      displayInDialog(parent, datePanel, message, true, okBtn, closeEvent, true, null);

      return cancel.isActive() ? null : new Date(returnTime.getTimeInMillis());
    }
    catch (Exception e) {
      throw new RuntimeException("Exception while using JCalendar", e);
    }
  }

  /**
   * Retrieves a date from the user using a simple formatted text field
   * @param startDate the initial date, if null the current date is used
   * @param message the message to display as dialog title
   * @param parent the dialog parent
   * @return a Date from the user
   */
  public static Date getDateFromUserAsText(final Date startDate, final String message, final Container parent) {
    return getDateFromUserAsText(startDate, message, (SimpleDateFormat) DateFormat.getDateInstance(DateFormat.SHORT), parent);
  }

  /**
   * Retrieves a date from the user using a simple formatted text field
   * @param startDate the initial date, if null the current date is used
   * @param message the message to display as dialog title
   * @param inputDateFormat the date format to use
   * @param parent the dialog parent
   * @return a Date from the user
   */
  public static Date getDateFromUserAsText(final Date startDate, final String message, final SimpleDateFormat inputDateFormat,
                                           final Container parent) {
    try {
      final MaskFormatter formatter = new MaskFormatter(DateUtil.getDateMask(inputDateFormat));
      formatter.setPlaceholderCharacter('_');
      final JFormattedTextField txtField = new JFormattedTextField(inputDateFormat);
      txtField.setColumns(DEFAULT_DATE_FIELD_COLUMNS);
      txtField.setValue(startDate);

      final JPanel datePanel = new JPanel(createGridLayout(1, 1));
      datePanel.add(txtField);

      displayInDialog(parent, datePanel, message);

      return inputDateFormat.parse(txtField.getText());
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Creates a formatted text field using the given format
   * @param maskFormat the format
   * @param initialValue the initial value
   * @return the text field
   */
  public static JFormattedTextField createFormattedDateField(final SimpleDateFormat maskFormat, final Date initialValue) {
    final JFormattedTextField txtField = createFormattedField(DateUtil.getDateMask(maskFormat));
    if (initialValue != null) {
      txtField.setText(maskFormat.format(initialValue));
    }

    return txtField;
  }

  /**
   * Creates a JFormattedTextField with the given mask, using '_' as a placeholder character, disallowing invalid values,
   * with JFormattedTextField.COMMIT as focus lost behaviour. By default the value contains the literal characters.
   * @param mask the format mask
   * @return a JFormattedTextField
   */
  public static JFormattedTextField createFormattedField(final String mask) {
    return createFormattedField(mask, true);
  }

  /**
   * Creates a JFormattedTextField with the given mask, using '_' as a placeholder character, disallowing invalid values,
   * with JFormattedTextField.COMMIT as focus lost behaviour.
   * @param mask the format mask
   * @param valueContainsLiteralCharacter if true, the value will also contain the literal characters in mask
   * @return a JFormattedTextField
   */
  public static JFormattedTextField createFormattedField(final String mask, final boolean valueContainsLiteralCharacter) {
    return createFormattedField(mask, valueContainsLiteralCharacter, false);
  }

  /**
   * Creates a JFormattedTextField with the given mask, using '_' as a placeholder character, disallowing invalid values,
   * with JFormattedTextField.COMMIT as focus lost behaviour.
   * @param mask the format mask
   * @param valueContainsLiteralCharacter if true, the value will also contain the literal characters in mask
   * @param charsAsUpper if true then the field will automatically convert characters to upper case
   * @return a JFormattedTextField
   */
  public static JFormattedTextField createFormattedField(final String mask, final boolean valueContainsLiteralCharacter,
                                                         final boolean charsAsUpper) {
    try {
      final JFormattedTextField formattedTextField =
              new JFormattedTextField(new FieldFormatter(mask, charsAsUpper, valueContainsLiteralCharacter));
      formattedTextField.setFocusLostBehavior(JFormattedTextField.COMMIT);
      moveCaretToStartOnFocusGained(formattedTextField);

      return formattedTextField;
    }
    catch (ParseException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Links the given action to the given StateObserver, so that the action is enabled
   * only when the observed state is active
   * @param enabledState the StateObserver with which to link the action, if null then nothing is done
   * @param action the action, if null then nothing is done
   * @return the linked action
   */
  public static Action linkToEnabledState(final StateObserver enabledState, final Action action) {
    if (enabledState != null && action != null) {
      action.setEnabled(enabledState.isActive());
      enabledState.addListener(new EventListener() {
        @Override
        public void eventOccurred() {
          action.setEnabled(enabledState.isActive());
        }
      });
    }

    return action;
  }

  /**
   * Links the given components to the given StateObserver, so that each component is enabled and focusable
   * only when the observed state is active
   * @param enabledState the StateObserver with which to link the components, if null then nothing is done
   * @param components the components, if null nothing is done
   */
  public static void linkToEnabledState(final StateObserver enabledState, final JComponent... components) {
    linkToEnabledState(enabledState, true, components);
  }

  /**
   * Links the given components to the given StateObserver, so that each component is enabled only when the observed state is active
   * @param enabledState the StateObserver with which to link the components, if null then nothing is done
   * @param includeFocusable if true then the focusable attribute is set as well as the enabled attribute
   * @param components the components, if null nothing is done
   */
  public static void linkToEnabledState(final StateObserver enabledState, final boolean includeFocusable, final JComponent... components) {
    if (enabledState != null && components != null) {
      for (final JComponent component : components) {
        component.setEnabled(enabledState.isActive());
        if (includeFocusable) {
          component.setFocusable(enabledState.isActive());
        }
        enabledState.addListener(new EventListener() {
          @Override
          public void eventOccurred() {
            component.setEnabled(enabledState.isActive());
            if (includeFocusable) {
              component.setFocusable(enabledState.isActive());
            }
          }
        });
      }
    }
  }

  /**
   * @param ratio a ratio, 0.0 - 1.0
   * @return a Dimension which is the size of the available screen times ratio
   */
  public static Dimension getScreenSizeRatio(final double ratio) {
    final Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();

    return new Dimension((int) (screen.getWidth() * ratio), (int) (screen.getHeight() * ratio));
  }

  /**
   * Resizes the given window so that if fits within the current screen bounds,
   * if the window already fits then calling this method has no effect
   * @param window the window to resize
   */
  public static void setSizeWithinScreenBounds(final Window window) {
    final Dimension screenSize = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    final Dimension frameSize = window.getSize();
    if (frameSize.getHeight() > screenSize.getHeight() || frameSize.getWidth() > screenSize.getWidth()) {
      final Dimension newFrameSize = new Dimension((int) Math.min(frameSize.getWidth(), screenSize.getWidth()),
              (int) Math.min(frameSize.getHeight(), screenSize.getHeight()));
      window.setSize(newFrameSize);
    }
  }

  /**
   * Resizes the given window so that it is <code>screenSizeRatio</code> percent of the current screen size
   * @param window the window to resize
   * @param screenSizeRatio the screen size ratio
   */
  public static void resizeWindow(final Window window, final double screenSizeRatio) {
    resizeWindow(window, screenSizeRatio, null);
  }

  /**
   * Resizes the given window so that it is <code>screenSizeRatio</code> percent of the current screen size,
   * within the given minimum size
   * @param window the window to resize
   * @param screenSizeRatio the screen size ratio
   * @param minimumSize a minimum size
   */
  public static void resizeWindow(final Window window, final double screenSizeRatio,
                                  final Dimension minimumSize) {
    final Dimension ratioSize = getScreenSizeRatio(screenSizeRatio);
    if (minimumSize != null) {
      ratioSize.setSize(Math.max(minimumSize.width, ratioSize.width), Math.max(minimumSize.height, ratioSize.height));
    }

    window.setSize(ratioSize);
  }

  /**
   * @param component the component
   * @return the parent Window of the given component, null if none exists
   */
  public static Window getParentWindow(final Component component) {
    final Window window = getParentDialog(component);

    return window == null ? getParentFrame(component) : window;
  }

  /**
   * @param component the component
   * @return the parent JFrame of the given component, null if none exists
   */
  public static JFrame getParentFrame(final Component component) {
    return getParentOfType(component, JFrame.class);
  }

  /**
   * @param component the component
   * @return the parent JDialog of the given component, null if none exists
   */
  public static JDialog getParentDialog(final Component component) {
    return getParentOfType(component, JDialog.class);
  }

  /**
   * Searches the parent component hierarchy of the given component for
   * an ancestor of the given type
   * @param component the component
   * @param clazz the class of the parent to find
   * @return the parent of the given component of the given type, null if none is found
   */
  public static <T> T getParentOfType(final Component component, final Class<T> clazz) {
    return (T) SwingUtilities.getAncestorOfClass(clazz, component);
  }

  /**
   * Centers the given window on the screen
   * @param window the window to center on screen
   */
  public static void centerWindow(final Window window) {
    final Dimension size = window.getSize();
    final Dimension screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().getSize();
    window.setLocation((int) (screen.getWidth() - size.getWidth()) / 2,
            (int) (screen.getHeight() - size.getHeight()) / 2);
  }

  /**
   * Expands or collapses all the paths from a parent in the given tree
   * @param tree the tree
   * @param parent the parent from which to exapand/collapse
   * @param expand if true then the tree is expanded, collapsed otherwise
   */
  public static void expandAll(final JTree tree, final TreePath parent, final boolean expand) {
    // Traverse children
    final TreeNode node = (TreeNode) parent.getLastPathComponent();
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

  /**
   * Adds or subtracts a wait cursor request for the parent root pane of the given component,
   * the wait cursor is activated once a request is made, but only deactivated once all such
   * requests have been retracted. Best used in try/finally block combinations.
   * <pre>
   try {
   UiUtil.setWaitCursor(true, dialogParent);
   doSomething();
   }
   finally {
   UiUtil.setWaitCursor(false, dialogParent);
   }
   * </pre>
   * @param on if on, then the wait cursor is activated, otherwise it is deactivated
   * @param component the component
   */
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

  /**
   * @return the preferred width of a JScrollBar
   */
  public static int getPreferredScrollBarWidth() {
    if (verticalScrollBar == null) {
      verticalScrollBar = new JScrollBar(JScrollBar.VERTICAL);
    }

    return verticalScrollBar.getPreferredSize().width;
  }

  /**
   * @return the preferred size of a JTextField
   */
  public static Dimension getPreferredTextFieldSize() {
    if (textField == null) {
      textField = new JTextField();
    }

    return textField.getPreferredSize();
  }

  /**
   * @return the preferred height of a JTextField
   */
  public static int getPreferredTextFieldHeight() {
    if (textField == null) {
      textField = new JTextField();
    }

    return textField.getPreferredSize().height;
  }

  /**
   * Creates a TabbedPaneUI with content border insets only at the top, disregarding the tab placement
   * @return a tabbed pane ui without borders
   */
  public static TabbedPaneUI getBorderlessTabbedPaneUI() {
    return new BasicTabbedPaneUI() {
      @Override
      protected Insets getContentBorderInsets(final int tabPlacement) {
        return new Insets(2,0,0,0);
      }
    };
  }

  /**
   * Creates a JPanel, using a BorderLayout with a five pixel hgap and vgap, adding
   * the given components to their respective positions.
   * @param north the panel to display in the BorderLayout.NORTH position
   * @param center the panel to display in the BorderLayout.CENTER position
   * @return a panel displaying the given components in the NORTH an CENTER positions in a BorderLayout
   */
  public static JPanel northCenterPanel(final JComponent north, final JComponent center) {
    final JPanel panel = new JPanel(createBorderLayout());
    panel.add(north, BorderLayout.NORTH);
    panel.add(center, BorderLayout.CENTER);

    return panel;
  }

  /**
   * Makes <code>textField</code> convert all lower case input to upper case
   * @param textField the text field
   * @return the text field
   */
  public static JTextComponent makeUpperCase(final JTextComponent textField) {
    if (textField.getDocument() instanceof SizedDocument) {
      ((SizedDocument) textField.getDocument()).setUpperCase(true);
    }
    else {
      ((PlainDocument) textField.getDocument()).setDocumentFilter(new CaseDocumentFilter(true));
    }

    return textField;
  }

  /**
   * Makes <code>textField</code> convert all upper case input to lower case
   * @param textField the text field
   * @return the text field
   */
  public static JTextComponent makeLowerCase(final JTextComponent textField) {
    if (textField.getDocument() instanceof SizedDocument) {
      ((SizedDocument) textField.getDocument()).setLowerCase(true);
    }
    else {
      ((PlainDocument) textField.getDocument()).setDocumentFilter(new CaseDocumentFilter(false));
    }

    return textField;
  }

  /**
   * Adds a key event to the component which transfers focus
   * on enter, and backwards if shift is down
   * @param component the component
   * @see #removeTransferFocusOnEnter(javax.swing.JComponent)
   */
  public static void transferFocusOnEnter(final JComponent component) {
    addKeyEvent(component, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false, new TransferFocusAction(component));
    addKeyEvent(component, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false, new TransferFocusAction(component, true));
  }

  /**
   * Removes the transfer focus action added via {@link #transferFocusOnEnter(javax.swing.JComponent)}
   * @param component the component
   */
  public static void removeTransferFocusOnEnter(final JComponent component) {
    addKeyEvent(component, KeyEvent.VK_ENTER, 0, JComponent.WHEN_FOCUSED, false, null);
    addKeyEvent(component, KeyEvent.VK_ENTER, KeyEvent.SHIFT_DOWN_MASK, JComponent.WHEN_FOCUSED, false, null);
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
        textComponent.select(0, 0);
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

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title) {
    return displayInDialog(owner, component, title, true);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true then the dialog is modal
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final boolean modal) {
    return displayInDialog(owner, component, title, modal, null, null, true, null);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param closeEvent the dialog will be disposed of when this event occurs
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final EventObserver closeEvent) {
    return displayInDialog(owner, component, title, true, closeEvent);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true then the dialog is modal
   * @param closeEvent the dialog will be disposed of when this event occurs
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final boolean modal, final EventObserver closeEvent) {
    return displayInDialog(owner, component, title, modal, null, closeEvent, true, null);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param onClosedAction this action will be registered as a windowClosed action for the dialog
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final Action onClosedAction) {
    return displayInDialog(owner, component, title, true, onClosedAction);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true then the dialog is modal
   * @param onClosedAction this action will be registered as a windowClosed action for the dialog
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final boolean modal, final Action onClosedAction) {
    return displayInDialog(owner, component, title, modal, true, onClosedAction);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true then the dialog is modal
   * @param disposeOnEscape if true then the dialog is disposed when the ESC button is pressed
   * @param onClosedAction this action will be registered as a windowClosed action for the dialog
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final boolean modal, final boolean disposeOnEscape, final Action onClosedAction) {
    return displayInDialog(owner, component, title, modal, null, null, disposeOnEscape, onClosedAction);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true then the dialog is modal
   * @param defaultButton the the default dialog button
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title, final boolean modal,
                                        final JButton defaultButton, final boolean disposeOnEscape) {
    return displayInDialog(owner, component, title, modal, defaultButton, null, disposeOnEscape, null);
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true then the dialog is modal
   * @param defaultButton the the default dialog button
   * @param closeEvent the dialog will be closed and disposed of when and only when this event occurs
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title, final boolean modal,
                                        final JButton defaultButton, final EventObserver closeEvent) {
    return displayInDialog(owner, component, title, modal, defaultButton, closeEvent, false, null);
  }

  /**
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param closeObserver the dialog will be closed when this observer notifies
   * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
   * the dialog is closed, using the State info object to signal confirmation, the dialog
   * will only be closed if that state is active after a call to {@link EventInfoListener#eventOccurred(Object)}
   * @return the dialog
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final EventObserver closeObserver, final EventInfoListener<State> confirmCloseListener) {
    return displayInDialog(owner, component, title, true, closeObserver, confirmCloseListener);
  }

  /**
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true the dialog will be modal
   * @param closeObserver the dialog will be closed when this observer notifies
   * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
   * the dialog is closed, using the State info object to signal confirmation, the dialog
   * will only be closed if that state is active after a call to {@link EventInfoListener#eventOccurred(Object)}
   * @return the dialog
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final boolean modal, final EventObserver closeObserver,
                                        final EventInfoListener<State> confirmCloseListener) {
    final JDialog dialog = new JDialog(getParentWindow(owner), title);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        closeIfConfirmed(confirmCloseListener, dialog);
      }
    });
    if (closeObserver != null) {
      closeObserver.addListener(new EventListener() {
        @Override
        public void eventOccurred() {
          closeIfConfirmed(confirmCloseListener, dialog);
        }
      });
    }
    dialog.setLayout(createBorderLayout());
    dialog.add(component, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(modal);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  private static void closeIfConfirmed(final EventInfoListener<State> confirmCloseListener, final JDialog dialog) {
    if (confirmCloseListener == null) {
      dialog.dispose();
    }
    else {
      final State confirmClose = States.state();
      confirmCloseListener.eventOccurred(confirmClose);
      if (confirmClose.isActive()) {
        dialog.dispose();
      }
    }
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
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap, JComponent.WHEN_FOCUSED as condition, 0 as modifier and true for onKeyRelease.
   * @param component the component
   * @param keyEvent the key event
   * @param action the action
   * @throws IllegalArgumentException in case <code>component</code>, <code>action</code> or the action name is null
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final Action action) {
    addKeyEvent(component, keyEvent, 0, action);
  }


  /**
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap, JComponent.WHEN_FOCUSED as condition and true for onKeyRelease.
   * @param component the component
   * @param keyEvent the key event
   * @param modifiers the modifiers
   * @param action the action
   * @throws IllegalArgumentException in case <code>component</code>, <code>action</code> or the action name is null
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers,
                                 final Action action) {
    addKeyEvent(component, keyEvent, modifiers, JComponent.WHEN_FOCUSED, action);
  }


  /**
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap and true for onKeyRelease.
   * @param component the component
   * @param keyEvent the key event
   * @param modifiers the modifiers
   * @param condition the condition
   * @param action the action
   * @throws IllegalArgumentException in case <code>component</code>, <code>action</code> or the action name is null
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers, final int condition,
                                 final Action action) {
    addKeyEvent(component, keyEvent, modifiers, condition, true, action);
  }

  /**
   * Links the given action to the given key event on the given component via inputMap/actionMap, using the name
   * of the action as key for the actionMap, if <code>action</code> is null the binding is removed
   * @param component the component
   * @param keyEvent the key event
   * @param modifiers the modifiers
   * @param condition the condition
   * @param onKeyRelease the onKeyRelease condition
   * @param action the action, if null then the action binding is removed
   * @throws IllegalArgumentException in case <code>component</code> or the action name is null
   * @see KeyStroke#getKeyStroke(int, int, boolean)
   */
  public static void addKeyEvent(final JComponent component, final int keyEvent, final int modifiers, final int condition,
                                 final boolean onKeyRelease, final Action action) {
    Util.rejectNullValue(component, "component");
    Object actionName = null;
    if (action != null) {
      actionName = action.getValue(Action.NAME);
      if (actionName == null) {
        throw new IllegalArgumentException("Action name must be specified");
      }
      component.getActionMap().put(actionName, action);
    }
    component.getInputMap(condition).put(KeyStroke.getKeyStroke(keyEvent, modifiers, onKeyRelease), actionName);
  }

  /**
   * Adds a CTRL-SPACE action the the given text field for displaying a lookup dialog showing the values provided
   * by the given value provider
   * @param txtField the text field
   * @param valueCollectionProvider provides the values for the lookup dialog
   */
  public static void addLookupDialog(final JTextField txtField, final ValueCollectionProvider valueCollectionProvider) {
    addKeyEvent(txtField, KeyEvent.VK_SPACE, InputEvent.CTRL_DOWN_MASK, new AbstractAction("UiUtil.lookupValue") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final Object value = selectValue(txtField, valueCollectionProvider.getValues());
        if (value != null) {
          txtField.setText(value.toString());
        }
      }
    });
  }

  /**
   * Displays a dialog for selecting on of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @return the selected value, null if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> T selectValue(final JComponent dialogOwner, final Collection<T> values) {
    return selectValue(dialogOwner, values, Messages.get(Messages.SELECT_VALUE));
  }

  /**
   * Displays a dialog for selecting on of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @return the selected value, null if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> T selectValue(final JComponent dialogOwner, final Collection<T> values, final String dialogTitle) {
    final JList<T> list = new JList<>(new Vector<>(values));
    final Window owner = getParentWindow(dialogOwner);
    final JDialog dialog = new JDialog(owner, dialogTitle);
    final Action okAction = new DisposeWindowAction(dialog);
    final Action cancelAction = new AbstractAction("UiUtil.cancel") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        list.clearSelection();
        dialog.dispose();
      }
    };
    list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    prepareScrollPanelDialog(dialog, dialogOwner, list, okAction, cancelAction);
    if (dialog.getSize().width > MAX_SELECT_VALUE_DIALOG_WIDTH) {
      dialog.setSize(new Dimension(MAX_SELECT_VALUE_DIALOG_WIDTH, dialog.getSize().height));
    }

    return list.getSelectedValue();
  }

  /**
   * Prepares the dialog.
   * @param dialog the dialog
   * @param dialogOwner the dialog owner
   * @param toScroll added to a central scroll pane
   * @param okAction the action for the OK button
   * @param cancelAction the action for the cancel button
   */
  public static void prepareScrollPanelDialog(final JDialog dialog, final JComponent dialogOwner, final JComponent toScroll,
                                              final Action okAction, final Action cancelAction) {
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, cancelAction);
    toScroll.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    toScroll.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          okAction.actionPerformed(null);
        }
      }
    });
    final JButton btnOk  = new JButton(okAction);
    final JButton btnCancel = new JButton(cancelAction);
    btnOk.setText(Messages.get(Messages.OK));
    btnCancel.setText(Messages.get(Messages.CANCEL));
    final String cancelMnemonic = Messages.get(Messages.CANCEL_MNEMONIC);
    final String okMnemonic = Messages.get(Messages.OK_MNEMONIC);
    btnOk.setMnemonic(okMnemonic.charAt(0));
    btnCancel.setMnemonic(cancelMnemonic.charAt(0));
    dialog.setLayout(createBorderLayout());
    final JScrollPane scroller = new JScrollPane(toScroll);
    dialog.add(scroller, BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(createGridLayout(1, 2));
    buttonPanel.add(btnOk);
    buttonPanel.add(btnCancel);
    final JPanel buttonBasePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(buttonPanel);
    dialog.getRootPane().setDefaultButton(btnOk);
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    if (dialogOwner != null) {
      dialog.setLocationRelativeTo(getParentWindow(dialogOwner));
    }
    dialog.setModal(true);
    dialog.setResizable(true);
    dialog.setVisible(true);
  }

  /**
   * Creates an Action instance, with a triple-dot name ('...') for selecting a file path to display in the given text field
   * @param txtFilename the text field for displaying the file path
   * @return the Action
   */
  public static Action getBrowseAction(final JTextField txtFilename) {
    return new AbstractAction("...") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        try {
          final File file = selectFile(txtFilename, getParentPath(txtFilename.getText()));
          txtFilename.setText(file.getAbsolutePath());
        }
        catch (CancelException ignored) {}
      }
    };
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
   * @throws IOException in case of an IO exception, f.ex. if the image file is not found
   * @throws IllegalArgumentException in case the file type is not accepted
   */
  public static void showImage(final String imagePath, final JComponent dialogParent,
                               final Collection<String> acceptedFileTypes) throws IOException {
    Util.rejectNullValue(imagePath, "imagePath");
    if (imagePath.length() == 0) {
      return;
    }

    final int lastDotIndex = imagePath.lastIndexOf('.');
    if (lastDotIndex != -1) {//if the type is specified check it
      final String type = imagePath.substring(lastDotIndex + 1, imagePath.length()).toLowerCase();
      if (!acceptedFileTypes.contains(type)) {
        throw new IllegalArgumentException(Messages.get(Messages.UNKNOWN_FILE_TYPE) + ": " + type);
      }
    }
    final NavigableImagePanel imagePanel;
    try {
      setWaitCursor(true, dialogParent);
      imagePanel = new NavigableImagePanel();
      final BufferedImage image;
      if (imagePath.toLowerCase().startsWith("http")) {
        final URL url = new URL(imagePath);
        image = ImageIO.read(url);
      }
      else {
        final File imageFile = new File(imagePath);
        if (!imageFile.exists()) {
          throw new FileNotFoundException(Messages.get(Messages.FILE_NOT_FOUND) + ": " + imagePath);
        }
        image = ImageIO.read(imageFile);
      }
      imagePanel.setImage(image);
    }
    finally {
      setWaitCursor(false, dialogParent);
    }
    imagePanel.setPreferredSize(getScreenSizeRatio(DEFAULT_IMAGE_PANEL_SCREEN_SIZE_RATIO));
    displayInDialog(dialogParent, imagePanel, imagePath, false);
  }

  /**
   * @param support a drag'n drop transfer support instance
   * @return true if the given transfer support instance represents a file or a list of files
   */
  public static boolean isFileDataFlavor(final TransferHandler.TransferSupport support) {
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

  /**
   * Returns the File described by the given transfer support object, in case of many files the first one is used.
   * Null is returned if no files are found.
   * @param support the drag'n drop transfer support
   * @return the absolute path of the first file described the given transfer support object
   * @throws RuntimeException in case of an exception
   */
  @SuppressWarnings({"unchecked"})
  public static File getFileDataFlavor(final TransferHandler.TransferSupport support) {
    try {
      for (final DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.isFlavorJavaFileListType()) {
          final List<File> files = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);

          return files.isEmpty() ? null : files.get(0);
        }
      }
      //the code below is for handling unix/linux
      final DataFlavor nixFileDataFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
      final String data = (String) support.getTransferable().getTransferData(nixFileDataFlavor);
      for (final StringTokenizer st = new StringTokenizer(data, "\r\n"); st.hasMoreTokens();) {
        final String token = st.nextToken().trim();
        if (token.startsWith("#") || token.length() == 0) {// comment line, by RFC 2483
          continue;
        }

        return new File(new URI(token));
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }

    return null;
  }

  /**
   * http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5018574
   * @param component the component
   * @param onFocusAction the action to run when the focus has been requested
   */
  public static void addInitialFocusHack(final JComponent component, final Action onFocusAction) {
    component.addHierarchyListener(new HierarchyListener() {
      @Override
      public void hierarchyChanged(final HierarchyEvent e) {
        if (component.isShowing() && (e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
          SwingUtilities.getWindowAncestor(component).addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(final WindowEvent evt) {
              component.requestFocusInWindow();
              if (onFocusAction != null) {
                onFocusAction.actionPerformed(new ActionEvent(component, 0, "onFocusAction"));
              }
            }
          });
        }
      }
    });
  }

  /**
   * Sets the given string as clipboard contents
   * @param string the string to put on the clipboard
   */
  public static void setClipboard(final String string) {
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(string), null);
  }

  /**
   * Runs the given Runnable instance while displaying a simple indeterminate progress bar
   * @param dialogParent the dialog parent
   * @param progressBarTitle the progress bar title
   * @param successMessage the message to display after the task has run
   * @param successTitle the title for the success message dialog
   * @param failTitle the title of the failure dialog
   * @param task the task to run
   */
  public static void runWithProgressBar(final JComponent dialogParent, final String progressBarTitle,
                                        final String successMessage, final String successTitle, final String failTitle,
                                        final Runnable task) {
    final JProgressBar bar = new JProgressBar();
    bar.setIndeterminate(true);
    bar.setPreferredSize(new Dimension(DEFAULT_PROGRESS_BAR_WIDTH, bar.getPreferredSize().height));
    final JDialog dialog = new JDialog(UiUtil.getParentWindow(dialogParent), progressBarTitle, Dialog.ModalityType.APPLICATION_MODAL);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.setLayout(createBorderLayout());
    dialog.add(bar, BorderLayout.SOUTH);
    dialog.pack();
    UiUtil.centerWindow(dialog);
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        dialog.setVisible(true);
      }
    });
    //todo move UI work to EDT
    Executors.newSingleThreadExecutor().execute(new Runnable() {
      @Override
      public void run() {
        try {
          task.run();
          dialog.dispose();
          if (!Util.nullOrEmpty(successMessage)) {
            JOptionPane.showMessageDialog(UiUtil.getParentWindow(dialogParent), successMessage, successTitle, JOptionPane.INFORMATION_MESSAGE);
          }
        }
        catch (Exception ex) {
          dialog.dispose();
          ExceptionDialog.showExceptionDialog(UiUtil.getParentWindow(dialogParent), failTitle, ex);
        }
      }
    });
  }

  /**
   * Displays the given component in a dialog
   * @param owner the dialog owner
   * @param component the component to display
   * @param title the dialog title
   * @param modal if true then the dialog is modal
   * @param defaultButton the the default dialog button
   * @param closeEvent if specified the dialog will be disposed of when and only when this event occurs
   * @param disposeOnEscape if true then the dialog is disposed when the ESC button is pressed,
   * has no effect if a <ocde>closeEvent</ocde> is specified
   * @param onClosedAction this action will be registered as a windowClosed action for the dialog
   * @return the dialog used to display the component
   */
  private static JDialog displayInDialog(final Container owner, final JComponent component, final String title, final boolean modal,
                                         final JButton defaultButton, final EventObserver closeEvent, final boolean disposeOnEscape,
                                         final Action onClosedAction) {
    final Window dialogOwner = owner instanceof Window ? (Window) owner : getParentWindow(owner);
    final JDialog dialog = new JDialog(dialogOwner, title, modal ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);
    if (defaultButton != null) {
      dialog.getRootPane().setDefaultButton(defaultButton);
    }

    final Action disposeActionListener = new DisposeWindowAction(dialog);
    if (closeEvent == null) {
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      if (disposeOnEscape) {
        addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, disposeActionListener);
      }
    }
    else {
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      closeEvent.addListener(new EventListener() {
        @Override
        public void eventOccurred() {
          disposeActionListener.actionPerformed(null);
        }
      });
    }
    if (onClosedAction != null) {
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(final WindowEvent e) {
          onClosedAction.actionPerformed(new ActionEvent(dialog, -1, null));
        }
      });
    }

    dialog.setLayout(createBorderLayout());
    dialog.add(component, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  private static String getParentPath(final String text) {
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
   * An action which disposes a given window when performed
   */
  public static final class DisposeWindowAction extends AbstractAction {

    private final Window window;

    /**
     * Instantiates a new DisposeWindowAction, which disposes of the given window when performed
     * @param window the window to dispose
     */
    public DisposeWindowAction(final Window window) {
      super("UiUtil.disposeDialog");
      this.window = window;
    }

    /**
     * Calls dispose on the window
     * @param e ignored
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
      window.dispose();
    }
  }

  /**
   * An action which transfers focus either forward or backward for a given component
   */
  public static final class TransferFocusAction extends AbstractAction {

    private final JComponent component;
    private final boolean backward;

    /**
     * @param component the component
     */
    public TransferFocusAction(final JComponent component) {
      this(component, false);
    }

    /**
     * @param component the component
     * @param backward if true the focus is transferred backward
     */
    public TransferFocusAction(final JComponent component, final boolean backward) {
      super(backward ? "UiUtil.transferFocusBackward" : "UiUtil.transferFocusForward");
      this.component = component;
      this.backward = backward;
    }

    /**
     * Transfers focus according the the value of <code>backward</code>
     * @param e the action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
      if (backward) {
        component.transferFocusBackward();
      }
      else {
        component.transferFocus();
      }
    }
  }

  private static final class FileTransferHandler extends TransferHandler {

    private final JTextComponent textComponent;

    private FileTransferHandler(final JTextComponent textComponent) {
      this.textComponent = textComponent;
    }

    @Override
    public boolean canImport(final TransferSupport support) {
      return isFileDataFlavor(support);
    }

    @Override
    public boolean importData(final TransferSupport support) {
      final File file = getFileDataFlavor(support);
      if (file == null) {
        return false;
      }

      textComponent.setText(file.getAbsolutePath());
      textComponent.requestFocusInWindow();
      return true;
    }
  }

  private static final class CaseDocumentFilter extends DocumentFilter {

    private final boolean upperCase;

    private CaseDocumentFilter(final boolean upperCase) {
      this.upperCase = upperCase;
    }

    @Override
    public void insertString(final FilterBypass bypass, final int offset, final String string,
                             final AttributeSet attributeSet) throws BadLocationException {
      super.insertString(bypass, offset, string == null ? null :
              (upperCase ? string.toUpperCase(Locale.getDefault()) : string.toLowerCase(Locale.getDefault())), attributeSet);
    }

    @Override
    public void replace(final FilterBypass bypass, final int offset, final int length, final String string,
                        final AttributeSet attributeSet) throws BadLocationException {
      super.replace(bypass, offset, length, string == null ? null :
              (upperCase ? string.toUpperCase(Locale.getDefault()) : string.toLowerCase(Locale.getDefault())), attributeSet);
    }
  }

  /**
   * Somewhat of a hack to keep the current field selection and caret position when
   * the field gains focus, in case the content length has not changed
   * http://stackoverflow.com/a/2202073/317760
   */
  private static final class FieldFormatter extends MaskFormatter {

    private final boolean toUpperCase;

    private FieldFormatter(final String mask, final boolean toUpperCase, final boolean valueContainsLiteralCharacter) throws ParseException {
      super(mask);
      this.toUpperCase = toUpperCase;
      setPlaceholderCharacter('_');
      setAllowsInvalid(false);
      setValueContainsLiteralCharacters(valueContainsLiteralCharacter);
    }

    @Override
    public Object stringToValue(final String value) throws ParseException {
      String ret = value;
      if (toUpperCase) {
        ret = ret.toUpperCase(Locale.getDefault());
      }

      return super.stringToValue(ret);
    }

    @Override
    public void install(final JFormattedTextField field) {
      final int previousLength = field.getDocument().getLength();
      final int currentCaretPosition = field.getCaretPosition();
      final int currentSelectionStart = field.getSelectionStart();
      final int currentSelectionEnd = field.getSelectionEnd();
      super.install(field);
      if (previousLength == field.getDocument().getLength()) {
        if (currentSelectionEnd - currentSelectionStart > 0) {
          field.setCaretPosition(currentSelectionStart);
          field.moveCaretPosition(currentSelectionEnd);
        }
        else {
          field.setCaretPosition(currentCaretPosition);
        }
      }
    }
  }
}
