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
  private final BorderPane basePane;
  private final Pane centerPane;

  public EntityTableColumn(final EntityListModel listModel, final Property property,
                           final Callback<CellDataFeatures<Entity, Object>, ObservableValue<Object>> cellValueFactory) {
    this.property = property;
    this.basePane = new BorderPane();
    final Label headerLabel = new Label(property.getCaption());
    this.basePane.setTop(headerLabel);
    this.centerPane = createCenterPane(listModel);
    setCellValueFactory(cellValueFactory);
    final int preferredWidth = property.getPreferredColumnWidth();
    if (preferredWidth > 0) {
      setPrefWidth(preferredWidth);
    }
    setGraphic(basePane);
  }

  public void setCenterPaneVisible(final boolean visible) {
    basePane.setCenter(visible ? centerPane : null);
  }

  private Pane createCenterPane(final EntityListModel listModel) {
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

    return new Pane();
  }

  public Property getProperty() {
    return property;
  }
}
