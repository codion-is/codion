/*
 * Copyright (c) 2009 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.db.exception.ReferentialIntegrityException;
import is.codion.common.i18n.Messages;
import is.codion.common.property.PropertyValue;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntityEditModel;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.WaitCursor;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.component.EntityComboBox;
import is.codion.swing.framework.ui.component.EntityComponents;
import is.codion.swing.framework.ui.component.EntitySearchField;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.border.Borders.emptyBorder;
import static is.codion.swing.framework.ui.EntityDependenciesPanel.displayDependenciesDialog;
import static java.awt.event.InputEvent.ALT_DOWN_MASK;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.VK_INSERT;
import static java.awt.event.KeyEvent.VK_V;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Objects.requireNonNull;
import static javax.swing.JOptionPane.showConfirmDialog;

/**
 * A UI component based on a {@link EntityEditModel}.
 */
public abstract class EntityEditPanel extends EntityEditComponentPanel {

  private static final Logger LOG = LoggerFactory.getLogger(EntityEditPanel.class);

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(EntityEditPanel.class.getName());
  private static final ResourceBundle TABLE_PANEL_MESSAGES = ResourceBundle.getBundle(EntityTablePanel.class.getName());

  private static final Confirmer DEFAULT_INSERT_CONFIRMER = new InsertConfirmer();
  private static final Confirmer DEFAULT_UPDATE_CONFIRMER = new UpdateConfirmer();
  private static final Confirmer DEFAULT_DELETE_CONFIRMER = new DeleteConfirmer();

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
   * Specifies whether to include a {@link EntityPopupMenu} on this edit panel, triggered with CTRL-ALT-V.<br>
   * Value type: Boolean<br>
   * Default value: true
   */
  public static final PropertyValue<Boolean> INCLUDE_ENTITY_MENU =
          Configuration.booleanValue("is.codion.swing.framework.ui.EntityEditPanel.includeEntityMenu", true);

  /**
   * The standard controls available to the EditPanel
   */
  public enum ControlCode {
    INSERT, UPDATE, DELETE, REFRESH, CLEAR
  }

  private static final ControlCode[] DEFAULT_CONTROL_CODES = {
          ControlCode.INSERT, ControlCode.UPDATE, ControlCode.DELETE, ControlCode.CLEAR, ControlCode.REFRESH
  };

  private static final Control NULL_CONTROL = Control.control(() -> {});

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
  private final State active = State.state(!USE_FOCUS_ACTIVATION.get());

  /**
   * The insert, update and delete confirmers
   */
  private final EnumMap<Confirmer.Action, Confirmer> confirmers = new EnumMap<>(Confirmer.Action.class);

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
   * True after {@link #initialize()} has been called
   */
  private boolean initialized = false;

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
    this(editModel, DEFAULT_CONTROL_CODES);
  }

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   * @param entityComponents the entity components instance to use when creating components
   */
  public EntityEditPanel(SwingEntityEditModel editModel, EntityComponents entityComponents) {
    this(editModel, entityComponents, DEFAULT_CONTROL_CODES);
  }

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   * @param controlCodes if specified only controls with those keys are initialized,
   * null or an empty array will result in no controls being initialized
   */
  public EntityEditPanel(SwingEntityEditModel editModel, ControlCode... controlCodes) {
    this(editModel, new EntityComponents(editModel.entityDefinition()), controlCodes);
  }

  /**
   * Instantiates a new EntityEditPanel based on the given {@link EntityEditModel}
   * @param editModel the {@link EntityEditModel} instance to base this EntityEditPanel on
   * @param entityComponents the entity components instance to use when creating components
   * @param controlCodes if specified only controls with those keys are initialized,
   * null or an empty array will result in no controls being initialized
   */
  public EntityEditPanel(SwingEntityEditModel editModel, EntityComponents entityComponents, ControlCode... controlCodes) {
    super(editModel, entityComponents);
    if (USE_FOCUS_ACTIVATION.get()) {
      ACTIVE_STATE_GROUP.add(active);
    }
    this.controlCodes = validateControlCodes(controlCodes);
    if (editModel.entityNew().get()) {
      editModel.setDefaultValues();
    }
  }

  @Override
  public final String toString() {
    return editModel().toString();
  }

  /**
   * @return a {@link State} controlling whether this panel is active, enabled and ready to receive input
   */
  public final State active() {
    return active;
  }

