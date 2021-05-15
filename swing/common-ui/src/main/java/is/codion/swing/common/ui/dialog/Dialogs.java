/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.event.EventDataListener;
import is.codion.common.event.EventObserver;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
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
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.common.Util.nullOrEmpty;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * A utility class for displaying Dialogs.
 */
public final class Dialogs {

  public static final ResourceBundle MESSAGES = ResourceBundle.getBundle(Dialogs.class.getName());
  private static final int MAX_SELECT_VALUE_DIALOG_WIDTH = 500;
  /**
   * Caching the file chooser instances since the constructor is quite slow, especially on Win. with many mapped network drives
   */
  private static JFileChooser fileChooserOpen;
  private static JFileChooser fileChooserSave;

  /**
   * Specifies how a file selection dialog restricts it's selection.
   */
  public enum FilesOrDirectories {
    /**
     * Only files.
     */
    FILES,
    /**
     * Only directories.
     */
    DIRECTORIES,
    /**
     * Both files and directories.
     */
    BOTH
  }

  /**
   * Specifies if a overwriting a file should be confirmed.
   */
  public enum ConfirmOverwrite {
    /**
     * If overwriting should be confirmed.
     */
    YES,
    /**
     * If overwriting should not be confirmed.
     */
    NO
  }

  /**
   * Specifies whether single item selection is enabled when selecting files and or directories.
   */
  public enum SingleSelection {
    /**
     * Single selection is enabled.
     */
    YES,
    /**
     * Single selection is not enabled.
     */
    NO
  }

