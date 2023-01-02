/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.domain;

import is.codion.framework.demos.chinook.domain.impl.ChinookImpl;
import is.codion.framework.domain.entity.test.EntityTestUnit;

import org.junit.jupiter.api.Test;

import static is.codion.framework.demos.chinook.domain.Chinook.*;

public class ChinookTest extends EntityTestUnit {

  public ChinookTest() {
    super(ChinookImpl.class.getName());
  }

  @Test
  void album() throws Exception {
    test(Album.TYPE);
  }

  @Test
  void artist() throws Exception {
    test(Artist.TYPE);
  }

  @Test
  void customer() throws Exception {
    test(Customer.TYPE);
  }

  @Test
  void employee() throws Exception {
    test(Employee.TYPE);
  }

  @Test
  void genre() throws Exception {
    test(Genre.TYPE);
  }

  @Test
  void invoce() throws Exception {
    test(Invoice.TYPE);
  }

  @Test
  void invoiceLine() throws Exception {
    test(InvoiceLine.TYPE);
  }

  @Test
  void mediaType() throws Exception {
    test(MediaType.TYPE);
  }

  @Test
  void playlist() throws Exception {
    test(Playlist.TYPE);
  }

  @Test
  void playlistTrack() throws Exception {
    test(PlaylistTrack.TYPE);
  }

  @Test
  void track() throws Exception {
    test(Track.TYPE);
  }
}
