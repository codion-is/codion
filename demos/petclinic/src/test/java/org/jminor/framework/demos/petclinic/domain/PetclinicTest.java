/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petclinic.domain;

import org.jminor.common.db.exception.DatabaseException;
import org.jminor.framework.demos.petclinic.domain.impl.PetclinicImpl;
import org.jminor.framework.domain.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

public final class PetclinicTest extends EntityTestUnit {

  public PetclinicTest() {
    super(PetclinicImpl.class.getName());
  }

  @Test
  void vet() throws DatabaseException {
    testEntity(Petclinic.T_VET);
  }

  @Test
  void specialty() throws DatabaseException {
    testEntity(Petclinic.T_SPECIALTY);
  }

  @Test
  void vetSpecialty() throws DatabaseException {
    testEntity(Petclinic.T_VET_SPECIALTY);
  }

  @Test
  void petType() throws DatabaseException {
    testEntity(Petclinic.T_PET_TYPE);
  }

  @Test
  void owner() throws DatabaseException {
    testEntity(Petclinic.T_OWNER);
  }

  @Test
  void pet() throws DatabaseException {
    testEntity(Petclinic.T_PET);
  }

  @Test
  void visit() throws DatabaseException {
    testEntity(Petclinic.T_VISIT);
  }
}
