/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.dialog;

import org.jminor.common.event.EventDataListener;
import org.jminor.common.event.EventObserver;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.KeyEvents;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.jminor.common.Util.nullOrEmpty;

/**
 * A utility class for displaying Dialogs.
 */
public final class Dialogs {

  public static final ResourceBundle MESSAGES = ResourceBundle.getBundle(Dialogs.class.getName(), Locale.getDefault());
  private static final int MAX_SELECT_VALUE_DIALOG_WIDTH = 500;
  /**
   * Caching the file chooser instances since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  private static JFileChooser fileChooserOpen;
  private static JFileChooser fileChooserSave;

  private Dialogs() {}

  /**
   * Displays the given component in a modal dialog
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
   * @param disposeOnEscape if true then dispose is called on the dialog on ESC
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
   * will only be closed if that state is active after a call to {@link EventDataListener#onEvent(Object)}
   * @return the dialog
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final EventObserver closeObserver, final EventDataListener<State> confirmCloseListener) {
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
   * will only be closed if that state is active after a call to {@link EventDataListener#onEvent(Object)}
   * @return the dialog
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final boolean modal, final EventObserver closeObserver,
                                        final EventDataListener<State> confirmCloseListener) {
    final JDialog dialog = new JDialog(Windows.getParentWindow(owner), title);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        closeIfConfirmed(confirmCloseListener, dialog);
      }
    });
    if (closeObserver != null) {
      closeObserver.addListener(() -> closeIfConfirmed(confirmCloseListener, dialog));
    }
    dialog.setLayout(Layouts.createBorderLayout());
    dialog.add(component, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(modal);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  /**
   * Shows an exception dialog for the given throwable
   * @param window the dialog parent window
   * @param title the dialog title
   * @param throwable the exception to display
   */
  public static void showExceptionDialog(final Window window, final String title, final Throwable throwable) {
    showExceptionDialog(window, title, throwable.getMessage(), throwable);
  }

  /**
   * Shows an exception dialog for the given throwable
   * @param window the dialog parent window
   * @param message the message
   * @param title the dialog title
   * @param throwable the exception to display
   */
  public static void showExceptionDialog(final Window window, final String title, final String message,
                                         final Throwable throwable) {
    showExceptionDialog(window, title, message, throwable, true);
  }

  /**
   * Shows an exception dialog for the given throwable
   * @param window the dialog parent window
   * @param message the message
   * @param title the dialog title
   * @param modal if true then the dialog will be modal
   * @param throwable the exception to display
   */
  public static void showExceptionDialog(final Window window, final String title, final String message,
                                         final Throwable throwable, final boolean modal) {
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        new ExceptionDialog(window).showForThrowable(title, message, throwable, modal);
      }
      else {
        SwingUtilities.invokeAndWait(() -> new ExceptionDialog(window).showForThrowable(title, message, throwable, modal));
      }
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
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
   * has no effect if a <code>closeEvent</code> is specified
   * @param onClosedAction this action will be registered as a windowClosed action for the dialog
   * @return the dialog used to display the component
   */
  public static JDialog displayInDialog(final Container owner, final JComponent component, final String title,
                                        final boolean modal,
                                        final JButton defaultButton, final EventObserver closeEvent,
                                        final boolean disposeOnEscape,
                                        final Action onClosedAction) {
    final Window dialogOwner = owner instanceof Window ? (Window) owner : Windows.getParentWindow(owner);
    final JDialog dialog = new JDialog(dialogOwner, title, modal ? Dialog.ModalityType.APPLICATION_MODAL : Dialog.ModalityType.MODELESS);
    if (defaultButton != null) {
      dialog.getRootPane().setDefaultButton(defaultButton);
    }

    final Action disposeAction = new DisposeWindowAction(dialog);
    if (closeEvent == null) {
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      if (disposeOnEscape) {
        KeyEvents.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, disposeAction);
      }
    }
    else {
      dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
      closeEvent.addListener(() -> disposeAction.actionPerformed(null));
    }
    if (onClosedAction != null) {
      dialog.addWindowListener(new WindowAdapter() {
        @Override
        public void windowClosed(final WindowEvent e) {
          onClosedAction.actionPerformed(new ActionEvent(dialog, -1, null));
        }
      });
    }

    dialog.setLayout(Layouts.createBorderLayout());
    dialog.add(component, BorderLayout.CENTER);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setResizable(true);
    dialog.setVisible(true);

