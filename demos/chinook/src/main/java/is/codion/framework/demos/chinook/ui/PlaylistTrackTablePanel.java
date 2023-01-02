/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
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
import is.codion.swing.framework.ui.EntityTablePanel;

import static is.codion.swing.framework.ui.EntityTableConditionPanel.entityTableConditionPanel;

public final class PlaylistTrackTablePanel extends EntityTablePanel {

  public PlaylistTrackTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel, entityTableConditionPanel(tableModel.tableConditionModel(), tableModel.columnModel(),
            new PlaylistTrackConditionPanelFactory(tableModel.tableConditionModel())));
    setUpdateSelectedComponentFactory(PlaylistTrack.TRACK_FK, new TrackComponentFactory());
  }

  private static final class PlaylistTrackConditionPanelFactory extends EntityConditionPanelFactory {

    private PlaylistTrackConditionPanelFactory(EntityTableConditionModel tableConditionModel) {
      super(tableConditionModel);
    }

    @Override
    protected <C extends Attribute<T>, T> ColumnConditionPanel<C, T> createConditionPanel(ColumnConditionModel<C, T> conditionModel) {
      ColumnConditionPanel<C, T> conditionPanel = super.createConditionPanel(conditionModel);
      if (PlaylistTrack.TRACK_FK.equals(conditionModel.columnIdentifier())) {
        EntitySearchField equalField = (EntitySearchField) conditionPanel.equalField();
        equalField.setSelectionProvider(new TrackSelectionProvider(equalField.model()));
      }

      return conditionPanel;
    }
  }
}
