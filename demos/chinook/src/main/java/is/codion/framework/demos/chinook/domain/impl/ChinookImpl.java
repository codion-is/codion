/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.domain.impl;

import is.codion.common.db.operation.DatabaseFunction;
import is.codion.common.db.result.ResultPacker;
import is.codion.common.format.LocaleDateTimePattern;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.domain.Chinook.Playlist.RandomPlaylistParameters;
import is.codion.framework.demos.chinook.domain.Chinook.Track.RaisePriceParameters;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.StringFactory;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.Column.Converter;
import is.codion.framework.domain.entity.condition.ConditionProvider;

import java.math.BigDecimal;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;

import static is.codion.framework.db.EntityConnection.Select.where;
import static is.codion.framework.domain.entity.KeyGenerator.identity;
import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.plugin.jasperreports.JasperReports.classPathReport;
import static java.lang.String.join;
import static java.util.Collections.nCopies;
import static java.util.stream.Collectors.toList;

public final class ChinookImpl extends DomainModel implements Chinook {

	public ChinookImpl() {
		super(DOMAIN);
		add(artist(), album(), employee(), customer(), genre(), mediaType(),
						track(), invoice(), invoiceLine(), playlist(), playlistTrack());
		add(Customer.REPORT, classPathReport(Chinook.class, "customer_report.jasper"));
		add(Track.RAISE_PRICE, new RaisePriceFunction());
		add(Invoice.UPDATE_TOTALS, new UpdateTotalsFunction());
		add(Playlist.RANDOM_PLAYLIST, new CreateRandomPlaylistFunction(entities()));
	}

	EntityDefinition artist() {
		return Artist.TYPE.define(
										Artist.ID.define()
														.primaryKey(),
										Artist.NAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(120),
										Artist.NUMBER_OF_ALBUMS.define()
														.subquery("""
																		SELECT COUNT(*)
																		FROM chinook.album
																		WHERE album.artistid = artist.artistid"""),
										Artist.NUMBER_OF_TRACKS.define()
														.subquery("""
																		SELECT count(*)
																		FROM chinook.track
																		JOIN chinook.album ON track.albumid = album.albumid
																		WHERE album.artistid = artist.artistid"""))
						.tableName("chinook.artist")
						.keyGenerator(identity())
						.orderBy(ascending(Artist.NAME))
						.stringFactory(Artist.NAME)
						.build();
	}

	EntityDefinition album() {
		return Album.TYPE.define(
										Album.ID.define()
														.primaryKey(),
										Album.ARTIST_ID.define()
														.column()
														.nullable(false),
										Album.ARTIST_FK.define()
														.foreignKey()
														.attributes(Artist.NAME),
										Album.TITLE.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(160),
										Album.COVER.define()
														.column()
														.format(new CoverFormatter()),
										Album.NUMBER_OF_TRACKS.define()
														.subquery("""
																		SELECT COUNT(*) FROM chinook.track
																		WHERE track.albumid = album.albumid"""),
										Album.TAGS.define()
														.column()
														.columnClass(Array.class, new TagsConverter(), ResultSet::getArray),
										Album.RATING.define()
														.subquery("""
																		SELECT AVG(rating) FROM chinook.track
																		WHERE track.albumid = album.albumid"""))
						.tableName("chinook.album")
						.keyGenerator(identity())
						.orderBy(ascending(Album.ARTIST_ID, Album.TITLE))
						.stringFactory(Album.TITLE)
						.build();
	}

