/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entities;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ViewCustomerReport extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      final SwingEntityTableModel customerModel = application.getEntityModel(Customer.TYPE).getTableModel();
      customerModel.refresh();
      EntityLoadTestModel.selectRandomRow(customerModel);

      final Collection<Long> customerIDs =
              Entities.getDistinctValues(Customer.ID, customerModel.getSelectionModel().getSelectedItems());
      final Map<String, Object> reportParameters = new HashMap<>();
      reportParameters.put("CUSTOMER_IDS", customerIDs);
      customerModel.getConnectionProvider().getConnection().fillReport(Customer.REPORT, reportParameters);
    }
    catch (final Exception e) {
      throw new ScenarioException(e);
    }
  }

  @Override
  public int getDefaultWeight() {
    return 2;
  }
}
