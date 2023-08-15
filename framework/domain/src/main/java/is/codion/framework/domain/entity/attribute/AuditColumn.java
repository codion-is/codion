/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.entity.attribute;

public interface AuditColumn {

  /**
   * The possible audit actions
   */
  enum AuditAction {
    INSERT, UPDATE
  }
}
