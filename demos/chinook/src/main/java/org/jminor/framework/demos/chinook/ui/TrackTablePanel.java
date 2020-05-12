/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.ui;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.common.model.CancelException;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.demos.chinook.model.TrackTableModel;
import org.jminor.framework.domain.property.Property;
import org.jminor.swing.common.ui.control.ControlList;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.common.ui.dialog.Dialogs;
import org.jminor.swing.common.ui.dialog.Modal;
import org.jminor.swing.common.ui.textfield.DecimalField;
import org.jminor.swing.common.ui.value.ComponentValue;
import org.jminor.swing.common.ui.value.ComponentValuePanel;
import org.jminor.swing.common.ui.value.NumericalValues;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityComponentValues;
import org.jminor.swing.framework.ui.EntityTableConditionPanel;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.math.BigDecimal;
import java.util.List;

public class TrackTablePanel extends EntityTablePanel {

  public TrackTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel, new TrackComponentValues(), new EntityTableConditionPanel(tableModel));
  }

  @Override
  protected ControlList getPopupControls(final List<ControlList> additionalPopupControls) {
    final ControlList controls = super.getPopupControls(additionalPopupControls);
    controls.addAt(0, Controls.control(this::raisePriceOfSelected, "Raise price...",
            getTableModel().getSelectionModel().getSelectionNotEmptyObserver()));
    controls.addSeparatorAt(1);

    return controls;
  }

  private void raisePriceOfSelected() throws DatabaseException {
    final TrackTableModel tableModel = (TrackTableModel) getTableModel();

    tableModel.raisePriceOfSelected(getAmountFromUser());
  }

  private BigDecimal getAmountFromUser() {
    final ComponentValuePanel<BigDecimal, DecimalField> inputPanel =
            new ComponentValuePanel<>("Amount",
                    NumericalValues.bigDecimalValue(new DecimalField()));
    Dialogs.displayInDialog(this, inputPanel, "Price Raise", Modal.YES,
            inputPanel.getOkAction(), inputPanel.getButtonClickObserver());
    if (inputPanel.isInputAccepted() && inputPanel.getValue() != null) {
      return inputPanel.getValue();
    }

    throw new CancelException();
  }

  private static final class TrackComponentValues extends EntityComponentValues {

    @Override
    public ComponentValue createComponentValue(final Property property,
                                               final SwingEntityEditModel editModel,
                                               final Object initialValue) {
      if (property.is(Chinook.TRACK_MILLISECONDS)) {
        return new MinutesSecondsPanelValue();
      }

      return super.createComponentValue(property, editModel, initialValue);
    }
  }
}
