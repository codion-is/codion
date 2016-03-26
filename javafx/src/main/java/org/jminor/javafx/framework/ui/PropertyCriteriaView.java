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
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;

public final class PropertyCriteriaView extends BorderPane {

  private final PropertyCriteriaModel<? extends Property.SearchableProperty> model;
  private final ComboBox<Item<SearchType>> searchTypeComboBox;
  private final CheckBox enabledBox;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;
  private final State advancedCriteria = States.state();

  public PropertyCriteriaView(final PropertyCriteriaModel<? extends Property.SearchableProperty> model) {
    this.model = model;
    this.searchTypeComboBox = createSearchTypeComboBox();
    this.enabledBox = createEnabledBox();
    this.upperBoundControl = createUpperBoundControl();
    this.lowerBoundControl = createLowerBoundControl();
    initializeUI();
    bindEvents();
  }

  public CheckBox getEnabledCheckBox() {
    return enabledBox;
  }

  public void setAdvanced(final boolean advanced) {
    advancedCriteria.setActive(advanced);
    initializeUI();
  }

  private ComboBox<Item<SearchType>> createSearchTypeComboBox() {
    final ComboBox<Item<SearchType>> comboBox = new ComboBox<>(model.getSearchTypeList());
    comboBox.getSelectionModel().select(new Item<>(model.getSearchTypeValue().get()));
    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      model.getSearchTypeValue().set(newValue.getItem());
    });

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

  private BorderPane createAdvancedView() {
    final BorderPane borderPane = new BorderPane();
    borderPane.setTop(searchTypeComboBox);
    if (model.isLowerBoundRequired()) {
      final HBox box = new HBox(lowerBoundControl, upperBoundControl);
      borderPane.setCenter(box);
    }
    else {
      borderPane.setCenter(upperBoundControl);
    }

    return borderPane;
  }
}