  private Dialogs() {}

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
    try {
      if (SwingUtilities.isEventDispatchThread()) {
        new ExceptionDialog(window).showForThrowable(title, message, throwable, true).dispose();
      }
      else {
        SwingUtilities.invokeAndWait(() -> new ExceptionDialog(window).showForThrowable(title, message, throwable, true).dispose());
      }
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return a new JDialog {@link Builder} instance.
   */
  public static Builder builder() {
    return new DefaultDialogBuilder();
  }

  /**
   * @return a new login dialog builder
   */
  public static LoginDialogBuilder loginDialogBuilder() {
    return new DefaultLoginDialogBuilder();
  }

  /**
   * Displays a dialog for selecting one of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @return the selected value, {@link Optional#empty()} if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> Optional<T> selectValue(final JComponent dialogOwner, final Collection<T> values) {
    return selectValue(dialogOwner, values, MESSAGES.getString("select_value"));
  }

  /**
   * Displays a dialog for selecting one of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @return the selected value, {@link Optional#empty()} if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> Optional<T> selectValue(final JComponent dialogOwner, final Collection<T> values, final String dialogTitle) {
    return selectValue(dialogOwner, values, dialogTitle, null);
  }

  /**
   * Displays a dialog for selecting one of a collection of values
   * @param dialogOwner the dialog owner
   * @param values the values to choose from
   * @param dialogTitle the dialog title
   * @param defaultSelection the item selected by default
   * @return the selected value, {@link Optional#empty()} if none was selected
   * @param <T> the type of values being selected
   */
  public static <T> Optional<T> selectValue(final JComponent dialogOwner, final Collection<T> values, final String dialogTitle,
                                            final T defaultSelection) {
    final List<T> selected = selectValues(dialogOwner, values, dialogTitle, true,
            defaultSelection == null ? emptyList() : singletonList(defaultSelection));
    if (selected.isEmpty()) {
      return Optional.empty();
    }

    return Optional.of(selected.get(0));
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
   * Prepares a modal dialog for displaying the given {@code component},
   * with OK and Cancel buttons based on the given actions.
   * @param dialog the dialog
   * @param component added to the center position
   * @param okAction the action for the OK button
   * @param cancelAction the action for the cancel button
   */
  public static void prepareOkCancelDialog(final JDialog dialog, final JComponent component,
                                           final Action okAction, final Action cancelAction) {
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(cancelAction)
            .enable(dialog.getRootPane());
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ENTER).condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(okAction)
            .enable(dialog.getRootPane());
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);
    final JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(Components.createOkCancelButtonPanel(okAction, cancelAction));
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    if (dialog.getOwner() != null) {
      dialog.setLocationRelativeTo(dialog.getOwner());
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
    return selectFileOrDirectory(dialogParent, startDir, FilesOrDirectories.DIRECTORIES, dialogTitle);
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
    return selectFileOrDirectory(dialogParent, startDir, FilesOrDirectories.FILES, dialogTitle);
  }

  /**
   * Displays a file selection dialog for selecting an existing file or directory
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @param filesOrDirectories specifies whether selection should be restricted
   * @param dialogTitle the dialog title
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static File selectFileOrDirectory(final JComponent dialogParent, final String startDir,
                                           final FilesOrDirectories filesOrDirectories, final String dialogTitle) {
    return selectFilesOrDirectories(dialogParent, startDir, filesOrDirectories, SingleSelection.NO, dialogTitle).get(0);
  }

  /**
   * Displays a file selection dialog for selecting files or directories
   * @param dialogParent the dialog parent
   * @param startDir the start directory, user.home if not specified
   * @param filesOrDirectories specifies whether selection should be restricted
   * @param singleSelection if true then the dialog will be restricted to single item selection
   * @param dialogTitle the dialog title
   * @return a List containing the selected files, contains at least one file
   * @throws CancelException in case the user cancels or no files are selected
   */
  public static synchronized List<File> selectFilesOrDirectories(final JComponent dialogParent, final String startDir,
                                                                 final FilesOrDirectories filesOrDirectories,
                                                                 final SingleSelection singleSelection,
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
    switch (filesOrDirectories) {
      case FILES:
        fileChooserOpen.setFileSelectionMode(JFileChooser.FILES_ONLY);
        break;
      case DIRECTORIES:
        fileChooserOpen.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        break;
      case BOTH:
        fileChooserOpen.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        break;
    }
    fileChooserOpen.setSelectedFiles(new File[] {new File("")});
    fileChooserOpen.removeChoosableFileFilter(fileChooserOpen.getFileFilter());
    fileChooserOpen.setMultiSelectionEnabled(singleSelection == SingleSelection.NO);
    if (!nullOrEmpty(startDir) && new File(startDir).exists()) {
      fileChooserOpen.setCurrentDirectory(new File(startDir));
    }
    if (dialogTitle != null) {
      fileChooserOpen.setDialogTitle(dialogTitle);
    }
    final int option = fileChooserOpen.showOpenDialog(dialogParent);
    if (option == JFileChooser.APPROVE_OPTION) {
      final List<File> selectedFiles;
      if (singleSelection == SingleSelection.NO) {
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
    return selectFileToSave(dialogParent, startDir, defaultFileName, ConfirmOverwrite.YES);
  }

  /**
   * Displays a save file dialog for creating a new file
   * @param dialogParent the dialog parent
   * @param startDir the start dir, user.dir if not specified
   * @param defaultFileName the default file name to suggest
   * @param confirmOverwrite specifies whether overwriting a file should be confirmed
   * @return the selected file
   * @throws CancelException in case the user cancels
   */
  public static synchronized File selectFileToSave(final JComponent dialogParent, final String startDir,
                                                   final String defaultFileName, final ConfirmOverwrite confirmOverwrite) {
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
        if (selectedFile.exists() && confirmOverwrite == ConfirmOverwrite.YES) {
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
    final Action okAction = new DisposeDialogAction(dialog, null);
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
    list.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "none");
    prepareOkCancelDialog(dialog, new JScrollPane(list), okAction, cancelAction);
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
   * A builder for JDialog.
   */
  public interface Builder {

    /**
     * @param owner the dialog owner
     * @return this Builder instance
     */
    Builder owner(Container owner);

    /**
     * @param component the component to display
     * @return this Builder instance
     */
    Builder component(JComponent component);

    /**
     * @param title the dialog title
     * @return this Builder instance
     */
    Builder title(String title);

    /**
     * @param icon the dialog icon
     * @return this Builder instance
     */
    Builder icon(ImageIcon icon);

    /**
     * @param modal true if the dialog should be modal
     * @return this Builder instance
     */
    Builder modal(boolean modal);

    /**
     * @param enterAction the action to associate with the ENTER key
     * @return this Builder instance
     */
    Builder enterAction(Action enterAction);

    /**
     * @param onClosedAction this action will be registered as a windowClosed action for the dialog
     * @return this Builder instance
     */
    Builder onClosedAction(Action onClosedAction);

    /**
     * Sets the Event which triggers the closing of the dialog, note that {@link #disposeOnEscape(boolean)}
     * has no effect if the closeEvent is specified.
     * @param closeEvent if specified the dialog will be disposed of when and only when this event occurs
     * @return this Builder instance
     */
    Builder closeEvent(EventObserver<?> closeEvent);

    /**
     * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
     * the dialog is closed, using the State instance to signal confirmation, the dialog
     * will only be closed if that state is active after a call to {@link EventDataListener#onEvent(Object)}
     * @return this Builder instance
     */
    Builder confirmCloseListener(EventDataListener<State> confirmCloseListener);

    /**
     * @param disposeOnEscape if yes then the dialog is disposed when the ESC button is pressed,
     * has no effect if a <code>closeEvent</code> is specified
     * @return this Builder instance
     */
    Builder disposeOnEscape(boolean disposeOnEscape);

    /**
     * @return a new JDialog instance based on this builder.
     * @throws IllegalStateException in case no component has been specified
     */
    JDialog build();
  }

  /**
   * A login panel builder
   */
  public interface LoginDialogBuilder {

    /**
     * @param defaultUser the default user credentials to display
     * @return this Builder instance
     */
    LoginDialogBuilder defaultUser(User defaultUser);

    /**
     * @param validator the login validator to use
     * @return this Builder instance
     */
    LoginDialogBuilder validator(LoginValidator validator);

    /**
     * @param southComponent a component to add to the south of the credentials input fields
     * @return this Builder instance
     */
    LoginDialogBuilder southComponent(JComponent southComponent);

    /**
     * @param dialogParent the dialog parent component
     * @return this Builder instance
     */
    LoginDialogBuilder dialogParent(JComponent dialogParent);

    /**
     * @param dialogTitle the dialog title
     * @return this Builder instance
     */
    LoginDialogBuilder dialogTitle(String dialogTitle);

    /**
     * @param icon the dialog icon
     * @return this Builder instance
     */
    LoginDialogBuilder icon(ImageIcon icon);

    /**
     * @return the logged in user
     * @throws CancelException in case the login is cancelled
     */
    User show();
  }

  /**
   * Validates a login attempt.
   */
  public interface LoginValidator {

    /**
     * Valdates a login with the given user
     * @param user the user
     * @throws Exception in case validation fails
     */
    void validate(User user) throws Exception;
  }
}
