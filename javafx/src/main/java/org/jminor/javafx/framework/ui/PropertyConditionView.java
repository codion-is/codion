/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.Item;
import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.common.Values;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultForeignKeyConditionModel;
import org.jminor.javafx.framework.model.FXForeignKeyConditionListModel;

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

public final class PropertyConditionView extends BorderPane {

  private final ColumnConditionModel<? extends Property.SearchableProperty> model;
  private final Pane conditionTypePane;
  private final Pane topPane;
  private final Label header;
  private final CheckBox enabledBox;
  private final Pane checkBoxPane;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;
  private final State advancedCondition = States.state();

  public PropertyConditionView(final ColumnConditionModel<? extends Property.SearchableProperty> model) {
    this.model = model;
    this.header = new Label(model.getColumnIdentifier().getCaption());
    this.enabledBox = createEnabledBox();
    this.checkBoxPane = createCheckBoxPane();
    this.upperBoundControl = createUpperBoundControl();
    this.lowerBoundControl = createLowerBoundControl();
    this.topPane = createTopPane();
    this.conditionTypePane = createConditionTypePane();
    setTop(topPane);
    initializeUI();
    bindEvents();
  }

  public void setAdvanced(final boolean advanced) {
    advancedCondition.setActive(advanced);
    initializeUI();
  }

  private BorderPane createTopPane() {
    final BorderPane pane = new BorderPane(header);
    pane.setRight(checkBoxPane);

    return pane;
  }

  private BorderPane createConditionTypePane() {
    final BorderPane pane = new BorderPane(createConditionTypeComboBox());
    final Label filler = new Label();
    pane.setRight(filler);

    return pane;
  }

  private BorderPane createCheckBoxPane() {
    final BorderPane checkBoxPane = new BorderPane();
    checkBoxPane.setCenter(enabledBox);

    return checkBoxPane;
  }

  private ComboBox<Item<Condition.Type>> createConditionTypeComboBox() {
    final ComboBox<Item<Condition.Type>> comboBox = new ComboBox<>(
            FXCollections.observableArrayList(getConditionTypes(model.getColumnIdentifier())));
    comboBox.getSelectionModel().select(new Item<>(model.getConditionType()));
    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> model.setConditionType(newValue.getItem()));
    comboBox.maxWidthProperty().set(Double.MAX_VALUE);
    comboBox.minWidthProperty().set(0);
    FXUiUtil.link(comboBox.disableProperty(), model.getLockedObserver());

    return comboBox;
  }

  private CheckBox createEnabledBox() {
    final CheckBox box = new CheckBox();
    Values.link(Values.beanValue(model, "enabled", boolean.class, model.getEnabledObserver()), FXUiUtil.createBooleanValue(box));
    FXUiUtil.link(box.disableProperty(), model.getLockedObserver());

    return box;
  }

  private Control createUpperBoundControl() {
    final Control control = createControl();
    if (!(control instanceof EntityLookupField)) {
      Values.link(model.getUpperBoundValue(), FXUiUtil.createValue(model.getColumnIdentifier(), control, null));
    }

    return control;
  }

  private Control createLowerBoundControl() {
    if (model.getColumnIdentifier() instanceof Property.ForeignKeyProperty) {
      //never required
      return null;
    }
    final Control control = createControl();
    Values.link(model.getLowerBoundValue(), FXUiUtil.createValue(model.getColumnIdentifier(), control, null));

    return control;
  }

  private Control createControl() {
    final Control control;
    if (model instanceof FXForeignKeyConditionListModel) {
      final FXForeignKeyConditionListModel listModel = (FXForeignKeyConditionListModel) model;
      control = new ComboBox<>(listModel.getListModel().getSortedList());
      listModel.getListModel().setSelectionModel(((ComboBox<Entity>) control).getSelectionModel());
    }
    else if (model instanceof DefaultForeignKeyConditionModel) {
      control = new EntityLookupField(((DefaultForeignKeyConditionModel) model).getEntityLookupModel());
    }
    else {
      control = FXUiUtil.createControl(model.getColumnIdentifier(), null);
    }
    if (!(control instanceof EntityLookupField)) {
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
    model.addLowerBoundRequiredListener(this::initializeUI);
  }

  private void initializeUI() {
    if (advancedCondition.isActive()) {
      setCenter(createAdvancedView());
    }
    else {
      setCenter(upperBoundControl);
    }
  }

  private Pane createAdvancedView() {
    final BorderPane borderPane = new BorderPane();
    borderPane.setTop(conditionTypePane);
    if (model.isLowerBoundRequired()) {
      final GridPane gridPane = new GridPane();
      gridPane.addColumn(0, lowerBoundControl);
      gridPane.addColumn(1, upperBoundControl);
      borderPane.setCenter(gridPane);
    }
    else {
      borderPane.setCenter(upperBoundControl);
    }

    return borderPane;
  }

  private static Collection<Item<Condition.Type>> getConditionTypes(final Property property) {
    final Collection<Item<Condition.Type>> types = new ArrayList<>();
    if (property instanceof Property.ForeignKeyProperty) {
      types.add(new Item<>(Condition.Type.LIKE, Condition.Type.LIKE.getCaption()));
      types.add(new Item<>(Condition.Type.NOT_LIKE, Condition.Type.NOT_LIKE.getCaption()));
    }
    else if (property.isBoolean()) {
      types.add(new Item<>(Condition.Type.LIKE, Condition.Type.LIKE.getCaption()));
    }
    else {
      for (final Condition.Type conditionType : Condition.Type.values()) {
        types.add(new Item<>(conditionType, conditionType.getCaption()));
      }
    }

    return types;
  }
}
