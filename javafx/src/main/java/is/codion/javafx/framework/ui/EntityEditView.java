/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.i18n.Messages;
import is.codion.common.item.Item;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.domain.property.ItemProperty;
import is.codion.framework.domain.property.Properties;
import is.codion.framework.domain.property.Property;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.javafx.framework.model.FXEntityEditModel;

import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * A View for editing entity instances
 */
public abstract class EntityEditView extends BorderPane {

  private static final KeyCode INSERT_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.ADD_MNEMONIC));
  private static final KeyCode UPDATE_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.UPDATE_MNEMONIC));
  private static final KeyCode DELETE_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.DELETE_MNEMONIC));
  private static final KeyCode CLEAR_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.CLEAR_MNEMONIC));
  private static final KeyCode REFRESH_KEY_CODE = KeyCode.getKeyCode(FrameworkMessages.get(FrameworkMessages.REFRESH_MNEMONIC));

  private final FXEntityEditModel editModel;
  private final Map<Attribute<?>, Control> controls = new HashMap<>();

  private boolean initialized = false;
  private boolean requestFocusAfterInsert = true;
  private Attribute<?> initialFocusAttribute;
  private Attribute<?> afterInsertFocusAttribute;

  /**
   * Instantiates a new {@link EntityEditView} instance
   * @param editModel the edit model to base this edit view on
   */
  public EntityEditView(final FXEntityEditModel editModel) {
    this.editModel = editModel;
  }

  /**
   * Initializes this edit view
   * @return the initialized view
   */
  public final EntityEditView initializePanel() {
    if (!initialized) {
      initializeUI();
      initialized = true;
    }

    return this;
  }

  /**
   * @return the underlying edit model
   */
  public final FXEntityEditModel getEditModel() {
    return editModel;
  }

  /**
   * Transfers focus to the component associated with the initial focus property
   * @see #setInitialFocusAttribute(Attribute)
   */
  public final void requestInitialFocus() {
    if (isVisible()) {
      requestInitialFocus(false);
    }
  }

  /**
   * @return the button panel for this edit view
   */
  public final Node getButtonPanel() {
    final Button save = createSaveButton();
    save.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button update = createUpdateButton();
    update.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button delete = createDeleteButton();
    delete.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button clear = createClearButton();
    clear.maxWidthProperty().setValue(Double.MAX_VALUE);
    final Button refresh = createRefreshButton();
    refresh.maxWidthProperty().setValue(Double.MAX_VALUE);

    return new VBox(save, update, delete, clear, refresh);
  }

  /**
   * Displays a dialog for choosing an input component to receive focus
   */
  public void selectInputControl() {
    final List<Property<?>> properties = Properties.sort(getEditModel()
            .getEntityDefinition().getProperties(controls.keySet()));
    properties.removeIf(property -> {
      final Control control = controls.get(property.getAttribute());

      return control == null || control.isDisabled() || !control.isVisible();
    });
    controls.get(FXUiUtil.selectValues(properties).get(0).getAttribute()).requestFocus();
  }

  /**
   * Sets the given attribute as the attribute which component should receive focus when this edit view is initialized
   * @param initialFocusAttribute the attribute
   */
  public final void setInitialFocusAttribute(final Attribute<?> initialFocusAttribute) {
    this.initialFocusAttribute = initialFocusAttribute;
  }

  /**
   * Sets the given property as the property which component should receive focus after an insert has been performed
   * @param afterInsertFocusAttribute the attribute
   */
  public final void setAfterInsertFocusAttribute(final Attribute<?> afterInsertFocusAttribute) {
    this.afterInsertFocusAttribute = afterInsertFocusAttribute;
  }

  /**
   * @param requestFocusAfterInsert if true then the input focus is set after insert
   * @see #setInitialFocusAttribute(Attribute)
   */
  public void setRequestFocusAfterInsert(final boolean requestFocusAfterInsert) {
    this.requestFocusAfterInsert = requestFocusAfterInsert;
  }

  /**
   * @return the edit view containing the input components
   */
  protected abstract Node initializeEditPanel();

  /**
   * for overriding, called before insert/update
   * @throws ValidationException in case of a validation failure
   */
  protected void validateData() throws ValidationException {}

  /**
   * Creates a {@link EntitySearchField} based on the entities referenced by the given foreign key
   * @param foreignKey the foreign key
   * @return a {@link EntitySearchField} based on the given foreign key
   */
  protected final EntitySearchField createForeignKeySearchField(final ForeignKey foreignKey) {
    checkControl(foreignKey);
    getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKey);
    final EntitySearchField searchField = FXUiUtil.createSearchField(foreignKey, editModel);

    controls.put(foreignKey, searchField);

    return searchField;
  }

  /**
   * Creates a {@link ComboBox} based on the entities referenced by the given foreign key
   * @param foreignKey the foreign key
   * @return a {@link ComboBox} based on the given foreign key
   */
  protected final ComboBox<Entity> createForeignKeyComboBox(final ForeignKey foreignKey) {
    checkControl(foreignKey);
    getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKey);
    final ComboBox<Entity> box = FXUiUtil.createForeignKeyComboBox(foreignKey, editModel);

    controls.put(foreignKey, box);

    return box;
  }

  /**
   * Creates a {@link ComboBox} based on the items of the given property
   * @param attribute the attribute
   * @param <T> the property type
   * @return a {@link ComboBox} for the given property
   */
  protected final <T> ComboBox<Item<T>> createItemComboBox(final Attribute<T> attribute) {
    checkControl(attribute);
    final ComboBox<Item<T>> box = FXUiUtil.createItemComboBox((ItemProperty<T>)
            getEditModel().getEntityDefinition().getProperty(attribute), editModel);

    controls.put(attribute, box);

    return box;
  }

  /**
   * Creates a {@link TextField} for the given attribute
   * @param attribute the attribute
   * @param <T> the value type
   * @return a {@link TextField} for the given attribute
   */
  protected final <T> TextField createTextField(final Attribute<T> attribute) {
    checkControl(attribute);
    final Property<?> property = getEditModel().getEntityDefinition().getProperty(attribute);
    final TextField textField;
    if (attribute.isInteger()) {
      textField = FXUiUtil.createIntegerField((Property<Integer>) property, editModel);
    }
    else if (attribute.isLong()) {
      textField = FXUiUtil.createLongField((Property<Long>) property, editModel);
    }
    else if (attribute.isDouble()) {
      textField = FXUiUtil.createDoubleField((Property<Double>) property, editModel);
    }
    else if (attribute.isBigDecimal()) {
      textField = FXUiUtil.createBigDecimalField((Property<BigDecimal>) property, editModel);
    }
    else if (attribute.isString()) {
      textField = FXUiUtil.createTextField((Property<String>) property, editModel);
    }
    else {
      throw new IllegalArgumentException("Text field type for attribute: " + attribute + " is not implemented");
    }

    controls.put(attribute, textField);

    return textField;
  }

  /**
   * Creates a {@link DatePicker} for the given attribute, assuming the property is date based
   * @param attribute the attribute
   * @return a {@link DatePicker} based on the given attribute
   */
  protected final DatePicker createDatePicker(final Attribute<LocalDate> attribute) {
    checkControl(attribute);
    final DatePicker picker = FXUiUtil.createDatePicker(getEditModel()
            .getEntityDefinition().getProperty(attribute), editModel);
    controls.put(attribute, picker);

    return picker;
  }

  /**
   * Creates a {@link Label} with caption associated with the given attribute
   * @param attribute the attribute
   * @return a {@link Label} for the given attribute
   */
  protected final Label createLabel(final Attribute<?> attribute) {
    return new Label(getEditModel().getEntityDefinition().getProperty(attribute).getCaption());
  }

  protected final BorderPane createPropertyPanel(final Attribute<?> attribute) {
    final BorderPane pane = new BorderPane();
    pane.setTop(createLabel(attribute));
    final Control control = controls.get(attribute);
    control.setMaxWidth(Double.MAX_VALUE);
    pane.setCenter(control);

    return pane;
  }

  private void initializeUI() {
    final BorderPane top = new BorderPane();
    final BorderPane topLeft = new BorderPane();
    topLeft.setCenter(initializeEditPanel());
    top.setLeft(topLeft);
    setTop(top);
    setOnKeyReleased(this::onKeyReleased);
  }

  private Button createSaveButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.SAVE));
    button.setOnAction(event -> save());

    return button;
  }

  private Button createUpdateButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.UPDATE));
    button.setOnAction(event -> update(true));
    final StateObserver existingAndModifiedState = State.and(
            editModel.getEntityNewObserver().getReversedObserver(),
            editModel.getModifiedObserver());
    FXUiUtil.link(button.disableProperty(), existingAndModifiedState.getReversedObserver());

    return button;
  }

  private Button createDeleteButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.DELETE));
    button.setOnAction(event -> delete());
    FXUiUtil.link(button.disableProperty(), editModel.getEntityNewObserver());

    return button;
  }

  private Button createClearButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.CLEAR));
    button.setOnAction(event -> {
      editModel.setDefaultValues();
      requestInitialFocus();
    });

    return button;
  }

  private Button createRefreshButton() {
    final Button button = new Button(FrameworkMessages.get(FrameworkMessages.REFRESH));
    button.setOnAction(event -> editModel.refresh());

    return button;
  }

  private void save() {
    if (editModel.isEntityNew() || !editModel.getModifiedObserver().get()) {
      //no entity selected or selected entity is unmodified can only insert
      insert();
    }
    else {//possibly update
      final Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
      alert.setTitle(FrameworkMessages.get(FrameworkMessages.UPDATE_OR_ADD_TITLE));
      alert.setHeaderText(FrameworkMessages.get(FrameworkMessages.UPDATE_OR_ADD));

      final ButtonType update = new ButtonType(FrameworkMessages.get(FrameworkMessages.UPDATE_SELECTED_RECORD));
      final ButtonType insert = new ButtonType(FrameworkMessages.get(FrameworkMessages.ADD_NEW));
      final ButtonType cancel = new ButtonType(Messages.get(Messages.CANCEL), ButtonBar.ButtonData.CANCEL_CLOSE);
      alert.getButtonTypes().setAll(update, insert, cancel);
      ((Button) alert.getDialogPane().lookupButton(update)).setDefaultButton(true);

      final Optional<ButtonType> result = alert.showAndWait();
      if (!result.isPresent() || result.get().equals(cancel)) {
        return;
      }

      if (result.get().equals(update)) {
        update(false);
      }
      else {
        insert();
      }
    }
  }

  private void insert() {
    try {
      validateData();
      editModel.insert();
      editModel.setDefaultValues();
      if (requestFocusAfterInsert) {
        requestInitialFocus(true);
      }
    }
    catch (final ValidationException e) {
      FXUiUtil.showExceptionDialog(e);
      requestComponentFocus(e.getAttribute());
    }
    catch (final DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void update(final boolean confirm) {
    if (!confirm || FXUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_UPDATE))) {
      try {
        validateData();
        editModel.update();
        requestInitialFocus(false);
      }
      catch (final ValidationException e) {
        FXUiUtil.showExceptionDialog(e);
        requestComponentFocus(e.getAttribute());
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void delete() {
    if (FXUiUtil.confirm(FrameworkMessages.get(FrameworkMessages.CONFIRM_DELETE_ENTITY))) {
      try {
        editModel.delete();
      }
      catch (final DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void requestComponentFocus(final Attribute<?> attribute) {
    final Control control = controls.get(attribute);
    if (control != null) {
      control.requestFocus();
    }
  }

  private void requestInitialFocus(final boolean afterInsert) {
    final Control focusControl = afterInsert && afterInsertFocusAttribute != null ?
            controls.get(afterInsertFocusAttribute) :
            controls.get(initialFocusAttribute);
    if (focusControl != null && focusControl.isFocusTraversable()) {
      focusControl.requestFocus();
    }
    else {
      requestFocus();
    }
  }

  private void checkControl(final Attribute<?> attribute) {
    if (controls.containsKey(attribute)) {
      throw new IllegalStateException("Control has already been created for property: " + attribute);
    }
  }

  private void onKeyReleased(final KeyEvent event) {
    if (event.isAltDown()) {
      if (event.getCode().equals(INSERT_KEY_CODE)) {
        save();
        event.consume();
      }
      else if (event.getCode().equals(UPDATE_KEY_CODE)) {
        update(true);
        event.consume();
      }
      else if (event.getCode().equals(DELETE_KEY_CODE)) {
        delete();
        event.consume();
      }
      else if (event.getCode().equals(CLEAR_KEY_CODE)) {
        editModel.setDefaultValues();
        event.consume();
      }
      else if (event.getCode().equals(REFRESH_KEY_CODE)) {
        editModel.refresh();
        event.consume();
      }
    }
    else if (event.isControlDown() && event.getCode().equals(KeyCode.I)) {
      selectInputControl();
      event.consume();
    }
  }
}
