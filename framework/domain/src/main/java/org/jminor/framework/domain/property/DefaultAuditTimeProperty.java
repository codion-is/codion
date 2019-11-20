/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.domain.property;

import java.sql.Types;

public final class DefaultAuditTimeProperty extends DefaultAuditProperty
        implements AuditProperty.AuditTimeProperty {

  private static final long serialVersionUID = 1;

  DefaultAuditTimeProperty(final String propertyId, final AuditAction auditAction, final String caption) {
    super(propertyId, Types.TIMESTAMP, auditAction, caption);
  }
}
