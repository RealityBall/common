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

import org.joda.time._
import org.joda.time.format._
import slick.jdbc.MySQLProfile.api._
import scala.math.pow
import scala.util.Properties.envOrElse

object RealityballConfig {

  val GamedayURL = "http://mlb.com/"
  val MlbURL = "http://mlb.mlb.com/"
  val FantasyAlarmURL = "http://www.fantasyalarm.com/"
  val DataRoot = "/Users/mauricio/Google Drive/Projects/fantasySports/data/"

  val WUNDERGROUND_APIURL = "http://api.wunderground.com/api/"
  val WUNDERGROUND_APIKEY = envOrElse("WUNDERGROUND_API_KEY", "")

  val CurrentYear = {
    (new DateTime).getYear
  }

  val db = {
    val mysqlURL = envOrElse("MLB_MYSQL_URL", "jdbc:mysql://localhost:3306/mlbretrosheet")
    val mysqlUser = envOrElse("MLB_MYSQL_USER", "root")
    val mysqlPassword = envOrElse("MLB_MYSQL_PASSWORD", "")
    Database.forURL(mysqlURL, user = mysqlUser, password = mysqlPassword)
  }

  val CcyymmddFormatter = DateTimeFormat.forPattern("yyyyMMdd")
  val CcyymmddTimeFormatter = DateTimeFormat.forPattern("yyyyMMdd H:mm")
  val MMDDFormatter = DateTimeFormat.forPattern("MM/dd")
  val CcyymmddDelimFormatter = DateTimeFormat.forPattern("yyyy_MM_dd")
  val CcyymmddSlashDelimFormatter = DateTimeFormat.forPattern("yyyy/MM/dd")
  val ISOdateFormatter = DateTimeFormat.forPattern("yyyy-MM-dd")
  val YearFormatter = DateTimeFormat.forPattern("yyyy")

  val MovingAverageWindow = 25
  val TeamMovingAverageWindow = 10
  val VolatilityWindow = 100

  val MovingAverageExponentialWeights: List[Double] = {
    val alpha = 0.9

    val rawWeights = (0 to MovingAverageWindow - 1).map({ x => pow(alpha, x.toDouble) })
    val totalWeight = rawWeights.foldLeft(0.0)(_ + _)
    rawWeights.map(_ / totalWeight).toList
  }

  val StrikeOut = "StrikeOut"
  val FlyBall = "FlyBall"
  val GroundBall = "GroundBall"
  val BaseOnBalls = "BaseOnBalls"

  // Threshold that makes a day productive
  val ProductionThreshold = 2.0

  // Mean + 1 SD
  val StrikeOutBatterStyleThreshold = (0.206 + 0.062)
  val FlyballBatterStyleThreshold = (0.381 + 0.057)
  val GroundballBatterStyleThreshold = (0.328 + 0.070)
  val BaseOnBallsBatterStyleThreshold = (0.085 + 0.031)

  // Mean + 0.5 SD
  val StrikeOutPitcherStyleThreshold = (0.301 + 0.076 / 2.0)
  val FlyballPitcherStyleThreshold = (0.339 + 0.073 / 2.0)
  val GroundballPitcherStyleThreshold = (0.353 + 0.088 / 2.0)

  // Matchups (Pitcher/Batter)
  val MatchupBase = 0.5142
  val MatchupNeutralNeutral = 0.5024
  val MatchupStrikeOutNeutral = 0.4439
  val MatchupFlyBallNeutral = 0.5678
  val MatchupGroundballNeutral = 0.5519
  val MatchupNeutralStrikeOut = 0.4587
  val MatchupStrikeOutStrikeOut = 0.3812
  val MatchupFlyballStrikeOut = 0.5257
  val MatchupGroundballStrikeOut = 0.4928
  val MatchupNeutralFlyball = 0.5325
  val MatchupStrikeOutFlyball = 0.4595
  val MatchupFlyballFlyball = 0.6132
  val MatchupGroundballFlyball = 0.5697
  val MatchupNeutralGroundball = 0.5065
  val MatchupStrikeOutGroundball = 0.4071
  val MatchupFlyballGroundball = 0.5524
  val MatchupGroundballGroundball = 0.5144

  // Valuation Model
  /*
  val Intercept = -0.9234
  val BetaFanDuelBase = 0.4986
  val BetaOddsAdj = 0.9994
  val BetaMatchupAdj = 1.3730
  *
  */
  val Intercept = -1.2611
  val BetaFanDuelBase = 0.4866
  val BetaPitcherAdj = 0.3135
  val BetaParkAdj = 0.0257
  val BetaBaTrendAdj = 0.0520
  val BetaOddsAdj = 0.99475
  val BetaMatchupAdj = 1.3640

}
