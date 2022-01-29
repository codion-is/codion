package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.LookupTableModel;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import java.io.File;
import java.io.IOException;
import java.util.List;

public final class LookupTablePanel extends EntityTablePanel {

  public LookupTablePanel(SwingEntityTableModel tableModel) {
    super(tableModel);
    setConditionPanelVisible(true);
    setControl(ControlCode.CLEAR, Control.builder(this::clearTableAndConditions)
            .caption("Clear")
            .mnemonic('C')
            .build());
  }

  @Override
  protected Controls getPopupControls(List<Controls> additionalPopupControls) {
    return super.getPopupControls(additionalPopupControls)
            .addSeparatorAt(1)
            .addAt(2, Control.builder(this::exportCSV)
                    .caption("Export CSV...")
                    .build());
  }

  private void exportCSV() throws IOException {
    File fileToSave = Dialogs.fileSelectionDialog()
            .owner(this)
            .selectFileToSave("export.csv");
    Dialogs.progressWorkerDialog(() -> ((LookupTableModel) getTableModel()).exportCSV(fileToSave))
            .owner(this)
            .title("Exporting data")
            .successMessage("Export successful")
            .failTitle("Export failed")
            .execute();
  }

  private void clearTableAndConditions() {
    getTableModel().clear();
    getTableModel().getTableConditionModel().clearConditions();
  }
}
