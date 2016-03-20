/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.server;

import org.jminor.common.db.AbstractProcedure;
import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.db.EntityConnection;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.db.criteria.EntitySelectCriteria;
import org.jminor.framework.demos.chinook.domain.Chinook;
import org.jminor.framework.domain.Entity;
import org.jminor.framework.domain.EntityUtil;

import java.util.List;

public final class UpdateTotalsProcedure extends AbstractProcedure<EntityConnection> {

  public UpdateTotalsProcedure(final String id) {
    super(id, "Update invoice totals");
  }

  @Override
  public void execute(final EntityConnection entityConnection, final Object... arguments) throws DatabaseException {
    try {
      entityConnection.beginTransaction();
      final EntitySelectCriteria selectCriteria = EntityCriteriaUtil.selectCriteria(Chinook.T_INVOICE);
      selectCriteria.setForUpdate(true);
      selectCriteria.setForeignKeyFetchDepthLimit(0);
      final List<Entity> invoices = entityConnection.selectMany(selectCriteria);
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
