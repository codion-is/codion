/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.CancelException;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.TrackTableModel;
import is.codion.framework.domain.property.Property;
import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.common.ui.dialog.Modal;
import is.codion.swing.common.ui.textfield.DecimalField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValuePanel;
import is.codion.swing.common.ui.value.NumericalValues;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityComponentValues;
import is.codion.swing.framework.ui.EntityTableConditionPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

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
                    NumericalValues.bigDecimalValue());
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
      if (property.is(Chinook.Track.MILLISECONDS)) {
        return new MinutesSecondsPanelValue();
      }

      return super.createComponentValue(property, editModel, initialValue);
    }
  }
}
