/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petclinic.domain.api;

import is.codion.framework.demos.petclinic.domain.PetClinic;
import is.codion.framework.domain.DomainType;

/**
 * The domain model type.
 */
public interface Clinic {

  DomainType DOMAIN = DomainType.domainType(PetClinic.class);
}
