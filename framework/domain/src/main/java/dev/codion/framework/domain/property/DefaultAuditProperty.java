/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.domain.property;

class DefaultAuditProperty extends DefaultColumnProperty implements AuditProperty {

  private static final long serialVersionUID = 1;

  private final AuditAction auditAction;

  DefaultAuditProperty(final String propertyId, final int type, final AuditAction auditAction, final String caption) {
    super(propertyId, type, caption);
    this.auditAction = auditAction;
    super.setInsertable(false);
    super.setUpdatable(false);
  }

  @Override
  public final AuditAction getAuditAction() {
    return auditAction;
  }
}
