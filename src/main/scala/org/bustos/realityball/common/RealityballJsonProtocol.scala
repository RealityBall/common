/*

    Copyright (C) 2019 Mauricio Bustos (m@bustos.org)

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.

*/

package org.bustos.realityball.common

import org.joda.time.{DateTime, DateTimeZone}
import org.joda.time.format.{DateTimeFormatter, ISODateTimeFormat}
import slick.jdbc.MySQLProfile.api._
import spray.json.{DefaultJsonProtocol, DeserializationException, JsString, JsValue, RootJsonFormat}

trait RealityballJsonProtocol extends DefaultJsonProtocol {

  import RealityballRecords._

  implicit def dateTimeOrdering: Ordering[DateTime] = Ordering.fromLessThan(_ isBefore _)
  implicit def dateTime =
    MappedColumnType.base[DateTime, java.sql.Timestamp](
      datetime => new java.sql.Timestamp(datetime.getMillis),
      timestamp => new DateTime(timestamp.getTime, DateTimeZone.UTC)
    )

  implicit object DateJsonFormat extends RootJsonFormat[DateTime] {
    private val parserISO: DateTimeFormatter = ISODateTimeFormat.dateTimeNoMillis()
    private val parserMillisISO: DateTimeFormatter = ISODateTimeFormat.dateTime()
    override def write(obj: DateTime) = JsString(parserISO.print(obj))
    override def read(json: JsValue) : DateTime = json match {
      case JsString(s) =>
        try {
          parserISO.parseDateTime(s)
        } catch {
          case _: Throwable => parserMillisISO.parseDateTime(s)
        }
      case _ => throw new DeserializationException("Error info you want here ...")
    }
  }

  implicit val playerFormat = jsonFormat8(Player)
  implicit val playerSummaryFormat = jsonFormat8(PlayerSummary)
  implicit val pitcherSummaryFormat = jsonFormat9(PitcherSummary)
  implicit val playerDataFormat = jsonFormat2(PlayerData)
  implicit val pitcherDataFormat = jsonFormat2(PitcherData)
  implicit val teamFormat = jsonFormat13(Team)
  implicit val gamedayScheduleFormat = jsonFormat17(GamedaySchedule)
  implicit val gameOddsFormat = jsonFormat5(GameOdds)
  implicit val fullGameInfoFormat = jsonFormat2(FullGameInfo)
  implicit val injuryReportFormat = jsonFormat6(InjuryReport)
}
