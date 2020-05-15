/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.sql.Types;

public final class DefaultAuditUserProperty extends DefaultAuditProperty
        implements AuditProperty.AuditUserProperty {

  private static final long serialVersionUID = 1;

  DefaultAuditUserProperty(final String propertyId, final AuditAction auditAction, final String caption) {
    super(propertyId, Types.VARCHAR, auditAction, caption);
  }
}
