/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

class DefaultAuditProperty<T> extends DefaultColumnProperty<T> implements AuditProperty<T> {

  private static final long serialVersionUID = 1;

  private final AuditAction auditAction;

  DefaultAuditProperty(Attribute<T> attribute, AuditAction auditAction, String caption) {
    super(attribute, caption);
    this.auditAction = auditAction;
  }

  @Override
  public final AuditAction getAuditAction() {
    return auditAction;
  }

  @Override
  <P extends ColumnProperty<T>, B extends ColumnProperty.Builder<T, P, B>> ColumnProperty.Builder<T, P, B> builder() {
    return (ColumnProperty.Builder<T, P, B>) new DefaultColumnPropertyBuilder<>(this).readOnly();
  }
}
