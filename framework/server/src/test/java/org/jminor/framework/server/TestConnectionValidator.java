/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.server.ConnectionRequest;
import org.jminor.common.server.ConnectionValidator;
import org.jminor.common.server.ServerException;

public class TestConnectionValidator implements ConnectionValidator {

  @Override
  public String getClientTypeId() {
    return "testClient";
  }

  @Override
  public void validate(final ConnectionRequest connectionRequest) throws ServerException.ConnectionValidationException {}
}
