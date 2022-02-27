/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.plugin.nextreports.model;

import is.codion.common.db.database.Database;
import is.codion.common.db.exception.DatabaseException;
import is.codion.common.db.report.Report;
import is.codion.common.db.report.ReportException;
import is.codion.common.user.User;
import is.codion.dbms.h2database.H2DatabaseFactory;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;

import org.junit.jupiter.api.Test;
import ro.nextreports.engine.ReportRunner;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class NextReportsTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  @Test
  void fillReport() throws ReportException, IOException, DatabaseException {
    EntityConnectionProvider connectionProvider = new LocalEntityConnectionProvider(
            new H2DatabaseFactory().createDatabase("jdbc:h2:mem:h2db", Database.DATABASE_INIT_SCRIPTS.get()))
            .setDomainClassName(NextDomain.class.getName()).setUser(UNIT_TEST_USER);
    Report.REPORT_PATH.set("src/test/reports/");
    LocalEntityConnection connection = (LocalEntityConnection) connectionProvider.getConnection();
    NextReportsResult result = NextReports.nextReport("test-report.report", ReportRunner.CSV_FORMAT)
            .fillReport(connection.getDatabaseConnection().getConnection(), Collections.emptyMap());
    File file = null;
    try {
      String tmpDir = System.getProperty("java.io.tmpdir");
      String filename = "NextReportsWrapperTest" + System.currentTimeMillis();
      file = result.writeResultToFile(tmpDir, filename);
      file.deleteOnExit();
      assertEquals(file.length(), result.getResult().length);
      //throws IllegalArgumentException
      result.writeResultToFile(tmpDir, filename);
      fail("Should not overwrite file");
    }
    catch (IllegalArgumentException e) {/*expected*/}
    finally {
      if (file != null) {
        file.delete();
      }
    }
  }

  public static final class NextDomain extends DefaultDomain {
    public NextDomain() {
      super(DomainType.domainType(NextDomain.class));
    }
  }
}