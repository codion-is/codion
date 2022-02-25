/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

import is.codion.framework.domain.entity.Attribute;

public final class DefaultAuditUserProperty extends DefaultAuditProperty<String>
        implements AuditProperty.AuditUserProperty {

  private static final long serialVersionUID = 1;

  DefaultAuditUserProperty(Attribute<String> attribute, AuditAction auditAction, String caption) {
    super(attribute, auditAction, caption);
  }
}
