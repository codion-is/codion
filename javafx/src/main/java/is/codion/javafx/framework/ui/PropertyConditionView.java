/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.Operator;
import is.codion.common.item.Item;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.state.State;
import is.codion.common.value.Value;
import is.codion.common.value.ValueSet;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.DefaultForeignKeyConditionModel;
import is.codion.javafx.framework.model.FXForeignKeyConditionListModel;

import javafx.collections.FXCollections;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import static is.codion.common.item.Item.item;

/**
 * A View for configuring a query condition for a single property
 * @param <T> the column value type
 */
public final class PropertyConditionView<T> extends BorderPane {

  private final ColumnConditionModel<? extends Attribute<?>, T> model;
  private final Pane operatorPane;
  private final Pane topPane;
  private final Label header;
  private final CheckBox enabledBox;
  private final Pane checkBoxPane;
  private final Control equalsValueControl;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;
  private final State advancedCondition = State.state();

  /**
   * Instantiates a new {@link PropertyConditionView}
   * @param model the {@link ColumnConditionModel} to base this view on
   * @param property the underlying property
   */
  public PropertyConditionView(final ColumnConditionModel<? extends Attribute<?>, T> model, final Property<T> property) {
    this.model = model;
    this.header = new Label(property.getCaption());
    this.enabledBox = createEnabledBox();
    this.checkBoxPane = createCheckBoxPane();
    this.equalsValueControl = createEqualsValueControl(property);
    this.upperBoundControl = createUpperBoundControl(property);
    this.lowerBoundControl = createLowerBoundControl(property);
    this.topPane = createTopPane();
    this.operatorPane = createOperatorPane();
    setTop(topPane);
    initializeUI();
    bindEvents();
  }

  /**
   * Toggles the advanced view
   * @param advanced the toggle value
   */
  public void setAdvanced(final boolean advanced) {
    advancedCondition.set(advanced);
    initializeUI();
  }

  private BorderPane createTopPane() {
    BorderPane pane = new BorderPane(header);
    pane.setRight(checkBoxPane);

    return pane;
  }

  private BorderPane createOperatorPane() {
    BorderPane pane = new BorderPane(createOperatorComboBox());
    Label filler = new Label();
    pane.setRight(filler);

    return pane;
  }

  private BorderPane createCheckBoxPane() {
    BorderPane pane = new BorderPane();
    pane.setCenter(enabledBox);

    return pane;
  }

  private ComboBox<Item<Operator>> createOperatorComboBox() {
    ComboBox<Item<Operator>> comboBox = new ComboBox<>(
            FXCollections.observableArrayList(getOperators(model.getColumnIdentifier())));
    comboBox.getSelectionModel().select(item(model.getOperator()));
    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> model.setOperator(newValue.getValue()));
    comboBox.maxWidthProperty().set(Double.MAX_VALUE);
    comboBox.minWidthProperty().set(0);
    FXUiUtil.link(comboBox.disableProperty(), model.getLockedObserver());

