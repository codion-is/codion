/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.world.ui;

import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;
import org.jfree.data.general.PieDataset;

import javax.swing.JComponent;
import javax.swing.JPanel;

import static is.codion.framework.demos.world.ui.ChartPanels.createPieChartPanel;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static is.codion.swing.common.ui.component.Components.tabbedPane;
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
  protected final void layoutPanel(JComponent tableComponent, JPanel southPanel) {
    super.layoutPanel(tabbedPane()
            .tabBuilder("Table", borderLayoutPanel()
                    .centerComponent(tableComponent)
                    .southComponent(southPanel)
                    .build())
            .mnemonic(VK_1)
            .add()
            .tabBuilder("Chart", chartPanel)
            .mnemonic(VK_2)
            .add()
            .build(), southPanel);
  }
}
