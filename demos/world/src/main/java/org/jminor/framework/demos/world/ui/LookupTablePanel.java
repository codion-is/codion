package org.jminor.framework.demos.world.ui;

import org.jminor.framework.demos.world.model.LookupTableModel;
import org.jminor.swing.common.ui.control.ControlSet;
import org.jminor.swing.common.ui.control.Controls;
import org.jminor.swing.framework.ui.EntityTablePanel;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static org.jminor.swing.common.ui.dialog.Dialogs.selectFileToSave;
import static org.jminor.swing.common.ui.worker.ProgressWorker.runWithProgressBar;

public final class LookupTablePanel extends EntityTablePanel {

  public LookupTablePanel(LookupTableModel tableModel) {
    super(tableModel);
    setConditionPanelVisible(true);
  }

  @Override
  protected ControlSet getPopupControls(List<ControlSet> additionalPopupControlSets) {
    ControlSet controls = super.getPopupControls(additionalPopupControlSets);
    controls.addSeparatorAt(2);
    controls.addAt(3, Controls.control(this::exportCSV, "Export CSV..."));

    return controls;
  }

  private void exportCSV() throws IOException {
    File fileToSave = selectFileToSave(this, null, "export.csv");
    runWithProgressBar(this, "Exporting data",
            "Export successful", "Export failed",
            () -> ((LookupTableModel) getTableModel()).exportCSV(fileToSave));
  }
}
