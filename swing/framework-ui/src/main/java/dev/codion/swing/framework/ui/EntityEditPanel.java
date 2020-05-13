/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.Conjunction;
import org.jminor.common.db.exception.ReferentialIntegrityException;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.i18n.Messages;
import org.jminor.common.state.State;
import org.jminor.common.state.States;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.domain.entity.Entity;
import org.jminor.framework.domain.entity.exception.ValidationException;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.KeyEvents;
import org.jminor.swing.common.ui.Windows;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlList;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.dialog.DialogExceptionHandler;
import org.jminor.swing.common.ui.layout.Layouts;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.jminor.swing.common.ui.Components.hideWaitCursor;
import static org.jminor.swing.common.ui.Components.showWaitCursor;
import static org.jminor.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends EntityEditComponentPanel implements DialogExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

  /**
   * Indicates whether all entity panels should be enabled and receiving input by default<br>
   * Value type: Boolean<br>
   * Default value: false
   * @see EntityPanel#USE_FOCUS_ACTIVATION
   */
  public static final PropertyValue<Boolean> ALL_PANELS_ACTIVE = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityEditPanel.allPanelsActive", false);

  /**
   * Specifies whether edit panels should include a SAVE button (insert or update, depending on selection) or just a INSERT button<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> USE_SAVE_CONTROL = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityEditPanel.useSaveControl", true);

  /**
   * The standard controls available to the EditPanel
   */
  public enum ControlCode {
    SAVE, INSERT, UPDATE, DELETE, REFRESH, CLEAR
  }

  /**
   * Specifies whether a user confirmation is required.
   */
  public enum ConfirmRequired {
    /**
     * Specifies that a confirm is required.
     */
    YES,
    /**
     * Specifies that a confirm is not required.
     */
    NO
  }

  /**
   * The actions meriting user confirmation
   */
  protected enum ConfirmType {
    INSERT, UPDATE, DELETE
  }

  private static final String ALT_PREFIX = " (ALT-";

  /**
   * Controls mapped to their respective control codes
   */
  private final Map<ControlCode, Control> controls = new EnumMap<>(ControlCode.class);

  /**
   * Indicates whether the panel is active and ready to receive input
   */
  private final State activeState = States.state(ALL_PANELS_ACTIVE.get());

  /**
   * The mechanism for restricting a single active EntityEditPanel at a time
   */
  private static final State.Group ACTIVE_STATE_GROUP = States.group();

  /**
   * Indicates whether or not the UI should be cleared after insert has been performed
   */
  private boolean clearAfterInsert = true;

  /**
   * Indicates whether or not the UI should request focus after insert has been performed
   * @see #requestInitialFocus()
   */
  private boolean requestFocusAfterInsert = true;

  /**
   * True after {@link #initializePanel()} has been called
   */
  private boolean panelInitialized = false;

  /**
   * The action to take when a referential integrity error occurs on delete
   */
  private ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling =
          ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   */
  public EntityEditPanel(final SwingEntityEditModel editModel) {
    this(editModel, (USE_SAVE_CONTROL.get() ? ControlCode.SAVE : ControlCode.INSERT), ControlCode.UPDATE, ControlCode.DELETE,
            ControlCode.CLEAR, ControlCode.REFRESH);
  }

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   * @param controlCodes if specified only controls with those keys are initialized,
   * null or an empty String array will result in no controls being initialized
   */
  public EntityEditPanel(final SwingEntityEditModel editModel, final ControlCode... controlCodes) {
    super(editModel);
    if (!ALL_PANELS_ACTIVE.get()) {
      ACTIVE_STATE_GROUP.addState(activeState);
    }
    setupControls(controlCodes);
    bindEventsInternal();
  }

  @Override
  public final String toString() {
    return getEditModel().toString();
  }

  /**
   * @param listener a listener notified each time the active state changes
   */
  public final void addActiveListener(final EventDataListener<Boolean> listener) {
    activeState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeActiveListener(final EventDataListener<Boolean> listener) {
    activeState.removeDataListener(listener);
  }

  /**
   * @return true if this edit panel is active, enabled and ready to receive input
   */
  public final boolean isActive() {
    return activeState.get();
  }

  /**
   * Sets the active state of this edit panel, an active edit panel should be
   * enabled and ready to receive input
   * @param active the active state
   */
  public final void setActive(final boolean active) {
    activeState.set(active);
  }

  /**
   * Clears the underlying edit model and requests the initial focus.
   * @see EntityEditModel#setEntity(Entity)
   * @see #requestInitialFocus()
   */
  public final void clearAndRequestFocus() {
    getEditModel().setEntity(null);
    requestInitialFocus();
  }

  /**
   * @return true if the UI should be cleared after insert has been performed
   */
  public final boolean isClearAfterInsert() {
    return clearAfterInsert;
  }

  /**
   * @param clearAfterInsert true if the UI should be cleared after insert has been performed
   */
  public final void setClearAfterInsert(final boolean clearAfterInsert) {
    this.clearAfterInsert = clearAfterInsert;
  }

  /**
   * @return true if the UI should request focus after insert has been performed
   * @see #requestInitialFocus()
   */
  public final boolean isRequestFocusAfterInsert() {
    return requestFocusAfterInsert;
  }

  /**
   * @param requestFocusAfterInsert true if the UI should request focus after insert has been performed
   * @see #requestInitialFocus()
   */
  public final void setRequestFocusAfterInsert(final boolean requestFocusAfterInsert) {
    this.requestFocusAfterInsert = requestFocusAfterInsert;
  }

  /**
   * @param referentialIntegrityErrorHandling the action to take on a referential integrity error on delete
   */
  public final void setReferentialIntegrityErrorHandling(final ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
    this.referentialIntegrityErrorHandling = referentialIntegrityErrorHandling;
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode}
   * @throws IllegalArgumentException in case no control is associated with the given control code
   */
  public final Control getControl(final ControlCode controlCode) {
    if (!controls.containsKey(controlCode)) {
      throw new IllegalArgumentException(controlCode + " control not available in panel: " + this);
    }

    return controls.get(controlCode);
  }

  /**
   * @return a control for refreshing the model data
   */
  public final Control getRefreshControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.REFRESH_MNEMONIC);
    return Controls.control(getEditModel()::refresh, FrameworkMessages.get(FrameworkMessages.REFRESH),
            activeState, FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + ALT_PREFIX
                    + mnemonic + ")", mnemonic.charAt(0), null, frameworkIcons().refresh());
  }

  /**
   * @return a control for deleting the active entity
   */
  public final Control getDeleteControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC);
    return Controls.control(this::delete, FrameworkMessages.get(FrameworkMessages.DELETE),
            States.aggregateState(Conjunction.AND,
                    activeState,
                    getEditModel().getDeleteEnabledObserver(),
                    getEditModel().getEntityNewObserver().getReversedObserver()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP) + ALT_PREFIX + mnemonic + ")", mnemonic.charAt(0), null,
            frameworkIcons().delete());
  }

  /**
   * @return a control for clearing the UI controls
   */
  public final Control getClearControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC);
    return Controls.control(this::clearAndRequestFocus, FrameworkMessages.get(FrameworkMessages.CLEAR),
            activeState, FrameworkMessages.get(FrameworkMessages.CLEAR_ALL_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, frameworkIcons().clear());
  }

  /**
   * @return a control for performing an update on the active entity
   */
  public final Control getUpdateControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC);
    return Controls.control(this::update, FrameworkMessages.get(FrameworkMessages.UPDATE),
            States.aggregateState(Conjunction.AND,
                    activeState,
                    getEditModel().getUpdateEnabledObserver(),
                    getEditModel().getEntityNewObserver().getReversedObserver(),
                    getEditModel().getModifiedObserver()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_TIP) + ALT_PREFIX + mnemonic + ")", mnemonic.charAt(0),
            null, frameworkIcons().update());
  }

  /**
   * @return a control for performing an insert on the active entity
   */
  public final Control getInsertControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC);
    return Controls.control(this::insert, FrameworkMessages.get(FrameworkMessages.INSERT),
            States.aggregateState(Conjunction.AND, activeState, getEditModel().getInsertEnabledObserver()),
            FrameworkMessages.get(FrameworkMessages.INSERT_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, frameworkIcons().add());
  }

  /**
   * @return a control for performing a save on the active entity, that is, update if an entity
   * is selected and modified or insert otherwise
   */
  public final Control getSaveControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.SAVE_MNEMONIC);
    final State insertUpdateState = States.aggregateState(Conjunction.OR, getEditModel().getInsertEnabledObserver(),
            States.aggregateState(Conjunction.AND, getEditModel().getUpdateEnabledObserver(),
                    getEditModel().getModifiedObserver()));
    return Controls.control(this::save, FrameworkMessages.get(FrameworkMessages.SAVE),
            States.aggregateState(Conjunction.AND, activeState, insertUpdateState),
            FrameworkMessages.get(FrameworkMessages.SAVE_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, frameworkIcons().add());
  }

  /**
   * Handles the given exception. If the referential error handling is {@link ReferentialIntegrityErrorHandling#DEPENDENCIES}, the dependencies of the given entity are displayed
   * to the user, otherwise {@link #onException(Exception)} is called.
   * @param exception the exception
   * @param entity the entity causing the exception
   * @see #setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  public void onReferentialIntegrityException(final ReferentialIntegrityException exception, final Entity entity) {
    if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DEPENDENCIES) {
      EntityTablePanel.showDependenciesDialog(singletonList(entity), getEditModel().getConnectionProvider(), this);
    }
    else {
      onException(exception);
    }
  }

  /**
   * Displays the exception message after which the component involved receives the focus.
   * @param exception the exception
   */
  public void onValidationException(final ValidationException exception) {
    JOptionPane.showMessageDialog(this, exception.getMessage(),
            Messages.get(Messages.EXCEPTION), JOptionPane.ERROR_MESSAGE);
    requestComponentFocus(exception.getPropertyId());
  }

  /**
   * Handles the given exception, simply displays the error message to the user by default.
   * @param exception the exception to handle
   * @see #displayException(Throwable, Window)
   */
  public void onException(final Exception exception) {
    displayException(exception, Windows.getParentWindow(this));
  }

  @Override
  public final void displayException(final Throwable throwable, final Window dialogParent) {
    DefaultDialogExceptionHandler.getInstance().displayException(throwable, dialogParent);
  }

  /**
   * Initializes a horizontally laid out control panel, that is, the panel containing buttons for editing entities (Insert, Update...)
   * @return the control panel, null if no controls are defined
   * @see #initializeControlPanelControls()
   */
  public final JPanel createHorizontalControlPanel() {
    return createControlPanel(true);
  }

  /**
   * Initializes a vertically laid out control panel, that is, the panel containing buttons for editing entities (Insert, Update...)
   * @return the control panel, null if no controls are defined
   * @see #initializeControlPanelControls()
   */
  public final JPanel createVerticalControlPanel() {
    return createControlPanel(false);
  }

  /**
   * Initializes the control toolbar, that is, the toolbar containing buttons for editing entities (Insert, Update...)
   * @param orientation the orientation
   * @return the control toolbar, null if no controls are defined
   * @see #initializeControlPanelControls()
   */
  public final JToolBar createControlToolBar(final int orientation) {
    final ControlList controlPanelControls = initializeControlPanelControls();
    if (controlPanelControls.size() == 0) {
      return null;
    }
    return ControlProvider.createToolBar(controlPanelControls, orientation);
  }

  /**
   * Initializes this EntityEditPanel UI.
   * This method marks this panel as initialized which prevents it from running again,
   * whether an exception occurs or not.
   * @return this EntityEditPanel instance
   * @see #isPanelInitialized()
   */
  public final EntityEditPanel initializePanel() {
    if (!panelInitialized) {
      try {
        showWaitCursor(this);
        initializeUI();
      }
      finally {
        panelInitialized = true;
        hideWaitCursor(this);
      }
    }

    return this;
  }

  /**
   * @return true if the method initializePanel() has been called on this EntityEditPanel instance
   * @see #initializePanel()
   */
  public final boolean isPanelInitialized() {
    return panelInitialized;
  }

  //#############################################################################################
  // Begin - control methods, see setupControls
  //#############################################################################################

  /**
   * Saves the active entity, that is, if no entity is selected it performs a insert otherwise the user
   * is asked whether to update the selected entity or insert a new one
   */
  public final void save() {
    if (getEditModel().isEntityNew() || !getEditModel().isModified() || !getEditModel().isUpdateEnabled()) {
      //no entity selected, selected entity is unmodified or update is not enabled, can only insert
      insert();
    }
    else {//possibly update
      final int choiceIdx = JOptionPane.showOptionDialog(this, FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT),
              FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT_TITLE), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_RECORD),
                      FrameworkMessages.get(FrameworkMessages.INSERT_NEW), Messages.get(Messages.CANCEL)},
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE)});
      if (choiceIdx == 0) {//update
        update(ConfirmRequired.NO);
      }
      else if (choiceIdx == 1) {//insert
        insert(ConfirmRequired.NO);
      }
    }
  }

  /**
   * Performs a insert on the active entity
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insert() {
    return insert(ConfirmRequired.YES);
  }

  /**
   * Performs a insert on the active entity
   * @param confirmRequired specifies whether a user confirmation is required.
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insert(final ConfirmRequired confirmRequired) {
    try {
      if (confirmRequired == ConfirmRequired.NO || confirmInsert()) {
        validateData();
        try {
          showWaitCursor(this);
          getEditModel().insert();
        }
        finally {
          hideWaitCursor(this);
        }
        if (clearAfterInsert) {
          getEditModel().setEntity(null);
        }
        if (requestFocusAfterInsert) {
          requestAfterInsertFocus();
        }
        return true;
      }
    }
    catch (final ValidationException e) {
      LOG.debug(e.getMessage(), e);
      onValidationException(e);
    }
    catch (final Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }

    return false;
  }

  /**
   * Performs a delete on the active entity
   * @return true if the delete operation was successful
   */
  public final boolean delete() {
    return delete(ConfirmRequired.YES);
  }

  /**
   * Performs a delete on the active entity
   * @param confirmRequired specifies whether a user confirmation is required.
   * @return true if the delete operation was successful
   */
  public final boolean delete(final ConfirmRequired confirmRequired) {
    try {
      if (confirmRequired == ConfirmRequired.NO || confirmDelete()) {
        try {
          showWaitCursor(this);
          getEditModel().delete();
        }
        finally {
          hideWaitCursor(this);
        }

        return true;
      }
    }
    catch (final ReferentialIntegrityException e) {
      LOG.debug(e.getMessage(), e);
      onReferentialIntegrityException(e, getEditModel().getEntityCopy());
    }
    catch (final Exception ex) {
      LOG.error(ex.getMessage(), ex);
      onException(ex);
    }

    return false;
  }

  /**
   * Performs an update on the active entity
   * @return true if the update operation was successful
   */
  public final boolean update() {
    return update(ConfirmRequired.YES);
  }

  /**
   * Performs an update on the active entity
   * @param confirmRequired specifies whether a user confirmation is required.
   * @return true if the update operation was successful or if no update was required
   */
  public final boolean update(final ConfirmRequired confirmRequired) {
    try {
      if (confirmRequired == ConfirmRequired.NO || confirmUpdate()) {
        validateData();
        try {
          showWaitCursor(this);
          getEditModel().update();
        }
        finally {
          hideWaitCursor(this);
        }
        requestInitialFocus();

        return true;
      }
    }
    catch (final ValidationException e) {
      LOG.debug(e.getMessage(), e);
      onValidationException(e);
    }
    catch (final Exception ex) {
      LOG.error(ex.getMessage(), ex);
      onException(ex);
    }

    return false;
  }

  //#############################################################################################
  // End - control methods
  //#############################################################################################

  /**
   * Override to add UI level validation, called before insert/update
   * @throws ValidationException in case of a validation failure
   */
  protected void validateData() throws ValidationException {}

  /**
   * Called before a insert is performed, the default implementation simply returns true
   * @return true if a insert should be performed, false if it should be vetoed
   */
  protected boolean confirmInsert() {
    return true;
  }

  /**
   * Called before a delete is performed, if true is returned the delete action is performed otherwise it is cancelled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    final String[] messages = getConfirmationMessages(ConfirmType.DELETE);
    return confirm(messages[0], messages[1]);
  }

  /**
   * Called before an update is performed, if true is returned the update action is performed otherwise it is cancelled
   * @return true if the update action should be performed
   */
  protected boolean confirmUpdate() {
    final String[] messages = getConfirmationMessages(ConfirmType.UPDATE);
    return confirm(messages[0], messages[1]);
  }

  /**
   * Presents a OK/Cancel confirm dialog with the given message and title,
   * returns true if OK was selected.
   * @param message the message
   * @param title the dialog title
   * @return true if OK was selected
   */
  protected boolean confirm(final String message, final String title) {
    final int res = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @param type the confirmation message type
   * @return a string array containing two elements, the element at index 0 is used
   * as the message displayed in the dialog and the element at index 1 is used as the dialog title,
   * i.e. ["Are you sure you want to delete the selected records?", "About to delete selected records"]
   */
  protected String[] getConfirmationMessages(final ConfirmType type) {
    switch (type) {
      case DELETE:
        return new String[] {FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_ENTITY),
                FrameworkMessages.get(FrameworkMessages.DELETE)};
      case INSERT:
        return new String[] {FrameworkMessages.get(FrameworkMessages.CONFIRM_INSERT),
                FrameworkMessages.get(FrameworkMessages.INSERT)};
      case UPDATE:
        return new String[] {FrameworkMessages.get(FrameworkMessages.CONFIRM_UPDATE),
                FrameworkMessages.get(FrameworkMessages.UPDATE)};
      default:
        throw new IllegalArgumentException("Unknown confirmation type constant: " + type);
    }
  }

  /**
   * Associates {@code control} with {@code controlCode}
   * @param controlCode the control code
   * @param control the control to associate with {@code controlCode}
   */
  protected final void setControl(final ControlCode controlCode, final Control control) {
    if (control == null) {
      controls.remove(controlCode);
    }
    else {
      controls.put(controlCode, control);
    }
  }

  /**
   * Initializes a ControlList on which to base the control panel
   * @return the ControlList on which to base the control panel
   */
  protected ControlList initializeControlPanelControls() {
    final ControlList controlPanelControls = Controls.controlList("Actions");
    if (this.controls.containsKey(ControlCode.SAVE)) {
      controlPanelControls.add(this.controls.get(ControlCode.SAVE));
    }
    if (this.controls.containsKey(ControlCode.INSERT)) {
      controlPanelControls.add(this.controls.get(ControlCode.INSERT));
    }
    if (this.controls.containsKey(ControlCode.UPDATE)) {
      controlPanelControls.add(this.controls.get(ControlCode.UPDATE));
    }
    if (this.controls.containsKey(ControlCode.DELETE)) {
      controlPanelControls.add(this.controls.get(ControlCode.DELETE));
    }
    if (this.controls.containsKey(ControlCode.CLEAR)) {
      controlPanelControls.add(this.controls.get(ControlCode.CLEAR));
    }
    if (this.controls.containsKey(ControlCode.REFRESH)) {
      controlPanelControls.add(this.controls.get(ControlCode.REFRESH));
    }

    return controlPanelControls;
  }

  /**
   * Initializes this EntityEditPanel UI, that is, creates and lays out the components
   * required for editing the underlying entity type.
   * <pre>
   *   protected void initializeUI() {
   *      createTextField(DomainModel.USER_NAME);
   *      createTextField(DomainModel.USER_ADDRESS);
   *      setLayout(new GridLayout(2, 1, 5, 5);
   *      addPropertyPanel(DomainModel.USER_NAME);
   *      addPropertyPanel(DomainModel.USER_ADDRESS);
   *  }
   * </pre>
   */
  protected abstract void initializeUI();

  /**
   * Initializes the controls available to this EntityEditPanel by mapping them to their respective
   * control codes ({@link ControlCode#INSERT}, {@link ControlCode#UPDATE} etc)
   * via the {@code setControl(String, Control) method, these can then be retrieved via the {@link #getControl(ControlCode)} method.
   * @param controlCodes the control codes for which controls should be initialized
   * @see org.jminor.swing.common.ui.control.Control
   * @see #setControl(ControlCode, org.jminor.swing.common.ui.control.Control)
   * @see #getControl(ControlCode)
   * todo updateEnabled(false) þá vantar Insert control nema það sé tiltekið í smið
   */
  private void setupControls(final ControlCode... controlCodes) {
    if (controlCodes == null || controlCodes.length == 0) {
      return;
    }
    final Collection<ControlCode> codes = asList(controlCodes);
    if (!getEditModel().isReadOnly()) {
      setupEditControls(codes);
    }
    if (codes.contains(ControlCode.CLEAR)) {
      setControl(ControlCode.CLEAR, getClearControl());
    }
    if (codes.contains(ControlCode.REFRESH)) {
      setControl(ControlCode.REFRESH, getRefreshControl());
    }
  }

  private void setupEditControls(final Collection<ControlCode> controlCodes) {
    if (getEditModel().isInsertEnabled() && getEditModel().isUpdateEnabled() && controlCodes.contains(ControlCode.SAVE)) {
      setControl(ControlCode.SAVE, getSaveControl());
    }
    if (getEditModel().isInsertEnabled() && controlCodes.contains(ControlCode.INSERT)) {
      setControl(ControlCode.INSERT, getInsertControl());
    }
    if (getEditModel().isUpdateEnabled() && controlCodes.contains(ControlCode.UPDATE)) {
      setControl(ControlCode.UPDATE, getUpdateControl());
    }
    if (getEditModel().isDeleteEnabled() && controlCodes.contains(ControlCode.DELETE)) {
      setControl(ControlCode.DELETE, getDeleteControl());
    }
  }

  private JPanel createControlPanel(final boolean horizontal) {
    final ControlList controlPanelControls = initializeControlPanelControls();
    if (controlPanelControls.size() == 0) {
      return null;
    }
    if (horizontal) {
      final JPanel panel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
      panel.add(ControlProvider.createHorizontalButtonPanel(controlPanelControls));

      return panel;
    }
    final JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(ControlProvider.createVerticalButtonPanel(controlPanelControls), BorderLayout.NORTH);

    return panel;
  }

  private void bindEventsInternal() {
    KeyEvents.addKeyEvent(this, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, Controls.control(this::showEntityMenu,
                    "EntityEditPanel.showEntityMenu"));
    getEditModel().addBeforeRefreshListener(() -> showWaitCursor(EntityEditPanel.this));
    getEditModel().addAfterRefreshListener(() -> hideWaitCursor(EntityEditPanel.this));
    getEditModel().addConfirmSetEntityObserver(confirmationState -> {
      final int result = JOptionPane.showConfirmDialog(Windows.getParentWindow(EntityEditPanel.this),
              FrameworkMessages.get(FrameworkMessages.UNSAVED_DATA_WARNING), FrameworkMessages.get(FrameworkMessages.UNSAVED_DATA_WARNING_TITLE),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      confirmationState.set(result == JOptionPane.YES_OPTION);
    });
  }

  private void showEntityMenu() {
    new EntityPopupMenu(getEditModel().getEntityCopy(), getEditModel().getConnectionProvider()).show(this, 0, 0);
  }
}
