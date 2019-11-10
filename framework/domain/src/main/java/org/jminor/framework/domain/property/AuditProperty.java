/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.sql.Types;

/**
 * A property representing an audit column
 */
public interface AuditProperty extends ColumnProperty {

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
  interface AuditTimeProperty extends AuditProperty {}

  /**
   * Specifies a audit property with a username value
   */
  interface AuditUserProperty extends AuditProperty {}

  final class DefaultAuditTimeProperty extends DefaultAuditProperty implements AuditTimeProperty {

    private static final long serialVersionUID = 1;

    DefaultAuditTimeProperty(final String propertyId, final AuditAction auditAction, final String caption) {
      super(propertyId, Types.TIMESTAMP, auditAction, caption);
    }
  }

  final class DefaultAuditUserProperty extends DefaultAuditProperty implements AuditUserProperty {

    private static final long serialVersionUID = 1;

    DefaultAuditUserProperty(final String propertyId, final AuditAction auditAction, final String caption) {
      super(propertyId, Types.VARCHAR, auditAction, caption);
    }
  }
}
