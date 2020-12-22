/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

class DefaultAuditProperty<T> extends DefaultColumnProperty<T> implements AuditProperty<T> {

  private static final long serialVersionUID = 1;

  private final AuditAction auditAction;

  DefaultAuditProperty(final Attribute<T> attribute, final AuditAction auditAction, final String caption) {
    super(attribute, caption);
    super.readOnly();
    this.auditAction = auditAction;
  }

  @Override
  public final AuditAction getAuditAction() {
    return auditAction;
  }
}
