/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

import java.time.LocalDateTime;

public final class DefaultAuditTimeProperty extends DefaultAuditProperty<LocalDateTime>
        implements AuditProperty.AuditTimeProperty {

  private static final long serialVersionUID = 1;

  DefaultAuditTimeProperty(final Attribute<LocalDateTime> attribute, final AuditAction auditAction, final String caption) {
    super(attribute, auditAction, caption);
  }
}