    return comboBox;
  }

  private CheckBox createEnabledBox() {
    CheckBox box = new CheckBox();
    FXUiUtil.createBooleanValue(box).link(model.getEnabledState());
    FXUiUtil.link(box.disableProperty(), model.getLockedObserver());

    return box;
  }

  private Control createEqualsValueControl(final Property<T> property) {
    Control control = createControl(property);
    if (!(control instanceof EntitySearchField)) {
      ValueSet<T> valueSet = model.getEqualValueSet();
      Value<T> value = Value.value();
      value.addDataListener(object -> valueSet.set(object == null ? Collections.emptySet() : Collections.singleton(object)));

      FXUiUtil.createValue(property, control, null).link(value);
    }

    return control;
  }

  private Control createUpperBoundControl(final Property<T> property) {
    if (model.getTypeClass().equals(Boolean.class) || model.getColumnIdentifier() instanceof ForeignKey) {
      //never required
      return null;
    }
    Control control = createControl(property);
    FXUiUtil.createValue(property, control, null).link(model.getUpperBoundValue());

    return control;
  }

  private Control createLowerBoundControl(final Property<T> property) {
    if (model.getTypeClass().equals(Boolean.class) || model.getColumnIdentifier() instanceof ForeignKey) {
      //never required
      return null;
    }
    Control control = createControl(property);
    FXUiUtil.createValue(property, control, null).link(model.getLowerBoundValue());

    return control;
  }

  private Control createControl(final Property<T> property) {
    Control control;
    if (model instanceof FXForeignKeyConditionListModel) {
      FXForeignKeyConditionListModel listModel = (FXForeignKeyConditionListModel) model;
      control = new ComboBox<>(listModel.getListModel().getSortedList());
      listModel.getListModel().setSelectionModel(((ComboBox<Entity>) control).getSelectionModel());
    }
    else if (model instanceof DefaultForeignKeyConditionModel) {
      control = new EntitySearchField(((DefaultForeignKeyConditionModel) model).getEntitySearchModel());
    }
    else {
      control = FXUiUtil.createControl(property, null);
    }
    if (!(control instanceof EntitySearchField)) {
      control.setOnKeyReleased(event -> {
        if (event.getCode().equals(KeyCode.ENTER)) {
          model.setEnabled(!model.isEnabled());
          event.consume();
        }
      });
    }
    control.minWidthProperty().setValue(0);
    control.maxWidthProperty().setValue(Double.MAX_VALUE);
    FXUiUtil.link(control.disableProperty(), model.getLockedObserver());

    return control;
  }

  private void bindEvents() {
    model.addOperatorListener(operator -> initializeUI());
  }

  private void initializeUI() {
    if (advancedCondition.get()) {
      setCenter(createAdvancedView());
    }
    else {
      setCenter(equalsValueControl);
    }
  }

  private Pane createAdvancedView() {
    BorderPane borderPane = new BorderPane();
    borderPane.setTop(operatorPane);
    borderPane.setCenter(createInputPane());

    return borderPane;
  }

  private Pane createInputPane() {
    switch (model.getOperator()) {
      case EQUAL:
      case NOT_EQUAL: return singleValuePane(equalsValueControl);
      case GREATER_THAN:
      case GREATER_THAN_OR_EQUAL: return singleValuePane(lowerBoundControl);
      case LESS_THAN:
      case LESS_THAN_OR_EQUAL: return singleValuePane(upperBoundControl);
      case BETWEEN_EXCLUSIVE:
      case BETWEEN:
      case NOT_BETWEEN_EXCLUSIVE:
      case NOT_BETWEEN: return rangePane();
      default:
        throw new IllegalArgumentException("Unknown operator: " + model.getOperator());
    }
  }

  private Pane rangePane() {
    GridPane gridPane = new GridPane();
    gridPane.addColumn(0, lowerBoundControl);
    gridPane.addColumn(1, upperBoundControl);

    return gridPane;
  }

  private static Pane singleValuePane(final Control control) {
    GridPane gridPane = new GridPane();
    gridPane.addColumn(0, control);

    return gridPane;
  }

  private static Collection<Item<Operator>> getOperators(final Attribute<?> attribute) {
    Collection<Item<Operator>> types = new ArrayList<>();
    if (attribute instanceof ForeignKey) {
      types.add(item(Operator.EQUAL, Operator.EQUAL.getCaption()));
      types.add(item(Operator.NOT_EQUAL, Operator.NOT_EQUAL.getCaption()));
    }
    else if (attribute.isBoolean()) {
      types.add(item(Operator.EQUAL, Operator.EQUAL.getCaption()));
    }
    else {
      for (final Operator operator : Operator.values()) {
        types.add(item(operator, operator.getCaption()));
      }
    }

    return types;
  }
}
