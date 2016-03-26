/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityListModel;
import org.jminor.javafx.framework.model.PropertyCriteriaModel;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

public final class EntityTableColumn extends TableColumn<Entity, Object> {

  private final Property property;
  private final BorderPane basePane;
  private final BorderPane topPane;
  private final PropertyCriteriaView criteriaView;

  public EntityTableColumn(final EntityListModel listModel, final Property property,
                           final Callback<CellDataFeatures<Entity, Object>, ObservableValue<Object>> cellValueFactory) {
    super(property.getCaption());
    this.property = property;
    this.criteriaView = initializeCriteriaView(listModel);
    this.topPane = createTopPane();
    this.basePane = new BorderPane();
    setCellValueFactory(cellValueFactory);
    final int preferredWidth = property.getPreferredColumnWidth();
    if (preferredWidth > 0) {
      setPrefWidth(preferredWidth);
    }
    setGraphic(basePane);
  }

  public Property getProperty() {
    return property;
  }

  public void setCriteriaViewVisible(final boolean visible) {
    basePane.setTop(visible ? topPane : null);
    basePane.setCenter(visible ? criteriaView : new Pane());
  }

  private PropertyCriteriaView initializeCriteriaView(final EntityListModel listModel) {
    if (property instanceof Property.SearchableProperty) {
      final PropertyCriteriaModel<Property.SearchableProperty> criteriaModel =
              listModel.getCriteriaModel().getPropertyCriteriaModel((Property.SearchableProperty) property);
      final PropertyCriteriaView criteriaView = new PropertyCriteriaView(criteriaModel);
      criteriaView.prefWidthProperty().setValue(getWidth());
      widthProperty().addListener((observable, oldValue, newValue) -> {
        criteriaView.prefWidthProperty().set(newValue.doubleValue());
      });

      return criteriaView;
    }

    return null;
  }

  private BorderPane createTopPane() {
    final BorderPane topPane = new BorderPane(new Label(property.getCaption()));
    topPane.setRight(createCheckBoxPane());

    return topPane;
  }

  private BorderPane createCheckBoxPane() {
    if (property instanceof Property.SearchableProperty) {
      final CheckBox enabledBox = criteriaView.getEnabledCheckBox();
      final BorderPane checkBoxPane = new BorderPane();
      checkBoxPane.setCenter(enabledBox);

      return checkBoxPane;
    }

    return null;
  }

  private Pane createCenterPane(final EntityListModel listModel) {
    if (property instanceof Property.SearchableProperty) {
      return criteriaView;
    }

    return new Pane();
  }
}
