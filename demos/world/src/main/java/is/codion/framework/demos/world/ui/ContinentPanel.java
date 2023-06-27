package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.Country;
import is.codion.framework.demos.world.model.ContinentModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;

import org.jfree.chart.ChartPanel;

import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;
import java.awt.Dimension;

import static is.codion.framework.demos.world.ui.ChartPanels.createBarChartPanel;
import static is.codion.framework.demos.world.ui.ChartPanels.createPieChartPanel;
import static is.codion.swing.common.ui.Sizes.setPreferredHeight;
import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.awt.event.KeyEvent.VK_1;
import static java.awt.event.KeyEvent.VK_2;

final class ContinentPanel extends EntityPanel {

  private final EntityPanel countryPanel;

  ContinentPanel(SwingEntityModel continentModel) {
    super(continentModel, new ContinentTablePanel(continentModel.tableModel()));
    countryPanel = new EntityPanel(continentModel.detailModel(Country.TYPE));
    countryPanel.tablePanel().setIncludeConditionPanel(false);
  }

  @Override
  protected void initializeUI() {
    ContinentModel model = model();

    EntityTablePanel tablePanel = tablePanel();
    tablePanel.initializePanel();
    setPreferredHeight(tablePanel, 200);

    ChartPanel populationChartPanel = createPieChartPanel(this, model.populationDataset(), "Population");
    ChartPanel surfaceAreaChartPanel = createPieChartPanel(this, model.surfaceAreaDataset(), "Surface area");
    ChartPanel gnpChartPanel = createPieChartPanel(this, model.gnpDataset(), "GNP");
    ChartPanel lifeExpectancyChartPanel = createBarChartPanel(this, model.lifeExpectancyDataset(), "Life expectancy", "Continent", "Years");
    lifeExpectancyChartPanel.setPreferredSize(new Dimension(lifeExpectancyChartPanel.getPreferredSize().width, 120));

    Dimension pieChartSize = new Dimension(260, 260);
    populationChartPanel.setPreferredSize(pieChartSize);
    surfaceAreaChartPanel.setPreferredSize(pieChartSize);
    gnpChartPanel.setPreferredSize(pieChartSize);

    JPanel pieChartChartPanel = Components.panel(gridLayout(1, 3))
            .add(populationChartPanel)
            .add(surfaceAreaChartPanel)
            .add(gnpChartPanel)
            .build();

    JPanel chartPanel = Components.borderLayoutPanel()
            .northComponent(lifeExpectancyChartPanel)
            .centerComponent(pieChartChartPanel)
            .build();

    countryPanel.initializePanel();
    countryPanel.setPreferredSize(new Dimension(countryPanel.getPreferredSize().width, 100));

    JTabbedPane tabbedPane = Components.tabbedPane()
            .tabBuilder("Charts", chartPanel)
            .mnemonic(VK_1)
            .add()
            .tabBuilder("Countries", countryPanel)
            .mnemonic(VK_2)
            .add()
            .build();

    setLayout(borderLayout());

    add(tablePanel, BorderLayout.CENTER);
    add(tabbedPane, BorderLayout.SOUTH);

    setupKeyboardActions();
    setupNavigation();
  }
}
