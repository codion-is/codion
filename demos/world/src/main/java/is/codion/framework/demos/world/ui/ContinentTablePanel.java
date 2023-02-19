package is.codion.framework.demos.world.ui;

import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.util.List;

final class ContinentTablePanel extends EntityTablePanel {

  ContinentTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setIncludeSouthPanel(false);
    table().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected Controls createPopupMenuControls(List<Controls> additionalPopupMenuControls) {
    return Controls.builder()
            .control(createRefreshControl())
            .build();
  }
}
