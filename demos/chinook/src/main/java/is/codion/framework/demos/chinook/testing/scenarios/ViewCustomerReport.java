/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ViewCustomerReport extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  @Override
  protected void perform(ChinookApplicationModel application) throws Exception {
    SwingEntityTableModel customerModel = application.entityModel(Customer.TYPE).tableModel();
    customerModel.refresh();
    EntityLoadTestModel.selectRandomRow(customerModel);

    Collection<Long> customerIDs =
            Entity.getDistinct(Customer.ID, customerModel.selectionModel().getSelectedItems());
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);
    customerModel.connectionProvider().connection().fillReport(Customer.REPORT, reportParameters);
  }

  @Override
  public int defaultWeight() {
    return 2;
  }
}
