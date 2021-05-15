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
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * A utility class for displaying Dialogs.
 */
public final class Dialogs {

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
    catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return a new {@link DialogBuilder} instance.
   */
  public static DialogBuilder builder() {
    return new DefaultDialogBuilder();
  }

  /**
   * @return a new {@link ProgressDialogBuilder} instance.
   */
  public static ProgressDialogBuilder progressDialogBuilder() {
    return new DefaultProgressDialogBuilder();
  }

  /**
   * @return a new login dialog builder
   */
  public static LoginDialogBuilder loginDialogBuilder() {
    return new DefaultLoginDialogBuilder();
  }

  /**
   * @return a new FileSelectionDialogBuilder
   */
  public static FileSelectionDialogBuilder fileSelectionDialogBuilder() {
    return new DefaultFileSelectionDialogBuilder();
  }

  /**
   * @param values the values to select from
   * @param <T> the value type
   * @return a new selection dialog builder
   * @throws IllegalArgumentException in case values is empty
   */
  public static <T> SelectionDialogBuilder<T> selectionDialogBuilder(final Collection<T> values) {
    return new DefaultSelectionDialogBuilder<>(values);
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
   * A builder for JDialog.
   */
  public interface DialogBuilder {

    /**
     * @param owner the dialog owner
     * @return this DialogBuilder instance
     */
    DialogBuilder owner(Window owner);

    /**
     * @param dialogParent the dialog parent component
     * @return this DialogBuilder instance
     */
    DialogBuilder dialogParent(JComponent dialogParent);

    /**
     * @param component the component to display
     * @return this DialogBuilder instance
     */
    DialogBuilder component(JComponent component);

    /**
     * @param title the dialog title
     * @return this DialogBuilder instance
     */
    DialogBuilder title(String title);

    /**
     * @param icon the dialog icon
     * @return this DialogBuilder instance
     */
    DialogBuilder icon(ImageIcon icon);

    /**
     * @param modal true if the dialog should be modal
     * @return this DialogBuilder instance
     */
    DialogBuilder modal(boolean modal);

    /**
     * @param enterAction the action to associate with the ENTER key
     * @return this DialogBuilder instance
     */
    DialogBuilder enterAction(Action enterAction);

    /**
     * @param onClosedAction this action will be registered as a windowClosed action for the dialog
     * @return this DialogBuilder instance
     */
    DialogBuilder onClosedAction(Action onClosedAction);

    /**
     * Sets the Event which triggers the closing of the dialog, note that {@link #disposeOnEscape(boolean)}
     * has no effect if the closeEvent is specified.
     * @param closeEvent if specified the dialog will be disposed of when and only when this event occurs
     * @return this DialogBuilder instance
     */
    DialogBuilder closeEvent(EventObserver<?> closeEvent);

    /**
     * @param confirmCloseListener this listener, if specified, will be queried for confirmation before
     * the dialog is closed, using the State instance to signal confirmation, the dialog
     * will only be closed if that state is active after a call to {@link EventDataListener#onEvent(Object)}
     * @return this DialogBuilder instance
     */
    DialogBuilder confirmCloseListener(EventDataListener<State> confirmCloseListener);

    /**
     * @param disposeOnEscape if yes then the dialog is disposed when the ESC button is pressed,
     * has no effect if a <code>closeEvent</code> is specified
     * @return this DialogBuilder instance
     */
    DialogBuilder disposeOnEscape(boolean disposeOnEscape);

    /**
     * @return a new JDialog instance based on this builder.
     * @throws IllegalStateException in case no component has been specified
     */
    JDialog build();
  }

  /**
   * A builder for a selection dialog.
   * @param <T> the value type
   */
  public interface SelectionDialogBuilder<T> {

    /**
     * @param owner the dialog owner
     * @return this SelectionDialogBuilder instance
     */
    SelectionDialogBuilder<T> owner(Window owner);

    /**
     * @param dialogParent the dialog parent component
     * @return this SelectionDialogBuilder instance
     */
    SelectionDialogBuilder<T> dialogParent(JComponent dialogParent);

    /**
     * @param title the dialog title
     * @return this SelectionDialogBuilder instance
     */
    SelectionDialogBuilder<T> title(String title);

    /**
     * @param singleSelection if true then the selection is restricted to a single value
     * @return this SelectionDialogBuilder instance
     */
    SelectionDialogBuilder<T> singleSelection(boolean singleSelection);

    /**
     * @param defaultSelection the item selected by default
     * @return this SelectionDialogBuilder instance
     */
    SelectionDialogBuilder<T> defaultSelection(T defaultSelection);

    /**
     * @param defaultSelection the items selected by default
     * @return this SelectionDialogBuilder instance
     */
    SelectionDialogBuilder<T> defaultSelection(Collection<T> defaultSelection);

    /**
     * @return the selected value, {@link Optional#empty()} if none was selected
     */
    Optional<T> selectSingle();

    /**
     * @return the selected values, en empty Collection if none was selected
     */
    Collection<T> select();
  }

  /**
   * A builder for a file/directory selection dialog
   */
  public interface FileSelectionDialogBuilder {

    /**
     * @param owner the dialog owner
     * @return this DialogBuilder instance
     */
    FileSelectionDialogBuilder owner(Window owner);

    /**
     * @param dialogParent the dialog parent component
     * @return this DialogBuilder instance
     */
    FileSelectionDialogBuilder dialogParent(JComponent dialogParent);

    /**
     * @param title the dialog title
     * @return this DialogBuilder instance
     */
    FileSelectionDialogBuilder title(String title);

    /**
     * @param startDirectory the start directory
     * @return this DialogBuilder instance
     */
    FileSelectionDialogBuilder startDirectory(String startDirectory);

    /**
     * @param confirmOverwrite specifies whether overwriting a file should be confirmed
     * @return this DialogBuilder instance
     */
    FileSelectionDialogBuilder confirmOverwrite(boolean confirmOverwrite);

    /**
     * @return the selected file
     * @throws CancelException in case the user cancels
     */
    File selectFile();

    /**
     * @return the selected files
     * @throws CancelException in case the user cancels
     */
    List<File> selectFiles();

    /**
     * @return the selected directory
     * @throws CancelException in case the user cancels
     */
    File selectDirectory();

    /**
     * @return the selected directories
     * @throws CancelException in case the user cancels
     */
    List<File> selectDirectories();

    /**
     * @return a List containing the selected files, contains at least one file
     * @throws CancelException in case the user cancels or no files are selected
     */
    List<File> selectFilesOrDirectories();

    /**
     * @return the selected file
     * @throws CancelException in case the user cancels
     */
    File selectFileToSave();

    /**
     * @param defaultFileName the default file name
     * @return the selected file
     * @throws CancelException in case the user cancels
     */
    File selectFileToSave(String defaultFileName);
  }

  /**
   * A builder for {@link ProgressDialog}.
   */
  public interface ProgressDialogBuilder {

    /**
     * @param owner the dialog owner
     * @return this ProgressDialogBuilder instance
     */
    ProgressDialogBuilder owner(Window owner);

    /**
     * @param dialogParent the dialog parent component
     * @return this ProgressDialogBuilder instance
     */
    ProgressDialogBuilder dialogParent(JComponent dialogParent);

    /**
     * @param title the title
     * @return this ProgressDialogBuilder instance
     */
    ProgressDialogBuilder title(String title);

    /**
     * @param indeterminate the indeterminate status of the progress bar
     * @return this ProgressDialogBuilder instance
     */
    ProgressDialogBuilder indeterminate(boolean indeterminate);

    /**
     * @param northPanel if specified this panel is added to the {@link BorderLayout#NORTH} position
     * @return this ProgressDialogBuilder instance
     */
    ProgressDialogBuilder northPanel(JPanel northPanel);

    /**
     * @param westPanel if specified this panel is added to the {@link BorderLayout#WEST} position
     * @return this ProgressDialogBuilder instance
     */
    ProgressDialogBuilder westPanel(JPanel westPanel);

    /**
     * @param buttonControls if specified buttons based on these controls are added to the {@link BorderLayout#SOUTH} position
     * @return this ProgressDialogBuilder instance
     */
    ProgressDialogBuilder buttonControls(Controls buttonControls);

    /**
     * @return a new ProgressDialog
     */
    ProgressDialog build();
  }

  /**
   * A login dialog builder
   */
  public interface LoginDialogBuilder {

    /**
     * @param owner the dialog owner
     * @return this LoginDialogBuilder instance
     */
    LoginDialogBuilder owner(Window owner);

    /**
     * @param dialogParent the dialog parent component
     * @return this LoginDialogBuilder instance
     */
    LoginDialogBuilder dialogParent(JComponent dialogParent);

    /**
     * @param defaultUser the default user credentials to display
     * @return this LoginDialogBuilder instance
     */
    LoginDialogBuilder defaultUser(User defaultUser);

    /**
     * @param validator the login validator to use
     * @return this LoginDialogBuilder instance
     */
    LoginDialogBuilder validator(LoginValidator validator);

    /**
     * @param southComponent a component to add to the south of the credentials input fields
     * @return this LoginDialogBuilder instance
     */
    LoginDialogBuilder southComponent(JComponent southComponent);

    /**
     * @param title the dialog title
     * @return this LoginDialogBuilder instance
     */
    LoginDialogBuilder title(String title);

    /**
     * @param icon the dialog icon
     * @return this LoginDialogBuilder instance
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
