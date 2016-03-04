/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.server.ClientValidator;
import org.jminor.common.server.ConnectionInfo;
import org.jminor.common.server.ServerException;

public class TestClientValidator implements ClientValidator {

  @Override
  public String getClientTypeID() {
    return "testClient";
  }

  @Override
  public void validate(final ConnectionInfo connectionInfo) throws ServerException.ClientValidationException {}
}