	EntityDefinition employee() {
		return Employee.TYPE.define(
										Employee.ID.define()
														.primaryKey(),
										Employee.LASTNAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(20),
										Employee.FIRSTNAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(20),
										Employee.TITLE.define()
														.column()
														.maximumLength(30),
										Employee.REPORTSTO.define()
														.column(),
										Employee.REPORTSTO_FK.define()
														.foreignKey()
														.attributes(Employee.FIRSTNAME, Employee.LASTNAME),
										Employee.BIRTHDATE.define()
														.column(),
										Employee.HIREDATE.define()
														.column()
														.localeDateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDot()
																		.yearFourDigits()
																		.build()),
										Employee.ADDRESS.define()
														.column()
														.maximumLength(70),
										Employee.CITY.define()
														.column()
														.maximumLength(40),
										Employee.STATE.define()
														.column()
														.maximumLength(40),
										Employee.COUNTRY.define()
														.column()
														.maximumLength(40),
										Employee.POSTALCODE.define()
														.column()
														.maximumLength(10),
										Employee.PHONE.define()
														.column()
														.maximumLength(24),
										Employee.FAX.define()
														.column()
														.maximumLength(24),
										Employee.EMAIL.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(60))
						.tableName("chinook.employee")
						.keyGenerator(identity())
						.validator(new EmailValidator(Employee.EMAIL))
						.orderBy(ascending(Employee.LASTNAME, Employee.FIRSTNAME))
						.stringFactory(StringFactory.builder()
										.value(Employee.LASTNAME)
										.text(", ")
										.value(Employee.FIRSTNAME)
										.build())
						.build();
	}

	EntityDefinition customer() {
		return Customer.TYPE.define(
										Customer.ID.define()
														.primaryKey(),
										Customer.LASTNAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(20),
										Customer.FIRSTNAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(40),
										Customer.COMPANY.define()
														.column()
														.maximumLength(80),
										Customer.ADDRESS.define()
														.column()
														.maximumLength(70),
										Customer.CITY.define()
														.column()
														.maximumLength(40),
										Customer.STATE.define()
														.column()
														.maximumLength(40),
										Customer.COUNTRY.define()
														.column()
														.maximumLength(40),
										Customer.POSTALCODE.define()
														.column()
														.maximumLength(10),
										Customer.PHONE.define()
														.column()
														.maximumLength(24),
										Customer.FAX.define()
														.column()
														.maximumLength(24),
										Customer.EMAIL.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(60),
										Customer.SUPPORTREP_ID.define()
														.column(),
										Customer.SUPPORTREP_FK.define()
														.foreignKey()
														.attributes(Employee.FIRSTNAME, Employee.LASTNAME))
						.tableName("chinook.customer")
						.keyGenerator(identity())
						.validator(new EmailValidator(Customer.EMAIL))
						.orderBy(ascending(Customer.LASTNAME, Customer.FIRSTNAME))
						.stringFactory(new CustomerStringProvider())
						.build();
	}

	EntityDefinition genre() {
		return Genre.TYPE.define(
										Genre.ID.define()
														.primaryKey(),
										Genre.NAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(120))
						.tableName("chinook.genre")
						.keyGenerator(identity())
						.orderBy(ascending(Genre.NAME))
						.stringFactory(Genre.NAME)
						.smallDataset(true)
						.build();
	}

	EntityDefinition mediaType() {
		return MediaType.TYPE.define(
										MediaType.ID.define()
														.primaryKey(),
										MediaType.NAME.define()
														.column()
														.nullable(false)
														.maximumLength(120))
						.tableName("chinook.mediatype")
						.keyGenerator(identity())
						.stringFactory(MediaType.NAME)
						.smallDataset(true)
						.build();
	}

	EntityDefinition track() {
		return Track.TYPE.define(
										Track.ID.define()
														.primaryKey(),
										Track.ALBUM_ID.define()
														.column()
														.nullable(false),
										// tag::fetchDepth2[]
										Track.ALBUM_FK.define()
														.foreignKey(2)
														.attributes(Album.ARTIST_FK, Album.TITLE),
										// end::fetchDepth2[]
										Track.ARTIST.define()
														.denormalized(Track.ALBUM_FK, Album.ARTIST_FK),
										Track.NAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(200),
										Track.GENRE_ID.define()
														.column(),
										Track.GENRE_FK.define()
														.foreignKey(),
										Track.COMPOSER.define()
														.column()
														.maximumLength(220),
										Track.MEDIATYPE_ID.define()
														.column()
														.nullable(false),
										Track.MEDIATYPE_FK.define()
														.foreignKey(),
										Track.MILLISECONDS.define()
														.column()
														.nullable(false)
														.format(NumberFormat.getIntegerInstance()),
										Track.BYTES.define()
														.column()
														.format(NumberFormat.getIntegerInstance()),
										Track.RATING.define()
														.column()
														.nullable(false)
														.defaultValue(5)
														.valueRange(1, 10),
										Track.UNITPRICE.define()
														.column()
														.nullable(false)
														.minimumValue(0)
														.maximumFractionDigits(2),
										Track.RANDOM.define()
														.column()
														.readOnly(true)
														.selectable(false))
						.tableName("chinook.track")
						.keyGenerator(identity())
						.orderBy(ascending(Track.NAME))
						.condition(Track.NOT_IN_PLAYLIST, new NotInPlaylistConditionProvider())
						.stringFactory(Track.NAME)
						.build();
	}

	EntityDefinition invoice() {
		return Invoice.TYPE.define(
										Invoice.ID.define()
														.primaryKey(),
										Invoice.CUSTOMER_ID.define()
														.column()
														.nullable(false),
										Invoice.CUSTOMER_FK.define()
														.foreignKey()
														.attributes(Customer.FIRSTNAME, Customer.LASTNAME, Customer.EMAIL),
										Invoice.DATE.define()
														.column()
														.nullable(false)
														.defaultValue(Invoice.DATE_DEFAULT_VALUE)
														.localeDateTimePattern(LocaleDateTimePattern.builder()
																		.delimiterDot()
																		.yearFourDigits()
																		.build()),
										Invoice.BILLINGADDRESS.define()
														.column()
														.maximumLength(70),
										Invoice.BILLINGCITY.define()
														.column()
														.maximumLength(40),
										Invoice.BILLINGSTATE.define()
														.column()
														.maximumLength(40),
										Invoice.BILLINGCOUNTRY.define()
														.column()
														.maximumLength(40),
										Invoice.BILLINGPOSTALCODE.define()
														.column()
														.maximumLength(10),
										Invoice.TOTAL.define()
														.column()
														.maximumFractionDigits(2),
										Invoice.CALCULATED_TOTAL.define()
														.subquery("""
																		SELECT SUM(unitprice * quantity)
																		FROM chinook.invoiceline
																		WHERE invoiceid = invoice.invoiceid""")
														.maximumFractionDigits(2))
						.tableName("chinook.invoice")
						// tag::identity[]
						.keyGenerator(identity())
						// end::identity[]
						.orderBy(OrderBy.builder()
										.ascending(Invoice.CUSTOMER_ID)
										.descending(Invoice.DATE)
										.build())
						.stringFactory(Invoice.ID)
						.build();
	}

	EntityDefinition invoiceLine() {
		return InvoiceLine.TYPE.define(
										InvoiceLine.ID.define()
														.primaryKey(),
										InvoiceLine.INVOICE_ID.define()
														.column()
														.nullable(false),
										// tag::fetchDepth0[]
										InvoiceLine.INVOICE_FK.define()
														.foreignKey(0)
														.hidden(true),
										// end::fetchDepth0[]
										InvoiceLine.TRACK_ID.define()
														.column()
														.nullable(false),
										InvoiceLine.TRACK_FK.define()
														.foreignKey()
														.attributes(Track.NAME, Track.UNITPRICE),
										InvoiceLine.UNITPRICE.define()
														.column()
														.nullable(false),
										InvoiceLine.QUANTITY.define()
														.column()
														.nullable(false)
														.defaultValue(1),
										InvoiceLine.TOTAL.define()
														.derived(new InvoiceLineTotalProvider(),
																		InvoiceLine.QUANTITY, InvoiceLine.UNITPRICE))
						.tableName("chinook.invoiceline")
						.keyGenerator(identity())
						.build();
	}

	EntityDefinition playlist() {
		return Playlist.TYPE.define(
										Playlist.ID.define()
														.primaryKey(),
										Playlist.NAME.define()
														.column()
														.searchable(true)
														.nullable(false)
														.maximumLength(120))
						.tableName("chinook.playlist")
						.keyGenerator(identity())
						.orderBy(ascending(Playlist.NAME))
						.stringFactory(Playlist.NAME)
						.build();
	}

	EntityDefinition playlistTrack() {
		return PlaylistTrack.TYPE.define(
										PlaylistTrack.ID.define()
														.primaryKey(),
										PlaylistTrack.PLAYLIST_ID.define()
														.column()
														.nullable(false),
										PlaylistTrack.PLAYLIST_FK.define()
														.foreignKey(),
										PlaylistTrack.ARTIST.define()
														.denormalized(PlaylistTrack.ALBUM, Album.ARTIST_FK),
										PlaylistTrack.TRACK_ID.define()
														.column()
														.nullable(false),
										PlaylistTrack.TRACK_FK.define()
														.foreignKey(3),
										PlaylistTrack.ALBUM.define()
														.denormalized(PlaylistTrack.TRACK_FK, Track.ALBUM_FK))
						.tableName("chinook.playlisttrack")
						.keyGenerator(identity())
						.stringFactory(StringFactory.builder()
										.value(PlaylistTrack.PLAYLIST_FK)
										.text(" - ")
										.value(PlaylistTrack.TRACK_FK)
										.build())
						.build();
	}

	// Converts between a SQL VARCHAR ARRAY and a List<String>.
	private static final class TagsConverter implements Converter<List<String>, Array> {

		private static final int ARRAY_VALUE_INDEX = 2;

		private final ResultPacker<String> packer = resultSet -> resultSet.getString(ARRAY_VALUE_INDEX);

		@Override
		public Array toColumnValue(List<String> value, Statement statement) throws SQLException {
			return value.isEmpty() ? null :
							statement.getConnection().createArrayOf("VARCHAR", value.toArray(new Object[0]));
		}

		@Override
		public List<String> fromColumnValue(Array columnValue) throws SQLException {
			try (ResultSet resultSet = columnValue.getResultSet()) {
				return packer.pack(resultSet);
			}
		}
	}

	private static final class NotInPlaylistConditionProvider implements ConditionProvider {

		@Override
		public String toString(List<Column<?>> columns, List<?> values) {
			return new StringBuilder("""
							trackid NOT IN (
							    SELECT trackid
							    FROM chinook.playlisttrack
							    WHERE playlistid IN (""")
							.append(join(", ", nCopies(values.size(), "?"))).append(")\n")
							.append(")")
							.toString();
		}
	}

	private static final class UpdateTotalsFunction implements DatabaseFunction<EntityConnection, Collection<Long>, Collection<Entity>> {

		@Override
		public Collection<Entity> execute(EntityConnection connection,
																			Collection<Long> invoiceIds) {
			Collection<Entity> invoices =
							connection.select(where(Invoice.ID.in(invoiceIds))
											.forUpdate()
											.build());

			return connection.updateSelect(invoices.stream()
							.peek(UpdateTotalsFunction::updateTotal)
							.filter(Entity::modified)
							.collect(toList()));
		}

		private static void updateTotal(Entity invoice) {
			invoice.put(Invoice.TOTAL, invoice.optional(Invoice.CALCULATED_TOTAL).orElse(BigDecimal.ZERO));
		}
	}

	private static final class CreateRandomPlaylistFunction implements DatabaseFunction<EntityConnection, RandomPlaylistParameters, Entity> {

		private final Entities entities;

		private CreateRandomPlaylistFunction(Entities entities) {
			this.entities = entities;
		}

		@Override
		public Entity execute(EntityConnection connection,
													RandomPlaylistParameters parameters) {
			List<Long> trackIds = randomTrackIds(connection, parameters.noOfTracks(), parameters.genres());

			return insertPlaylist(connection, parameters.playlistName(), trackIds);
		}

		private Entity insertPlaylist(EntityConnection connection, String playlistName,
																	List<Long> trackIds) {
			Entity playlist = connection.insertSelect(createPlaylist(playlistName));

			connection.insert(createPlaylistTracks(playlist.primaryKey().get(), trackIds));

			return playlist;
		}

		private Entity createPlaylist(String playlistName) {
			return entities.builder(Playlist.TYPE)
							.with(Playlist.NAME, playlistName)
							.build();
		}

		private List<Entity> createPlaylistTracks(Long playlistId, List<Long> trackIds) {
			return trackIds.stream()
							.map(trackId -> createPlaylistTrack(playlistId, trackId))
							.collect(toList());
		}

		private Entity createPlaylistTrack(Long playlistId, Long trackId) {
			return entities.builder(PlaylistTrack.TYPE)
							.with(PlaylistTrack.PLAYLIST_ID, playlistId)
							.with(PlaylistTrack.TRACK_ID, trackId)
							.build();
		}

		private static List<Long> randomTrackIds(EntityConnection connection, int noOfTracks,
																						 Collection<Entity> genres) {
			return connection.select(Track.ID,
							where(Track.GENRE_FK.in(genres))
											.orderBy(ascending(Track.RANDOM))
											.limit(noOfTracks)
											.build());
		}
	}

	private static final class RaisePriceFunction implements DatabaseFunction<EntityConnection, RaisePriceParameters, Collection<Entity>> {

		@Override
		public Collection<Entity> execute(EntityConnection entityConnection,
																			RaisePriceParameters parameters) {
			Select select =
							where(Track.ID.in(parameters.trackIds()))
											.forUpdate()
											.build();

			return entityConnection.updateSelect(entityConnection.select(select).stream()
							.peek(track -> raisePrice(track, parameters.priceIncrease()))
							.collect(toList()));
		}

		private static void raisePrice(Entity track, BigDecimal priceIncrease) {
			track.put(Track.UNITPRICE, track.get(Track.UNITPRICE).add(priceIncrease));
		}
	}
}