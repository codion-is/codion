package is.codion.framework.demos.world.ui;

import is.codion.swing.common.ui.control.ControlList;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import javax.swing.JTable;
import java.util.List;

import static is.codion.swing.common.ui.control.ControlList.controlListBuilder;

public final class ContinentTablePanel extends EntityTablePanel {

  public ContinentTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setIncludeSouthPanel(false);
    getTable().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
  }

  @Override
  protected ControlList getPopupControls(List<ControlList> additionalPopupControls) {
    return controlListBuilder().control(createRefreshControl()).build();
  }
}