  /**
   * Clears the underlying edit model and requests the initial focus.
   * @see EntityEditModel#setEntity(Entity)
   * @see #requestInitialFocus()
   */
  public final void clearAndRequestFocus() {
    editModel().setDefaultValues();
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
   * Sets the confirmer to use for the given action.
   * @param action the confirmation action
   * @param confirmer the confirmer to use for the given action, null for the default one
   */
  public final void setConfirmer(Confirmer.Action action, Confirmer confirmer) {
    confirmers.put(requireNonNull(action), confirmer);
  }

  /**
   * @param controlCode the control code
   * @return true if this edit panel contains a control assocated with the given {@code controlCode}
   */
  public final boolean containsControl(ControlCode controlCode) {
    Control control = controls.get(requireNonNull(controlCode));

    return control != null && control != NULL_CONTROL;
  }

  /**
   * @param controlCode the control code
   * @return the control associated with {@code controlCode}
   * @throws IllegalArgumentException in case no control is associated with the given {@code controlCode}
   * @see #containsControl(EntityEditPanel.ControlCode)
   */
  public final Control control(ControlCode controlCode) {
    if (!containsControl(controlCode)) {
      throw new IllegalArgumentException(controlCode + " control not available in panel: " + this);
    }

    return controls.get(controlCode);
  }

  /**
   * Initializes this EntityEditPanel.
   * This method marks this panel as initialized which prevents it from running again,
   * whether an exception occurs or not.
   * @return this EntityEditPanel instance
   */
  public final EntityEditPanel initialize() {
    if (!initialized) {
      WaitCursor.show(this);
      try {
        setupControls();
        bindEvents();
        initializeUI();
      }
      finally {
        initialized = true;
        WaitCursor.hide(this);
      }
    }

    return this;
  }

  /**
   * Performs insert on the active entity after asking for confirmation using the {@link Confirmer}
   * associated with the {@link Confirmer.Action#INSERT} action.
   * Note that the default insert {@link Confirmer} simply returns true, so in order to implement
   * a insert confirmation you must set the {@link Confirmer} via {@link #setConfirmer(Confirmer.Action, Confirmer)}.
   * @return true in case of successful insert, false otherwise
   * @see #beforeInsert()
   * @see #setConfirmer(Confirmer.Action, Confirmer)
   */
  public final boolean insertWithConfirmation() {
    if (confirmInsert()) {
      return insert();
    }

    return false;
  }

  /**
   * Performs insert on the active entity without asking for confirmation
   * @return true in case of successful insert, false otherwise
   * @see #beforeInsert()
   */
  public final boolean insert() {
    try {
      beforeInsert();
      WaitCursor.show(this);
      try {
        editModel().insert();
        if (clearAfterInsert) {
          editModel().setDefaultValues();
        }
        if (requestFocusAfterInsert) {
          requestFocusAfterInsert();
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
   * Performs delete on the active entity after asking for confirmation using the {@link Confirmer}
   * associated with the {@link Confirmer.Action#DELETE} action.
   * @return true if the delete operation was successful
   * @see #beforeDelete()
   * @see #setConfirmer(Confirmer.Action, Confirmer)
   */
  public final boolean deleteWithConfirmation() {
    if (confirmDelete()) {
      return delete();
    }

    return false;
  }

  /**
   * Performs delete on the active entity without asking for confirmation
   * @return true if the delete operation was successful
   * @see #beforeDelete()
   */
  public final boolean delete() {
    try {
      beforeDelete();
      WaitCursor.show(this);
      try {
        editModel().delete();
        requestInitialFocus();

        return true;
      }
      finally {
        WaitCursor.hide(this);
      }
    }
    catch (ReferentialIntegrityException e) {
      LOG.debug(e.getMessage(), e);
      onReferentialIntegrityException(e);
    }
    catch (Exception ex) {
      LOG.error(ex.getMessage(), ex);
      onException(ex);
    }

    return false;
  }

  /**
   * Performs update on the active entity after asking for confirmation using the {@link Confirmer}
   * associated with the {@link Confirmer.Action#UPDATE} action.
   * @return true if the update operation was successful
   * @see #beforeUpdate()
   * @see #setConfirmer(Confirmer.Action, Confirmer)
   */
  public final boolean updateWithConfirmation() {
    if (confirmUpdate()) {
      return update();
    }

    return false;
  }

  /**
   * Performs update on the active entity without asking for confirmation.
   * @return true if the update operation was successful
   * @see #beforeUpdate()
   */
  public final boolean update() {
    try {
      beforeUpdate();
      WaitCursor.show(this);
      try {
        editModel().update();
        requestFocusAfterUpdate();

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
   * @return true if this panel has been initialized
   * @see #initialize()
   */
  public final boolean isInitialized() {
    return initialized;
  }

  /**
   * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog and if insert is performed
   * adds the new entity to the {@code comboBox} and selects it.
   * Creates a INSERT key binding on the given component for triggering the resulting Control.
   * @param comboBox the combo box in which to select the new entity
   * @param editPanelSupplier the edit panel supplier
   * @return the insert Control
   */
  public static Control createInsertControl(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
    return createInsertControl(new InsertEntityCommand(requireNonNull(comboBox), requireNonNull(editPanelSupplier)), comboBox);
  }

  /**
   * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog and if insert is performed
   * selects the new entity in the {@code searchField}.
   * Creates a INSERT key binding on the given component for triggering the resulting Control.
   * @param searchField the search field in which to select the new entity
   * @param editPanelSupplier the edit panel supplier
   * @return the insert Control
   */
  public static Control createInsertControl(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
    return createInsertControl(new InsertEntityCommand(requireNonNull(searchField), requireNonNull(editPanelSupplier)), searchField);
  }

  /**
   * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog displaying
   * the selected item for editing, and replaces the updated entity in the combo box.
   * Creates a CTRL-INSERT key binding on the given component for triggering the resulting Control.
   * @param comboBox the combo box which selected item to edit
   * @param editPanelSupplier the edit panel supplier
   * @return the edit Control
   */
  public static Control createEditControl(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
    requireNonNull(comboBox);
    requireNonNull(editPanelSupplier);

    return createUpdateControl(new EditEntityCommand(comboBox, editPanelSupplier), comboBox,
            comboBox.getModel().selectionEmpty().not());
  }

  /**
   * Creates a new Control which displays the edit panel provided by the {@code editPanelSupplier} in a dialog displaying
   * the selected item for editing, and replaces the updated entity in the search field.
   * Creates a CTRL-INSERT key binding on the given component for triggering the resulting Control.
   * @param searchField the search field which selected item to edit
   * @param editPanelSupplier the edit panel supplier
   * @return the edit Control
   */
  public static Control createEditControl(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
    requireNonNull(searchField);
    requireNonNull(editPanelSupplier);

    return createUpdateControl(new EditEntityCommand(searchField, editPanelSupplier), searchField,
            searchField.model().selectionEmpty().not());
  }

  /**
   * Called before insert is performed.
   * To cancel the insert throw a {@link is.codion.common.model.CancelException}.
   * @throws ValidationException in case of a validation failure
   */
  protected void beforeInsert() throws ValidationException {}

  /**
   * Called before update is performed.
   * To cancel the update throw a {@link is.codion.common.model.CancelException}.
   * @throws ValidationException in case of a validation failure
   */
  protected void beforeUpdate() throws ValidationException {}

  /**
   * Called before delete is performed.
   * To cancel the delete throw a {@link is.codion.common.model.CancelException}.
   */
  protected void beforeDelete() {}

  /**
   * Called when a {@link ReferentialIntegrityException} occurs during a delete operation on the active entity.
   * If the referential integrity error handling is {@link ReferentialIntegrityErrorHandling#DISPLAY_DEPENDENCIES},
   * the dependencies of the entity involved are displayed to the user, otherwise {@link #onException(Throwable)} is called.
   * @param exception the exception
   * @see #setReferentialIntegrityErrorHandling(ReferentialIntegrityErrorHandling)
   */
  protected void onReferentialIntegrityException(ReferentialIntegrityException exception) {
    requireNonNull(exception);
    if (referentialIntegrityErrorHandling == ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES) {
      displayDependenciesDialog(singletonList(editModel().entity()), editModel().connectionProvider(),
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
  protected void onValidationException(ValidationException exception) {
    requireNonNull(exception);
    String title = editModel().entities()
            .definition(exception.attribute().entityType())
            .attributeDefinition(exception.attribute())
            .caption();
    JOptionPane.showMessageDialog(this, exception.getMessage(), title, JOptionPane.ERROR_MESSAGE);
    requestComponentFocus(exception.attribute());
  }

  /**
   * Associates {@code control} with {@code controlCode}
   * @param controlCode the control code
   * @param control the control to associate with {@code controlCode}, null for none
   * @throws IllegalStateException in case the panel has already been initialized
   */
  protected final void setControl(ControlCode controlCode, Control control) {
    requireNonNull(controlCode);
    if (isInitialized()) {
      throw new IllegalStateException("Method must be called before the panel is initialized");
    }
    controls.put(controlCode, control == null ? NULL_CONTROL : control);
  }

  /**
   * Creates a Controls instance containing all the controls available in this edit panel
   * @return the Controls available in this edit panel
   * @throws IllegalStateException in case the panel has not been initialized
   * @see #isInitialized()
   */
  protected Controls createControls() {
    if (!isInitialized()) {
      throw new IllegalStateException("Method must be called after the panel is initialized");
    }

    return Controls.controls(controlCodes.stream()
            .filter(this::containsControl)
            .map(controls::get)
            .toArray(Control[]::new));
  }

  /**
   * Initializes this EntityEditPanel UI, that is, creates and lays out the components
   * required for editing the underlying entity type.
   * <pre>
   *   protected void initializeUI() {
   *      setInitialFocusAttribute(DomainModel.USER_NAME);
   *
   *      createTextField(DomainModel.USER_NAME);
   *      createTextField(DomainModel.USER_ADDRESS);
   *
   *      setLayout(new GridLayout(2, 1, 5, 5);
   *
   *      addInputPanel(DomainModel.USER_NAME);
   *      addInputPanel(DomainModel.USER_ADDRESS);
   *  }
   * </pre>
   */
  protected abstract void initializeUI();

  /**
   * Initializes the controls available to this EntityEditPanel by mapping them to their respective
   * control codes ({@link ControlCode#INSERT}, {@link ControlCode#UPDATE} etc.)
   * via the {@link #setControl(ControlCode, Control)}) method, these can then be retrieved via the {@link #control(ControlCode)} method.
   * @see Control
   * @see #setControl(ControlCode, Control)
   * @see #control(ControlCode)
   */
  private void setupControls() {
    if (!editModel().readOnly().get()) {
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
    if (editModel().insertEnabled().get() && controlCodes.contains(ControlCode.INSERT)) {
      controls.putIfAbsent(ControlCode.INSERT, createInsertControl());
    }
    if (editModel().updateEnabled().get() && controlCodes.contains(ControlCode.UPDATE)) {
      controls.putIfAbsent(ControlCode.UPDATE, createUpdateControl());
    }
    if (editModel().deleteEnabled().get() && controlCodes.contains(ControlCode.DELETE)) {
      controls.putIfAbsent(ControlCode.DELETE, createDeleteControl());
    }
  }

  private Control createRefreshControl() {
    return Control.builder(editModel()::refresh)
            .name(FrameworkMessages.refresh())
            .enabled(active)
            .description(FrameworkMessages.refreshTip() + ALT_PREFIX + FrameworkMessages.refreshMnemonic() + ")")
            .mnemonic(FrameworkMessages.refreshMnemonic())
            .smallIcon(FrameworkIcons.instance().refresh())
            .build();
  }

  private Control createDeleteControl() {
    return Control.builder(this::deleteWithConfirmation)
            .name(FrameworkMessages.delete())
            .enabled(State.and(active,
                    editModel().deleteEnabled(),
                    editModel().entityNew().not()))
            .description(FrameworkMessages.deleteCurrentTip() + ALT_PREFIX + FrameworkMessages.deleteMnemonic() + ")")
            .mnemonic(FrameworkMessages.deleteMnemonic())
            .smallIcon(FrameworkIcons.instance().delete())
            .build();
  }

  private Control createClearControl() {
    return Control.builder(this::clearAndRequestFocus)
            .name(Messages.clear())
            .enabled(active)
            .description(Messages.clearTip() + ALT_PREFIX + Messages.clearMnemonic() + ")")
            .mnemonic(Messages.clearMnemonic())
            .smallIcon(FrameworkIcons.instance().clear())
            .build();
  }

  private Control createUpdateControl() {
    return Control.builder(this::updateWithConfirmation)
            .name(FrameworkMessages.update())
            .enabled(State.and(active,
                    editModel().updateEnabled(),
                    editModel().entityNew().not(),
                    editModel().modified()))
            .description(FrameworkMessages.updateTip() + ALT_PREFIX + FrameworkMessages.updateMnemonic() + ")")
            .mnemonic(FrameworkMessages.updateMnemonic())
            .smallIcon(FrameworkIcons.instance().update())
            .build();
  }

  private Control createInsertControl() {
    boolean useSaveCaption = USE_SAVE_CAPTION.get();
    char mnemonic = useSaveCaption ? FrameworkMessages.saveMnemonic() : FrameworkMessages.addMnemonic();
    String caption = useSaveCaption ? FrameworkMessages.save() : FrameworkMessages.add();
    return Control.builder(this::insertWithConfirmation)
            .name(caption)
            .enabled(State.and(active, editModel().insertEnabled()))
            .description(FrameworkMessages.addTip() + ALT_PREFIX + mnemonic + ")")
            .mnemonic(mnemonic)
            .smallIcon(FrameworkIcons.instance().add())
            .build();
  }

  private void bindEvents() {
    if (INCLUDE_ENTITY_MENU.get()) {
      KeyEvents.builder(VK_V)
              .modifiers(CTRL_DOWN_MASK | ALT_DOWN_MASK)
              .condition(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
              .action(Control.control(this::showEntityMenu))
              .enable(this);
    }
    editModel().refreshing().addDataListener(this::onRefreshingChanged);
    editModel().addConfirmSetEntityObserver(confirmationState -> {
      int result = showConfirmDialog(Utilities.parentWindow(EntityEditPanel.this),
              FrameworkMessages.unsavedDataWarning(), FrameworkMessages.unsavedDataWarningTitle(),
              JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
      confirmationState.set(result == JOptionPane.YES_OPTION);
    });
  }

  private boolean confirmInsert() {
    return confirmers.getOrDefault(Confirmer.Action.INSERT, DEFAULT_INSERT_CONFIRMER).confirm(this);
  }

  private boolean confirmDelete() {
    return confirmers.getOrDefault(Confirmer.Action.DELETE, DEFAULT_DELETE_CONFIRMER).confirm(this);
  }

  private boolean confirmUpdate() {
    return confirmers.getOrDefault(Confirmer.Action.UPDATE, DEFAULT_UPDATE_CONFIRMER).confirm(this);
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
    new EntityPopupMenu(editModel().entity(), editModel().connectionProvider().connection()).show(this, 0, 0);
  }

  private static Set<ControlCode> validateControlCodes(ControlCode[] controlCodes) {
    if (controlCodes == null) {
      return emptySet();
    }
    for (ControlCode controlCode : controlCodes) {
      requireNonNull(controlCode, "controlCode");
    }

    return new LinkedHashSet<>(Arrays.asList(controlCodes));
  }

  private static Control createInsertControl(InsertEntityCommand insertEntityCommand, JComponent component) {
    Control control = Control.builder(insertEntityCommand)
            .smallIcon(FrameworkIcons.instance().add())
            .description(MESSAGES.getString("insert_new"))
            .enabled(createComponentEnabledState(component))
            .build();

    KeyEvents.builder(VK_INSERT)
            .action(control)
            .enable(component);

    return control;
  }

  private static Control createUpdateControl(EditEntityCommand editEntityCommand, JComponent component,
                                             StateObserver selectionNonEmptyState) {
    Control control = Control.builder(editEntityCommand)
            .smallIcon(FrameworkIcons.instance().edit())
            .description(MESSAGES.getString("edit_selected"))
            .enabled(State.and(createComponentEnabledState(component), selectionNonEmptyState))
            .build();

    KeyEvents.builder(VK_INSERT)
            .modifiers(CTRL_DOWN_MASK)
            .action(control)
            .enable(component);

    return control;
  }

  private static State createComponentEnabledState(JComponent component) {
    State componentEnabledState = State.state(component.isEnabled());
    component.addPropertyChangeListener("enabled", changeEvent ->
            componentEnabledState.set((Boolean) changeEvent.getNewValue()));

    return componentEnabledState;
  }

  /**
   * Handles displaying confirmation messages for common actions to the user.
   */
  public interface Confirmer {

    /**
     * The actions meriting user confirmation
     */
    enum Action {
      INSERT, UPDATE, DELETE
    }

    /**
     * Returns true if the action is confirmed, presents an OK/Cancel confirm dialog to the user if required.
     * @param dialogOwner the owner for the dialog
     * @return true if the action is confirmed
     */
    boolean confirm(JComponent dialogOwner);

    /**
     * Shows a confirmation dialog
     * @param dialogOwner the dialog owner
     * @param message the dialog message
     * @param title the dialog title
     * @return true if OK was pressed
     */
    default boolean confirm(JComponent dialogOwner, String message, String title) {
      return showConfirmDialog(dialogOwner, message, title, JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION;
    }
  }

  private static final class InsertConfirmer implements Confirmer {

    @Override
    public boolean confirm(JComponent dialogOwner) {
      return true;
    }
  }

  private static final class UpdateConfirmer implements Confirmer {

    @Override
    public boolean confirm(JComponent dialogOwner) {
      return confirm(dialogOwner, FrameworkMessages.confirmUpdate(), FrameworkMessages.update());
    }
  }

  private static final class DeleteConfirmer implements Confirmer {

    @Override
    public boolean confirm(JComponent dialogOwner) {
      return confirm(dialogOwner, FrameworkMessages.confirmDelete(), FrameworkMessages.delete());
    }
  }

  private static final class InsertEntityCommand implements Control.Command {

    private final Supplier<EntityEditPanel> editPanelSupplier;
    private final JComponent component;
    private final EntityConnectionProvider connectionProvider;
    private final Consumer<List<Entity>> onInsert;
    private final List<Entity> insertedEntities = new ArrayList<>();

    private InsertEntityCommand(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
      this.editPanelSupplier = editPanelSupplier;
      this.component = comboBox;
      this.connectionProvider = comboBox.getModel().connectionProvider();
      this.onInsert = new EntityComboBoxOnInsert();
    }

    private InsertEntityCommand(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
      this.editPanelSupplier = editPanelSupplier;
      this.component = searchField;
      this.connectionProvider = searchField.model().connectionProvider();
      this.onInsert = new EntitySearchFieldOnInsert();
    }

    @Override
    public void perform() {
      EntityEditPanel editPanel = createEditPanel();
      State cancelled = State.state();
      Value<Attribute<?>> invalidAttribute = Value.value();
      JDialog dialog = Dialogs.okCancelDialog(editPanel)
              .owner(component)
              .title(connectionProvider.entities().definition(editPanel.editModel().entityType()).caption())
              .onShown(d -> invalidAttribute.optional()
                      .ifPresent(editPanel::requestComponentFocus))
              .onCancel(() -> cancelled.set(true))
              .build();
      try {
        boolean successfulInsert = false;
        while (!successfulInsert) {
          dialog.setVisible(true);
          if (cancelled.get()) {
            return;//cancelled
          }
          successfulInsert = insert(editPanel.editModel(), invalidAttribute);
          if (successfulInsert && !insertedEntities.isEmpty()) {
            onInsert.accept(insertedEntities);
          }
        }
      }
      finally {
        insertedEntities.clear();
      }
    }

    private EntityEditPanel createEditPanel() {
      EntityEditPanel editPanel = editPanelSupplier.get().initialize();
      editPanel.setBorder(emptyBorder());
      editPanel.editModel().addAfterInsertListener(new PopulateInsertedEntities());

      return editPanel;
    }

    private boolean insert(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
      try {
        WaitCursor.show(component);
        try {
          editModel.insert();

          return true;
        }
        finally {
          WaitCursor.hide(component);
        }
      }
      catch (ValidationException e) {
        attributeWithInvalidValue.set(e.attribute());
        JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
      }
      catch (Exception e) {
        Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
      }

      return false;
    }

    private final class PopulateInsertedEntities implements Consumer<Collection<Entity>> {

      @Override
      public void accept(Collection<Entity> inserted) {
        insertedEntities.clear();
        insertedEntities.addAll(inserted);
      }
    }

    private class EntityComboBoxOnInsert implements Consumer<List<Entity>> {

      @Override
      public void accept(List<Entity> inserted) {
        EntityComboBoxModel comboBoxModel = ((EntityComboBox) component).getModel();
        Entity item = inserted.get(0);
        comboBoxModel.addItem(item);
        comboBoxModel.setSelectedItem(item);
      }
    }

    private class EntitySearchFieldOnInsert implements Consumer<List<Entity>> {

      @Override
      public void accept(List<Entity> inserted) {
        ((EntitySearchField) component).model().setEntities(inserted);
      }
    }
  }

  private static final class EditEntityCommand implements Control.Command {

    private final Supplier<EntityEditPanel> editPanelSupplier;
    private final JComponent component;
    private final EntityConnectionProvider connectionProvider;
    private final Consumer<List<Entity>> onUpdate;
    private final List<Entity> updatedEntities = new ArrayList<>();

    private Entity entityToUpdate;

    private EditEntityCommand(EntityComboBox comboBox, Supplier<EntityEditPanel> editPanelSupplier) {
      this.editPanelSupplier = editPanelSupplier;
      this.component = comboBox;
      this.connectionProvider = comboBox.getModel().connectionProvider();
      this.onUpdate = new EntityComboBoxOnUpdate();
    }

    private EditEntityCommand(EntitySearchField searchField, Supplier<EntityEditPanel> editPanelSupplier) {
      this.editPanelSupplier = editPanelSupplier;
      this.component = searchField;
      this.connectionProvider = searchField.model().connectionProvider();
      this.onUpdate = new EntitySearchFieldOnUpdate();
    }

    @Override
    public void perform() throws Exception {
      if (component instanceof EntityComboBox) {
        if (((EntityComboBox) component).isPopupVisible()) {
          ((EntityComboBox) component).hidePopup();
        }
        entityToUpdate = ((EntityComboBox) component).getModel().selectedValue();
      }
      else {
        entityToUpdate = ((EntitySearchField) component).model().getEntities().get(0);
      }
      EntityEditPanel editPanel = createEditPanel();
      editPanel.editModel().setEntity(connectionProvider.connection().select(entityToUpdate.primaryKey()));
      State cancelled = State.state();
      Value<Attribute<?>> invalidAttribute = Value.value();
      JDialog dialog = Dialogs.okCancelDialog(editPanel)
              .owner(component)
              .title(connectionProvider.entities().definition(editPanel.editModel().entityType()).caption())
              .okEnabled(editPanel.editModel().modified())
              .onShown(d -> invalidAttribute.optional()
                      .ifPresent(editPanel::requestComponentFocus))
              .onCancel(() -> cancelled.set(true))
              .build();
      try {
        boolean successfulUpdate = false;
        while (!successfulUpdate) {
          dialog.setVisible(true);
          if (cancelled.get()) {
            return;//cancelled
          }
          successfulUpdate = update(editPanel.editModel(), invalidAttribute);
          if (successfulUpdate && !updatedEntities.isEmpty()) {
            onUpdate.accept(updatedEntities);
          }
        }
      }
      finally {
        entityToUpdate = null;
        updatedEntities.clear();
      }
    }

    private EntityEditPanel createEditPanel() {
      EntityEditPanel editPanel = editPanelSupplier.get().initialize();
      editPanel.setBorder(emptyBorder());
      editPanel.editModel().addAfterUpdateListener(new PopulateUpdatedEntities());

      return editPanel;
    }

    private boolean update(SwingEntityEditModel editModel, Value<Attribute<?>> attributeWithInvalidValue) {
      try {
        WaitCursor.show(component);
        try {
          if (editModel.modified().get()) {
            editModel.update();
          }

          return true;
        }
        finally {
          WaitCursor.hide(component);
        }
      }
      catch (ValidationException e) {
        attributeWithInvalidValue.set(e.attribute());
        JOptionPane.showMessageDialog(component, e.getMessage(), Messages.error(), JOptionPane.ERROR_MESSAGE);
      }
      catch (Exception e) {
        Dialogs.displayExceptionDialog(e, Utilities.parentWindow(component));
      }

      return false;
    }

    private final class EntityComboBoxOnUpdate implements Consumer<List<Entity>> {

      @Override
      public void accept(List<Entity> updated) {
        EntityComboBoxModel comboBoxModel = ((EntityComboBox) component).getModel();
        Entity item = updated.get(0);
        comboBoxModel.replaceItem(entityToUpdate, item);
        comboBoxModel.setSelectedItem(item);
      }
    }

    private final class EntitySearchFieldOnUpdate implements Consumer<List<Entity>> {

      @Override
      public void accept(List<Entity> updated) {
        ((EntitySearchField) component).model().setEntities(updated);
      }
    }

    private final class PopulateUpdatedEntities implements Consumer<Map<Entity.Key, Entity>> {

      @Override
      public void accept(Map<Entity.Key, Entity> updated) {
        updatedEntities.clear();
        updatedEntities.addAll(updated.values());
      }
    }
  }
}
