/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

import java.time.temporal.Temporal;

public final class DefaultAuditTimeProperty<T extends Temporal> extends DefaultAuditProperty<T>
        implements AuditProperty.AuditTimeProperty<T> {

  private static final long serialVersionUID = 1;

  DefaultAuditTimeProperty(Attribute<T> attribute, AuditAction auditAction, String caption) {
    super(attribute, auditAction, caption);
  }
}
