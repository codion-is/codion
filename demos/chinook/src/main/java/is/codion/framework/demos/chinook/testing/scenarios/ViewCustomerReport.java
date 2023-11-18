/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Customer;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.randomCustomerId;

public final class ViewCustomerReport extends AbstractUsageScenario<EntityConnectionProvider> {

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) throws Exception {
    EntityConnection connection = connectionProvider.connection();
    Entity customer = connection.selectSingle(Customer.ID.equalTo(randomCustomerId()));
    Collection<Long> customerIDs = Collections.singletonList(customer.primaryKey().get());
    Map<String, Object> reportParameters = new HashMap<>();
    reportParameters.put("CUSTOMER_IDS", customerIDs);
    connection.report(Customer.REPORT, reportParameters);
  }

  @Override
  public int defaultWeight() {
    return 2;
  }
}
