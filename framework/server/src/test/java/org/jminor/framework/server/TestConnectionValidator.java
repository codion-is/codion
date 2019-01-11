/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.remote.ConnectionRequest;
import org.jminor.common.remote.ConnectionValidator;
import org.jminor.common.remote.ServerException;

public class TestConnectionValidator implements ConnectionValidator {

  @Override
  public String getClientTypeId() {
    return "testClient";
  }

  @Override
  public void validate(final ConnectionRequest connectionRequest) throws ServerException.ConnectionValidationException {}
}
