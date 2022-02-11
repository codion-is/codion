package is.codion.framework.demos.world.ui;

import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.data.general.PieDataset;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

import static is.codion.framework.demos.world.ui.ChartPanels.createPieChartPanel;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;

abstract class ChartTablePanel extends EntityTablePanel {

  private final ChartPanel chartPanel;

  protected ChartTablePanel(SwingEntityTableModel tableModel, PieDataset<String> chartDataset,
                            String chartTitle) {
    super(tableModel);
    chartPanel = createPieChartPanel(this, chartDataset, chartTitle);
  }

  @Override
  protected final void layoutPanel(JPanel tablePanel, JPanel southPanel) {
    JPanel tableViewPanel = new JPanel(borderLayout());
    tableViewPanel.add(tablePanel, BorderLayout.CENTER);
    tableViewPanel.add(southPanel, BorderLayout.SOUTH);
    JTabbedPane tabbedPane = new JTabbedPane();
    tabbedPane.addTab("Table", tableViewPanel);
    tabbedPane.addTab("Chart", chartPanel);
    setLayout(borderLayout());
    add(tabbedPane, BorderLayout.CENTER);
  }
}
