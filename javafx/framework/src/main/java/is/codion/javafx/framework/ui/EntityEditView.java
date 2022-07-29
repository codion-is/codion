/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.db.exception.DatabaseException;
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
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A View for editing entity instances
 */
public abstract class EntityEditView extends BorderPane {

  private static final KeyCode INSERT_KEY_CODE = KeyCode.getKeyCode(String.valueOf(FrameworkMessages.addMnemonic()));
  private static final KeyCode UPDATE_KEY_CODE = KeyCode.getKeyCode(String.valueOf(FrameworkMessages.updateMnemonic()));
  private static final KeyCode DELETE_KEY_CODE = KeyCode.getKeyCode(String.valueOf(FrameworkMessages.deleteMnemonic()));
  private static final KeyCode CLEAR_KEY_CODE = KeyCode.getKeyCode(String.valueOf(FrameworkMessages.clearMnemonic()));
  private static final KeyCode REFRESH_KEY_CODE = KeyCode.getKeyCode(String.valueOf(FrameworkMessages.refreshMnemonic()));

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
  public EntityEditView(FXEntityEditModel editModel) {
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
    Button save = createInsertButton();
    save.maxWidthProperty().setValue(Double.MAX_VALUE);
    Button update = createUpdateButton();
    update.maxWidthProperty().setValue(Double.MAX_VALUE);
    Button delete = createDeleteButton();
    delete.maxWidthProperty().setValue(Double.MAX_VALUE);
    Button clear = createClearButton();
    clear.maxWidthProperty().setValue(Double.MAX_VALUE);
    Button refresh = createRefreshButton();
    refresh.maxWidthProperty().setValue(Double.MAX_VALUE);

    return new VBox(save, update, delete, clear, refresh);
  }

  /**
   * Displays a dialog for choosing an input component to receive focus
   */
  public void selectInputControl() {
    List<Property<?>> properties = Properties.sort(getEditModel()
            .getEntityDefinition().getProperties(controls.keySet()));
    properties.removeIf(property -> {
      Control control = controls.get(property.getAttribute());

      return control == null || control.isDisabled() || !control.isVisible();
    });
    controls.get(FXUiUtil.selectValues(properties).get(0).getAttribute()).requestFocus();
  }

  /**
   * Sets the given attribute as the attribute which component should receive focus when this edit view is initialized
   * @param initialFocusAttribute the attribute
   */
  public final void setInitialFocusAttribute(Attribute<?> initialFocusAttribute) {
    this.initialFocusAttribute = initialFocusAttribute;
  }

  /**
   * Sets the given property as the property which component should receive focus after an insert has been performed
   * @param afterInsertFocusAttribute the attribute
   */
  public final void setAfterInsertFocusAttribute(Attribute<?> afterInsertFocusAttribute) {
    this.afterInsertFocusAttribute = afterInsertFocusAttribute;
  }

  /**
   * @param requestFocusAfterInsert if true then the input focus is set after insert
   * @see #setInitialFocusAttribute(Attribute)
   */
  public void setRequestFocusAfterInsert(boolean requestFocusAfterInsert) {
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
  protected final EntitySearchField createForeignKeySearchField(ForeignKey foreignKey) {
    checkControl(foreignKey);
    getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKey);
    EntitySearchField searchField = FXUiUtil.createSearchField(foreignKey, editModel);

    controls.put(foreignKey, searchField);

    return searchField;
  }

  /**
   * Creates a {@link ComboBox} based on the entities referenced by the given foreign key
   * @param foreignKey the foreign key
   * @return a {@link ComboBox} based on the given foreign key
   */
  protected final ComboBox<Entity> createForeignKeyComboBox(ForeignKey foreignKey) {
    checkControl(foreignKey);
    getEditModel().getEntityDefinition().getForeignKeyProperty(foreignKey);
    ComboBox<Entity> box = FXUiUtil.createForeignKeyComboBox(foreignKey, editModel);

    controls.put(foreignKey, box);

    return box;
  }

