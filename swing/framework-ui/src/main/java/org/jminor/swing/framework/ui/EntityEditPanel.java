/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.framework.ui;

import org.jminor.common.Configuration;
import org.jminor.common.Conjunction;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.db.exception.ReferentialIntegrityException;
import org.jminor.common.db.valuemap.exception.ValidationException;
import org.jminor.common.event.EventDataListener;
import org.jminor.common.i18n.Messages;
import org.jminor.common.state.State;
import org.jminor.common.state.StateObserver;
import org.jminor.common.state.States;
import org.jminor.common.value.PropertyValue;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ColumnProperty;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Properties;
import org.jminor.framework.domain.property.Property;
import org.jminor.framework.domain.property.ValueListProperty;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.framework.model.EntityEditModel;
import org.jminor.swing.common.ui.DefaultDialogExceptionHandler;
import org.jminor.swing.common.ui.DialogExceptionHandler;
import org.jminor.swing.common.ui.TemporalInputPanel;
import org.jminor.swing.common.ui.TextInputPanel;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.checkbox.TristateCheckBox;
import org.jminor.swing.common.ui.combobox.MaximumMatch;
import org.jminor.swing.common.ui.combobox.SteppedComboBox;
import org.jminor.swing.common.ui.control.Control;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.images.Images;
import org.jminor.swing.framework.model.SwingEntityEditModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
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
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends JPanel implements DialogExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

  /**
   * Specifies whether focus should be transferred from components on enter,
   * this does not work for editable combo boxes, combo boxes with the
   * maximum match functionality enabled or text areas<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> TRANSFER_FOCUS_ON_ENTER = Configuration.booleanValue(
          "org.jminor.swing.framework.ui.EntityEditPanel.transferFocusOnEnter", true);

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
   * The actions meriting user confirmation
   */
  protected enum ConfirmType {
    INSERT, UPDATE, DELETE
  }

  private static final String ALT_PREFIX = " (ALT-";
  private static final int ENTITY_MENU_X_OFFSET = 42;

  /**
   * The edit model this edit panel is associated with
   */
  private final SwingEntityEditModel editModel;

  /**
   * Input components mapped to their respective propertyIds
   */
  private final Map<String, JComponent> components = new HashMap<>();

  /**
   * Controls mapped to their respective control codes
   */
  private final Map<ControlCode, Control> controls = new EnumMap(ControlCode.class);

  /**
   * Indicates whether the panel is active and ready to receive input
   */
  private final State activeState = States.state(ALL_PANELS_ACTIVE.get());

  /**
   * The mechanism for restricting a single active EntityEditPanel at a time
   */
  private static final State.Group ACTIVE_STATE_GROUP = States.group();

  /**
   * The component that should receive focus when the UI is prepared
   */
  private JComponent initialFocusComponent;

  /**
   * The propertyId for which component should receive the focus when the UI is prepared
   */
  private String initialFocusPropertyId;

  /**
   * The component that should receive focus when the UI is prepared after insert
   */
  private JComponent afterInsertFocusComponent;

  /**
   * The propertyId for which component should receive the focus when the UI is prepared after insert
   */
  private String afterInsertFocusPropertyId;

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
   * True after {@code initializePanel()} has been called
   */
  private boolean panelInitialized = false;

  /**
   * The action to take when a referential integrity error occurs on delete
   */
  private EntityTablePanel.ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling = EntityTablePanel.REFERENTIAL_INTEGRITY_ERROR_HANDLING.get();

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
    this.editModel = requireNonNull(editModel, "editModel");
    if (!ALL_PANELS_ACTIVE.get()) {
      ACTIVE_STATE_GROUP.addState(activeState);
    }
    setupControls(controlCodes);
    bindEventsInternal();
  }

  /** {@inheritDoc} */
  @Override
  public final String toString() {
    return editModel.toString();
  }

  /**
   * @return the edit model this panel is based on
   */
  public final SwingEntityEditModel getEditModel() {
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
    activeState.set(active);
  }

  /**
   * Prepares the UI.
   * @param requestInitialFocus if true then the initial focus is set
   * @param clearUI if true the UI is cleared.
   * @see EntityEditModel#clear()
   */
  public final void prepareUI(final boolean requestInitialFocus, final boolean clearUI) {
    if (clearUI) {
      clearModelValues();
    }
    if (requestInitialFocus) {
      requestInitialFocus();
    }
  }

  /**
   * Sets the component that should receive the focus when the UI is cleared or activated.
   * Overrides the value set via {@link #setInitialFocusProperty(String)}
   * @param initialFocusComponent the component
   * @return the component
   * @see #prepareUI(boolean, boolean)
   */
  public final JComponent setInitialFocusComponent(final JComponent initialFocusComponent) {
    this.initialFocusComponent = initialFocusComponent;
    return initialFocusComponent;
  }

  /**
   * Sets the component associated with the given propertyId as the component
   * that should receive the initial focus in this edit panel.
   * This is overridden by setInitialFocusComponent().
   * @param propertyId the component propertyId
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void setInitialFocusProperty(final String propertyId) {
    this.initialFocusPropertyId = propertyId;
  }

  /**
   * Sets the component that should receive the focus after an insert has been performed..
   * Overrides the value set via {@link #setAfterInsertFocusProperty(String)}
   * @param afterInsertFocusComponent the component
   * @return the component
   */
  public final JComponent setAfterInsertFocusComponent(final JComponent afterInsertFocusComponent) {
    this.afterInsertFocusComponent = afterInsertFocusComponent;
    return afterInsertFocusComponent;
  }

  /**
   * Sets the component associated with the given propertyId as the component
   * that should receive the focus after an insert is performed in this edit panel.
   * This is overridden by setAfterInsertFocusComponent().
   * @param propertyId the component propertyId
   * @see #setAfterInsertFocusComponent(JComponent)
   */
  public final void setAfterInsertFocusProperty(final String propertyId) {
    this.afterInsertFocusPropertyId = propertyId;
  }

  /**
   * Sets the initial focus, if a initial focus component or component propertyId
   * has been set that component receives the focus, if not, or if that component
   * is not focusable, this panel receives the focus
   * @see #setInitialFocusProperty
   * @see #setInitialFocusComponent(javax.swing.JComponent)
   */
  public final void requestInitialFocus() {
    if (isVisible()) {
      requestInitialFocus(false);
    }
  }

  /**
   * @return the propertyIds that have been associated with components.
   */
  public final List<String> getComponentPropertyIds() {
    return new ArrayList<>(components.keySet());
  }

  /**
   * @param propertyId the propertyId
   * @return the component associated with the given propertyId, null if no component has been
   * associated with the given propertyId
   */
  public final JComponent getComponent(final String propertyId) {
    return components.get(propertyId);
  }

  /**
   * @param component the component
   * @return the propertyId the given component is associated with, null if the component has not been
   * associated with a propertyId
   */
  public final String getComponentPropertyId(final JComponent component) {
    return components.entrySet().stream().filter(entry -> entry.getValue().equals(component))
            .findFirst().map(Map.Entry::getKey).orElse(null);
  }

  /**
   * Displays a dialog allowing the user the select a input component which should receive the keyboard focus,
   * if only one input component is available then that component is selected automatically.
   * @see #includeComponentSelectionPropertyId(String)
   * @see #requestComponentFocus(String)
   */
  public void selectInputComponent() {
    final List<String> propertyIds = getSelectComponentPropertyIds();
    final List<Property> properties =
            Properties.sort(editModel.getEntityDefinition().getProperties(propertyIds));
    final Property property = properties.size() == 1 ?  properties.get(0) :
            UiUtil.selectValue(this, properties, Messages.get(Messages.SELECT_INPUT_FIELD));
    if (property != null) {
      requestComponentFocus(property.getPropertyId());
    }
  }

  /**
   * Request focus for the component associated with the given propertyId
   * @param propertyId the propertyId of the component to select
   */
  public final void requestComponentFocus(final String propertyId) {
    if (components.containsKey(propertyId)) {
      components.get(propertyId).requestFocus();
    }
  }

  /**
   * @return a list of propertyIds to use when selecting a input component in this panel,
   * this returns all propertyIds that have mapped components in this panel
   * that are enabled, displayable, visible and focusable.
   * @see #includeComponentSelectionPropertyId(String) (String)
   * @see #setComponent(String, javax.swing.JComponent)
   */
  public final List<String> getSelectComponentPropertyIds() {
    final List<String> propertyIds = getComponentPropertyIds();
    propertyIds.removeIf(propertyId -> {
      final JComponent component = getComponent(propertyId);

      return component == null || !includeComponentSelectionPropertyId(propertyId) || !component.isDisplayable() ||
              !component.isVisible() || !component.isFocusable() || !component.isEnabled();
    });

    return propertyIds;
  }

  /**
   * Override to exclude components from the component focus selection.
   * @param propertyId the component propertyId
   * @return true if the component associated with the given propertyId should be included when allowing the user
   * to select a input component in this panel, true by default.
   */
  public boolean includeComponentSelectionPropertyId(final String propertyId) {
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
  public final void setReferentialIntegrityErrorHandling(final EntityTablePanel.ReferentialIntegrityErrorHandling referentialIntegrityErrorHandling) {
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
    return Controls.control(editModel::refresh, FrameworkMessages.get(FrameworkMessages.REFRESH),
            getActiveObserver(), FrameworkMessages.get(FrameworkMessages.REFRESH_TIP) + ALT_PREFIX
                    + mnemonic + ")", mnemonic.charAt(0), null, Images.loadImage(Images.IMG_REFRESH_16));
  }

  /**
   * @return a control for deleting the active entity
   */
  public final Control getDeleteControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC);
    return Controls.control(this::delete, FrameworkMessages.get(FrameworkMessages.DELETE),
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
    return Controls.control(() -> prepareUI(true, true), FrameworkMessages.get(FrameworkMessages.CLEAR),
            getActiveObserver(), FrameworkMessages.get(FrameworkMessages.CLEAR_ALL_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_NEW_16));
  }

  /**
   * @return a control for performing an update on the active entity
   */
  public final Control getUpdateControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC);
    return Controls.control(this::update, FrameworkMessages.get(FrameworkMessages.UPDATE),
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
    return Controls.control(this::insert, FrameworkMessages.get(FrameworkMessages.INSERT),
            States.aggregateState(Conjunction.AND, getActiveObserver(), editModel.getAllowInsertObserver()),
            FrameworkMessages.get(FrameworkMessages.INSERT_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_ADD_16));
  }

  /**
   * @return a control for performing a save on the active entity, that is, update if an entity
   * is selected and modified or insert otherwise
   */
  public final Control getSaveControl() {
    final String mnemonic = FrameworkMessages.get(FrameworkMessages.SAVE_MNEMONIC);
    final State insertUpdateState = States.aggregateState(Conjunction.OR, editModel.getAllowInsertObserver(),
            States.aggregateState(Conjunction.AND, editModel.getAllowUpdateObserver(),
                    editModel.getModifiedObserver()));
    return Controls.control(this::save, FrameworkMessages.get(FrameworkMessages.SAVE),
            States.aggregateState(Conjunction.AND, getActiveObserver(), insertUpdateState),
            FrameworkMessages.get(FrameworkMessages.SAVE_TIP) + ALT_PREFIX + mnemonic + ")",
            mnemonic.charAt(0), null, Images.loadImage(Images.IMG_ADD_16));
  }

  /**
   * Handles the given exception, which usually means simply logging it and displaying it to the user.
   * @param throwable the exception to handle
   */
  public final void handleException(final Throwable throwable) {
    LOG.error(throwable.getMessage(), throwable);
    if (throwable instanceof ValidationException) {
      handleException((ValidationException) throwable);
    }
    else if (throwable instanceof DatabaseException) {
      handleException((DatabaseException) throwable);
    }
    else {
      displayException(throwable, UiUtil.getParentWindow(this));
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void displayException(final Throwable throwable, final Window dialogParent) {
    DefaultDialogExceptionHandler.getInstance().displayException(throwable, dialogParent);
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
        if (clearAfterInsert) {
          clearModelValues();
        }
        if (requestFocusAfterInsert) {
          requestInitialFocus(true);
        }
        return true;
      }
    }
    catch (final Exception ex) {
      handleException(ex);
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
    catch (final ReferentialIntegrityException e) {
      if (referentialIntegrityErrorHandling == EntityTablePanel.ReferentialIntegrityErrorHandling.DEPENDENCIES) {
        EntityTablePanel.showDependenciesDialog(singletonList(editModel.getEntityCopy()),
                getEditModel().getConnectionProvider(), this);
      }
      else {
        handleException(e);
      }
    }
    catch (final Exception ex) {
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
        requestInitialFocus(false);

        return true;
      }
    }
    catch (final Exception ex) {
      handleException(ex);
    }

    return false;
  }

  //#############################################################################################
  // End - control methods
  //#############################################################################################

  /**
   * Creates a new Action which shows the edit panel provided by {@code panelProvider} and if an insert is performed
   * selects the new entity in the {@code lookupField}.
   * @param comboBox the combo box in which to select the new entity, if created
   * @param panelProvider the EntityPanelProvider for providing the EntityEditPanel to use for creating the new entity
   * @return the Action
   */
  public static Action createEditPanelAction(final EntityComboBox comboBox, final EntityPanelProvider panelProvider) {
    return new InsertEntityAction(comboBox, panelProvider);
  }

  /**
   * Creates a new Action which shows the edit panel provided by {@code panelProvider} and if an insert is performed
   * selects the new entity in the {@code lookupField}.
   * @param lookupField the lookup field in which to select the new entity, if created
   * @param panelProvider the EntityPanelProvider for providing the EntityEditPanel to use for creating the new entity
   * @return the Action
   */
  public static Action createEditPanelAction(final EntityLookupField lookupField, final EntityPanelProvider panelProvider) {
    return new InsertEntityAction(lookupField, panelProvider);
  }

  /**
   * Creates a new Action which shows the edit panel provided by {@code panelProvider} and if an insert is performed
   * {@code insertListener} is notified.
   * @param component this component used as dialog parent, receives the focus after insert
   * @param panelProvider the EntityPanelProvider for providing the EntityEditPanel to use for creating the new entity
   * @param connectionProvider the connection provider
   * @param insertListener the listener notified when insert has been performed
   * @return the Action
   */
  public static Action createEditPanelAction(final JComponent component, final EntityPanelProvider panelProvider,
                                             final EntityConnectionProvider connectionProvider,
                                             final EventDataListener<List<Entity>> insertListener) {
    return new InsertEntityAction(component, panelProvider, connectionProvider, insertListener);
  }

  /**
   * for overriding, called before insert/update
   * @throws ValidationException in case of a validation failure
   */
  protected void validateData() throws ValidationException {}

  /**
   * Handles ValidationExceptions.
   * By default displays the exception message to the user and requests focus for the component involved.
   * @param exception the exception to handle
   */
  protected void handleException(final ValidationException exception) {
    JOptionPane.showMessageDialog(this, exception.getMessage(), Messages.get(Messages.EXCEPTION),
            JOptionPane.ERROR_MESSAGE);
    requestComponentFocus((String) exception.getKey());
  }

  /**
   * Handles DatabaseExceptions
   * By default displays the exception message to the user.
   * @param exception the exception to handle
   */
  protected void handleException(final DatabaseException exception) {
    displayException(exception, UiUtil.getParentWindow(this));
  }

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
   * Initializes a ControlSet on which to base the control panel
   * @return the ControlSet on which to base the control panel
   */
  protected ControlSet initializeControlPanelControlSet() {
    final ControlSet controlSet = new ControlSet("Actions");
    if (controls.containsKey(ControlCode.SAVE)) {
      controlSet.add(controls.get(ControlCode.SAVE));
    }
    if (controls.containsKey(ControlCode.INSERT)) {
      controlSet.add(controls.get(ControlCode.INSERT));
    }
    if (controls.containsKey(ControlCode.UPDATE)) {
      controlSet.add(controls.get(ControlCode.UPDATE));
    }
    if (controls.containsKey(ControlCode.DELETE)) {
      controlSet.add(controls.get(ControlCode.DELETE));
    }
    if (controls.containsKey(ControlCode.CLEAR)) {
      controlSet.add(controls.get(ControlCode.CLEAR));
    }
    if (controls.containsKey(ControlCode.REFRESH)) {
      controlSet.add(controls.get(ControlCode.REFRESH));
    }

    return controlSet;
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
   * @return the component that should get the initial focus when the UI is prepared
   * @see #prepareUI(boolean, boolean)
   */
  protected JComponent getInitialFocusComponent() {
    if (initialFocusComponent != null) {
      return initialFocusComponent;
    }

    if (initialFocusPropertyId != null) {
      return components.get(initialFocusPropertyId);
    }

    return null;
  }

  /**
   * @return the component that should get the focus when the UI is prepared after insert
   */
  protected JComponent getAfterInsertFocusComponent() {
    if (afterInsertFocusComponent != null) {
      return afterInsertFocusComponent;
    }

    if (afterInsertFocusPropertyId != null) {
      return components.get(afterInsertFocusPropertyId);
    }

    return getInitialFocusComponent();
  }

  /**
   * Associates the given input component with the given propertyId,
   * preferably this should be called for components associated with
   * properties.
   * @param propertyId the propertyId
   * @param component the input component
   */
  protected final void setComponent(final String propertyId, final JComponent component) {
    if (components.containsKey(propertyId)) {
      throw new IllegalStateException("Component already set for propertyId: " + propertyId);
    }
    components.put(propertyId, component);
  }

  /**
   * Adds a property panel for the given property to this panel
   * @param propertyId the ID of the property
   * @see #createPropertyPanel(String)
   */
  protected final void addPropertyPanel(final String propertyId) {
    add(createPropertyPanel(propertyId));
  }

  /**
   * Creates a panel containing a label and the component associated with the given property.
   * The label text is the caption of the property identified by {@code propertyId}.
   * The default layout of the resulting panel is with the label on top and inputComponent below.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @return a panel containing a label and a component
   * @throws IllegalArgumentException in case no component has been associated with the given property
   */
  protected final JPanel createPropertyPanel(final String propertyId) {
    final JComponent component = getComponent(propertyId);
    if (component == null) {
      throw new IllegalArgumentException("No component associated with property: " + propertyId);
    }

    return createPropertyPanel(propertyId, component);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by {@code propertyId}.
   * The default layout of the resulting panel is with the label on top and {@code inputComponent} below.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyId, final JComponent inputComponent) {
    return createPropertyPanel(propertyId, inputComponent, true);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by {@code propertyId}.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @param labelOnTop if true then the label is positioned above {@code inputComponent},
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyId, final JComponent inputComponent,
                                             final boolean labelOnTop) {
    return createPropertyPanel(propertyId, inputComponent, labelOnTop, JLabel.LEADING);
  }

  /**
   * Creates a panel containing a label and the given component.
   * The label text is the caption of the property identified by {@code propertyId}.
   * @param propertyId the id of the property from which to retrieve the label caption
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @param labelOnTop if true then the label is positioned above {@code inputComponent},
   * otherwise it uses FlowLayout.LEADING in a FlowLayout.
   * @param labelAlignment the label alignment
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final String propertyId, final JComponent inputComponent,
                                             final boolean labelOnTop, final int labelAlignment) {
    return createPropertyPanel(createLabel(propertyId, labelAlignment), inputComponent, labelOnTop);
  }

  /**
   * Creates a panel containing a label component and the {@code inputComponent} with the label
   * component positioned above the input component.
   * @param labelComponent the label component
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @return a panel containing a label and a component
   */
  protected final JPanel createPropertyPanel(final JComponent labelComponent, final JComponent inputComponent) {
    return createPropertyPanel(labelComponent, inputComponent, true);
  }

  /**
   * Creates a panel containing a label component and the {@code inputComponent}.
   * @param labelComponent the label component
   * @param inputComponent a component bound to the property with id {@code propertyId}
   * @param labelOnTop if true then the label is positioned above {@code inputComponent},
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
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId) {
    return createTextArea(propertyId, -1, -1);
  }

  /**
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId, final int rows, final int columns) {
    return createTextArea(propertyId, rows, columns, true);
  }

  /**
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId, final int rows, final int columns,
                                           final boolean updateOnKeystroke) {
    return createTextArea(propertyId, rows, columns, updateOnKeystroke, null);
  }

  /**
   * Creates a JTextArea component bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param rows the number of rows in the text area
   * @param columns the number of columns in the text area
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param enabledState a state indicating when this text area should be enabled
   * @return a JTextArea bound to the property
   */
  protected final JTextArea createTextArea(final String propertyId, final int rows, final int columns,
                                           final boolean updateOnKeystroke, final StateObserver enabledState) {
    final Property property = editModel.getEntityDefinition().getProperty(propertyId);
    final JTextArea textArea = EntityUiUtil.createTextArea(property, editModel, rows, columns, updateOnKeystroke, enabledState);
    setComponent(propertyId, textArea);

    return textArea;
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyId) {
    return createTextInputPanel(propertyId, true, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final String propertyId, final boolean updateOnKeystroke,
                                                      final boolean buttonFocusable) {
    return createTextInputPanel(editModel.getEntityDefinition().getProperty(propertyId),
            updateOnKeystroke, buttonFocusable);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param property the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final boolean updateOnKeystroke) {
    return createTextInputPanel(property, updateOnKeystroke, true);
  }

  /**
   * Creates a TextInputPanel bound to the property identified by {@code propertyId}.
   * @param property the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param buttonFocusable specifies whether the edit button should be focusable.
   * @return a TextInputPanel bound to the property
   */
  protected final TextInputPanel createTextInputPanel(final Property property, final boolean updateOnKeystroke,
                                                      final boolean buttonFocusable) {
    final TextInputPanel inputPanel = EntityUiUtil.createTextInputPanel(property, editModel, updateOnKeystroke, buttonFocusable);
    setComponent(property.getPropertyId(), inputPanel.getTextField());

    return inputPanel;
  }

  /**
   * Creates a new DateInputPanel using the default short date format, bound to the property
   * identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @return a DateInputPanel using the default short date format
   * @see Property#DATE_FORMAT
   */
  protected final TemporalInputPanel createDateInputPanel(final String propertyId) {
    return createDateInputPanel(propertyId, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel using the default short date format
   * @see Property#DATE_FORMAT
   */
  protected final TemporalInputPanel createDateInputPanel(final String propertyId, final boolean includeButton) {
    final Property property = editModel.getEntityDefinition().getProperty(propertyId);
    return createDateInputPanel(property, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final TemporalInputPanel createDateInputPanel(final String propertyId, final boolean includeButton,
                                                          final StateObserver enabledState) {
    return createDateInputPanel(propertyId, includeButton, enabledState, true);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by {@code propertyId}.
   * @param propertyId the ID of the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a DateInputPanel bound to the property
   */
  protected final TemporalInputPanel createDateInputPanel(final String propertyId, final boolean includeButton,
                                                          final StateObserver enabledState, final boolean updateOnKeystroke) {
    return createDateInputPanel(editModel.getEntityDefinition().getProperty(propertyId),
            includeButton, enabledState, updateOnKeystroke);
  }

  /**
   * Creates a new DateInputPanel bound to the property identified by {@code propertyId}.
   * @param property the property for which to create the panel
   * @return a DateInputPanel bound to the property
   */
  protected final TemporalInputPanel createDateInputPanel(final Property property) {
    return createDateInputPanel(property, true);
  }

  /**
   * Creates a new DateInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @return a DateInputPanel bound to the property
   */
  protected final TemporalInputPanel createDateInputPanel(final Property property, final boolean includeButton) {
    return createDateInputPanel(property, includeButton, null);
  }

  /**
   * Creates a new DateInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @return a DateInputPanel bound to the property
   */
  protected final TemporalInputPanel createDateInputPanel(final Property property, final boolean includeButton,
                                                          final StateObserver enabledState) {
    return createDateInputPanel(property, includeButton, enabledState, true);
  }

  /**
   * Creates a new DateInputPanel bound to the given property.
   * @param property the property for which to create the panel
   * @param includeButton if true a button for visually editing the date is included
   * @param enabledState a state for controlling the enabled state of the input component
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a DateInputPanel bound to the property
   */
  protected final TemporalInputPanel createDateInputPanel(final Property property, final boolean includeButton,
                                                          final StateObserver enabledState, final boolean updateOnKeystroke) {
    final TemporalInputPanel panel = EntityUiUtil.createDateInputPanel(property, editModel, updateOnKeystroke, includeButton, enabledState);
    setComponent(property.getPropertyId(), panel);

    return panel;
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId) {
    return createTextField(propertyId, true);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final boolean updateOnKeystroke) {
    return createTextField(propertyId, updateOnKeystroke, null);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final boolean updateOnKeystroke,
                                             final String maskString) {
    return createTextField(propertyId, updateOnKeystroke, maskString, null);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final boolean updateOnKeystroke,
                                             final String maskString, final StateObserver enabledState) {
    return createTextField(propertyId, updateOnKeystroke, maskString, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if {@code maskString} is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final String propertyId, final boolean updateOnKeystroke,
                                             final String maskString, final StateObserver enabledState,
                                             final boolean valueIncludesLiteralCharacters) {
    return createTextField(editModel.getEntityDefinition().getProperty(propertyId),
            updateOnKeystroke, maskString, enabledState, valueIncludesLiteralCharacters);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property) {
    return createTextField(property, true);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final boolean updateOnKeystroke) {
    return createTextField(property, null, updateOnKeystroke);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final String maskString,
                                             final boolean updateOnKeystroke) {
    return createTextField(property, maskString, updateOnKeystroke, null);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the ID of the property to bind
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param enabledState a state for controlling the enabled state of the component
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final String maskString,
                                             final boolean updateOnKeystroke, final StateObserver enabledState) {
    return createTextField(property, updateOnKeystroke, maskString, enabledState, false);
  }

  /**
   * Creates a JTextField bound to the given property
   * @param property the property to bind
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @param maskString if specified then a JFormattedTextField with the given mask is returned
   * @param enabledState a state for controlling the enabled state of the component
   * @param valueIncludesLiteralCharacters only applicable if {@code maskString} is specified
   * @return a text field bound to the property
   */
  protected final JTextField createTextField(final Property property, final boolean updateOnKeystroke,
                                             final String maskString, final StateObserver enabledState,
                                             final boolean valueIncludesLiteralCharacters) {
    final JTextField textField = EntityUiUtil.createTextField(property, editModel,  maskString, updateOnKeystroke,
            enabledState, valueIncludesLiteralCharacters);
    setComponent(property.getPropertyId(), textField);

    return textField;
  }

  /**
   * Creates a JCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyId) {
    return createCheckBox(propertyId, null);
  }

  /**
   * Creates a JCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyId, final StateObserver enabledState) {
    return createCheckBox(propertyId, enabledState, true);
  }

  /**
   * Creates a JCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a JCheckBox bound to the property
   */
  protected final JCheckBox createCheckBox(final String propertyId, final StateObserver enabledState,
                                           final boolean includeCaption) {
    return createCheckBox(editModel.getEntityDefinition().getProperty(propertyId), enabledState, includeCaption);
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
    setComponent(property.getPropertyId(), box);

    return box;
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyId) {
    return createTristateCheckBox(propertyId, null);
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyId, final StateObserver enabledState) {
    return createTristateCheckBox(propertyId, enabledState, true);
  }

  /**
   * Creates a TristateCheckBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param includeCaption specifies whether or not the caption should be included
   * @return a TristateCheckBox bound to the property
   */
  protected final TristateCheckBox createTristateCheckBox(final String propertyId, final StateObserver enabledState,
                                                          final boolean includeCaption) {
    return createTristateCheckBox(editModel.getEntityDefinition().getProperty(propertyId), enabledState, includeCaption);
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
    setComponent(property.getPropertyId(), box);

    return box;
  }

  /**
   * Create a JComboBox for the property identified by {@code propertyId}, containing
   * values for the boolean values: true, false, null
   * @param propertyId the ID of the property to bind
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyId) {
    return createBooleanComboBox(propertyId, null);
  }

  /**
   * Create a JComboBox for the property identified by {@code propertyId}, containing
   * values for the boolean values: true, false, null
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return JComboBox for the given property
   */
  protected final JComboBox createBooleanComboBox(final String propertyId, final StateObserver enabledState) {
    return createBooleanComboBox(editModel.getEntityDefinition().getProperty(propertyId), enabledState);
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
    final JComboBox comboBox = EntityUiUtil.createBooleanComboBox(property, editModel, enabledState);
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyId, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch) {
    return createComboBox(propertyId, comboBoxModel, maximumMatch, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final String propertyId, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final StateObserver enabledState) {
    return createComboBox(editModel.getEntityDefinition().getProperty(propertyId),
            comboBoxModel, maximumMatch, enabledState);
  }

  /**
   * Creates a SteppedComboBox bound to the given property
   * @param property the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param maximumMatch true if maximum match should be used
   * @return a SteppedComboBox bound the the property
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
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
   * @see org.jminor.swing.common.ui.combobox.MaximumMatch
   */
  protected final SteppedComboBox createComboBox(final Property property, final ComboBoxModel comboBoxModel,
                                                 final boolean maximumMatch, final StateObserver enabledState) {
    final SteppedComboBox comboBox = EntityUiUtil.createComboBox(property, editModel, comboBoxModel, enabledState);
    if (maximumMatch) {
      MaximumMatch.enable(comboBox);
    }
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId) {
    return createValueListComboBox(propertyId, true);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId, final boolean sortItems) {
    return createValueListComboBox(propertyId, sortItems, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId, final StateObserver enabledState) {
    return createValueListComboBox(propertyId, true, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined by the given value list property,
   * bound to the given property.
   * @param propertyId the propertyId
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   * @throws IllegalArgumentException in case the property is not a value list property
   */
  protected final SteppedComboBox createValueListComboBox(final String propertyId, final boolean sortItems, final StateObserver enabledState) {
    final Property property = editModel.getEntityDefinition().getProperty(propertyId);
    if (!(property instanceof ValueListProperty)) {
      throw new IllegalArgumentException("Property identified by '" + propertyId + "' is not a ValueListProperty");
    }

    return createValueListComboBox((ValueListProperty) property, sortItems, enabledState);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property) {
    return createValueListComboBox(property, true);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param sortItems if true the items are sorted, otherwise the original ordering is preserved
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property, final boolean sortItems) {
    return createValueListComboBox(property, sortItems, null);
  }

  /**
   * Creates a SteppedComboBox containing the values defined in the given value list property,
   * bound to the given property.
   * @param property the property
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property, final StateObserver enabledState) {
    final SteppedComboBox box = EntityUiUtil.createValueListComboBox(property, editModel, true, enabledState);
    setComponent(property.getPropertyId(), box);

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
  protected final SteppedComboBox createValueListComboBox(final ValueListProperty property, final boolean sortItems,
                                                          final StateObserver enabledState) {
    final SteppedComboBox box = EntityUiUtil.createValueListComboBox(property, editModel, sortItems, enabledState);
    setComponent(property.getPropertyId(), box);

    return box;
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyId, final ComboBoxModel comboBoxModel) {
    return createEditableComboBox(propertyId, comboBoxModel, null);
  }

  /**
   * Creates an editable SteppedComboBox bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @param comboBoxModel the ComboBoxModel
   * @param enabledState a state for controlling the enabled state of the component
   * @return an editable SteppedComboBox bound the the property
   */
  protected final SteppedComboBox createEditableComboBox(final String propertyId, final ComboBoxModel comboBoxModel,
                                                         final StateObserver enabledState) {
    return createEditableComboBox(editModel.getEntityDefinition().getProperty(propertyId),
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
    final SteppedComboBox comboBox = EntityUiUtil.createComboBox(property, editModel, comboBoxModel, enabledState, true);
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}, the combo box
   * contains the underlying values of the property
   * @param propertyId the ID of the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyId) {
    return createPropertyComboBox(propertyId, null);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}, the combo box
   * contains the underlying values of the property
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyId, final StateObserver enabledState) {
    return createPropertyComboBox(propertyId, enabledState, false);
  }

  /**
   * Creates a SteppedComboBox bound to the property identified by {@code propertyId}, the combo box
   * contains the underlying values of the property
   * @param propertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @param editable true if the combo box should be editable, only works with combo boxes based on String.class properties
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final String propertyId, final StateObserver enabledState,
                                                         final boolean editable) {
    return createPropertyComboBox(editModel.getEntityDefinition().getColumnProperty(propertyId),
            enabledState, editable);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final ColumnProperty property) {
    return createPropertyComboBox(property, null);
  }

  /**
   * Creates a SteppedComboBox bound to the given property, the combo box
   * contains the underlying values of the property
   * @param property the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a SteppedComboBox bound to the property
   */
  protected final SteppedComboBox createPropertyComboBox(final ColumnProperty property, final StateObserver enabledState) {
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
  protected final SteppedComboBox createPropertyComboBox(final ColumnProperty property, final StateObserver enabledState,
                                                         final boolean editable) {
    final SteppedComboBox comboBox = EntityUiUtil.createPropertyComboBox(property, editModel, enabledState, editable);
    setComponent(property.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates a EntityComboBox bound to the foreign key property identified by {@code foreignKeyPropertyId}
   * @param foreignKeyPropertyId the ID of the property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return a EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final String foreignKeyPropertyId, final StateObserver enabledState) {
    return createForeignKeyComboBox((ForeignKeyProperty)
            editModel.getEntityDefinition().getProperty(foreignKeyPropertyId), enabledState);
  }

  /**
   * Creates an EntityComboBox bound to the foreign key property identified by {@code foreignKeyPropertyId}
   * @param foreignKeyPropertyId the ID of the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final String foreignKeyPropertyId) {
    return createForeignKeyComboBox(editModel.getEntityDefinition().getForeignKeyProperty(
            foreignKeyPropertyId), null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final ForeignKeyProperty foreignKeyProperty) {
    return createForeignKeyComboBox(foreignKeyProperty, null);
  }

  /**
   * Creates an EntityComboBox bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * combination used to create new instances of the entity this EntityComboBox is based on
   * EntityComboBox is focusable
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityComboBox bound to the property
   */
  protected final EntityComboBox createForeignKeyComboBox(final ForeignKeyProperty foreignKeyProperty,
                                                          final StateObserver enabledState) {
    final EntityComboBox comboBox = EntityUiUtil.createForeignKeyComboBox(foreignKeyProperty, editModel, enabledState);
    setComponent(foreignKeyProperty.getPropertyId(), comboBox);

    return comboBox;
  }

  /**
   * Creates an EntityLookupField bound to the property identified by {@code foreignKeypropertyId}, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyId the ID of the foreign key property to bind
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final String foreignKeyPropertyId) {
    return createForeignKeyLookupField(foreignKeyPropertyId, (StateObserver) null);
  }

  /**
   * Creates an EntityLookupField bound to the property identified by {@code foreignKeypropertyId}, the property
   * must be an Property.ForeignKeyProperty
   * @param foreignKeyPropertyId the ID of the foreign key property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final String foreignKeyPropertyId,
                                                                final StateObserver enabledState) {
    final ForeignKeyProperty fkProperty =
            editModel.getEntityDefinition().getForeignKeyProperty(foreignKeyPropertyId);

    return createForeignKeyLookupField(fkProperty, enabledState);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final ForeignKeyProperty foreignKeyProperty) {
    return createForeignKeyLookupField(foreignKeyProperty, null);
  }

  /**
   * Creates an EntityLookupField bound to the given foreign key property
   * @param foreignKeyProperty the foreign key property to bind
   * @param enabledState a state for controlling the enabled state of the component
   * @return an EntityLookupField bound the property
   */
  protected final EntityLookupField createForeignKeyLookupField(final ForeignKeyProperty foreignKeyProperty,
                                                                final StateObserver enabledState) {
    final EntityLookupField lookupField = EntityUiUtil.createForeignKeyLookupField(foreignKeyProperty, editModel, enabledState);
    setComponent(foreignKeyProperty.getPropertyId(), lookupField);

    return lookupField;
  }

  /**
   * Creates an uneditable JTextField bound to the property identified by {@code propertyId}
   * @param propertyId the ID of the property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createForeignKeyField(final String propertyId) {
    return createForeignKeyField(editModel.getEntityDefinition().getForeignKeyProperty(propertyId));
  }

  /**
   * Creates an uneditable JTextField bound to the given property
   * @param foreignKeyProperty the foreign key property to bind
   * @return an uneditable JTextField bound to the property
   */
  protected final JTextField createForeignKeyField(final ForeignKeyProperty foreignKeyProperty) {
    final JTextField textField = EntityUiUtil.createForeignKeyField(foreignKeyProperty, editModel);
    setComponent(foreignKeyProperty.getPropertyId(), textField);

    return textField;
  }

  /**
   * Creates a JLabel with a caption from the property identified by {@code propertyId}
   * @param propertyId the ID of the property from which to retrieve the caption
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyId) {
    return createLabel(propertyId, JLabel.LEFT);
  }

  /**
   * Creates a JLabel with a caption from the given property identified by {@code propertyId}
   * @param propertyId the ID of the property from which to retrieve the caption
   * @param horizontalAlignment the horizontal text alignment
   * @return a JLabel for the given property
   */
  protected final JLabel createLabel(final String propertyId, final int horizontalAlignment) {
    return EntityUiUtil.createLabel(editModel.getEntityDefinition().getProperty(propertyId), horizontalAlignment);
  }

  /**
   * Initializes the controls available to this EntityEditPanel by mapping them to their respective
   * control codes ({@link ControlCode#INSERT}, {@link ControlCode#UPDATE} etc)
   * via the {@code setControl(String, Control) method, these can then be retrieved via the {@link #getControl(ControlCode)} method.
   * @param controlCodes the control codes for which controls should be initialized
   * @see org.jminor.swing.common.ui.control.Control
   * @see #setControl(ControlCode, org.jminor.swing.common.ui.control.Control)
   * @see #getControl(ControlCode)
   * todo updateAllowed(false) þá vantar Insert control nema það sé tiltekið í smið
   */
  private void setupControls(final ControlCode... controlCodes) {
    if (controlCodes == null || controlCodes.length == 0) {
      return;
    }
    final Collection<ControlCode> codes = asList(controlCodes);
    if (!editModel.isReadOnly()) {
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
    if (editModel.isInsertAllowed() && editModel.isUpdateAllowed() && controlCodes.contains(ControlCode.SAVE)) {
      setControl(ControlCode.SAVE, getSaveControl());
    }
    if (editModel.isInsertAllowed() && controlCodes.contains(ControlCode.INSERT)) {
      setControl(ControlCode.INSERT, getInsertControl());
    }
    if (editModel.isUpdateAllowed() && controlCodes.contains(ControlCode.UPDATE)) {
      setControl(ControlCode.UPDATE, getUpdateControl());
    }
    if (editModel.isDeleteAllowed() && controlCodes.contains(ControlCode.DELETE)) {
      setControl(ControlCode.DELETE, getDeleteControl());
    }
  }

  private void bindEventsInternal() {
    addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(final MouseEvent e) {
        if (e.isAltDown() && e.getClickCount() == 2) {
          EntityUiUtil.showEntityMenu(getEditModel().getEntityCopy(), EntityEditPanel.this, new Point(e.getX(), e.getY()), getEditModel().getConnectionProvider());
        }
      }
    });
    UiUtil.addKeyEvent(this, KeyEvent.VK_V, KeyEvent.CTRL_DOWN_MASK + KeyEvent.ALT_DOWN_MASK,
            WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, Controls.control(() -> {
              final int x = getBounds().getLocation().x + ENTITY_MENU_X_OFFSET;
              final int y = getHeight();
              EntityUiUtil.showEntityMenu(getEditModel().getEntityCopy(), EntityEditPanel.this, new Point(x, y),
                      getEditModel().getConnectionProvider());
            }, "EntityEditPanel.showEntityMenu"));
    editModel.addBeforeRefreshListener(() -> UiUtil.setWaitCursor(true, EntityEditPanel.this));
    editModel.addAfterRefreshListener(() -> UiUtil.setWaitCursor(false, EntityEditPanel.this));
    editModel.addConfirmSetEntityObserver(confirmationState -> {
      final int result = JOptionPane.showConfirmDialog(UiUtil.getParentWindow(EntityEditPanel.this),
              FrameworkMessages.get(FrameworkMessages.UNSAVED_DATA_WARNING), FrameworkMessages.get(FrameworkMessages.UNSAVED_DATA_WARNING_TITLE),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      confirmationState.set(result == JOptionPane.YES_OPTION);
    });
  }

  private void requestInitialFocus(final boolean afterInsert) {
    final JComponent focusComponent = afterInsert ? getAfterInsertFocusComponent() : getInitialFocusComponent();
    if (focusComponent != null && focusComponent.isFocusable()) {
      focusComponent.requestFocus();
    }
    else {
      requestFocus();
    }
  }

  private static final class InsertEntityAction extends AbstractAction {

    private final JComponent component;
    private final EntityPanelProvider panelProvider;
    private final EntityConnectionProvider connectionProvider;
    private final EventDataListener<List<Entity>> insertListener;
    private final List<Entity> lastInsertedEntities = new ArrayList<>();

    private InsertEntityAction(final EntityComboBox comboBox, final EntityPanelProvider panelProvider) {
      this(comboBox, panelProvider, ((EntityComboBoxModel) comboBox.getModel()).getConnectionProvider(), insertedEntities -> {
        final EntityComboBoxModel comboBoxModel = (EntityComboBoxModel) comboBox.getModel();
        final Entity item = insertedEntities.get(0);
        comboBoxModel.addItem(item);
        comboBoxModel.setSelectedItem(item);
      });
    }

    private InsertEntityAction(final EntityLookupField lookupField, final EntityPanelProvider panelProvider) {
      this(lookupField, panelProvider, lookupField.getModel().getConnectionProvider(), insertedEntities ->
              lookupField.getModel().setSelectedEntities(insertedEntities));
    }

    private InsertEntityAction(final JComponent component, final EntityPanelProvider panelProvider,
                               final EntityConnectionProvider connectionProvider,
                               final EventDataListener<List<Entity>> insertListener) {
      super("", Images.loadImage(Images.IMG_ADD_16));
      this.component = component;
      this.panelProvider = panelProvider;
      this.connectionProvider = connectionProvider;
      this.insertListener = insertListener;
      this.component.addPropertyChangeListener("enabled", changeEvent -> setEnabled((Boolean) changeEvent.getNewValue()));
      setEnabled(component.isEnabled());
      addLookupKey();
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      final EntityEditPanel editPanel = panelProvider.createEditPanel(connectionProvider);
      editPanel.initializePanel();
      editPanel.getEditModel().addAfterInsertListener(data -> {
        lastInsertedEntities.clear();
        lastInsertedEntities.addAll(data.getInsertedEntities());
      });
      final JOptionPane pane = new JOptionPane(editPanel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION);
      final JDialog dialog = pane.createDialog(component, panelProvider.getCaption() == null ?
              connectionProvider.getDomain().getDefinition(panelProvider.getEntityId()).getCaption() :
              panelProvider.getCaption());
      dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
      UiUtil.addInitialFocusHack(editPanel, new InitialFocusAction(editPanel));
      dialog.setVisible(true);
      if (pane.getValue() != null && pane.getValue().equals(0)) {
        final boolean insertPerformed = editPanel.insert();//todo exception during insert, f.ex validation failure not handled
        if (insertPerformed && !lastInsertedEntities.isEmpty()) {
          insertListener.eventOccurred(lastInsertedEntities);
        }
      }
      component.requestFocusInWindow();
    }

    private void addLookupKey() {
      JComponent keyComponent = component;
      if (component instanceof JComboBox && ((JComboBox) component).isEditable()) {
        keyComponent = (JComponent) ((JComboBox) component).getEditor().getEditorComponent();
      }
      UiUtil.addKeyEvent(keyComponent, KeyEvent.VK_ADD, KeyEvent.CTRL_DOWN_MASK, this);
      UiUtil.addKeyEvent(keyComponent, KeyEvent.VK_PLUS, KeyEvent.CTRL_DOWN_MASK, this);
    }
  }

  private static final class InitialFocusAction extends AbstractAction {

    private final EntityEditPanel editPanel;

    private InitialFocusAction(final EntityEditPanel editPanel) {
      this.editPanel = editPanel;
    }

    @Override
    public void actionPerformed(final ActionEvent e) {
      editPanel.requestInitialFocus();
    }
  }
}