    return dialog;
  }

  /**
   * Displays a dialog for selecting one of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @return the selected value, null if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> T selectValue(final JComponent dialogOwner, final Collection<T> values) {
    return selectValue(dialogOwner, values, MESSAGES.getString("select_value"));
  }

  /**
   * Displays a dialog for selecting one of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @return the selected value, null if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> T selectValue(final JComponent dialogOwner, final Collection<T> values, final String dialogTitle) {
    return selectValue(dialogOwner, values, dialogTitle, null);
  }

  /**
   * Displays a dialog for selecting one of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @param defaultSelection the item selected by default
   * @return the selected value, null if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> T selectValue(final JComponent dialogOwner, final Collection<T> values, final String dialogTitle,
                                  final T defaultSelection) {
    final List<T> selected = selectValues(dialogOwner, values, dialogTitle, true,
            defaultSelection == null ? emptyList() : singletonList(defaultSelection));
    if (selected.isEmpty()) {
      return null;
    }

    return selected.get(0);
  }

  /**
   * Displays a dialog for selecting from of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @return the selected values, en empty Collection if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> List<T> selectValues(final JComponent dialogOwner, final Collection<T> values, final String dialogTitle) {
    return selectValues(dialogOwner, values, dialogTitle, false, emptyList());
  }

  /**
   * Displays a dialog for selecting from of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @param defaultSelection the items selected by default
   * @return the selected values, en empty Collection if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> List<T> selectValues(final JComponent dialogOwner, final Collection<T> values, final String dialogTitle,
                                         final Collection<T> defaultSelection) {
    return selectValues(dialogOwner, values, dialogTitle, false, defaultSelection);
  }

  /**
   * Prepares a dialog for displaying {@code toScroll}, with OK and Cancel buttons.
   * Note that the default Enter key action is disabled on the {@code toScroll} component.
   * @param dialog the dialog
   * @param dialogOwner the dialog owner
   * @param toScroll added to a central scroll pane
   * @param okAction the action for the OK button
   * @param cancelAction the action for the cancel button
   */
  public static void prepareScrollPanelDialog(final JDialog dialog, final JComponent dialogOwner, final JComponent toScroll,
                                              final Action okAction, final Action cancelAction) {
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    KeyEvents.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ESCAPE, 0, JComponent.WHEN_IN_FOCUSED_WINDOW, cancelAction);
    KeyEvents.addKeyEvent(dialog.getRootPane(), KeyEvent.VK_ENTER, 0, JComponent.WHEN_IN_FOCUSED_WINDOW, false, okAction);
    toScroll.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    final JButton okButton = new JButton(okAction);
    final JButton cancelButton = new JButton(cancelAction);
    okButton.setText(Messages.get(Messages.OK));
    okButton.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));
    cancelButton.setText(Messages.get(Messages.CANCEL));
    cancelButton.setMnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));
    dialog.setLayout(new BorderLayout(5, 5));
    dialog.add(new JScrollPane(toScroll), BorderLayout.CENTER);
    final JPanel buttonPanel = new JPanel(new GridLayout(1, 2));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    final JPanel buttonBasePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(buttonPanel);
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    if (dialogOwner != null) {
      dialog.setLocationRelativeTo(Windows.getParentWindow(dialogOwner));
    }
    dialog.setModal(true);
    dialog.setResizable(true);
  }

  /**
   * Displays a file selection dialog for selecting an existing directory
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectDirectory(final JComponent dialogParent, final String startDir) {
    return selectDirectory(dialogParent, startDir, MESSAGES.getString("select_directory"));
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
    return selectFile(dialogParent, startDir, MESSAGES.getString("select_file"));
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
   * @param filesOnly if true then files are displayed, otherwise only directories
   * @param dialogTitle the dialog title
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectFileOrDirectory(final JComponent dialogParent, final String startDir, final boolean filesOnly,
                                           final String dialogTitle) {
    return selectFilesOrDirectories(dialogParent, startDir, filesOnly, false, dialogTitle).get(0);
  }

  /**
   * Displays a file selection dialog for selecting files or directories
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @param filesOnly if true then files are displayed, otherwise only directories
   * @param multiSelection if true then the dialog will allow selection of multiple items
   * @param dialogTitle the dialog title
   * @return a List containing the selected files, contains at least one file
   * @throws CancelException in case the user cancels or no files are selected
   */
  public static synchronized List<File> selectFilesOrDirectories(final JComponent dialogParent, final String startDir,
                                                                 final boolean filesOnly, final boolean multiSelection,
                                                                 final String dialogTitle) {
    if (fileChooserOpen == null) {
      try {
        Components.showWaitCursor(dialogParent);
        fileChooserOpen = new JFileChooser(new File(startDir == null ? System.getProperty("user.home") : startDir));
      }
      finally {
        Components.hideWaitCursor(dialogParent);
      }
    }
    if (filesOnly) {
      fileChooserOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
    }
    else {
      fileChooserOpen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    }
    fileChooserOpen.setSelectedFiles(new File[] {new File("")});
    fileChooserOpen.removeChoosableFileFilter(fileChooserOpen.getFileFilter());
    fileChooserOpen.setMultiSelectionEnabled(multiSelection);
    if (!nullOrEmpty(startDir) && new File(startDir).exists()) {
      fileChooserOpen.setCurrentDirectory(new File(startDir));
    }
    if (dialogTitle != null) {
      fileChooserOpen.setDialogTitle(dialogTitle);
    }
    final int option = fileChooserOpen.showOpenDialog(dialogParent);
    if (option == JFileChooser.APPROVE_OPTION) {
      final List<File> selectedFiles;
      if (multiSelection) {
        selectedFiles = asList(fileChooserOpen.getSelectedFiles());
      }
      else {
        selectedFiles = singletonList(fileChooserOpen.getSelectedFile());
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
  public static synchronized File selectFileToSave(final JComponent dialogParent, final String startDir, final String defaultFileName) {
    return selectFileToSave(dialogParent, startDir, defaultFileName, true);
  }

  /**
   * Displays a save file dialog for creating a new file
   * @param dialogParent the dialog parent
   * @param startDir the start dir, user.dir if not specified
   * @param defaultFileName the default file name to suggest
   * @param confirmOverwrite if true then the user is asked to confirm overwrite if the file exists
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static synchronized File selectFileToSave(final JComponent dialogParent, final String startDir,
                                                   final String defaultFileName, final boolean confirmOverwrite) {
    if (fileChooserSave == null) {
      try {
        Components.showWaitCursor(dialogParent);
        fileChooserSave = new JFileChooser();
      }
      finally {
        Components.hideWaitCursor(dialogParent);
      }
    }
    fileChooserSave.setSelectedFiles(new File[] {new File("")});
    fileChooserSave.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooserSave.removeChoosableFileFilter(fileChooserSave.getFileFilter());
    fileChooserSave.setMultiSelectionEnabled(false);
    final File startDirectory;
    if (!nullOrEmpty(startDir) && new File(startDir).exists()) {
      startDirectory = new File(startDir);
    }
    else {
      startDirectory = fileChooserSave.getCurrentDirectory();
    }
    File selectedFile = new File(startDirectory.getAbsolutePath() + (defaultFileName != null ? File.separator + defaultFileName : ""));
    boolean fileChosen = false;
    while (!fileChosen) {
      if (selectedFile.isDirectory()) {
        fileChooserSave.setCurrentDirectory(selectedFile);
      }
      else {
        fileChooserSave.setSelectedFile(selectedFile);
      }
      int option = fileChooserSave.showSaveDialog(dialogParent);
      if (option == JFileChooser.APPROVE_OPTION) {
        selectedFile = fileChooserSave.getSelectedFile();
        if (selectedFile.exists() && confirmOverwrite) {
          option = JOptionPane.showConfirmDialog(dialogParent, MESSAGES.getString("overwrite_file"),
                  MESSAGES.getString("file_exists"), JOptionPane.YES_NO_CANCEL_OPTION);
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

  private static void closeIfConfirmed(final EventDataListener<State> confirmCloseListener, final JDialog dialog) {
    if (confirmCloseListener == null) {
      dialog.dispose();
    }
    else {
      final State confirmClose = States.state();
      confirmCloseListener.onEvent(confirmClose);
      if (confirmClose.get()) {
        dialog.dispose();
      }
    }
  }

  /**
   * Displays a dialog for selecting from of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @param singleSelection if true then the selection is restricted to a single value
   * @param defaultSelection the items selected by default
   * @return the selected values, en empty Collection if none was selected
   */
  private static <T> List<T> selectValues(final JComponent dialogOwner, final Collection<T> values,
                                          final String dialogTitle, final boolean singleSelection,
                                          final Collection<T> defaultSelection) {
    final DefaultListModel<T> listModel = new DefaultListModel<>();
    values.forEach(listModel::addElement);
    final JList<T> list = new JList<>(listModel);
    final Window owner = Windows.getParentWindow(dialogOwner);
    final JDialog dialog = new JDialog(owner, dialogTitle);
    final Action okAction = new DisposeWindowAction(dialog);
    final Action cancelAction = new AbstractAction() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        list.clearSelection();
        dialog.dispose();
      }
    };
    if (singleSelection) {
      list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    }
    prepareScrollPanelDialog(dialog, dialogOwner, list, okAction, cancelAction);
    list.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.getClickCount() == 2) {
          okAction.actionPerformed(null);
        }
      }
    });
    if (dialog.getSize().width > MAX_SELECT_VALUE_DIALOG_WIDTH) {
      dialog.setSize(new Dimension(MAX_SELECT_VALUE_DIALOG_WIDTH, dialog.getSize().height));
    }
    if (defaultSelection != null) {
      defaultSelection.forEach(item -> {
        final int index = listModel.indexOf(item);
        list.getSelectionModel().addSelectionInterval(index, index);
        list.ensureIndexIsVisible(index);
      });
    }
    dialog.setVisible(true);

    return list.getSelectedValuesList();
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
      super("UiUtil.disposeWindow");
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
}
