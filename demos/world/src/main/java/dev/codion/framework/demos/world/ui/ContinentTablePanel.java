package dev.codion.framework.demos.world.ui;

import dev.codion.swing.common.ui.control.ControlList;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.framework.model.SwingEntityTableModel;
import dev.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.util.List;

public final class ContinentTablePanel extends EntityTablePanel {

  public ContinentTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setIncludeSouthPanel(false);
    getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected ControlList getPopupControls(List<ControlList> additionalPopupControls) {
    return Controls.controlList(getRefreshControl());
  }
}
