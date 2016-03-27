/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.model.Item;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.State;
import org.jminor.common.model.States;
import org.jminor.common.model.Value;
import org.jminor.common.model.Values;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.ForeignKeyCriteriaModel;
import org.jminor.javafx.framework.model.PropertyCriteriaModel;

import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;

public final class PropertyCriteriaView extends BorderPane {

  private final PropertyCriteriaModel<? extends Property.SearchableProperty> model;
  private final Pane searchTypePane;
  private final Pane topPane;
  private final Label header;
  private final CheckBox enabledBox;
  private final Pane checkBoxPane;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;
  private final State advancedCriteria = States.state();

  public PropertyCriteriaView(final PropertyCriteriaModel<? extends Property.SearchableProperty> model) {
    this.model = model;
    this.header = new Label(model.getProperty().getCaption());
    this.enabledBox = createEnabledBox();
    this.checkBoxPane = createCheckBoxPane();
    this.upperBoundControl = createUpperBoundControl();
    this.lowerBoundControl = createLowerBoundControl();
    this.topPane = createTopPane();
    this.searchTypePane = createSearchTypePane();
    setTop(topPane);
    initializeUI();
    bindEvents();
  }

  public void setAdvanced(final boolean advanced) {
    advancedCriteria.setActive(advanced);
    initializeUI();
  }

  private BorderPane createTopPane() {
    final BorderPane pane = new BorderPane(header);
    pane.setRight(checkBoxPane);

    return pane;
  }

  private BorderPane createSearchTypePane() {
    final BorderPane pane = new BorderPane(createSearchTypeComboBox());
    final Label filler = new Label();
    pane.setRight(filler);

    return pane;
  }

  private BorderPane createCheckBoxPane() {
    final BorderPane checkBoxPane = new BorderPane();
    checkBoxPane.setCenter(enabledBox);

    return checkBoxPane;
  }

  private ComboBox<Item<SearchType>> createSearchTypeComboBox() {
    final ComboBox<Item<SearchType>> comboBox = new ComboBox<>(model.getSearchTypeList());
    comboBox.getSelectionModel().select(new Item<>(model.getSearchTypeValue().get()));
    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      model.getSearchTypeValue().set(newValue.getItem());
    });
    comboBox.maxWidthProperty().set(Double.MAX_VALUE);
    comboBox.minWidthProperty().set(0);

    return comboBox;
  }

  private CheckBox createEnabledBox() {
    return FXUiUtil.createCheckBox(model.getEnabledState());
  }

  private Control createUpperBoundControl() {
    final Control control = createControl();
    final Value value = FXUiUtil.createValue(model.getProperty(), control, null);
    Values.link(model.getUpperBoundValue(), value);

    return control;
  }

  private Control createLowerBoundControl() {
    if (model.getProperty() instanceof Property.ForeignKeyProperty) {
      //never required
      return null;
    }
    final Control control = createControl();
    final Value value = FXUiUtil.createValue(model.getProperty(), control, null);
    Values.link(model.getLowerBoundValue(), value);

    return control;
  }

  private Control createControl() {
    final Control control;
    if (model instanceof ForeignKeyCriteriaModel) {
      control = FXUiUtil.createControl(model.getProperty(), ((ForeignKeyCriteriaModel) model).getConnectionProvider());
    }
    else {
      control = FXUiUtil.createControl(model.getProperty(), null);
    }
    control.setOnKeyReleased(event -> {
      if (event.getCode().equals(KeyCode.ENTER)) {
        model.getEnabledState().setActive(!model.getEnabledState().isActive());
      }
    });
    control.minWidthProperty().setValue(0);
    control.maxWidthProperty().setValue(Double.MAX_VALUE);

    return control;
  }

  private void bindEvents() {
    model.addLowerBoundRequiredListener(lowerBoundRequired -> initializeUI());
  }

  private void initializeUI() {
    if (advancedCriteria.isActive()) {
      setCenter(createAdvancedView());
    }
    else {
      setCenter(upperBoundControl);
    }
  }

  private Pane createAdvancedView() {
    final BorderPane borderPane = new BorderPane();
    borderPane.setTop(searchTypePane);
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
}
