/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

/**
 * A column definition representing an audit column
 * @param <T> the underlying type
 */
public interface AuditColumnDefinition<T> extends ColumnDefinition<T> {

  /**
   * @return the audit action this attribute represents
   */
  AuditColumn.AuditAction auditAction();
}
