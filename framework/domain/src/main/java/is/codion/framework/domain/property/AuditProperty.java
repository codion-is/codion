/*
 * Copyright (c) 2019 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

/**
 * A property representing an audit column
 * @param <T> the underlying type
 */
public interface AuditProperty<T> extends ColumnProperty<T> {

  /**
   * The possible audit actions
   */
  enum AuditAction {
    INSERT, UPDATE
  }

  /**
   * @return the audit action this property represents
   */
  AuditAction getAuditAction();
}
