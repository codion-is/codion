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
import org.jminor.common.model.table.ColumnCriteriaModel;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.DefaultForeignKeyCriteriaModel;
import org.jminor.javafx.framework.model.FXForeignKeyCriteriaListModel;

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

public final class PropertyCriteriaView extends BorderPane {

  private final ColumnCriteriaModel<? extends Property.SearchableProperty> model;
  private final Pane searchTypePane;
  private final Pane topPane;
  private final Label header;
  private final CheckBox enabledBox;
  private final Pane checkBoxPane;
  private final Control upperBoundControl;
  private final Control lowerBoundControl;
  private final State advancedCriteria = States.state();

  public PropertyCriteriaView(final ColumnCriteriaModel<? extends Property.SearchableProperty> model) {
    this.model = model;
    this.header = new Label(model.getColumnIdentifier().getCaption());
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
    final ComboBox<Item<SearchType>> comboBox = new ComboBox<>(
            FXCollections.observableArrayList(getSearchTypes(model.getColumnIdentifier())));
    comboBox.getSelectionModel().select(new Item<>(model.getSearchType()));
    comboBox.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
      model.setSearchType(newValue.getItem());
    });
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
      final Value value = FXUiUtil.createValue(model.getColumnIdentifier(), control, null);
      Values.link(model.getUpperBoundValue(), value);
    }

    return control;
  }

  private Control createLowerBoundControl() {
    if (model.getColumnIdentifier() instanceof Property.ForeignKeyProperty) {
      //never required
      return null;
    }
    final Control control = createControl();
    final Value value = FXUiUtil.createValue(model.getColumnIdentifier(), control, null);
    Values.link(model.getLowerBoundValue(), value);

    return control;
  }

  private Control createControl() {
    final Control control;
    if (model instanceof FXForeignKeyCriteriaListModel) {
      final FXForeignKeyCriteriaListModel listModel = (FXForeignKeyCriteriaListModel) model;
      control = new ComboBox<>(listModel.getListModel().getSortedList());
      listModel.getListModel().setSelectionModel(((ComboBox<Entity>) control).getSelectionModel());
    }
    else if (model instanceof DefaultForeignKeyCriteriaModel) {
      control = new EntityLookupField(((DefaultForeignKeyCriteriaModel) model).getEntityLookupModel());
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

  private static Collection<Item<SearchType>> getSearchTypes(final Property property) {
    final Collection<Item<SearchType>> types = new ArrayList<>();
    if (property instanceof Property.ForeignKeyProperty) {
      types.add(new Item<>(SearchType.LIKE, SearchType.LIKE.getCaption()));
      types.add(new Item<>(SearchType.NOT_LIKE, SearchType.NOT_LIKE.getCaption()));
    }
    else if (property.isBoolean()) {
      types.add(new Item<>(SearchType.LIKE, SearchType.LIKE.getCaption()));
    }
    else {
      for (final SearchType searchType : SearchType.values()) {
        types.add(new Item<>(searchType, searchType.getCaption()));
      }
    }

    return types;
  }
}
