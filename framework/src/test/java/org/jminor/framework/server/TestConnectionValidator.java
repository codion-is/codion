/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.server.ConnectionInfo;
import org.jminor.common.server.ConnectionValidator;
import org.jminor.common.server.ServerException;

public class TestConnectionValidator implements ConnectionValidator {

  @Override
  public String getClientTypeID() {
    return "testClient";
  }

  @Override
  public void validate(final ConnectionInfo connectionInfo) throws ServerException.ConnectionValidationException {}
}
