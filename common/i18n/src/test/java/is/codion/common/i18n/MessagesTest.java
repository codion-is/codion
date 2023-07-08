/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.common.i18n;

import org.junit.jupiter.api.Test;

public class MessagesTest {

  @Test
  void messages() {
    Messages.cancel();
    Messages.cancelMnemonic();
    Messages.print();
    Messages.printMnemonic();
    Messages.error();
    Messages.yes();
    Messages.no();
    Messages.ok();
    Messages.okMnemonic();
    Messages.copy();
    Messages.login();
    Messages.username();
    Messages.password();
    Messages.search();
    Messages.clear();
    Messages.clearTip();
    Messages.clearMnemonic();
    Messages.advanced();
    Messages.find();
  }
}
