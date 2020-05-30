/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.domain.property;

public final class DefaultAuditUserProperty extends DefaultAuditProperty<String>
        implements AuditProperty.AuditUserProperty {

  private static final long serialVersionUID = 1;

  DefaultAuditUserProperty(final Attribute<String> attribute, final AuditAction auditAction, final String caption) {
    super(attribute, auditAction, caption);
  }
}
