package org.jminor.framework.demos.world.ui;

import org.jminor.swing.common.ui.control.ControlList;
import org.jminor.swing.common.ui.control.Controls;
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
  protected ControlList getPopupControls(List<ControlList> additionalPopupControls) {
    return Controls.controlList(getRefreshControl());
  }
}
