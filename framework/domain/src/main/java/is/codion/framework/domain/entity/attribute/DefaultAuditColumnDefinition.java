/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

import static java.util.Objects.requireNonNull;

final class DefaultAuditColumnDefinition<T> extends DefaultColumnDefinition<T> implements AuditColumnDefinition<T> {

  private static final long serialVersionUID = 1;

  private final AuditColumn.AuditAction auditAction;

  private DefaultAuditColumnDefinition(DefaultAuditColumnDefinitionBuilder<T, ?> builder) {
    super(builder);
    this.auditAction = builder.auditAction;
  }

  @Override
  public AuditColumn.AuditAction auditAction() {
    return auditAction;
  }

  static class DefaultAuditColumnDefinitionBuilder<T, B extends ColumnDefinition.Builder<T, B>>
          extends AbstractReadOnlyColumnDefinitionBuilder<T, B> {

    private final AuditColumn.AuditAction auditAction;

    DefaultAuditColumnDefinitionBuilder(Column<T> column, AuditColumn.AuditAction auditAction) {
      super(column);
      this.auditAction = requireNonNull(auditAction);
    }

    @Override
    public AuditColumnDefinition<T> build() {
      return new DefaultAuditColumnDefinition<>(this);
    }
  }
}
