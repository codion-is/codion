package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.ContinentModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.framework.demos.world.ui.ChartPanels.createBarChartPanel;
import static is.codion.framework.demos.world.ui.ChartPanels.createPieChartPanel;
import static is.codion.swing.common.ui.Sizes.setPreferredHeight;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

final class ContinentPanel extends EntityPanel {

  ContinentPanel(SwingEntityModel continentModel) {
    super(continentModel, new ContinentTablePanel(continentModel.tableModel()));
  }

  @Override
  protected void initializeUI() {
    ContinentModel model = (ContinentModel) model();

    EntityTablePanel tablePanel = tablePanel();
    tablePanel.initializePanel();
    setPreferredHeight(tablePanel, 200);

    ChartPanel populationChartPanel = createPieChartPanel(this, model.getPopulationDataset(), "Population");
    ChartPanel surfaceAreaChartPanel = createPieChartPanel(this, model.getSurfaceAreaDataset(), "Surface area");
    ChartPanel gnpChartPanel = createPieChartPanel(this, model.getGnpDataset(), "GNP");
    ChartPanel lifeExpectancyChartPanel = createBarChartPanel(this, model.getLifeExpectancyDataset(), "Life expectancy", "Continent", "Years");

    Dimension pieChartSize = new Dimension(300, 300);
    populationChartPanel.setPreferredSize(pieChartSize);
    surfaceAreaChartPanel.setPreferredSize(pieChartSize);
    gnpChartPanel.setPreferredSize(pieChartSize);

    JPanel centerPanel = Components.panel(borderLayout())
            .add(tablePanel, BorderLayout.NORTH)
            .add(lifeExpectancyChartPanel, BorderLayout.CENTER)
            .build();

    JPanel southChartPanel = Components.panel(gridLayout(1, 3))
            .add(populationChartPanel)
            .add(surfaceAreaChartPanel)
            .add(gnpChartPanel)
            .build();

    setLayout(borderLayout());

    add(centerPanel, BorderLayout.CENTER);
    add(southChartPanel, BorderLayout.SOUTH);

    initializeKeyboardActions();
    initializeNavigation();
  }
}
