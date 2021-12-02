package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.model.ContinentModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.swing.common.ui.Sizes.setPreferredHeight;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static org.jfree.chart.ChartFactory.createBarChart;
import static org.jfree.chart.ChartFactory.createPieChart;

public final class ContinentPanel extends EntityPanel {

  public ContinentPanel(SwingEntityModel entityModel) {
    super(entityModel, new ContinentTablePanel(entityModel.getTableModel()));
  }

  @Override
  protected void initializeUI() {
    ContinentModel model = (ContinentModel) getModel();

    EntityTablePanel tablePanel = getTablePanel();
    tablePanel.initializePanel();
    setPreferredHeight(tablePanel, 200);

    ChartPanel populationChartPanel = new ChartPanel(createPieChart("Population",
            model.getPopulationDataset()));
    populationChartPanel.setPreferredSize(new Dimension(300, 300));
    ChartPanel surfaceAreaChartPanel = new ChartPanel(createPieChart("Surface area",
            model.getSurfaceAreaDataset()));
    surfaceAreaChartPanel.setPreferredSize(new Dimension(300, 300));
    ChartPanel gnpChartPanel = new ChartPanel(createPieChart("GNP",
            model.getGnpDataset()));
    gnpChartPanel.setPreferredSize(new Dimension(300, 300));
    ChartPanel lifeExpectancyChartPanel = new ChartPanel(createBarChart("Life expectancy",
            "Continent", "Years",
            model.getLifeExpectancyDataset()));

    JPanel centerPanel = new JPanel(borderLayout());
    centerPanel.add(tablePanel, BorderLayout.NORTH);
    centerPanel.add(lifeExpectancyChartPanel, BorderLayout.CENTER);

    JPanel southChartPanel = new JPanel(gridLayout(1, 3));
    southChartPanel.add(populationChartPanel);
    southChartPanel.add(surfaceAreaChartPanel);
    southChartPanel.add(gnpChartPanel);

    setLayout(borderLayout());

    add(centerPanel, BorderLayout.CENTER);
    add(southChartPanel, BorderLayout.SOUTH);

    initializeKeyboardActions();
    initializeNavigation();
  }
}
