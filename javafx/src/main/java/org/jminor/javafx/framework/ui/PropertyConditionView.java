/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.Item;
import org.jminor.common.State;
import org.jminor.common.States;
import org.jminor.common.Values;
import org.jminor.common.db.ConditionType;
import org.jminor.common.model.table.ColumnConditionModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.framework.domain.property.Property;
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

/**
 * A View for configuring a query condition for a single property
 */
public final class PropertyConditionView extends BorderPane {

  private final ColumnConditionModel<? extends Property> model;
  private final Pane conditionTypePane;
  private final Pane topPane;
  private final Label header;
  private final CheckBox enabledBox;
  private final Pane checkBoxPane;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;
  private final State advancedCondition = States.state();

  /**
   * Instantiates a new {@link PropertyConditionView}
   * @param model the {@link ColumnConditionModel} to base this view on
   */
  public PropertyConditionView(final ColumnConditionModel<? extends Property> model) {
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

  /**
   * Toggles the advanced view
   * @param advanced the toggle values
   */
  public void setAdvanced(final boolean advanced) {
    advancedCondition.set(advanced);
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
    final BorderPane pane = new BorderPane();
    pane.setCenter(enabledBox);

    return pane;
  }

  private ComboBox<Item<ConditionType>> createConditionTypeComboBox() {
    final ComboBox<Item<ConditionType>> comboBox = new ComboBox<>(
            FXCollections.observableArrayList(getConditionTypes(model.getColumnIdentifier())));
    comboBox.getSelectionModel().select(new Item<>(model.getConditionType()));
    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> model.setConditionType(newValue.getValue()));
    comboBox.maxWidthProperty().set(Double.MAX_VALUE);
    comboBox.minWidthProperty().set(0);
    FXUiUtil.link(comboBox.disableProperty(), model.getLockedObserver());

    return comboBox;
  }

  private CheckBox createEnabledBox() {
    final CheckBox box = new CheckBox();
    Values.link(Values.propertyValue(model, "enabled", boolean.class, model.getEnabledObserver()), FXUiUtil.createBooleanValue(box));
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
    if (model.getColumnIdentifier() instanceof ForeignKeyProperty) {
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
    if (advancedCondition.get()) {
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

  private static Collection<Item<ConditionType>> getConditionTypes(final Property property) {
    final Collection<Item<ConditionType>> types = new ArrayList<>();
    if (property instanceof ForeignKeyProperty) {
      types.add(new Item<>(ConditionType.LIKE, ConditionType.LIKE.getCaption()));
      types.add(new Item<>(ConditionType.NOT_LIKE, ConditionType.NOT_LIKE.getCaption()));
    }
    else if (property.isBoolean()) {
      types.add(new Item<>(ConditionType.LIKE, ConditionType.LIKE.getCaption()));
    }
    else {
      for (final ConditionType conditionType : ConditionType.values()) {
        types.add(new Item<>(conditionType, conditionType.getCaption()));
      }
    }

    return types;
  }
}