  /**
   * Creates a {@link ComboBox} based on the items of the given property
   * @param attribute the attribute
   * @param <T> the property type
   * @return a {@link ComboBox} for the given property
   */
  protected final <T> ComboBox<Item<T>> createItemComboBox(Attribute<T> attribute) {
    checkControl(attribute);
    ComboBox<Item<T>> box = FXUiUtil.createItemComboBox((ItemProperty<T>)
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
  protected final <T> TextField createTextField(Attribute<T> attribute) {
    checkControl(attribute);
    Property<?> property = getEditModel().getEntityDefinition().getProperty(attribute);
    TextField textField;
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
  protected final DatePicker createDatePicker(Attribute<LocalDate> attribute) {
    checkControl(attribute);
    DatePicker picker = FXUiUtil.createDatePicker(getEditModel()
            .getEntityDefinition().getProperty(attribute), editModel);
    controls.put(attribute, picker);

    return picker;
  }

  /**
   * Creates a {@link Label} with caption associated with the given attribute
   * @param attribute the attribute
   * @return a {@link Label} for the given attribute
   */
  protected final Label createLabel(Attribute<?> attribute) {
    return new Label(getEditModel().getEntityDefinition().getProperty(attribute).getCaption());
  }

  protected final BorderPane createPropertyPanel(Attribute<?> attribute) {
    BorderPane pane = new BorderPane();
    pane.setTop(createLabel(attribute));
    Control control = controls.get(attribute);
    control.setMaxWidth(Double.MAX_VALUE);
    pane.setCenter(control);

    return pane;
  }

  private void initializeUI() {
    BorderPane top = new BorderPane();
    BorderPane topLeft = new BorderPane();
    topLeft.setCenter(initializeEditPanel());
    top.setLeft(topLeft);
    setTop(top);
    setOnKeyReleased(this::onKeyReleased);
  }

  private Button createInsertButton() {
    Button button = new Button(FrameworkMessages.add());
    button.setTooltip(new Tooltip(FrameworkMessages.addTip()));
    button.setOnAction(event -> insert());

    return button;
  }

  private Button createUpdateButton() {
    Button button = new Button(FrameworkMessages.update());
    button.setTooltip(new Tooltip(FrameworkMessages.updateTip()));
    button.setOnAction(event -> update(true));
    StateObserver existingAndModifiedState = State.and(
            editModel.getEntityNewObserver().getReversedObserver(),
            editModel.getModifiedObserver());
    FXUiUtil.link(button.disableProperty(), existingAndModifiedState.getReversedObserver());

    return button;
  }

  private Button createDeleteButton() {
    Button button = new Button(FrameworkMessages.delete());
    button.setTooltip(new Tooltip(FrameworkMessages.deleteCurrentTip()));
    button.setOnAction(event -> delete());
    FXUiUtil.link(button.disableProperty(), editModel.getEntityNewObserver());

    return button;
  }

  private Button createClearButton() {
    Button button = new Button(FrameworkMessages.clear());
    button.setTooltip(new Tooltip(FrameworkMessages.clearTip()));
    button.setOnAction(event -> {
      editModel.setDefaultValues();
      requestInitialFocus();
    });

    return button;
  }

  private Button createRefreshButton() {
    Button button = new Button(FrameworkMessages.refresh());
    button.setTooltip(new Tooltip(FrameworkMessages.refreshTip()));
    button.setOnAction(event -> editModel.refresh());

    return button;
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
    catch (ValidationException e) {
      FXUiUtil.showExceptionDialog(e);
      requestComponentFocus(e.getAttribute());
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  private void update(boolean confirm) {
    if (!confirm || FXUiUtil.confirm(FrameworkMessages.confirmUpdate())) {
      try {
        validateData();
        editModel.update();
        requestInitialFocus(false);
      }
      catch (ValidationException e) {
        FXUiUtil.showExceptionDialog(e);
        requestComponentFocus(e.getAttribute());
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void delete() {
    if (FXUiUtil.confirm(FrameworkMessages.confirmDelete())) {
      try {
        editModel.delete();
      }
      catch (DatabaseException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void requestComponentFocus(Attribute<?> attribute) {
    Control control = controls.get(attribute);
    if (control != null) {
      control.requestFocus();
    }
  }

  private void requestInitialFocus(boolean afterInsert) {
    Control focusControl = afterInsert && afterInsertFocusAttribute != null ?
            controls.get(afterInsertFocusAttribute) :
            controls.get(initialFocusAttribute);
    if (focusControl != null && focusControl.isFocusTraversable()) {
      focusControl.requestFocus();
    }
    else {
      requestFocus();
    }
  }

  private void checkControl(Attribute<?> attribute) {
    if (controls.containsKey(attribute)) {
      throw new IllegalStateException("Control has already been created for property: " + attribute);
    }
  }

  private void onKeyReleased(KeyEvent event) {
    if (event.isAltDown()) {
      if (event.getCode().equals(INSERT_KEY_CODE)) {
        insert();
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
