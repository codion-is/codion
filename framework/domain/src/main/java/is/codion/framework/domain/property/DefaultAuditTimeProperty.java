/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import java.sql.Types;

public final class DefaultAuditTimeProperty extends DefaultAuditProperty
        implements AuditProperty.AuditTimeProperty {

  private static final long serialVersionUID = 1;

  DefaultAuditTimeProperty(final Attribute<?> attribute, final AuditAction auditAction, final String caption) {
    super(attribute, Types.TIMESTAMP, auditAction, caption);
  }
}
