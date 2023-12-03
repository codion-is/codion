/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import java.util.Random;

final class LoadTestUtil {

  private static final int MAX_ARTIST_ID = 275;
  private static final int MAX_CUSTOMER_ID = 59;
  private static final int MAX_TRACK_ID = 3503;

  static final Random RANDOM = new Random();

  static long randomArtistId() {
    return RANDOM.nextInt(MAX_ARTIST_ID) + 1;
  }

  static long randomCustomerId() {
    return RANDOM.nextInt(MAX_CUSTOMER_ID) + 1;
  }

  static long randomTrackId() {
    return RANDOM.nextInt(MAX_TRACK_ID) + 1;
  }
}
