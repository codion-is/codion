package dev.codion.framework.demos.world.ui;

import dev.codion.framework.demos.world.model.LookupTableModel;
import dev.codion.swing.common.ui.control.Control;
import dev.codion.swing.common.ui.control.ControlList;
import dev.codion.swing.common.ui.control.Controls;
import dev.codion.swing.framework.ui.EntityTablePanel;

import org.kordamp.ikonli.foundation.Foundation;
import org.kordamp.ikonli.swing.FontIcon;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static dev.codion.swing.common.ui.dialog.Dialogs.selectFileToSave;
import static dev.codion.swing.common.ui.worker.ProgressWorker.runWithProgressBar;
import static dev.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons.ICON_SIZE;
import static dev.codion.swing.plugin.ikonli.foundation.IkonliFoundationIcons.imageIcon;

public final class LookupTablePanel extends EntityTablePanel {

  public LookupTablePanel(LookupTableModel tableModel) {
    super(tableModel);
    setConditionPanelVisible(true);
  }

  @Override
  protected ControlList getPopupControls(List<ControlList> additionalPopupControls) {
    ControlList controls = super.getPopupControls(additionalPopupControls);
    controls.addSeparatorAt(2);

    Control exportControl = Controls.control(this::exportCSV, "Export CSV...");
    exportControl.setIcon(imageIcon(FontIcon.of(Foundation.PAGE_EXPORT_CSV, ICON_SIZE)));

    controls.addAt(3, exportControl);

    return controls;
  }

  private void exportCSV() throws IOException {
    File fileToSave = selectFileToSave(this, null, "export.csv");
    runWithProgressBar(this, "Exporting data",
            "Export successful", "Export failed",
            () -> ((LookupTableModel) getTableModel()).exportCSV(fileToSave));
  }
}
