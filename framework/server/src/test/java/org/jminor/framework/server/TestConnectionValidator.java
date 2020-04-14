/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.server;

import org.jminor.common.remote.client.ConnectionRequest;
import org.jminor.common.remote.exception.ConnectionValidationException;
import org.jminor.common.remote.server.ConnectionValidator;

public class TestConnectionValidator implements ConnectionValidator {

  @Override
  public String getClientTypeId() {
    return "testClient";
  }

  @Override
  public void validate(final ConnectionRequest connectionRequest) throws ConnectionValidationException {}
}
