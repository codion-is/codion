package org.jminor.framework.demos.world.ui;

import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.framework.model.SwingEntityTableModel;
import org.jminor.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.util.List;

public final class ContinentTablePanel extends EntityTablePanel {

  public ContinentTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setIncludeSouthPanel(false);
    getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected ControlSet getPopupControls(List<ControlSet> additionalPopupControlSets) {
    return new ControlSet(getRefreshControl());
  }
}
