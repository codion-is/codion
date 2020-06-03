/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
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
  public void album() throws Exception {
    test(Album.TYPE);
  }

  @Test
  public void artist() throws Exception {
    test(Artist.TYPE);
  }

  @Test
  public void customer() throws Exception {
    test(Customer.TYPE);
  }

  @Test
  public void employee() throws Exception {
    test(Employee.TYPE);
  }

  @Test
  public void genre() throws Exception {
    test(Genre.TYPE);
  }

  @Test
  public void invoce() throws Exception {
    test(Invoice.TYPE);
  }

  @Test
  public void invoiceLine() throws Exception {
    test(InvoiceLine.TYPE);
  }

  @Test
  public void mediaType() throws Exception {
    test(MediaType.TYPE);
  }

  @Test
  public void playlist() throws Exception {
    test(Playlist.TYPE);
  }

  @Test
  public void playlistTrack() throws Exception {
    test(PlaylistTrack.TYPE);
  }

  @Test
  public void track() throws Exception {
    test(Track.TYPE);
  }
}
