/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.domain.DomainType;

import static is.codion.framework.domain.DomainType.domainType;

/**
 * The domain model type.
 */
public interface Petclinic {

  DomainType DOMAIN = domainType(Petclinic.class);
}
