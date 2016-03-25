/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.javafx.framework.model.EntityListModel;
import org.jminor.javafx.framework.model.PropertyCriteriaModel;

import javafx.beans.value.ObservableValue;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;

public final class EntityTableColumn extends TableColumn<Entity, Object> {

  private final Property property;

  public EntityTableColumn(final EntityListModel listModel, final Property property,
                           final Callback<CellDataFeatures<Entity, Object>, ObservableValue<Object>> cellValueFactory) {
    super();
    this.property = property;
    final int preferredWidth = property.getPreferredColumnWidth();
    if (preferredWidth > 0) {
      setPrefWidth(preferredWidth);
    }
    final Label header = new Label(property.getCaption());
    if (property instanceof Property.SearchableProperty) {
      final PropertyCriteriaModel<Property.SearchableProperty> criteriaModel =
              listModel.getCriteriaModel().getPropertyCriteriaModel((Property.SearchableProperty) property);
      final PropertyCriteriaView criteriaView = new PropertyCriteriaView(header, criteriaModel);
      criteriaView.prefWidthProperty().setValue(getWidth());
      setGraphic(criteriaView);
      widthProperty().addListener((observable, oldValue, newValue) -> {
        criteriaView.prefWidthProperty().set(newValue.doubleValue());
      });
    }
    else {
      final BorderPane emptyPane = new BorderPane();
      emptyPane.setTop(header);
      emptyPane.setCenter(new Pane());
      setGraphic(emptyPane);
    }
    setCellValueFactory(cellValueFactory);
  }

  public Property getProperty() {
    return property;
  }
}
