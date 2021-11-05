/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.db.exception.DatabaseException;
import is.codion.framework.demos.chinook.domain.Chinook.Track;
import is.codion.framework.demos.chinook.model.TrackTableModel;
import is.codion.framework.domain.entity.Attribute;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.textfield.BigDecimalField;
import is.codion.swing.common.ui.value.ComponentValue;
import is.codion.swing.common.ui.value.ComponentValues;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityComponentValues;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JComponent;
import java.math.BigDecimal;
import java.util.List;
import java.util.ResourceBundle;

public final class TrackTablePanel extends EntityTablePanel {

  private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(TrackTablePanel.class.getName());

  public TrackTablePanel(final SwingEntityTableModel tableModel) {
    super(tableModel, new TrackComponentValues());
  }

  @Override
  protected Controls getPopupControls(final List<Controls> additionalPopupControls) {
    return super.getPopupControls(additionalPopupControls)
            .addAt(0, Control.builder(this::raisePriceOfSelected)
                    .caption(BUNDLE.getString("raise_price") + "...")
                    .enabledState(getTableModel().getSelectionModel().getSelectionNotEmptyObserver())
                    .build())
            .addSeparatorAt(1);
  }

  private void raisePriceOfSelected() throws DatabaseException {
    final TrackTableModel tableModel = (TrackTableModel) getTableModel();

    tableModel.raisePriceOfSelected(getAmountFromUser());
  }

  private BigDecimal getAmountFromUser() {
    return ComponentValues.bigDecimalField(new BigDecimalField())
            .showDialog(this, BUNDLE.getString("amount"));
  }

  private static final class TrackComponentValues extends EntityComponentValues {

    @Override
    public <T, C extends JComponent> ComponentValue<T, C> createComponentValue(final Attribute<T> attribute,
                                                                               final SwingEntityEditModel editModel,
                                                                               final T initialValue) {
      if (attribute.equals(Track.MILLISECONDS)) {
        final MinutesSecondsPanelValue minutesSecondsPanelValue = new MinutesSecondsPanelValue();
        minutesSecondsPanelValue.set((Integer) initialValue);

        return (ComponentValue<T, C>) minutesSecondsPanelValue;
      }

      return super.createComponentValue(attribute, editModel, initialValue);
    }
  }
}
