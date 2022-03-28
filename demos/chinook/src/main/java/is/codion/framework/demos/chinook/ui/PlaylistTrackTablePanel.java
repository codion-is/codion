/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.model.table.ColumnConditionModel;
import is.codion.framework.demos.chinook.domain.Chinook.PlaylistTrack;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityConditionPanelFactory;
import is.codion.swing.framework.ui.EntitySearchField;
import is.codion.swing.framework.ui.EntityTableConditionPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

public final class PlaylistTrackTablePanel extends EntityTablePanel {

  public PlaylistTrackTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel, new EntityTableConditionPanel(tableModel.getTableConditionModel(), tableModel.getColumnModel(),
            new PlaylistTrackConditionPanelFactory(tableModel.getTableConditionModel())));
    setUpdateSelectedComponentFactory(PlaylistTrack.TRACK_FK, new TrackComponentFactory());
  }

  private static final class PlaylistTrackConditionPanelFactory extends EntityConditionPanelFactory {

    private PlaylistTrackConditionPanelFactory(EntityTableConditionModel tableConditionModel) {
      super(tableConditionModel);
    }

    @Override
    protected <C extends Attribute<T>, T> ColumnConditionPanel<C, T> createConditionPanel(ColumnConditionModel<C, T> conditionModel) {
      ColumnConditionPanel<C, T> conditionPanel = super.createConditionPanel(conditionModel);
      if (PlaylistTrack.TRACK_FK.equals(conditionModel.getColumnIdentifier())) {
        EntitySearchField equalField = (EntitySearchField) conditionPanel.getEqualField();
        equalField.setSelectionProvider(new TrackSelectionProvider(equalField.getModel()));
      }

      return conditionPanel;
    }
  }
}
