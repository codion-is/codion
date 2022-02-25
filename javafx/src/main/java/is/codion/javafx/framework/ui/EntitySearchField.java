/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.javafx.framework.ui;

import is.codion.common.i18n.Messages;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.i18n.FrameworkMessages;
import is.codion.framework.model.EntitySearchModel;
import is.codion.javafx.framework.ui.values.PropertyValues;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.util.List;

import static is.codion.common.Util.nullOrEmpty;

/**
 * A {@link TextField} allowing entity search based on the text entered
 */
public final class EntitySearchField extends TextField {

  private final EntitySearchModel model;
  //todo used during focus lost, todo implement
  private boolean performingSearch = false;

  private Color validBackgroundColor;
  private Color invalidBackgroundColor;

  /**
   * Instantiates a new {@link EntitySearchField} based on the given model
   * @param model the {@link EntitySearchModel} model to base this search field on
   */
  public EntitySearchField(EntitySearchModel model) {
    this.model = model;
    linkToModel();
    setInvalidBackgroundColor(Color.DARKGRAY);
    setPromptText(Messages.get(Messages.SEARCH_FIELD_HINT));
    tooltipProperty().setValue(new Tooltip(model.getDescription()));
    setOnKeyPressed(new SearchKeyHandler());
    focusedProperty().addListener((observable, oldValue, newValue) -> onFocusChanged(newValue));
    updateColors();
  }

  /**
   * @return the search model
   */
  public EntitySearchModel getModel() {
    return model;
  }

  /**
   * @param validBackgroundColor the background color to display when the text fits the selected value
   */
  public void setValidBackgroundColor(Color validBackgroundColor) {
    this.validBackgroundColor = validBackgroundColor;
  }

  /**
   * @param invalidBackgroundColor the background color to display when the text does not fit the selected value
   */
  public void setInvalidBackgroundColor(Color invalidBackgroundColor) {
    this.invalidBackgroundColor = invalidBackgroundColor;
  }

  private void performSearch(boolean promptUser) {
    try {
      performingSearch = true;
      if (nullOrEmpty(model.getSearchString())) {
        model.setSelectedEntities(null);
      }
      else if (!model.searchStringRepresentsSelected()) {
        List<Entity> queryResult;
        queryResult = model.performQuery();
        if (queryResult.size() == 1) {
          model.setSelectedEntities(queryResult);
        }
        else if (promptUser) {
          if (queryResult.isEmpty()) {
            showEmptyResultMessage();
          }
          else {
            selectEntities(queryResult);
          }
        }
      }
      updateColors();
    }
    finally {
      performingSearch = false;
    }
  }

  private void linkToModel() {
    PropertyValues.stringPropertyValue(textProperty()).link(model.getSearchStringValue());
  }

  private void selectEntities(List<Entity> queryResult) {
    FXUiUtil.SingleSelection singleSelection;
    if (model.getMultipleSelectionEnabledValue().get()) {
      singleSelection = FXUiUtil.SingleSelection.NO;
    }
    else {
      singleSelection = FXUiUtil.SingleSelection.YES;
    }

    Platform.runLater(() -> model.setSelectedEntities(FXUiUtil.selectValues(queryResult, singleSelection)));
  }

  private void updateColors() {
//    final boolean validBackground = model.searchStringRepresentsSelected() || (searchHint != null && searchHint.isHintTextVisible());
//    setBackground(validBackground ? validBackgroundColor : invalidBackgroundColor);
  }

  private void onFocusChanged(Boolean hasFocus) {
    if (!hasFocus) {
      if (getText().isEmpty()) {
        getModel().setSelectedEntity(null);
      }
      else if (!performingSearch && !model.searchStringRepresentsSelected()) {
        performSearch(false);
      }
      updateColors();
    }
  }

  private static void showEmptyResultMessage() {
    Platform.runLater(new Alert(Alert.AlertType.INFORMATION,
            FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION), ButtonType.OK)::showAndWait);
  }

  private class SearchKeyHandler implements EventHandler<KeyEvent> {

    @Override
    public void handle(KeyEvent event) {
      switch (event.getCode()) {
        case ESCAPE:
          onEscape();
          break;
        case ENTER:
          onEnter();
          break;
        default:
          break;
      }
    }

    private void onEnter() {
      if (!model.getSearchStringRepresentsSelectedObserver().get()) {
        performSearch(true);
      }
    }

    private void onEscape() {
      if (model.getSearchStringRepresentsSelectedObserver().getReversedObserver().get()) {
        model.refreshSearchText();
        selectAll();
      }
    }
  }
}
