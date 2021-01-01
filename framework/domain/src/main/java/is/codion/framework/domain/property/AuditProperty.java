/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.time.LocalDateTime;

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

  /**
   * Specifies a audit property with a timestamp value
   */
  interface AuditTimeProperty extends AuditProperty<LocalDateTime> {}

  /**
   * Specifies a audit property with a username value
   */
  interface AuditUserProperty extends AuditProperty<String> {}
}
