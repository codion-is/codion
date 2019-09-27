/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui;

import org.jminor.common.Util;
import org.jminor.common.Values;
import org.jminor.common.i18n.Messages;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;
import org.jminor.framework.model.EntityLookupModel;
import org.jminor.javafx.framework.ui.values.PropertyValues;

import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;

import java.util.List;

/**
 * A {@link TextField} allowing entity lookup based on the text entered
 */
public final class EntityLookupField extends TextField {

  private final EntityLookupModel model;
  //todo used during focus lost, todo implement
  private boolean performingLookup = false;

  private Color validBackgroundColor;
  private Color invalidBackgroundColor;

  /**
   * Instantiates a new {@link EntityLookupField} based on the given model
   * @param model the {@link EntityLookupModel} model to base this lookup field on
   */
  public EntityLookupField(final EntityLookupModel model) {
    this.model = model;
    linkToModel();
    setInvalidBackgroundColor(Color.DARKGRAY);
    setPromptText(Messages.get(Messages.SEARCH_FIELD_HINT));
    tooltipProperty().setValue(new Tooltip(model.getDescription()));
    setOnKeyPressed(new LookupKeyHandler());
    focusedProperty().addListener((observable, oldValue, newValue) -> handleFocusChanged(newValue));
    updateColors();
  }

  /**
   * @return the lookup model
   */
  public EntityLookupModel getModel() {
    return model;
  }

  /**
   * @param validBackgroundColor the background color to display when the text fits the selected value
   */
  public void setValidBackgroundColor(final Color validBackgroundColor) {
    this.validBackgroundColor = validBackgroundColor;
  }

  /**
   * @param invalidBackgroundColor the background color to display when the text does not fit the selected value
   */
  public void setInvalidBackgroundColor(final Color invalidBackgroundColor) {
    this.invalidBackgroundColor = invalidBackgroundColor;
  }

  private void performLookup(final boolean promptUser) {
    try {
      performingLookup = true;
      if (Util.nullOrEmpty(model.getSearchString())) {
        model.setSelectedEntities(null);
      }
      else {
        if (!model.searchStringRepresentsSelected()) {
          final List<Entity> queryResult;
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
      }
      updateColors();
    }
    finally {
      performingLookup = false;
    }
  }

  private void linkToModel() {
    Values.link(model.getSearchStringValue(), PropertyValues.stringPropertyValue(textProperty()));
  }

  private void selectEntities(final List<Entity> queryResult) {
    Platform.runLater(() -> model.setSelectedEntities(FXUiUtil.selectValues(queryResult, !model.getMultipleSelectionAllowedValue().get())));
  }

  private void showEmptyResultMessage() {
    Platform.runLater(new Alert(Alert.AlertType.INFORMATION,
            FrameworkMessages.get(FrameworkMessages.NO_RESULTS_FROM_CONDITION), ButtonType.OK)::showAndWait);
  }

  private void updateColors() {
//    final boolean validBackground = model.searchStringRepresentsSelected() || (searchHint != null && searchHint.isHintTextVisible());
//    setBackground(validBackground ? validBackgroundColor : invalidBackgroundColor);
  }

  private void handleFocusChanged(final Boolean hasFocus) {
    if (!hasFocus) {
      if (getText().length() == 0) {
        getModel().setSelectedEntity(null);
      }
      else if (!performingLookup && !model.searchStringRepresentsSelected()) {
        performLookup(false);
      }
      updateColors();
    }
  }

  private class LookupKeyHandler implements EventHandler<KeyEvent> {

    @Override
    public void handle(final KeyEvent event) {
      switch (event.getCode()) {
        case ESCAPE:
          handleEscape();
          break;
        case ENTER:
          handleEnter();
          break;
        default:
          break;
      }
    }

    private void handleEnter() {
      if (!model.getSearchStringRepresentsSelectedObserver().get()) {
        performLookup(true);
      }
    }

    private void handleEscape() {
      if (model.getSearchStringRepresentsSelectedObserver().getReversedObserver().get()) {
        model.refreshSearchText();
        selectAll();
      }
    }
  }
}
