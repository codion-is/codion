/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

import static java.util.Objects.requireNonNull;

class DefaultAuditProperty<T> extends DefaultColumnProperty<T> implements AuditProperty<T> {

  private static final long serialVersionUID = 1;

  private final AuditAction auditAction;

  private DefaultAuditProperty(DefaultAuditPropertyBuilder<T, ?> builder) {
    super(builder);
    this.auditAction = builder.auditAction;
  }

  @Override
  public final AuditAction getAuditAction() {
    return auditAction;
  }

  static final class DefaultAuditPropertyBuilder<T, B extends ColumnProperty.Builder<T, AuditProperty<T>, B>> extends DefaultColumnPropertyBuilder<T, AuditProperty<T>, B> {

    private final AuditAction auditAction;

    DefaultAuditPropertyBuilder(Attribute<T> attribute, String caption, AuditAction auditAction) {
      super(attribute, caption);
      this.auditAction = requireNonNull(auditAction);
    }

    @Override
    public AuditProperty<T> build() {
      return new DefaultAuditProperty<>(this);
    }
  }
}
