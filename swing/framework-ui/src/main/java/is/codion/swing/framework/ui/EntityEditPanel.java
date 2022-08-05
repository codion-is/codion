/*
 * Copyright (c) 2009 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.event.EventDataListener;
import is.codion.common.i18n.Messages;
import is.codion.common.properties.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import static is.codion.swing.framework.ui.icons.FrameworkIcons.frameworkIcons;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends EntityEditComponentPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

  private static final ResourceBundle TABLE_PANEL_MESSAGES = ResourceBundle.getBundle(EntityTablePanel.class.getName());

  /**
   * Specifies whether edit panels should be activated when the panel (or its parent EntityPanel) receives focus<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> USE_FOCUS_ACTIVATION =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityEditPanel.useFocusActivation", true);

  /**
   * Specifies whether the add/insert button caption should be 'Save' (mnemonic S), instead of 'Add' (mnemonic A)<br>
   * Value type: Boolean<br>
   * Default value: false
   */
  public static final PropertyValue<Boolean> USE_SAVE_CAPTION =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityEditPanel.useSaveCaption", false);

  /**
   * The standard controls available to the EditPanel
   */
  public enum ControlCode {
    INSERT, UPDATE, DELETE, REFRESH, CLEAR
  }

  /**
   * The actions meriting user confirmation
   */
  protected enum ConfirmType {
    INSERT, UPDATE, DELETE
  }

  private static final String ALT_PREFIX = " (ALT-";

  /**
   * The mechanism for restricting a single active EntityEditPanel at a time
   */
  private static final State.Group ACTIVE_STATE_GROUP = State.group();

  /**
   * The controls this edit panel should include
   */
  private final Set<ControlCode> controlCodes;

  /**
   * Controls mapped to their respective control codes
   */
  private final Map<ControlCode, Control> controls = new EnumMap<>(ControlCode.class);

  /**
   * Indicates whether the panel is active and ready to receive input
   */
  private final State activeState = State.state(!USE_FOCUS_ACTIVATION.get());

  /**
   * Indicates whether the UI should be cleared after insert has been performed
   */
  private boolean clearAfterInsert = true;

  /**
   * Indicates whether the UI should request focus after insert has been performed
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
  public EntityEditPanel(SwingEntityEditModel editModel) {
    this(editModel, ControlCode.INSERT, ControlCode.UPDATE, ControlCode.DELETE, ControlCode.CLEAR, ControlCode.REFRESH);
  }

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   * @param controlCodes if specified only controls with those keys are initialized,
   * null or an empty String array will result in no controls being initialized
   */
  public EntityEditPanel(SwingEntityEditModel editModel, ControlCode... controlCodes) {
    super(editModel);
    if (USE_FOCUS_ACTIVATION.get()) {
      ACTIVE_STATE_GROUP.addState(activeState);
    }
    this.controlCodes = controlCodes == null ? emptySet() : new HashSet<>(Arrays.asList(controlCodes));
    editModel.setDefaultValues();
  }

  @Override
  public final String toString() {
    return getEditModel().toString();
  }

  /**
   * @param listener a listener notified each time the active state changes
   */
  public final void addActiveListener(EventDataListener<Boolean> listener) {
    activeState.addDataListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public final void removeActiveListener(EventDataListener<Boolean> listener) {
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
  public final void setActive(boolean active) {
    activeState.set(active);
  }

  /**
   * @return a {@link StateObserver} indicating whether this panel is active
   */
  public final StateObserver getActiveObserver() {
    return activeState.observer();
  }

  /**
   * Clears the underlying edit model and requests the initial focus.
   * @see EntityEditModel#setEntity(Entity)
   * @see #requestInitialFocus()
   */
  public final void clearAndRequestFocus() {
    getEditModel().setDefaultValues();
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
  public final void setClearAfterInsert(boolean clearAfterInsert) {
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
  public final void setRequestFocusAfterInsert(boolean requestFocusAfterInsert) {
    this.requestFocusAfterInsert = requestFocusAfterInsert;
  }

  /**
   * @param referentialIntegrityErrorHandling the action to take on a referential integrity error on delete
   */
  public final void setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
    this.referentialIntegrityErrorHandling = referentialIntegrityErrorHandling;
  }

  /**
   * @param controlCode the control code
   * @return true if this edit panel contains the given control
   */
  public final boolean containsControl(ControlCode controlCode) {
    return controls.containsKey(requireNonNull(controlCode));
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode}
   * @throws IllegalArgumentException in case no control is associated with the given control code
   * @see #containsControl(EntityEditPanel.ControlCode)
   */
  public final Control getControl(ControlCode controlCode) {
    if (!containsControl(controlCode)) {
      throw new IllegalArgumentException(controlCode + " control not available in panel: " + this);
    }

    return controls.get(controlCode);
  }

  /**
   * Handles the given exception. If the referential integrity error handling is {@link ReferentialIntegrityErrorHandling#DEPENDENCIES},
   * the dependencies of the given entity are displayed to the user, otherwise {@link #onException(Throwable)} is called.
   * @param exception the exception
   * @param entity the entity causing the exception
   * @see #setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  public void onReferentialIntegrityException(ReferentialIntegrityException exception, Entity entity) {
    requireNonNull(exception);
    requireNonNull(entity);
    if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DEPENDENCIES) {
      EntityTablePanel.showDependenciesDialog(singletonList(entity), getEditModel().getConnectionProvider(),
              this, TABLE_PANEL_MESSAGES.getString("unknown_dependent_records"));
    }
    else {
      onException(exception);
    }
  }

  /**
   * Displays the exception message after which the component involved receives the focus.
   * @param exception the exception
   */
  public void onValidationException(ValidationException exception) {
    JOptionPane.showMessageDialog(this, exception.getMessage(),
            Messages.error(), JOptionPane.ERROR_MESSAGE);
    requestComponentFocus(exception.getAttribute());
  }

  /**
   * Creates a horizontally laid out control panel, that is, the panel containing buttons for editing entities (Insert, Update...)
   * @return the control panel, null if no controls are defined
   * @see #createControlPanelControls()
   */
  public final JPanel createHorizontalControlPanel() {
    return createControlPanel(true);
  }

  /**
   * Creates a vertically laid out control panel, that is, the panel containing buttons for editing entities (Insert, Update...)
   * @return the control panel, null if no controls are defined
   * @see #createControlPanelControls()
   */
  public final JPanel createVerticalControlPanel() {
    return createControlPanel(false);
  }

  /**
   * Creates the control toolbar, that is, the toolbar containing buttons for editing entities (Insert, Update...)
   * @param orientation the orientation
   * @return the control toolbar, null if no controls are defined
   * @see #createControlPanelControls()
   * @see SwingConstants#VERTICAL
   * @see SwingConstants#HORIZONTAL
   */
  public final JToolBar createControlToolBar(int orientation) {
    Controls controlPanelControls = createControlPanelControls();
    if (controlPanelControls.isEmpty()) {
      return null;
    }
    if (orientation == SwingConstants.VERTICAL) {
      return controlPanelControls.createVerticalToolBar();
    }
    else if (orientation == SwingConstants.HORIZONTAL) {
      return controlPanelControls.createHorizontalToolBar();
    }

    throw new IllegalArgumentException("Unknown orientation value: " + orientation);
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
      WaitCursor.show(this);
      try {
        setupControls();
        bindEventsInternal();
        initializeUI();
      }
      finally {
        panelInitialized = true;
        WaitCursor.hide(this);
      }
    }

    return this;
  }

  /**
   * @return true if the method {@link #initializePanel()} has been called on this EntityEditPanel instance
   * @see #initializePanel()
   */
  public final boolean isPanelInitialized() {
    return panelInitialized;
  }

  /**
   * Performs insert on the active entity after asking for confirmation via {@link #confirmInsert()}.
   * Note that {@link #confirmInsert()} returns true by default, so it needs to be overridden to ask for confirmation.
   * @return true in case of successful insert, false otherwise
   * @see #confirmInsert()
   */
  public final boolean insert() {
    if (confirmInsert()) {
      return insertWithoutConfirmation();
    }

    return false;
  }

  /**
   * Performs insert on the active entity without asking for confirmation
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insertWithoutConfirmation() {
    try {
      validateData();
      WaitCursor.show(this);
      try {
        getEditModel().insert();
        if (clearAfterInsert) {
          getEditModel().setDefaultValues();
        }
        if (requestFocusAfterInsert) {
          requestAfterInsertFocus();
        }

        return true;
      }
      finally {
        WaitCursor.hide(this);
      }
    }
    catch (ValidationException e) {
      LOG.debug(e.getMessage(), e);
      onValidationException(e);
    }
    catch (Exception e) {
      LOG.error(e.getMessage(), e);
      onException(e);
    }

    return false;
  }

  /**
   * Performs delete on the active entity after asking for confirmation via {@link #confirmDelete()}.
   * @return true if the delete operation was successful
   */
  public final boolean delete() {
    if (confirmDelete()) {
      return deleteWithoutConfirmation();
    }

    return false;
  }

  /**
   * Performs delete on the active entity without asking for confirmation
   * @return true if the delete operation was successful
   */
  public final boolean deleteWithoutConfirmation() {
    try {
      WaitCursor.show(this);
      try {
        getEditModel().delete();
        requestInitialFocus();

        return true;
      }
      finally {
        WaitCursor.hide(this);
      }
    }
    catch (ReferentialIntegrityException e) {
      LOG.debug(e.getMessage(), e);
      onReferentialIntegrityException(e, getEditModel().getEntityCopy());
    }
    catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      onException(ex);
    }

    return false;
  }

  /**
   * Performs update on the active entity after asking for confirmation via {@link #confirmUpdate()}.
   * @return true if the update operation was successful
   */
  public final boolean update() {
    if (confirmUpdate()) {
      return updateWithoutConfirmation();
    }

    return false;
  }

  /**
   * Performs update on the active entity without asking for confirmation.
   * @return true if the update operation was successful or if no update was required
   */
  public final boolean updateWithoutConfirmation() {
    try {
      validateData();
      WaitCursor.show(this);
      try {
        getEditModel().update();
        requestInitialFocus();

        return true;
      }
      finally {
        WaitCursor.hide(this);
      }
    }
    catch (ValidationException e) {
      LOG.debug(e.getMessage(), e);
      onValidationException(e);
    }
    catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      onException(ex);
    }

    return false;
  }

  /**
   * Override to add UI level validation, called before insert/update
   * @throws ValidationException in case of a validation failure
   */
  protected void validateData() throws ValidationException {}

  /**
   * Called before insert is performed, the default implementation simply returns true
   * @return true if insert should be performed, false if it should be vetoed
   */
  protected boolean confirmInsert() {
    return true;
  }

  /**
   * Called before delete is performed, if true is returned the delete action is performed otherwise it is cancelled
   * @return true if the delete action should be performed
   */
  protected boolean confirmDelete() {
    String[] messages = getConfirmationMessages(ConfirmType.DELETE);
    return confirm(messages[0], messages[1]);
  }

  /**
   * Called before an update is performed, if true is returned the update action is performed otherwise it is cancelled
   * @return true if the update action should be performed
   */
  protected boolean confirmUpdate() {
    String[] messages = getConfirmationMessages(ConfirmType.UPDATE);
    return confirm(messages[0], messages[1]);
  }

  /**
   * Presents an OK/Cancel confirm dialog with the given message and title,
   * returns true if OK was selected.
   * @param message the message
   * @param title the dialog title
   * @return true if OK was selected
   */
  protected boolean confirm(String message, String title) {
    int res = JOptionPane.showConfirmDialog(this, message, title, JOptionPane.OK_CANCEL_OPTION);

    return res == JOptionPane.OK_OPTION;
  }

  /**
   * @param type the confirmation message type
   * @return a string array containing two elements, the element at index 0 is used
   * as the message displayed in the dialog and the element at index 1 is used as the dialog title,
   * i.e. ["Are you sure you want to delete the selected records?", "About to delete selected records"]
   */
  protected String[] getConfirmationMessages(ConfirmType type) {
    switch (type) {
      case DELETE:
        return new String[] {FrameworkMessages.confirmDelete(), FrameworkMessages.delete()};
      case INSERT:
        return new String[] {FrameworkMessages.confirmInsert(), FrameworkMessages.add()};
      case UPDATE:
        return new String[] {FrameworkMessages.confirmUpdate(), FrameworkMessages.update()};
      default:
        throw new IllegalArgumentException("Unknown confirmation type constant: " + type);
    }
  }

  /**
   * Associates {@code control} with {@code controlCode}
   * @param controlCode the control code
   * @param control the control to associate with {@code controlCode}, null for none
   * @throws IllegalStateException in case the panel has already been initialized
   */
  protected final void setControl(ControlCode controlCode, Control control) {
    if (panelInitialized) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
    requireNonNull(controlCode);
    if (control == null) {
      controls.remove(controlCode);
    }
    else {
      controls.put(controlCode, control);
    }
  }

  /**
   * Creates a Controls instance on which to base the control panel
   * @return the Controls on which to base the control panel
   */
  protected Controls createControlPanelControls() {
    Controls controlPanelControls = Controls.controls();
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
   *      addInputPanel(DomainModel.USER_NAME);
   *      addInputPanel(DomainModel.USER_ADDRESS);
   *  }
   * </pre>
   */
  protected abstract void initializeUI();

  /**
   * Initializes the controls available to this EntityEditPanel by mapping them to their respective
   * control codes ({@link ControlCode#INSERT}, {@link ControlCode#UPDATE} etc.)
   * via the {@code setControl(String, Control) method, these can then be retrieved via the {@link #getControl(ControlCode)} method.
   * @see is.codion.swing.common.ui.control.Control
   * @see #setControl(ControlCode, is.codion.swing.common.ui.control.Control)
   * @see #getControl(ControlCode)
   */
  private void setupControls() {
    if (!getEditModel().isReadOnly()) {
      setupEditControls();
    }
    if (controlCodes.contains(ControlCode.CLEAR)) {
      controls.putIfAbsent(ControlCode.CLEAR, createClearControl());
    }
    if (controlCodes.contains(ControlCode.REFRESH)) {
      controls.putIfAbsent(ControlCode.REFRESH, createRefreshControl());
    }
  }

  private void setupEditControls() {
    if (getEditModel().isInsertEnabled() && controlCodes.contains(ControlCode.INSERT)) {
      controls.putIfAbsent(ControlCode.INSERT, createInsertControl());
    }
    if (getEditModel().isUpdateEnabled() && controlCodes.contains(ControlCode.UPDATE)) {
      controls.putIfAbsent(ControlCode.UPDATE, createUpdateControl());
    }
    if (getEditModel().isDeleteEnabled() && controlCodes.contains(ControlCode.DELETE)) {
      controls.putIfAbsent(ControlCode.DELETE, createDeleteControl());
    }
  }

  private Control createRefreshControl() {
    return Control.builder(getEditModel()::refresh)
            .caption(FrameworkMessages.refresh())
            .enabledState(State.and(activeState, getEditModel().getRefreshingObserver().reversedObserver()))
            .description(FrameworkMessages.refreshTip() + ALT_PREFIX + FrameworkMessages.refreshMnemonic() + ")")
            .mnemonic(FrameworkMessages.refreshMnemonic())
            .smallIcon(frameworkIcons().refresh())
            .build();
  }

  private Control createDeleteControl() {
    return Control.builder(this::delete)
            .caption(FrameworkMessages.delete())
            .enabledState(State.and(activeState,
                    getEditModel().getDeleteEnabledObserver(),
                    getEditModel().getEntityNewObserver().reversedObserver()))
            .description(FrameworkMessages.deleteCurrentTip() + ALT_PREFIX + FrameworkMessages.deleteMnemonic() + ")")
            .mnemonic(FrameworkMessages.deleteMnemonic())
            .smallIcon(frameworkIcons().delete())
            .build();
  }

  private Control createClearControl() {
    return Control.builder(this::clearAndRequestFocus)
            .caption(FrameworkMessages.clear())
            .enabledState(activeState)
            .description(FrameworkMessages.clearTip() + ALT_PREFIX + FrameworkMessages.clearMnemonic() + ")")
            .mnemonic(FrameworkMessages.clearMnemonic())
            .smallIcon(frameworkIcons().clear())
            .build();
  }

  private Control createUpdateControl() {
    return Control.builder(this::update)
            .caption(FrameworkMessages.update())
            .enabledState(State.and(activeState,
                    getEditModel().getUpdateEnabledObserver(),
                    getEditModel().getEntityNewObserver().reversedObserver(),
                    getEditModel().getModifiedObserver()))
            .description(FrameworkMessages.updateTip() + ALT_PREFIX + FrameworkMessages.updateMnemonic() + ")")
            .mnemonic(FrameworkMessages.updateMnemonic())
            .smallIcon(frameworkIcons().update())
            .build();
  }

  private Control createInsertControl() {
    boolean useSaveCaption = USE_SAVE_CAPTION.get();
    char mnemonic = useSaveCaption ? FrameworkMessages.saveMnemonic() : FrameworkMessages.addMnemonic();
    String caption = useSaveCaption ? FrameworkMessages.save() : FrameworkMessages.add();
    return Control.builder(this::insert)
            .caption(caption)
            .enabledState(State.and(activeState, getEditModel().getInsertEnabledObserver()))
            .description(FrameworkMessages.addTip() + ALT_PREFIX + mnemonic + ")")
            .mnemonic(mnemonic)
            .smallIcon(frameworkIcons().add())
            .build();
  }

  private JPanel createControlPanel(boolean horizontal) {
    Controls controlPanelControls = createControlPanelControls();
    if (controlPanelControls.isEmpty()) {
      return null;
    }
    if (horizontal) {
      JPanel panel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
      panel.add(controlPanelControls.createHorizontalButtonPanel());

      return panel;
    }
    JPanel panel = new JPanel(Layouts.borderLayout());
    panel.add(controlPanelControls.createVerticalButtonPanel(), BorderLayout.NORTH);

    return panel;
  }

  private void bindEventsInternal() {
    KeyEvents.builder(KeyEvent.VK_V)
            .modifiers(InputEvent.CTRL_DOWN_MASK + InputEvent.ALT_DOWN_MASK)
            .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .action(Control.control(this::showEntityMenu))
            .enable(this);
    getEditModel().getRefreshingObserver().addDataListener(this::onRefreshingChanged);
    getEditModel().addConfirmSetEntityObserver(confirmationState -> {
      int result = JOptionPane.showConfirmDialog(Utilities.getParentWindow(EntityEditPanel.this).orElse(null),
              FrameworkMessages.unsavedDataWarning(), FrameworkMessages.unsavedDataWarningTitle(),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      confirmationState.set(result == JOptionPane.YES_OPTION);
    });
  }

  private void onRefreshingChanged(boolean refreshing) {
    if (refreshing) {
      WaitCursor.show(EntityEditPanel.this);
    }
    else {
      WaitCursor.hide(EntityEditPanel.this);
    }
  }

  private void showEntityMenu() {
    new EntityPopupMenu(getEditModel().getEntityCopy(), getEditModel().getConnectionProvider().getConnection()).show(this, 0, 0);
  }
}
