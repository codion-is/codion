/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.Conjunction;
import org.jminor.common.model.EventListener;
import org.jminor.common.model.State;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.States;
import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.exception.ValidationException;
import org.jminor.common.ui.DateInputPanel;
import org.jminor.common.ui.DefaultExceptionHandler;
import org.jminor.common.ui.ExceptionHandler;
import org.jminor.common.ui.TextInputPanel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.checkbox.TristateCheckBox;
import org.jminor.common.ui.combobox.MaximumMatch;
import org.jminor.common.ui.combobox.SteppedComboBox;
import org.jminor.common.ui.control.Control;
import org.jminor.common.ui.control.ControlProvider;
import org.jminor.common.ui.control.ControlSet;
import org.jminor.common.ui.control.Controls;
import org.jminor.common.ui.images.Images;
import org.jminor.framework.Configuration;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Property;
import org.jminor.framework.i18n.FrameworkMessages;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends JPanel implements ExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

  //Control codes
  public static final String SAVE = "EntityEditPanel.save";
  public static final String INSERT = "EntityEditPanel.insert";
  public static final String UPDATE = "EntityEditPanel.update";
  public static final String DELETE = "EntityEditPanel.delete";
  public static final String REFRESH = "EntityEditPanel.refresh";
  public static final String CLEAR = "EntityEditPanel.clear";

  protected static enum ConfirmType {
    INSERT, UPDATE, DELETE
  }

  private static final String ALT_PREFIX = " (ALT-";
  private static final int ENTITY_MENU_X_OFFSET = 42;

  /**
   * The edit model this edit panel is associated with
   */
  private final EntityEditModel editModel;

  /**
   * Input components mapped to their respective propertyIDs
   */
  private final Map<String, JComponent> components = new HashMap<>();

  /**
   * Controls mapped to their respective control codes
   */
  private final Map<String, Control> controls = new HashMap<>();

  /**
   * Indicates whether the panel is active and ready to receive input
   */
  private final State activeState = States.state(Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE));

  /**
   * The mechanism for restricting a single active EntityEditPanel at a time
   */
  private static final State.Group ACTIVE_STATE_GROUP = States.group();

  /**
   * The component that should receive focus when the UI is prepared
   */
  private JComponent initialFocusComponent;

  /**
   * The propertyID for which component should receive the focus when the UI is prepared
   */
  private String initialFocusPropertyID;

  /**
   * Indicates whether or not the UI should be cleared after insert has been performed
   */
  private boolean clearAfterInsert = true;

  /**
   * Indicates whether or not the UI should request focus after insert has been performed
   * @see #setInitialFocus()
   */
  private boolean requestFocusAfterInsert = true;

  /**
   * True after <code>initializePanel()</code> has been called
   */
  private boolean panelInitialized = false;

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   */
  public EntityEditPanel(final EntityEditModel editModel) {
    this(editModel, SAVE, UPDATE, DELETE, CLEAR, REFRESH);
  }

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   * @param controlKeys if specified only controls with those keys are initialized,
   * null or an empty String array will result in no controls being initialized
   */
  public EntityEditPanel(final EntityEditModel editModel, final String... controlKeys) {
    this.editModel = Util.rejectNullValue(editModel, "editModel");
    if (!Configuration.getBooleanValue(Configuration.ALL_PANELS_ACTIVE)) {
      ACTIVE_STATE_GROUP.addState(activeState);
    }
    setupDefaultControls(controlKeys);
    bindEvents();
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return editModel.toString();
  }

  /**
   * @return the edit model this panel is based on
   */
  public final EntityEditModel getEditModel() {
    return editModel;
  }

  /**
   * Indicates whether this panel is active and ready to receive input
   * @return a state indicating whether the active is active and ready to receive input
   */
  public final StateObserver getActiveObserver() {
    return activeState.getObserver();
  }

  /**
   * Sets the active state of this edit panel, an active edit panel should be
   * enabled and ready to receive input
   * @param active the active state
   */
  public final void setActive(final boolean active) {
    activeState.setActive(active);
  }

  /**
   * @return true if this edit panel is active and ready to receive input
   */
  public final boolean isActive() {
    return activeState.isActive();
  }

  /**
   * Prepares the UI.
   * @param setInitialFocus if true then the initial focus is set
   * @param clearUI if true the UI is cleared.
   * @see org.jminor.framework.client.model.EntityEditModel#clear()
   */
  public final void prepareUI(final boolean setInitialFocus, final boolean clearUI) {
    if (clearUI) {
      clearModelValues();
    }
    if (setInitialFocus && isVisible()) {
      setInitialFocus();
    }
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via <code>setInitialFocusComponentKey()</code>
   * @param initialFocusComponent the component
   * @return the component
   * @see #prepareUI(boolean, boolean)
   */
  public final JComponent setInitialFocusComponent(final JComponent initialFocusComponent) {
    this.initialFocusComponent = initialFocusComponent;
    return initialFocusComponent;
  }

  /**
   * @return the propertyID of the component to receive the initial focus
   */
  public final String getInitialFocusProperty() {
    return initialFocusPropertyID;
  }

  /**
   * Defines the component associated with the given propertyID as the component
   * that should receive the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param propertyID the component propertyID
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void setInitialFocusProperty(final String propertyID) {
    this.initialFocusPropertyID = propertyID;
  }

  /**
   * Sets the initial focus, if a initial focus component or component propertyID
   * has been set that component receives the focus, if not, or if that component
   * is not focusable, this panel receives the focus
   * @see #setInitialFocusProperty
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void setInitialFocus() {
    final JComponent focusComponent = getInitialFocusComponent();
    if (focusComponent == null || !focusComponent.isFocusable()) {
      requestFocus();//InWindow();
    }
    else {
      focusComponent.requestFocus();//InWindow();
    }
  }

  /**
   * @return the propertyIDs that have been associated with components.
   */
  public final Collection<String> getComponentPropertyIDs() {
    return new ArrayList<>(components.keySet());
  }

  /**
   * @param propertyID the propertyID
   * @return the component associated with the given propertyID, null if no component has been
   * associated with the given propertyID
   */
  public final JComponent getComponent(final String propertyID) {
    return components.get(propertyID);
  }

  /**
   * @param component the component
   * @return the propertyID the given component is associated with, null if the component has not been
   * associated with a propertyID
   */
  public final String getComponentPropertyID(final JComponent component) {
    for (final Map.Entry<String, JComponent> entry : components.entrySet()) {
      if (entry.getValue().equals(component)) {
        return entry.getKey();
      }
    }

    return null;
  }

  /**
   * Request focus for the component associated with the given propertyID
   * @param propertyID the propertyID of the component to select
   */
  public final void selectComponent(final String propertyID) {
    if (components.containsKey(propertyID)) {
      components.get(propertyID).requestFocus();
    }
  }

  /**
   * @return a list of propertyIDs to use when selecting a input component in this panel,
   * this returns all propertyIDs that have mapped components in this panel
   * that are enabled, visible and focusable.
   * @see #includeComponentSelectionPropertyID(String) (String)
   * @see #setComponent(String, javax.swing.JComponent)
   */
  public final List<String> getSelectComponentPropertyIDs() {
    final Collection<String> propertyIDs = getComponentPropertyIDs();
    final List<String> selectableComponentPropertyIDs = new ArrayList<>(propertyIDs.size());
    for (final String propertyID : propertyIDs) {
      final JComponent component = getComponent(propertyID);
      if (component != null && includeComponentSelectionPropertyID(propertyID) && component.isVisible() &&
              component.isFocusable() && component.isEnabled()) {
        selectableComponentPropertyIDs.add(propertyID);
      }
    }

    return selectableComponentPropertyIDs;
  }

  /**
   * Override to exclude components from the component selection.
   * @param propertyID the component propertyID
   * @return true if the component associated with the given propertyID should be included when allowing the user
   * to select a input component in this panel, true by default.
   */
  public boolean includeComponentSelectionPropertyID(final String propertyID) {
    return true;
  }

  /**
   * Clears the values from the underlying model
   */
  public final void clearModelValues() {
    editModel.setEntity(null);
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
   * @see #setInitialFocus()
   */
  public final boolean isRequestFocusAfterInsert() {
    return requestFocusAfterInsert;
  }

  /**
   * @param requestFocusAfterInsert true if the UI should request focus after insert has been performed
   * @see #setInitialFocus()
   */
  public final void setRequestFocusAfterInsert(final boolean requestFocusAfterInsert) {
    this.requestFocusAfterInsert = requestFocusAfterInsert;
  }

  /**
   * @param controlCode the control code
   * @return the control associated with <code>controlCode</code>
   * @throws IllegalArgumentException in case no control is associated with the given control code
   */
  public final Control getControl(final String controlCode) {
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
    return Controls.methodControl(editModel, "refresh", FrameworkMessages.get(FrameworkMessages.REFRESH),
            getActiveObserver(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + ALT_PREFIX
            + mnemonic + ")", mnemonic.charAt(0), null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a control for deleting the active entity
   */
  public final Control getDeleteControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC);
    return Controls.methodControl(this, "delete", FrameworkMessages.get(FrameworkMessages.DELETE),
            States.aggregateState(Conjunction.AND,
                    getActiveObserver(),
                    editModel.getAllowDeleteObserver(),
                    editModel.getEntityNewObserver().getReversedObserver()),
            FrameworkMessages.get(FrameworkMessages.DELETE_TIP) + ALT_PREFIX + mnemonic + ")", mnemonic.charAt(0), null,
            Images.loadImage(Images.IMG_DELETE_16));
  }

  /**
   * @return a control for clearing the UI controls
   */
  public final Control getClearControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC);
    return Controls.methodControl(this, "clearModelValues", FrameworkMessages.get(FrameworkMessages.CLEAR),
            getActiveObserver(), FrameworkMessages.get(FrameworkMessages.CLEAR_ALL_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_NEW_16));
  }

  /**
   * @return a control for performing an update on the active entity
   */
  public final Control getUpdateControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC);
    return Controls.methodControl(this, "update", FrameworkMessages.get(FrameworkMessages.UPDATE),
            States.aggregateState(Conjunction.AND,
                    getActiveObserver(),
                    editModel.getAllowUpdateObserver(),
                    editModel.getEntityNewObserver().getReversedObserver(),
                    editModel.getModifiedObserver()),
            FrameworkMessages.get(FrameworkMessages.UPDATE_TIP) + ALT_PREFIX + mnemonic + ")", mnemonic.charAt(0),
            null, Images.loadImage(Images.IMG_SAVE_16));
  }

  /**
   * @return a control for performing an insert on the active entity
   */
  public final Control getInsertControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC);
    return Controls.methodControl(this, "insert", FrameworkMessages.get(FrameworkMessages.INSERT),
            States.aggregateState(Conjunction.AND, getActiveObserver(), editModel.getAllowInsertObserver()),
            FrameworkMessages.get(FrameworkMessages.INSERT_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage("Add16.gif"));
  }

  /**
   * @return a control for performing a save on the active entity, that is, update if an entity
   * is selected and modified or insert otherwise
   */
  public final Control getSaveControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.INSERT_MNEMONIC);
    final State insertUpdateState = States.aggregateState(Conjunction.OR, editModel.getAllowInsertObserver(),
            States.aggregateState(Conjunction.AND, editModel.getAllowUpdateObserver(),
                    editModel.getModifiedObserver()));
    return Controls.methodControl(this, "save", FrameworkMessages.get(FrameworkMessages.INSERT),
            States.aggregateState(Conjunction.AND, getActiveObserver(), insertUpdateState),
            FrameworkMessages.get(FrameworkMessages.INSERT_UPDATE_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage("Add16.gif"));
  }

  /**
   * Handles the given exception, which usually means simply displaying it to the user
   * @param throwable the exception to handle
   */
  public final void handleException(final Throwable throwable) {
    if (throwable instanceof ValidationException) {
      JOptionPane.showMessageDialog(this, throwable.getMessage(), Messages.get(Messages.EXCEPTION),
              JOptionPane.ERROR_MESSAGE);
      selectComponent((String) ((ValidationException) throwable).getKey());
    }
    else {
      handleException(throwable, UiUtil.getParentWindow(this));
    }
  }

  /**
   * Handles the given exception
   * @param exception the exception to handle
   * @param dialogParent the component to use as exception dialog parent
   */
  @Override
  public final void handleException(final Throwable exception, final Window dialogParent) {
    LOG.error(exception.getMessage(), exception);
    DefaultExceptionHandler.getInstance().handleException(exception, dialogParent);
  }

  /**
   * Initializes the control panel, that is, the panel containing buttons for editing entities (Insert, Update...)
   * @param horizontal true if the buttons should be laid out horizontally, false otherwise
   * @return the control panel, null if no controls are defined
   * @see #initializeControlPanelControlSet()
   */
  public final JPanel createControlPanel(final boolean horizontal) {
    final ControlSet controlPanelControlSet = initializeControlPanelControlSet();
    if (controlPanelControlSet.size() == 0) {
      return null;
    }
    if (horizontal) {
      final JPanel panel = new JPanel(UiUtil.createFlowLayout(FlowLayout.CENTER));
      panel.add(ControlProvider.createHorizontalButtonPanel(controlPanelControlSet));
      return panel;
    }
    else {
      final JPanel panel = new JPanel(UiUtil.createBorderLayout());
      panel.add(ControlProvider.createVerticalButtonPanel(controlPanelControlSet), BorderLayout.NORTH);
      return panel;
    }
  }

  /**
   * Initializes the control toolbar, that is, the toolbar containing buttons for editing entities (Insert, Update...)
   * @param orientation the orientation
   * @return the control toolbar, null if no controls are defined
   * @see #initializeControlPanelControlSet()
   */
  public final JToolBar createControlToolBar(final int orientation) {
    final ControlSet controlPanelControlSet = initializeControlPanelControlSet();
    if (controlPanelControlSet.size() == 0) {
      return null;
    }
    return ControlProvider.createToolbar(controlPanelControlSet, orientation);
  }

  /**
   * Initializes this EntityEditPanel UI.
   * This method marks this panel as initialized which prevents it from running again, whether or not an exception occurs.
   * @return this EntityPanel instance
   * @see #isPanelInitialized()
   */
  public final EntityEditPanel initializePanel() {
    if (!panelInitialized) {
      try {
        UiUtil.setWaitCursor(true, this);
        initializeUI();
      }
      finally {
        panelInitialized = true;
        UiUtil.setWaitCursor(false, this);
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
    if (editModel.isEntityNew() || !editModel.isModified() || !editModel.isUpdateAllowed()) {
      //no entity selected, selected entity is unmodified or update is not allowed, can only insert
      insert();
    }
    else {//possibly update
      final int choiceIdx = JOptionPane.showOptionDialog(this, FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT),
              FrameworkMessages.get(FrameworkMessages.UPDATE_OR_INSERT_TITLE), JOptionPane.DEFAULT_OPTION, JOptionPane.QUESTION_MESSAGE, null,
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_RECORD),
                      FrameworkMessages.get(FrameworkMessages.INSERT_NEW), Messages.get(Messages.CANCEL)},
              new String[] {FrameworkMessages.get(FrameworkMessages.UPDATE)});
      if (choiceIdx == 0) {//update
        update(false);
      }
      else if (choiceIdx == 1) {//insert
        insert(false);
      }
    }
  }

  /**
   * Performs a insert on the active entity
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insert() {
    return insert(true);
  }

  /**
   * Performs a insert on the active entity
   * @param confirmRequired if true then confirmInsert() is called
   * @return true in case of successful insert, false otherwise
   */
  public final boolean insert(final boolean confirmRequired) {
    try {
      if (!confirmRequired || confirmInsert()) {
        validateData();
        try {
          UiUtil.setWaitCursor(true, this);
          editModel.insert();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
        prepareUI(requestFocusAfterInsert, clearAfterInsert);
        return true;
      }
    }
    catch (ValidationException | DatabaseException v) {
      handleException(v);
    }

    return false;
  }

  /**
   * Performs a delete on the active entity
   * @return true if the delete operation was successful
   */
  public final boolean delete() {
    return delete(true);
  }

  /**
   * Performs a delete on the active entity
   * @param confirmRequired if true then confirmDelete() is called
   * @return true if the delete operation was successful
   */
  public final boolean delete(final boolean confirmRequired) {
    try {
      if (!confirmRequired || confirmDelete()) {
        try {
          UiUtil.setWaitCursor(true, this);
          editModel.delete();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }

        return true;
      }
    }
    catch (final DatabaseException ex) {
      handleException(ex);
    }

    return false;
  }

  /**
   * Performs an update on the active entity
   * @return true if the update operation was successful
   */
  public final boolean update() {
    return update(true);
  }

  /**
   * Performs an update on the active entity
   * @param confirmRequired if true then confirmUpdate() is called
   * @return true if the update operation was successful or if no update was required
   */
  public final boolean update(final boolean confirmRequired) {
    try {
      if (!confirmRequired || confirmUpdate()) {
        validateData();
        try {
          UiUtil.setWaitCursor(true, this);
          editModel.update();
        }
        finally {
          UiUtil.setWaitCursor(false, this);
        }
        prepareUI(true, false);

        return true;
      }
    }
    catch (ValidationException | DatabaseException v) {
      handleException(v);
    }

    return false;
  }

  //#############################################################################################
  // End - control methods
  //#############################################################################################

  /**
   * for overriding, called before insert/update
   * @throws org.jminor.common.model.valuemap.exception.ValidationException in case of a validation failure
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
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_ENTITY),
                FrameworkMessages.get(FrameworkMessages.DELETE)};
      case INSERT:
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_INSERT),
                FrameworkMessages.get(FrameworkMessages.INSERT)};
      case UPDATE:
        return new String[]{FrameworkMessages.get(FrameworkMessages.CONFIRM_UPDATE),
                FrameworkMessages.get(FrameworkMessages.UPDATE)};
    }

    throw new IllegalArgumentException("Unknown confirmation type constant: " + type);
  }

  /**
   * Associates <code>control</code> with <code>controlCode</code>
   * @param controlCode the control code
   * @param control the control to associate with <code>controlCode</code>
   */
  protected final void setControl(final String controlCode, final Control control) {
    if (control == null) {
      controls.remove(controlCode);
    }
    else {
      controls.put(controlCode, control);
    }
  }

  /**
   * Initializes a ControlSet on which to base the control panel
   * @return the ControlSet on which to base the control panel
   */
  protected ControlSet initializeControlPanelControlSet() {
    final ControlSet controlSet = new ControlSet("Actions");
    if (controls.containsKey(SAVE)) {
      controlSet.add(controls.get(SAVE));
    }
    if (controls.containsKey(INSERT)) {
      controlSet.add(controls.get(INSERT));
    }
    if (controls.containsKey(UPDATE)) {
      controlSet.add(controls.get(UPDATE));
    }
    if (controls.containsKey(DELETE)) {
      controlSet.add(controls.get(DELETE));
    }
    if (controls.containsKey(CLEAR)) {
      controlSet.add(controls.get(CLEAR));
    }
    if (controls.containsKey(REFRESH)) {
      controlSet.add(controls.get(REFRESH));
    }

    return controlSet;
  }

  /**
   * Initializes this EntityEditPanel UI, that is, creates and lays out the components
   * required for editing the underlying entity type.
   * <pre>
   *   protected void initializeUI() {
   *      JTextField txtName = createTextField(DomainModel.USER_NAME);
   *      JTextField txtAddress = createTextField(DomainModel.USER_ADDRESS);
   *      setLayout(new GridLayout(2, 1, 5, 5);
   *      addPropertyPanel(DomainModel.USER_NAME);
   *      addPropertyPanel(DomainModel.USER_ADDRESS);
   *  }
   * </pre>
   */
  protected abstract void initializeUI();

  /**
   * @return the component that should get the initial focus when the UI is prepared
   * @see #prepareUI(boolean, boolean)
   */
  protected JComponent getInitialFocusComponent() {
    if (initialFocusComponent != null) {
      return initialFocusComponent;
    }

    if (initialFocusPropertyID != null) {
      return components.get(initialFocusPropertyID);
    }

    return null;
  }

  /**
   * Associates the given input component with the given propertyID,
   * preferably this should be called for components associated with
   * properties.
   * @param propertyID the propertyID
   * @param component the input component
   */
  protected final void setComponent(final String propertyID, final JComponent component) {
    if (components.containsKey(propertyID)) {
      throw new IllegalStateException("Component already set for propertyID: " + propertyID);
    }
    components.put(propertyID, component);
  }

  /**
   * Adds a property panel for the given property to this panel
   * @param propertyID the ID of the property
   * @see #createPropertyPanel(String)
   */
  protected final void addPropertyPanel(final String propertyID) {
    add(createPropertyPanel(propertyID));
  }

  /**
   * Creates a panel containing a label and the component associated with the given property.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * The default layout of the resulting panel is with the label on top and inputComponent below.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @return a panel containing a label and a component
   * @throws IllegalArgumentException in case no component has been associated with the given property
   */
  protected final JPanel createPropertyPanel(final String propertyID) {
    final JComponent component = getComponent(propertyID);
    if (component == null) {
      throw new IllegalArgumentException("No component associated with property: " + propertyID);
    }

    return createPropertyPanel(propertyID, component, true);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * The default layout of the resulting panel is with the label on top and <code>inputComponent</code> below.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent) {
    return createPropertyPanel(propertyID, inputComponent, true);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop) {
    return createPropertyPanel(propertyID, inputComponent, labelOnTop, JLabel.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by <code>propertyID</code>.
   * @param propertyID the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyID, final JComponent inputComponent,
                                             final boolean labelOnTop, final int labelAlignment) {
    return createPropertyPanel(EntityUiUtil.createLabel(Entities.getProperty(editModel.getEntityID(),
            propertyID), labelAlignment), inputComponent, labelOnTop);
  }

  /**
   * Creates a panel containing a label component and the <code>inputComponent</code> with the label
   * component positioned above the input component.
   * @param labelComponent the label component
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent) {
    return createPropertyPanel(labelComponent, inputComponent, true);
  }

  /**
   * Creates a panel containing a label component and the <code>inputComponent</code>.
   * @param labelComponent the label component
   * @param inputComponent a component bound to the property with id <code>propertyID</code>
   * @param labelOnTop if true then the label is positioned above <code>inputComponent</code>,
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent,
                                             final boolean labelOnTop) {
    final JPanel panel = new JPanel(labelOnTop ?
            UiUtil.createBorderLayout() : UiUtil.createFlowLayout(FlowLayout.LEADING));
    if (labelComponent instanceof JLabel) {
      ((JLabel) labelComponent).setLabelFor(inputComponent);
    }
    if (labelOnTop) {
      panel.add(labelComponent, BorderLayout.NORTH);
      panel.add(inputComponent, BorderLayout.CENTER);
    }
    else {
      panel.add(labelComponent);
      panel.add(inputComponent);
    }

    return panel;
  }

  /**
   * Creates a JTextArea component bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyID) {
    return createTextArea(propertyID, -1, -1);
  }

  /**
   * Creates a JTextArea component bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyID, final int rows, final int columns) {
    return createTextArea(propertyID, readOnly(propertyID), rows, columns, null);
  }

  /**
   * Creates a JTextArea component bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyID, final boolean readOnly, final int rows, final int columns,
                                           final StateObserver enabledState) {
    final Property property = Entities.getProperty(editModel.getEntityID(), propertyID);
    final JTextArea textArea = EntityUiUtil.createTextArea(property, editModel, readOnly, rows, columns, enabledState);
    setComponent(propertyID, textArea);

    return textArea;
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID) {
    return createTextInputPanel(propertyID, readOnly(propertyID));
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final boolean readOnly) {
    return createTextInputPanel(propertyID, readOnly, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final boolean readOnly,
                                                      final boolean immediateUpdate) {
    return createTextInputPanel(propertyID, readOnly, immediateUpdate, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyID, final boolean readOnly,
                                                      final boolean immediateUpdate, final boolean buttonFocusable) {
    return createTextInputPanel(Entities.getProperty(editModel.getEntityID(), propertyID), readOnly,
            immediateUpdate, buttonFocusable);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final boolean readOnly,
                                                      final boolean immediateUpdate) {
    return createTextInputPanel(property, readOnly, immediateUpdate, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final boolean readOnly,
                                                      final boolean immediateUpdate, final boolean buttonFocusable) {
    final TextInputPanel ret = EntityUiUtil.createTextInputPanel(property, editModel, readOnly, immediateUpdate, buttonFocusable);
    setComponent(property.getPropertyID(), ret.getTextComponent());

    return ret;
  }

  /**
   * Creates a new DateInputPanel using the default short date format, bound to the property
   * identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID) {
    return createDateInputPanel(propertyID, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel using the default short date format
   * @see org.jminor.framework.Configuration#DATE_FORMAT
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final boolean includeButton) {
    final Property property = Entities.getProperty(editModel.getEntityID(), propertyID);
    return createDateInputPanel(property, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final boolean includeButton,
                                                      final StateObserver enabledState) {
    return createDateInputPanel(propertyID, includeButton, enabledState, readOnly(propertyID));
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param propertyID the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param readOnly if true the component will be read only
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final String propertyID, final boolean includeButton,
                                                      final StateObserver enabledState, final boolean readOnly) {
    return createDateInputPanel(Entities.getProperty(editModel.getEntityID(), propertyID),
            includeButton, enabledState, readOnly);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by <code>propertyID</code>.
   * @param property the property for which to create the panel
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property) {
    return createDateInputPanel(property, true);
  }

  /**
   * Creates a new DateInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final boolean includeButton) {
    return createDateInputPanel(property, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final boolean includeButton,
                                                      final StateObserver enabledState) {
    return createDateInputPanel(property, includeButton, enabledState, readOnly(property.getPropertyID()));
  }

  /**
   * Creates a new DateInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param readOnly if true the component will be read only
   * @return a DateInputPanel bound to the property
   */
  protected final DateInputPanel createDateInputPanel(final Property property, final boolean includeButton,
                                                      final StateObserver enabledState, final boolean readOnly) {
    final DateInputPanel panel = EntityUiUtil.createDateInputPanel(property, editModel, readOnly, includeButton, enabledState);
    setComponent(property.getPropertyID(), panel.getInputField());

    return panel;
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID) {
    return createTextField(propertyID, readOnly(propertyID));
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final boolean readOnly) {
    return createTextField(propertyID, readOnly, true);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final boolean readOnly,
                                             final boolean immediateUpdate) {
    return createTextField(propertyID, readOnly, immediateUpdate, null);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final boolean readOnly,
                                             final boolean immediateUpdate, final String maskString) {
    return createTextField(propertyID, readOnly, immediateUpdate, maskString, null);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final boolean readOnly,
                                             final boolean immediateUpdate, final String maskString,
                                             final StateObserver enabledState) {
    return createTextField(propertyID, readOnly, immediateUpdate, maskString, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if <code>maskString</code> is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyID, final boolean readOnly,
                                             final boolean immediateUpdate, final String maskString,
                                             final StateObserver enabledState, final boolean valueIncludesLiteralCharacters) {
    return createTextField(Entities.getProperty(editModel.getEntityID(), propertyID),
            readOnly, maskString, immediateUpdate, enabledState, valueIncludesLiteralCharacters);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property) {
    return createTextField(property, readOnly(property.getPropertyID()));
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param readOnly if true the component will be read only
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final boolean readOnly) {
    return createTextField(property, readOnly, null, true);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final boolean readOnly,
                                             final String maskString, final boolean immediateUpdate) {
    return createTextField(property, readOnly, maskString, immediateUpdate, null);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the ID of the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final boolean readOnly,
                                             final String maskString, final boolean immediateUpdate,
                                             final StateObserver enabledState) {
    return createTextField(property, readOnly, maskString, immediateUpdate, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param readOnly if true the component will be read only
   * @param immediateUpdate if true then the underlying property value is updated on each keystroke,
   * otherwise it is updated when the component looses focus.
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if <code>maskString</code> is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final boolean readOnly,
                                             final String maskString, final boolean immediateUpdate,
                                             final StateObserver enabledState, final boolean valueIncludesLiteralCharacters) {
    final JTextField txt = EntityUiUtil.createTextField(property, editModel, readOnly, maskString, immediateUpdate,
            enabledState, valueIncludesLiteralCharacters);
    setComponent(property.getPropertyID(), txt);

    return txt;
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID) {
    return createCheckBox(propertyID, null);
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID, final StateObserver enabledState) {
    return createCheckBox(propertyID, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyID, final StateObserver enabledState,
                                           final boolean includeCaption) {
    return createCheckBox(Entities.getProperty(editModel.getEntityID(), propertyID), enabledState, includeCaption);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property) {
    return createCheckBox(property, null);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property, final StateObserver enabledState) {
    return createCheckBox(property, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final Property property, final StateObserver enabledState,
                                           final boolean includeCaption) {
    final JCheckBox box = EntityUiUtil.createCheckBox(property, editModel, enabledState, includeCaption);
    setComponent(property.getPropertyID(), box);

    return box;
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyID) {
    return createTristateCheckBox(propertyID, null);
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyID, final StateObserver enabledState) {
    return createTristateCheckBox(propertyID, enabledState, true);
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyID, final StateObserver enabledState,
                                                          final boolean includeCaption) {
    return createTristateCheckBox(Entities.getProperty(editModel.getEntityID(), propertyID), enabledState, includeCaption);
  }

  /**
   * Creates a TristateCheckBox bound to the given property
   * @param property the property to bind
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final Property property) {
    return createTristateCheckBox(property, null);
  }

  /**
   * Creates a TristateCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final Property property, final StateObserver enabledState) {
    return createTristateCheckBox(property, enabledState, true);
  }

  /**
   * Creates a TristateCheckBox bound to the given property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final Property property, final StateObserver enabledState,
                                                          final boolean includeCaption) {
    final TristateCheckBox box = EntityUiUtil.createTristateCheckBox(property, editModel, enabledState, includeCaption);
    setComponent(property.getPropertyID(), box);

    return box;
  }

  /**
   * Create a JComboBox for the property identified by <code>propertyID</code>, containing
   * values for the boolean values: true, false, null
   * @param propertyID the ID of the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyID) {
    return createBooleanComboBox(propertyID, null);
  }

  /**
   * Create a JComboBox for the property identified by <code>propertyID</code>, containing
   * values for the boolean values: true, false, null
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyID, final StateObserver enabledState) {
    return createBooleanComboBox(Entities.getProperty(editModel.getEntityID(), propertyID),enabledState);
  }

  /**
   * Create a JComboBox for the given property, containing
   * values for the boolean values: true, false, null
   * @param property the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final Property property) {
    return createBooleanComboBox(property, null);
  }

  /**
   * Create a JComboBox for the given property, containing
   * values for the boolean values: true, false, null
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final Property property, final StateObserver enabledState) {
    final JComboBox ret = EntityUiUtil.createBooleanComboBox(property, editModel, enabledState);
    setComponent(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyID, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final StateObserver enabledState) {
    return createComboBox(Entities.getProperty(editModel.getEntityID(), propertyID),
            comboBoxModel, maximumMatch, enabledState);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(property, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final StateObserver enabledState) {
    final SteppedComboBox comboBox = EntityUiUtil.createComboBox(property, editModel, comboBoxModel, enabledState);
    if (maximumMatch) {
      MaximumMatch.enable(comboBox);
    }
    setComponent(property.getPropertyID(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyID the propertyID
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyID) {
    return createValueListComboBox(propertyID, true);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyID the propertyID
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyID, final boolean sortItems) {
    return createValueListComboBox(propertyID, sortItems, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyID the propertyID
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyID, final StateObserver enabledState) {
    return createValueListComboBox(propertyID, true, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyID the propertyID
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyID, final boolean sortItems, final StateObserver enabledState) {
    final Property property = Entities.getProperty(editModel.getEntityID(), propertyID);
    if (!(property instanceof Property.ValueListProperty)) {
      throw new IllegalArgumentException("Property identified by '" + propertyID + "' is not a ValueListProperty");
    }

    return createValueListComboBox((Property.ValueListProperty) property, sortItems, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final Property.ValueListProperty property) {
    return createValueListComboBox(property, true);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final boolean sortItems) {
    return createValueListComboBox(property, sortItems, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final StateObserver enabledState) {
    final SteppedComboBox box = EntityUiUtil.createValueListComboBox(property, editModel, true, enabledState);
    setComponent(property.getPropertyID(), box);

    return box;
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final Property.ValueListProperty property, final boolean sortItems,
                                                          final StateObserver enabledState) {
    final SteppedComboBox box = EntityUiUtil.createValueListComboBox(property, editModel, sortItems, enabledState);
    setComponent(property.getPropertyID(), box);

    return box;
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel) {
    return createEditableComboBox(propertyID, comboBoxModel, null);
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyID, final ComboBoxModel comboBoxModel,
                                                         final StateObserver enabledState) {
    return createEditableComboBox(Entities.getProperty(editModel.getEntityID(), propertyID),
            comboBoxModel, enabledState);
  }

  /**
   * Creates an editable SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                         final StateObserver enabledState) {
    final SteppedComboBox ret = EntityUiUtil.createComboBox(property, editModel, comboBoxModel, enabledState, true);
    setComponent(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID) {
    return createPropertyComboBox(propertyID, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final StateObserver enabledState) {
    return createPropertyComboBox(propertyID, enabledState, false);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by <code>propertyID</code>, the combo box
   * contains the underlying values of the property
   * @param propertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param editable true if the combo box should be editable, only works with combo boxes based on String.class properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyID, final StateObserver enabledState,
                                                         final boolean editable) {
    return createPropertyComboBox(Entities.getColumnProperty(editModel.getEntityID(), propertyID),
            enabledState, editable);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property) {
    return createPropertyComboBox(property, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final StateObserver enabledState) {
    return createPropertyComboBox(property, enabledState, false);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param editable true if the combo box should be editable, only works with combo boxes based on String.class properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final Property.ColumnProperty property, final StateObserver enabledState,
                                                         final boolean editable) {
    final SteppedComboBox ret = EntityUiUtil.createPropertyComboBox(property, editModel, enabledState, editable);
    setComponent(property.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param foreignKeyPropertyID the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String foreignKeyPropertyID, final StateObserver enabledState) {
    return createEntityComboBox((Property.ForeignKeyProperty)
            Entities.getProperty(editModel.getEntityID(), foreignKeyPropertyID), enabledState);
  }

  /**
   * Creates an EntityComboBox bound to the property identified by <code>propertyID</code>
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final String foreignKeyPropertyID) {
    return createEntityComboBox(Entities.getForeignKeyProperty(editModel.getEntityID(),
            foreignKeyPropertyID), null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty) {
    return createEntityComboBox(foreignKeyProperty, null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createEntityComboBox(final Property.ForeignKeyProperty foreignKeyProperty,
                                                      final StateObserver enabledState) {
    final EntityComboBox ret = EntityUiUtil.createEntityComboBox(foreignKeyProperty, editModel, enabledState);
    setComponent(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID) {
    return createEntityLookupField(foreignKeyPropertyID, (StateObserver) null);
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID,
                                                            final StateObserver enabledState) {
    final Property.ForeignKeyProperty fkProperty = Entities.getForeignKeyProperty(editModel.getEntityID(),
            foreignKeyPropertyID);
    final Collection<String> searchPropertyIDs = Entities.getSearchPropertyIDs(fkProperty.getReferencedEntityID());
    return createEntityLookupField(fkProperty, enabledState, searchPropertyIDs.toArray(new String[searchPropertyIDs.size()]));
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID,
                                                            final String... searchPropertyIDs) {
    return createEntityLookupField(foreignKeyPropertyID, null, searchPropertyIDs);
  }

  /**
   * Creates an EntityLookupField bound to the property identified by <code>propertyID</code>, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyID the ID of the foreign key property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final String foreignKeyPropertyID,
                                                            final StateObserver enabledState,
                                                            final String... searchPropertyIDs) {
    final Property.ForeignKeyProperty fkProperty = Entities.getForeignKeyProperty(editModel.getEntityID(),
            foreignKeyPropertyID);
    if (searchPropertyIDs == null || searchPropertyIDs.length == 0) {
      final Collection<String> propertyIDs = Entities.getSearchPropertyIDs(fkProperty.getReferencedEntityID());
      return createEntityLookupField(fkProperty, enabledState, propertyIDs.toArray(new String[propertyIDs.size()]));
    }

    return createEntityLookupField(fkProperty, enabledState, searchPropertyIDs);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                            final String... searchPropertyIDs) {
    return createEntityLookupField(foreignKeyProperty, null, searchPropertyIDs);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param searchPropertyIDs the IDs of the properties to use in the lookup
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createEntityLookupField(final Property.ForeignKeyProperty foreignKeyProperty,
                                                            final StateObserver enabledState,
                                                            final String... searchPropertyIDs) {
    final EntityLookupField ret = EntityUiUtil.createEntityLookupField(foreignKeyProperty, editModel, enabledState, searchPropertyIDs);
    setComponent(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates an uneditable JTextField bound to the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final String propertyID) {
    return createEntityField(Entities.getForeignKeyProperty(editModel.getEntityID(), propertyID));
  }

  /**
   * Creates an uneditable JTextField bound to the given property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createEntityField(final Property.ForeignKeyProperty foreignKeyProperty) {
    final JTextField ret = EntityUiUtil.createEntityField(foreignKeyProperty, editModel);
    setComponent(foreignKeyProperty.getPropertyID(), ret);

    return ret;
  }

  /**
   * Creates a JLabel with a caption from the property identified by <code>propertyID</code>
   * @param propertyID the ID of the property from which to retrieve the caption
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyID) {
    return createLabel(propertyID, JLabel.LEFT);
  }

  /**
   * Creates a JLabel with a caption from the given property identified by <code>propertyID</code>
   * @param propertyID the ID of the property from which to retrieve the caption
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyID, final int horizontalAlignment) {
    return EntityUiUtil.createLabel(Entities.getProperty(editModel.getEntityID(), propertyID), horizontalAlignment);
  }

  /**
   * Initializes the default controls available to this EntityEditPanel by mapping them to their respective
   * control codes (EntityEditPanel.INSERT, UPDATE etc) via the <code>setControl(String, Control) method,
   * these can then be retrieved via the <code>getControl(String)</code> method.
   * @param controlKeys the control keys for which controls should be initialized
   * @see org.jminor.common.ui.control.Control
   * @see #setControl(String, org.jminor.common.ui.control.Control)
   * @see #getControl(String)
   * todo updateAllowed(false) þá vantar Insert control nema það sé tiltekið í smið
   */
  private void setupDefaultControls(final String... controlKeys) {
    if (controlKeys == null || controlKeys.length == 0) {
      return;
    }
    final Collection<String> keys = Arrays.asList(controlKeys);
    if (!editModel.isReadOnly()) {
      if (editModel.isInsertAllowed() && editModel.isUpdateAllowed() && keys.contains(SAVE)) {
        setControl(SAVE, getSaveControl());
      }
      if (editModel.isInsertAllowed() && keys.contains(INSERT)) {
        setControl(INSERT, getInsertControl());
      }
      if (editModel.isUpdateAllowed() && keys.contains(UPDATE)) {
        setControl(UPDATE, getUpdateControl());
      }
      if (editModel.isDeleteAllowed() && keys.contains(DELETE)) {
        setControl(DELETE, getDeleteControl());
      }
    }
    if (keys.contains(CLEAR)) {
      setControl(CLEAR, getClearControl());
    }
    if (keys.contains(REFRESH)) {
      setControl(REFRESH, getRefreshControl());
    }
  }

  private void bindEvents() {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.isAltDown() && e.getClickCount() == 2) {
          EntityUiUtil.showEntityMenu(getEditModel().getEntityCopy(), EntityEditPanel.this, new Point(e.getX(), e.getY()), getEditModel().getConnectionProvider());
        }
      }
    });
    UiUtil.addKeyEvent(this, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, new AbstractAction("EntityEditPanel.showEntityMenu") {
      @Override
      public void actionPerformed(final ActionEvent e) {
        final int x = getBounds().getLocation().x + ENTITY_MENU_X_OFFSET;
        final int y = getHeight();

        EntityUiUtil.showEntityMenu(getEditModel().getEntityCopy(), EntityEditPanel.this, new Point(x, y), getEditModel().getConnectionProvider());
      }
    });
    editModel.addBeforeRefreshListener(new EventListener() {
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(true, EntityEditPanel.this);
      }
    });
    editModel.addAfterRefreshListener(new EventListener() {
      @Override
      public void eventOccurred() {
        UiUtil.setWaitCursor(false, EntityEditPanel.this);
      }
    });
  }

  private boolean readOnly(final String propertyID) {
    final Property property = Entities.getProperty(editModel.getEntityID(), propertyID);
    return property.isReadOnly() || (property instanceof Property.ColumnProperty && !((Property.ColumnProperty) property).isUpdatable());
  }
}
