/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.server;

import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.db.condition.EntitySelectCondition;
import org.jminor.framework.db.local.LocalEntityConnection;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;

import java.util.List;

public final class UpdateTotalsProcedure extends AbstractProcedure<LocalEntityConnection> {

  public UpdateTotalsProcedure(final String id) {
    super(id, "Update invoice totals");
  }

  @Override
  public void execute(final LocalEntityConnection entityConnection, final Object... arguments) throws DatabaseException {
    try {
      entityConnection.beginTransaction();
      final EntitySelectCondition selectCondition = new EntityConditions(entityConnection.getDomain()).selectCondition(Chinook.T_INVOICE);
      selectCondition.setForUpdate(true);
      selectCondition.setForeignKeyFetchDepthLimit(0);
      final List<Entity> invoices = entityConnection.selectMany(selectCondition);
      for (final Entity invoice : invoices) {
        invoice.put(Chinook.INVOICE_TOTAL, invoice.get(Chinook.INVOICE_TOTAL_SUB));
      }
      final List<Entity> modifiedInvoices = EntityUtil.getModifiedEntities(invoices);
      if (!modifiedInvoices.isEmpty()) {
        entityConnection.update(modifiedInvoices);
      }
      entityConnection.commitTransaction();
    }
    catch (final DatabaseException dbException) {
      if (entityConnection.isTransactionOpen()) {
        entityConnection.rollbackTransaction();
      }
      throw dbException;
    }
  }
}
