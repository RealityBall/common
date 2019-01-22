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

import org.bustos.realityball.common.RealityballConfig._
import org.joda.time.DateTime
import spray.json._

class GoogleCell(val v: Any) {}
class GoogleColumn(val id: String, val label: String, val typeName: String) {}
class GoogleTooltipColumn() extends GoogleColumn("", "", "") {}

object GoogleCellJsonProtocol extends DefaultJsonProtocol {

  implicit object GoogleCellFormat extends RootJsonFormat[GoogleCell] {
    def write(c: GoogleCell) = c.v match {
      case x: String   => JsObject("v" -> JsString(x))
      case x: Int      => JsObject("v" -> JsNumber(x))
      case x: Double   => JsObject("v" -> JsNumber(x))
      case x: DateTime => JsObject("v" -> JsString(ISOdateFormatter.print(x)))
    }
    def read(value: JsValue) = value match {
      case _ => deserializationError("Undefined Read")
      // TODO: Provide read functionality
    }
  }

  implicit object GoogleColumnFormat extends RootJsonFormat[GoogleColumn] {
    def write(c: GoogleColumn) = {
      c match {
        case x: GoogleTooltipColumn => JsObject("type" -> JsString("string"), "role" -> JsString("tooltip"), "p" -> JsObject("html" -> JsBoolean(true)))
        case _ => JsObject(
          "id" -> JsString(c.id),
          "label" -> JsString(c.label),
          "type" -> JsString(c.typeName) // Required because `type' is a reserved word in Scala
          )
      }
    }
    def read(value: JsValue) = value match {
      case _ => deserializationError("Undefined Read")
      // TODO: Provide read functionality
    }
  }
}

object GoogleTableJsonProtocol extends DefaultJsonProtocol {

  import GoogleCellJsonProtocol._

  case class GoogleRow(c: List[GoogleCell])
  case class GoogleTable(cols: List[GoogleColumn], rows: List[GoogleRow])

  implicit val googleRowJSON = jsonFormat1(GoogleRow)
  implicit val googleTableJSON = jsonFormat2(GoogleTable)
}
