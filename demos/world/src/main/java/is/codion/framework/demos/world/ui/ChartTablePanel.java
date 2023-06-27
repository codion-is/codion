package is.codion.framework.demos.world.ui;

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.data.general.PieDataset;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

import static is.codion.framework.demos.world.ui.ChartPanels.createPieChartPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.awt.event.KeyEvent.VK_1;
import static java.awt.event.KeyEvent.VK_2;

abstract class ChartTablePanel extends EntityTablePanel {

  private final ChartPanel chartPanel;

  protected ChartTablePanel(SwingEntityTableModel tableModel, PieDataset<String> chartDataset,
                            String chartTitle) {
    super(tableModel);
    chartPanel = createPieChartPanel(this, chartDataset, chartTitle);
  }

  @Override
  protected final void layoutPanel(JPanel tablePanel, JPanel southPanel) {
    JPanel tableViewPanel = Components.borderLayoutPanel()
            .centerComponent(tablePanel)
            .southComponent(southPanel)
            .build();
    JTabbedPane tabbedPane = Components.tabbedPane()
            .tabBuilder("Table", tableViewPanel)
            .mnemonic(VK_1)
            .add()
            .tabBuilder("Chart", chartPanel)
            .mnemonic(VK_2)
            .add()
            .build();
    setLayout(borderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }
}
