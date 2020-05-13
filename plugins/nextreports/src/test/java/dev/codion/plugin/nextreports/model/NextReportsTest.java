/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.plugin.nextreports.model;

import dev.codion.common.db.exception.DatabaseException;
import dev.codion.common.db.reports.ReportException;
import dev.codion.common.db.reports.ReportWrapper;
import dev.codion.common.user.User;
import dev.codion.common.user.Users;
import dev.codion.dbms.h2database.H2DatabaseProvider;
import dev.codion.framework.db.EntityConnectionProvider;
import dev.codion.framework.db.local.LocalEntityConnection;
import dev.codion.framework.db.local.LocalEntityConnectionProvider;
import dev.codion.framework.domain.Domain;

import org.junit.jupiter.api.Test;
import ro.nextreports.engine.ReportRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class NextReportsTest {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  public void fillReport() throws ReportException, IOException, DatabaseException {
    final EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            new H2DatabaseProvider().createDatabase("jdbc:h2:mem:h2db", System.getProperty("codion.db.initScript")))
            .setDomainClassName(NextDomain.class.getName()).setUser(UNIT_TEST_USER);
    ReportWrapper.REPORT_PATH.set("src/test/reports/");
    final LocalEntityConnection connection = (LocalEntityConnection) connectionProvider.getConnection();
    final NextReportsResult result = NextReports.nextReportsWrapper("test-report.report", ReportRunner.CSV_FORMAT)
            .fillReport(connection.getDatabaseConnection().getConnection(), Collections.emptyMap());
    File file = null;
    try {
      final String tmpDir = System.getProperty("java.io.tmpdir");
      final String filename = "NextReportsWrapperTest" + System.currentTimeMillis();
      file = result.writeResultToFile(tmpDir, filename);
      file.deleteOnExit();
      assertEquals(file.length(), result.getResult().length);
      //throws IllegalArgumentException
      result.writeResultToFile(tmpDir, filename);
      fail("Should not overwrite file");
    }
    catch (final IllegalArgumentException e) {/*expected*/}
    finally {
      if (file != null) {
        file.delete();
      }
    }
  }

  public static final class NextDomain extends Domain {
    public NextDomain() {}
  }
}