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

import java.sql.Timestamp

import org.bustos.realityball.common.RealityballConfig._
import org.joda.time._
import org.joda.time.format._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.Query
import spray.json._

import scala.concurrent.Await
import scala.concurrent.duration.Duration._

class RealityballData extends RealityballJsonProtocol {

  import GoogleTableJsonProtocol._
  import RealityballRecords._

  def teams(year: String): List[Team] = {
    if (year == "All" || year.toInt > 2014) {
      Await.result(db.run(teamsTable.filter(_.year === "2014").sortBy(_.mnemonic).result), Inf).toList
    } else {
      Await.result(db.run(teamsTable.filter(_.year === year).sortBy(_.mnemonic).result), Inf).toList
    }
  }

  def teamFromName(name: String): Team = {
    Await.result(db.run(teamsTable.filter(_.mlbComName === name).sortBy(_.year.desc).result), Inf).head
  }

  def mlbComIdFromRetrosheet(team: String): String = {
      Await.result(db.run(teamsTable.filter(_.mnemonic === team).map(_.mlbComId).result), Inf).head
  }

  def games(date: DateTime): List[Game] = {

    val todaysGames = {
      Await.result(db.run(gamesTable.filter({ x => x.date === date }).result), Inf).toList
    }
    if (todaysGames.isEmpty) {
      Await.result(db.run(gamedayScheduleTable.filter({ x => x.date === date}).result), Inf).toList.map ({ x => Game(x.id, x.homeTeam, x.visitingTeam, x.site, date, x.number, x.startingHomePitcher, x.startingVisitingPitcher) })
    }
    else todaysGames
  }

  def startingBatters(game: Game, side: Int, year: String): List[Player] = {
      val query = (for {
        hitters <- hitterStats if (hitters.gameId === game.id && hitters.lineupPosition > 0)
        players <- playersTable if (players.id === hitters.id)
      } yield (players, hitters)).filter({ x => x._1.year === year && x._2.side === side && x._2.pitcherIndex === 1 }).sortBy({ x => x._2.lineupPosition})
      Await.result(db.run(query.result), Inf).map({ x => x._1 }).toList
  }

  def latestLineupRegime(game: Game, player: Player): Int = {
      val rows = Await.result(db.run(hitterStats.filter({ x => x.id === player.id && x.date < game.date }).sortBy({ _.date.desc }).map({ _.lineupPositionRegime }).take(1).result), Inf).toList
      if (rows.isEmpty) 0
      else rows.head
  }

  def recentFantasyData(game: Game, player: Player, lookback: Int): List[HitterFantasyDaily] = {
      val rows = Await.result(db.run(hitterFantasyTable.filter({ x => x.id === player.id && x.date <= game.date})
        .groupBy(_.date)
        .map({
          case (date, group) => (date, group.map(_.productionRate).sum, group.map(_.daysSinceProduction).sum,
            group.map(_.RHfanDuel).sum, group.map(_.LHfanDuel).sum, group.map(_.fanDuel).sum,
            group.map(_.RHdraftKings).sum, group.map(_.LHdraftKings).sum, group.map(_.draftKings).sum,
            group.map(_.RHdraftster).sum, group.map(_.LHdraftster).sum, group.map(_.draftster).sum)
        })
        .sortBy({ _._1.desc }).take(lookback).result), Inf).toList
      if (rows.isEmpty) List.empty[HitterFantasyDaily]
      else rows.map({ x => HitterFantasyDaily(x._1, player.id, game.id, 0, "", 0, x._2, x._3.get, x._4, x._5, x._6, x._7, x._8, x._9, x._10, x._11, x._12) })
  }

  def latestFantasyMovData(game: Game, player: Player): HitterFantasy = {
      val rows = Await.result(db.run(hitterFantasyMovingTable.filter({ x => x.id === player.id && x.date < game.date }).sortBy({ _.date.desc }).take(1).result), Inf).toList
      if (rows.isEmpty) HitterFantasy(new DateTime, "", "", 0, None, None, None, None, None, None, None, None, None)
      else rows.head
  }

  def latestFantasyVolData(game: Game, player: Player): HitterFantasy = {
      val rows = Await.result(db.run(hitterFantasyVolatilityTable.filter({ x => x.id === player.id && x.date < game.date }).sortBy({ _.date.desc }).take(1).result), Inf).toList
      if (rows.isEmpty) HitterFantasy(new DateTime, "", "", 0, None, None, None, None, None, None, None, None, None)
      else rows.head
  }

  def latestBAdata(game: Game, player: Player): HitterStatsMoving = {
      val rows = Await.result(db.run(hitterMovingStats.filter({ x => x.id === player.id && x.date < game.date }).sortBy({ _.date.desc }).take(1).result), Inf).toList
      if (rows.isEmpty) HitterStatsMoving(new DateTime, "", "", 0, None, None, None, None, None, None, None, None, None, "", "", "")
      else rows.head
  }

  def latestBAtrends(game: Game, player: Player, pitcher: Player): Double = {

    def slope(moving: List[HitterStatsMoving]): Double = {
      if (moving.length < 10) 0.0
      else {
        val observations = moving.foldLeft(List.empty[(Double, Double)])({ (x, y) =>
          {
            val independent = {
              if (pitcher.throwsWith == "R") {
                (y.RHbattingAverageMov.getOrElse(0.0) + y.RHonBasePercentageMov.getOrElse(0.0) + y.RHsluggingPercentageMov.getOrElse(0.0)) / 3.0
              } else {
                (y.LHbattingAverageMov.getOrElse(0.0) + y.LHonBasePercentageMov.getOrElse(0.0) + y.LHsluggingPercentageMov.getOrElse(0.0)) / 3.0
              }
            }
            (x.length.toDouble, independent) :: x
          }
        })
        val regress: LinearRegression = new LinearRegression(observations)
        if (regress.R2 > 0.75) {
        regress.betas._2
        } else 0.0
      }
    }
    val stats = Await.result(db.run(hitterMovingStats.filter({ x => x.id === player.id && x.date < game.date }).sortBy({ _.date.desc }).take(MovingAverageWindow).result), Inf).toList
    slope(stats)
  }

  def playerFromRetrosheetId(retrosheetId: String, year: String): Player = {
    val playerList = {
      if (year == "") {
        val playerList = Await.result(db.run(playersTable.filter({ x => x.id === retrosheetId }).result), Inf).toList
        if (playerList.isEmpty) throw new IllegalStateException("No one found with Retrosheet ID: " + retrosheetId)
        else playerList
      } else {
        val playerList = Await.result(db.run(playersTable.filter({ x => x.id === retrosheetId && x.year === year }).result), Inf).toList
        if (playerList.isEmpty) throw new IllegalStateException("No one found with Retrosheet ID: " + retrosheetId + " in year " + year)
        else if (playerList.length > 1) throw new IllegalStateException("Non Unique Retrosheet ID: " + retrosheetId + " in year " + year)
        else playerList
      }
    }
    playerList.head
  }

  def playerFromMlbId(mlbId: String, year: String): Player = {

    def useMlbId: Player = {
      try {
        playerFromRetrosheetId(mlbId, "")
      } catch {
        case e: IllegalStateException => {
          if (mlbId == "502162") {
            println("")
          }
          val mlbPlayer = new MlbPlayer(mlbId)
          Await.result(db.run(playersTable += mlbPlayer.player), Inf)
          mlbPlayer.player
        }
      }
    }

    val mappingList = Await.result(db.run(idMappingTable.filter({ x => x.mlbId === mlbId }).result), Inf).toList
    if (mappingList.isEmpty) {
      if (mlbId == "502304") return Await.result(db.run(playersTable.filter({ x => x.id === "carpd001" }).result), Inf).head // Crunchtime Baseball is missing
      else {
        return useMlbId
      }
    }
    else if (mappingList.length > 1) throw new IllegalStateException("Non Unique MLB ID: " + mlbId)
    try {
      playerFromRetrosheetId(mappingList.head.retroId, year)
    } catch {
      case e: IllegalStateException => useMlbId
    }
  }

  def playerFromName(firstName: String, lastName: String, year: String, team: String): Player = {
    val playerList = Await.result(db.run(playersTable.filter({ x => x.firstName.like(firstName + "%") && x.lastName === lastName && x.year === year }).result), Inf).toList
    if (playerList.isEmpty) {
      val mapping = mappingForName(firstName, lastName)
      if (mapping.retroId != "") playerFromRetrosheetId(mapping.retroId, "")
      else if (mapping.mlbId != "") playerFromMlbId(mapping.mlbId, year)
      else throw new IllegalStateException("No one found by the name of: " + firstName + " " + lastName)
    }
    else if (playerList.length > 1) {
      if (team != "") {
        val teamPlayerList = Await.result(db.run(playersTable.filter({ x => x.firstName.like(firstName + "%") && x.lastName === lastName && x.year === year && x.team === team }).result), Inf).toList
        if (teamPlayerList.isEmpty) throw new IllegalStateException("No one found by the name of: " + firstName + " " + lastName)
        else if (teamPlayerList.length > 1) throw new IllegalStateException("Non Unique Name: " + firstName + " " + lastName)
        else teamPlayerList.head
      } else throw new IllegalStateException("Non Unique Name: " + firstName + " " + lastName)
    } else playerList.head
  }

  def players(team: String, year: String): List[Player] = {
    if (year == "All") {
      val groups = Await.result(db.run(playersTable.filter(_.team === team).result), Inf).toList.groupBy(_.id)
      val players = groups.mapValues(_.head).values
      val partitions = players.partition(_.position != "P")
      partitions._1.toList.sortBy(_.lastName) ++ partitions._2.toList.sortBy(_.lastName)
    } else {
      val partitions = Await.result(db.run(playersTable.filter({ x => x.team === team && x.year === year }).result), Inf).toList.partition(_.position != "P")
      partitions._1.sortBy(_.lastName) ++ partitions._2.sortBy(_.lastName)
    }
  }

  def mappingForName(firstName: String, lastName: String): IdMapping = {
    val nameString = firstName + "%" + lastName
    val mappingList = Await.result(db.run(idMappingTable.filter({ x => x.mlbName like nameString }).result), Inf).toList
    val retroMappingList = Await.result(db.run(idMappingTable.filter({ x => x.retroName like nameString }).result), Inf).toList
    if (mappingList.length == 1) mappingList.head
    else if (retroMappingList.length == 1) retroMappingList.head
    else IdMapping("", "", "", "", "", "", "", "", "", "", "", "")
  }

  def mappingForRetroId(id: String): IdMapping = {
    val mappingList = Await.result(db.run(idMappingTable.filter({ x => x.retroId === id }).result), Inf).toList
    if (mappingList.length == 1) mappingList.head
    else IdMapping("", "", "", "", "", "", "", "", "", "", "", "")
  }

  def batterSummary(id: String, year: String): PlayerData = {
    val playerMnemonic = truePlayerID(id)
    val mapping = mappingForRetroId(playerMnemonic)
    if (year == "All") {
      val player = Await.result(db.run(playersTable.filter(_.id === playerMnemonic).result), Inf).head
      val lineupRegime = Await.result(db.run(hitterStats.filter(_.id === playerMnemonic).map(_.lineupPositionRegime).avg.result), Inf).head
      val RHatBats = Await.result(db.run(hitterRawRH.filter(_.id === playerMnemonic).map(_.RHatBat).sum.result), Inf).head
      val LHatBats = Await.result(db.run(hitterRawLH.filter(_.id === playerMnemonic).map(_.LHatBat).sum.result), Inf).head
      val games = Await.result(db.run(hitterStats.filter({ x => x.id === playerMnemonic && x.pitcherIndex === 1 }).result), Inf).toList.length
      val summary = PlayerSummary(playerMnemonic, lineupRegime, RHatBats, LHatBats, games, mapping.mlbId, mapping.brefId, mapping.espnId)
      PlayerData(player, summary)
    } else {
      val player = Await.result(db.run(playersTable.filter({ x => x.id === playerMnemonic && x.year.startsWith(year) }).result), Inf).head
      val lineupRegime = Await.result(db.run(hitterStats.filter({ x => x.id === playerMnemonic && yearFromDate(x.date) === year.toInt }).map(_.lineupPositionRegime).avg.result), Inf).head
      val RHatBats = Await.result(db.run(hitterRawRH.filter({ x => x.id === playerMnemonic && yearFromDate(x.date) === year.toInt }).map(_.RHatBat).sum.result), Inf).head
      val LHatBats = Await.result(db.run(hitterRawLH.filter({ x => x.id === playerMnemonic && yearFromDate(x.date) === year.toInt }).map(_.LHatBat).sum.result), Inf).head
      val games = Await.result(db.run(hitterStats.filter({ x => x.id === playerMnemonic && yearFromDate(x.date) === year.toInt && x.pitcherIndex === 1 }).result), Inf).toList.length
      val summary = PlayerSummary(playerMnemonic, lineupRegime, RHatBats, LHatBats, games, mapping.mlbId, mapping.brefId, mapping.espnId)
      PlayerData(player, summary)
    }
  }

  def pitcherSummary(id: String, year: String): PitcherData = {
    val playerMnemonic = truePlayerID(id)
    val mapping = mappingForRetroId(playerMnemonic)
    if (year == "All") {
      val player = Await.result(db.run(playersTable.filter(_.id === playerMnemonic).result), Inf).head
      val daysSinceLastApp = Await.result(db.run(pitcherStats.filter(_.id === playerMnemonic).map(_.daysSinceLastApp).avg.result), Inf).head
      val win = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && x.win === 1).result), Inf).length
      val loss = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && x.loss === 1).result), Inf).length
      val save = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && x.save === 1).result), Inf).length
      val games = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic).result), Inf).length
      val summary = PitcherSummary(playerMnemonic, daysSinceLastApp, win, loss, save, games, mapping.mlbId, mapping.brefId, mapping.espnId)
      PitcherData(player, summary)
    } else {
      val player = Await.result(db.run(playersTable.filter(x => x.id === playerMnemonic && x.year.startsWith(year)).result), Inf).head
      val daysSinceLastApp = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && yearFromDate(x.date) === year.toInt).map(_.daysSinceLastApp).avg.result), Inf).head
      val win = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && x.win === 1 && yearFromDate(x.date) === year.toInt).result), Inf).length
      val loss = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && x.loss === 1 && yearFromDate(x.date) === year.toInt).result), Inf).length
      val save = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && x.save === 1 && yearFromDate(x.date) === year.toInt).result), Inf).length
      val games = Await.result(db.run(pitcherStats.filter(x => x.id === playerMnemonic && yearFromDate(x.date) === year.toInt).result), Inf).length
      val summary = PitcherSummary(playerMnemonic, daysSinceLastApp, win, loss, save, games, mapping.mlbId, mapping.brefId, mapping.espnId)
      PitcherData(player, summary)
    }
  }

  def truePlayerID(id: String): String = {
    if (!id.contains("[")) {
      id
    } else {
      id.split("[")(1).replaceAll("]", "")
    }
  }

  def dataTable(data: List[BattingAverageObservation]): String = {
    val columns = List(new GoogleColumn("Date", "Date", "string"), new GoogleColumn("Total", "Total", "number"), new GoogleColumn("Lefties", "Against Lefties", "number"), new GoogleColumn("Righties", "Against Righties", "number"))
    val rows = data.map(ba => GoogleRow(List(new GoogleCell(ba.date), new GoogleCell(ba.bAvg), new GoogleCell(ba.lhBAvg), new GoogleCell(ba.rhBAvg))))
    GoogleTable(columns, rows).toJson.prettyPrint
  }

  def dataNumericTable(data: List[(DateTime, AnyVal)], title: String): String = {
    val columns = List(new GoogleColumn("Date", "Date", "string"), new GoogleColumn(title, title, "number"))
    val rows = data.map(obs => GoogleRow(List(new GoogleCell(obs._1), new GoogleCell(obs._2))))
    GoogleTable(columns, rows).toJson.prettyPrint
  }

  def dataNumericTable2(data: List[(DateTime, AnyVal, AnyVal)], titles: List[String], tooltips: List[String]): String = {
    val columns = if (tooltips == Nil) List(new GoogleColumn("Date", "Date", "string"), new GoogleColumn(titles(0), titles(0), "number"), new GoogleColumn(titles(1), titles(1), "number"))
    else List(new GoogleColumn("Date", "Date", "string"), new GoogleColumn(titles(0), titles(0), "number"), new GoogleColumn(titles(1), titles(1), "number"), new GoogleTooltipColumn)
    val rows = if (tooltips == Nil) data.map(obs => GoogleRow(List(new GoogleCell(obs._1), new GoogleCell(obs._2), new GoogleCell(obs._3))))
    else data.zip(tooltips).map(obs => GoogleRow(List(new GoogleCell(obs._1._1), new GoogleCell(obs._1._2), new GoogleCell(obs._1._3), new GoogleCell(obs._2))))
    GoogleTable(columns, rows).toJson.prettyPrint
  }

  def dataNumericTable3(data: List[(DateTime, AnyVal, AnyVal, AnyVal)], titles: List[String], tooltips: List[String]): String = {
    val columns = if (tooltips == Nil) List(new GoogleColumn("Date", "Date", "string"), new GoogleColumn(titles(0), titles(0), "number"), new GoogleColumn(titles(1), titles(1), "number"), new GoogleColumn(titles(2), titles(2), "number"))
    else List(new GoogleColumn("Date", "Date", "string"), new GoogleColumn(titles(0), titles(0), "number"), new GoogleColumn(titles(1), titles(1), "number"), new GoogleColumn(titles(2), titles(2), "number"), new GoogleTooltipColumn)
    val rows = data.map(obs => GoogleRow(List(new GoogleCell(obs._1), new GoogleCell(obs._2), new GoogleCell(obs._3), new GoogleCell(obs._4))))
    GoogleTable(columns, rows).toJson.prettyPrint
  }

  def dataNumericPieChart(data: List[(String, AnyVal)], title: String, units: String): String = {
    val columns = List(new GoogleColumn(title, title, "string"), new GoogleColumn(units, units, "string"))
    val rows = data.map(obs => GoogleRow(List(new GoogleCell(obs._1), new GoogleCell(obs._2))))
    GoogleTable(columns, rows).toJson.prettyPrint
  }

  def displayDouble(x: Option[Double]): Double = {
    x match {
      case None => Double.NaN
      case _    => x.get
    }
  }

  def years: List[String] = {
    "All" :: Await.result(db.run(sql"""select distinct(year(date)) as year from games order by year""".as[String]), Inf).toList
  }

  def hitterStatsQuery(id: String, year: String): Query[HitterDailyStatsTable, HitterDailyStatsTable#TableElementType, Seq] = {
    if (year == "All") hitterStats.filter(_.id === truePlayerID(id)).sortBy({ x => (x.date, x.pitcherIndex) })
    else hitterStats.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy({ x => (x.date, x.pitcherIndex) })
  }

  def hitterMovingStatsQuery(id: String, year: String): Query[HitterStatsMovingTable, HitterStatsMovingTable#TableElementType, Seq] = {
    if (year == "All") hitterMovingStats.filter(_.id === truePlayerID(id)).sortBy({ x => (x.date, x.pitcherIndex) })
    else hitterMovingStats.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy({ x => (x.date, x.pitcherIndex) })
  }

  def hitterFantasyQuery(id: String, year: String): Query[HitterFantasyTable, HitterFantasyTable#TableElementType, Seq] = {
    if (year == "All") hitterFantasyTable.filter(_.id === truePlayerID(id)).sortBy({ x => (x.date, x.pitcherIndex) })
    else hitterFantasyTable.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy({ x => (x.date, x.pitcherIndex) })
  }

  def hitterFantasyMovingQuery(id: String, year: String): Query[HitterFantasyMovingTable, HitterFantasyMovingTable#TableElementType, Seq] = {
    if (year == "All") hitterFantasyMovingTable.filter(_.id === truePlayerID(id)).sortBy({ x => (x.date, x.pitcherIndex) })
    else hitterFantasyMovingTable.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy({ x => (x.date, x.pitcherIndex) })
  }

  def hitterVolatilityStatsQuery(id: String, year: String): Query[HitterStatsVolatilityTable, HitterStatsVolatilityTable#TableElementType, Seq] = {
    if (year == "All") hitterVolatilityStats.filter(_.id === truePlayerID(id)).sortBy({ x => (x.date, x.pitcherIndex) })
    else hitterVolatilityStats.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy({ x => (x.date, x.pitcherIndex) })
  }

  def pitcherFantasyQuery(id: String, year: String): Query[PitcherFantasyTable, PitcherFantasyTable#TableElementType, Seq] = {
    if (year == "All") pitcherFantasyStats.filter(_.id === truePlayerID(id)).sortBy(_.date)
    else pitcherFantasyStats.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy(_.date)
  }

  def pitcherFantasyMovingQuery(id: String, year: String): Query[PitcherFantasyMovingTable, PitcherFantasyMovingTable#TableElementType, Seq] = {
    if (year == "All") pitcherFantasyMovingStats.filter(_.id === truePlayerID(id)).sortBy(_.date)
    else pitcherFantasyMovingStats.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy(_.date)
  }

  def pitcherDailyQuery(id: String, year: String): Query[PitcherDailyTable, PitcherDailyTable#TableElementType, Seq] = {
    if (year == "All") pitcherStats.filter(_.id === truePlayerID(id)).sortBy(_.date)
    else pitcherStats.filter({ x => x.id === truePlayerID(id) && yearFromDate(x.date) === year.toInt }).sortBy(_.date)
  }

  def BA(id: String, year: String): List[BattingAverageObservation] = {
    val playerStats = Await.result(db.run(hitterStatsQuery(id, year).map(p => (p.date, p.battingAverage, p.LHbattingAverage, p.RHbattingAverage)).result), Inf).toList
    playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def movingBA(id: String, year: String): List[BattingAverageObservation] = {
    val playerStats = Await.result(db.run(hitterMovingStatsQuery(id, year).map(p => (p.date, p.battingAverageMov, p.LHbattingAverageMov, p.RHbattingAverageMov)).result), Inf).toList
    playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def volatilityBA(id: String, year: String): List[BattingAverageObservation] = {
    val playerStats = Await.result(db.run(hitterVolatilityStatsQuery(id, year).map(p => (p.date, p.battingVolatility, p.LHbattingVolatility, p.RHbattingVolatility)).result), Inf).toList
    playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def dailyBA(id: String, year: String): List[BattingAverageObservation] = {
    val playerStats = Await.result(db.run(hitterStatsQuery(id, year).map(p => (p.date, p.dailyBattingAverage, p.LHdailyBattingAverage, p.RHdailyBattingAverage)).result), Inf).toList
    playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def fantasy(id: String, year: String, gameName: String): List[BattingAverageObservation] = {
    val playerStats = {
      gameName match {
        case "FanDuel"    => Await.result(db.run(hitterFantasyQuery(id, year).map(p => (p.date, p.fanDuel, p.LHfanDuel, p.RHfanDuel)).result), Inf).toList
        case "DraftKings" => Await.result(db.run(hitterFantasyQuery(id, year).map(p => (p.date, p.draftKings, p.LHdraftKings, p.RHdraftKings)).result), Inf).toList
        case "Draftster"  => Await.result(db.run(hitterFantasyQuery(id, year).map(p => (p.date, p.draftster, p.LHdraftster, p.RHdraftster)).result), Inf).toList
      }
    }
    playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def fantasyMoving(id: String, year: String, gameName: String): List[BattingAverageObservation] = {
    val playerStats = {
      gameName match {
        case "FanDuel"    => Await.result(db.run(hitterFantasyMovingQuery(id, year).map(p => (p.date, p.fanDuel, p.LHfanDuel, p.RHfanDuel)).result), Inf).toList
        case "DraftKings" => Await.result(db.run(hitterFantasyMovingQuery(id, year).map(p => (p.date, p.draftKings, p.LHdraftKings, p.RHdraftKings)).result), Inf).toList
        case "Draftster"  => Await.result(db.run(hitterFantasyMovingQuery(id, year).map(p => (p.date, p.draftster, p.LHdraftster, p.RHdraftster)).result), Inf).toList
      }
    }
    playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def slugging(id: String, year: String): List[BattingAverageObservation] = {
    val playerStats = Await.result(db.run(hitterStatsQuery(id, year).map(p => (p.date, p.sluggingPercentage, p.LHsluggingPercentage, p.RHsluggingPercentage)).result), Inf).toList
    playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def onBase(id: String, year: String): List[BattingAverageObservation] = {
      val playerStats = Await.result(db.run(hitterStatsQuery(id, year).map(p => (p.date, p.onBasePercentage, p.LHonBasePercentage, p.RHonBasePercentage)).result), Inf).toList
      playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def sluggingMoving(id: String, year: String): List[BattingAverageObservation] = {
      val playerStats = Await.result(db.run(hitterMovingStatsQuery(id, year).map(p => (p.date, p.sluggingPercentageMov, p.LHsluggingPercentageMov, p.RHsluggingPercentageMov)).result), Inf).toList
      playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def onBaseMoving(id: String, year: String): List[BattingAverageObservation] = {
      val playerStats = Await.result(db.run(hitterMovingStatsQuery(id, year).map(p => (p.date, p.onBasePercentageMov, p.LHonBasePercentageMov, p.RHonBasePercentageMov)).result), Inf).toList
      playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def sluggingVolatility(id: String, year: String): List[BattingAverageObservation] = {
      val playerStats = Await.result(db.run(hitterVolatilityStatsQuery(id, year).sortBy({ x => (x.date, x.pitcherIndex) }).map(p => (p.date, p.sluggingVolatility, p.LHsluggingVolatility, p.RHsluggingVolatility)).result), Inf).toList
      playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def onBaseVolatility(id: String, year: String): List[BattingAverageObservation] = {
      val playerStats = Await.result(db.run(hitterVolatilityStatsQuery(id, year).sortBy({ x => (x.date, x.pitcherIndex) }).map(p => (p.date, p.onBaseVolatility, p.LHonBaseVolatility, p.RHonBaseVolatility)).result), Inf).toList
      playerStats.map({ x => BattingAverageObservation(x._1, displayDouble(x._2), displayDouble(x._3), displayDouble(x._4)) })
  }

  def pitcherStyle(player: Player, game: Game): String = {
      val styleResults = Await.result(db.run(pitcherStats.filter({ x => x.id === player.id && x.date === game.date }).map(_.style).result), Inf).toList
      if (!styleResults.isEmpty) styleResults.head
      else ""
  }

  def batterStyle(player: Player, game: Game, pitcher: Player): String = {
    val styleResults = Await.result(db.run(hitterMovingStats.filter({ x => x.id === player.id && x.date === game.date }).result), Inf).map({ x =>
      if (pitcher.throwsWith == "R") x.RHstyle else x.LHstyle
    }).toList
    if (!styleResults.isEmpty) styleResults.head
    else ""
  }

  def batterStyleCounts(id: String, year: String): List[(String, Double)] = {
    if (year == "All") {
      val result = Await.result(db.run(sql"""
            select
              sum(RHstrikeOut + LHstrikeOut) as souts,
              sum(RHflyBall + LHflyBall) as fly,
              sum(RHgroundBall + LHgroundBall) as ground,
              sum(RHbaseOnBalls + LHbaseOnBalls + RHhitByPitch + LHhitByPitch) as baseOnBalls
            from
              hitterRawLHstats a, hitterRawRHstats b
            where
              a.id = $id and a.id = b.id and a.gameId = b.gameId
          """.as[(Double, Double, Double, Double)]), Inf).head
      List(("Strikeouts", result._1), ("Flyball", result._2), ("Groundball", result._3), ("Base On Balls", result._4))
    } else {
      val result = Await.result(db.run(sql"""
            select
              sum(RHstrikeOut + LHstrikeOut) as souts,
              sum(RHflyBall + LHflyBall) as fly,
              sum(RHgroundBall + LHgroundBall) as ground,
              sum(RHbaseOnBalls + LHbaseOnBalls + RHhitByPitch + LHhitByPitch) as baseOnBalls
            from
              hitterRawLHstats a, hitterRawRHstats b
            where
              a.id = $id and a.id = b.id and a.gameId = b.gameId and instr(a.date, $year) > 0""".as[(Double, Double, Double, Double)]), Inf).head
      List(("Strikeouts", result._1), ("Flyball", result._2), ("Groundball", result._3), ("Base On Balls", result._4))
    }
  }

  def outs(id: String, year: String): List[(DateTime, Double, Double, Double)] = {
    Await.result(db.run(pitcherDailyQuery(id, year).map(p => (p.date, p.strikeOuts, p.flyOuts, p.groundOuts)).result), Inf).toList.map({ x => (x._1, x._2.toDouble.max(0.001), x._3.toDouble.max(0.001), x._4.toDouble.max(0.001)) })
  }

  def outsTypeCount(id: String, year: String): List[(String, Double)] = {
    if (year == "All") {
      val result = Await.result(db.run(sql"""
            select sum(strikeOuts) as souts, sum(flyOuts) as fouts, sum(groundOuts) as gouts
            from
              pitcherDaily
            where
              id = $id
          """.as[(Double, Double, Double)]), Inf).head
      List(("Strikeouts", result._1), ("Flyouts", result._2), ("Groundouts", result._3))
    } else {
      val result = Await.result(db.run(sql"""
            select sum(strikeOuts) as souts, sum(flyOuts) as fouts, sum(groundOuts) as gouts
            from
              pitcherDaily
            where
              id = $id and instr(date, $year) > 0
            """.as[(Double, Double, Double)]), Inf).head
      List(("Strikeouts", result._1), ("Flyouts", result._2), ("Groundouts", result._3))
    }
  }

  def strikeRatio(id: String, year: String): List[(DateTime, Double)] = {
    Await.result(db.run(pitcherDailyQuery(id, year).map(p => (p.date, p.pitches, p.balls)).result), Inf).toList.map({ x => (x._1, (x._2 - x._3).toDouble / x._2.toDouble) })
  }

  def pitcherFantasy(id: String, year: String, gameName: String): List[(DateTime, Double)] = {
    val playerStats = {
      gameName match {
        case "FanDuel"    => Await.result(db.run(pitcherFantasyQuery(id, year).map(p => (p.date, p.fanDuel)).result), Inf).toList
        case "DraftKings" => Await.result(db.run(pitcherFantasyQuery(id, year).map(p => (p.date, p.draftKings)).result), Inf).toList
        case "Draftster"  => Await.result(db.run(pitcherFantasyQuery(id, year).map(p => (p.date, p.draftster)).result), Inf).toList
      }
    }
    playerStats.map({ x => (x._1, displayDouble(x._2)) })
  }

  def pitcherFantasyMoving(id: String, year: String, gameName: String): List[(DateTime, Double)] = {
    val playerStats = {
      gameName match {
        case "FanDuel"    => Await.result(db.run(pitcherFantasyMovingQuery(id, year).map(p => (p.date, p.fanDuelMov)).result), Inf).toList
        case "DraftKings" => Await.result(db.run(pitcherFantasyMovingQuery(id, year).map(p => (p.date, p.draftKingsMov)).result), Inf).toList
        case "Draftster"  => Await.result(db.run(pitcherFantasyMovingQuery(id, year).map(p => (p.date, p.draftsterMov)).result), Inf).toList
      }
    }
    playerStats.map({ x => (x._1, displayDouble(x._2)) })
  }

  def teamFantasy(team: String, year: String): List[(DateTime, Double, Double)] = {
    import scala.collection.mutable.Queue

    def withMovingAverage(list: List[(Timestamp, Double)]): List[(DateTime, Double, Double)] = {
      var running = Queue.empty[Double]
      list.map({ x =>
          running.enqueue(x._2)
          if (running.size > TeamMovingAverageWindow) running.dequeue
          (new DateTime(x._1), x._2, running.foldLeft(0.0)(_ + _) / running.size)
      })
    }

    if (year == "All") {
      var results = Await.result(db.run(sql"""
                select date, sum(fanDuel) from
                (select * from hitterFantasyStats where side = 0 and gameId in (select id from games where visitingTeam = $team)
                union
                select * from hitterFantasyStats where side = 1 and gameId in (select id from games where homeTeam = $team)) history
                group by date order by date
        """.as[(Timestamp, Double)]), Inf).toList
      withMovingAverage(results)
    } else {
      var dateYear = YearFormatter.parseDateTime(year).getYear
      var results = Await.result(db.run(sql"""
                select date, sum(fanDuel) from
                (select * from hitterFantasyStats where side = 0 and gameId in (select id from games where visitingTeam = $team and year(date) = $dateYear)
                union
                select * from hitterFantasyStats where side = 1 and gameId in (select id from games where homeTeam = $team and year(date) = $dateYear)) history
                group by date order by date
        """.as[(Timestamp, Double)]), Inf).toList
      withMovingAverage(results)
    }
  }

  def ballparkFantasy(team: String, date: String): Double = {

    Await.result(db.run(sql"""
              select sum(fanDuel) / sum(pa) from
               (select fanDuel, pa from hitterFantasyStats a, hitterDailyStats b
               where
                 a.gameId = b.gameId and
                 a.id = b.id and
                 a.pitcherIndex = b.pitcherIndex and
                 a.gameId like ? and
                 a.date < ?
               order by
                 a.date desc
               limit 600) a """.as[Double]), Inf).head
  }

  def ballparkBAbyDate(team: String, date: String): BattingAverageSummaries = {
    import scala.collection.mutable.Queue

    def safeRatio(x: Double, y: Double): Double = {
      if (y != 0.0) x / y
      else Double.NaN
      }

    def replaceWithMovingAverage(list: List[BattingAverageObservation]): List[BattingAverageObservation] = {
      var running_1 = Queue.empty[Double]
      var running_2 = Queue.empty[Double]
      var running_3 = Queue.empty[Double]

      list.map({ x =>
        {
          if (!x.bAvg.isNaN) running_1.enqueue(x.bAvg)
          if (running_1.size > MovingAverageWindow) running_1.dequeue
          if (!x.lhBAvg.isNaN) running_2.enqueue(x.lhBAvg)
          if (running_2.size > MovingAverageWindow) running_3.dequeue
          if (!x.rhBAvg.isNaN) running_3.enqueue(x.rhBAvg)
          if (running_3.size > MovingAverageWindow) running_3.dequeue
          BattingAverageObservation(x.date, running_1.foldLeft(0.0)(_ + _) / running_1.size, running_2.foldLeft(0.0)(_ + _) / running_2.size, running_3.foldLeft(0.0)(_ + _) / running_3.size)
      }
      })
    }

    val records = Await.result(db.run(ballparkDailiesTable.filter({ x => (x.id < (team + date + "0")) && (x.id like (team + "%")) }).sortBy(_.id).result), Inf).toList.take(MovingAverageWindow * 4)
    val baProcessed = records.map { x => BattingAverageObservation(x.date, safeRatio((x.LHhits + x.RHhits), (x.LHatBat + x.RHatBat)), safeRatio(x.LHhits, x.LHatBat), safeRatio(x.RHhits, x.RHatBat)) }
    val ba = replaceWithMovingAverage(baProcessed).reverse.head
    val obpProcessed = records.map { x =>
      BattingAverageObservation(x.date,
        safeRatio(x.LHhits + x.LHbaseOnBalls + x.LHhitByPitch + x.RHhits + x.RHbaseOnBalls + x.RHhitByPitch,
          x.RHatBat + x.RHbaseOnBalls + x.RHhitByPitch + x.RHsacFly + x.LHatBat + x.LHbaseOnBalls + x.LHhitByPitch + x.LHsacFly),
        safeRatio(x.RHhits + x.RHbaseOnBalls + x.RHhitByPitch, x.RHatBat + x.RHbaseOnBalls + x.RHhitByPitch + x.RHsacFly),
        safeRatio(x.LHhits + x.LHbaseOnBalls + x.LHhitByPitch, x.LHatBat + x.LHbaseOnBalls + x.LHhitByPitch + x.LHsacFly))
    }
    val obp = replaceWithMovingAverage(obpProcessed).reverse.head
    val slgProcessed = records.map { x =>
      BattingAverageObservation(x.date,
        safeRatio(x.LHhits + x.LHbaseOnBalls + x.LHhitByPitch + x.RHhits + x.RHbaseOnBalls + x.RHhitByPitch,
          x.RHatBat + x.RHbaseOnBalls + x.RHhitByPitch + x.RHsacFly + x.LHatBat + x.LHbaseOnBalls + x.LHhitByPitch + x.LHsacFly),
        safeRatio(x.RHhits + x.RHbaseOnBalls + x.RHhitByPitch, x.RHatBat + x.RHbaseOnBalls + x.RHhitByPitch + x.RHsacFly),
        safeRatio(x.LHhits + x.LHbaseOnBalls + x.LHhitByPitch, x.LHatBat + x.LHbaseOnBalls + x.LHhitByPitch + x.LHsacFly))
    }
    val slg = replaceWithMovingAverage(slgProcessed).reverse.head
    BattingAverageSummaries(ba, obp, slg)
  }

  def ballparkBA(team: String, year: String): List[BattingAverageObservation] = {
    import scala.collection.mutable.Queue

    def safeRatio(x: Double, y: Double): Double = {
      if (y != 0.0) x / y
      else Double.NaN
    }

    def replaceWithMovingAverage(list: List[BattingAverageObservation]): List[BattingAverageObservation] = {
      var running_1 = Queue.empty[Double]
      var running_2 = Queue.empty[Double]
      var running_3 = Queue.empty[Double]

      list.map({ x =>
        {
          if (!x.bAvg.isNaN) running_1.enqueue(x.bAvg)
          if (running_1.size > MovingAverageWindow) running_1.dequeue
          if (!x.lhBAvg.isNaN) running_2.enqueue(x.lhBAvg)
          if (running_2.size > MovingAverageWindow) running_3.dequeue
          if (!x.rhBAvg.isNaN) running_3.enqueue(x.rhBAvg)
          if (running_3.size > MovingAverageWindow) running_3.dequeue
          BattingAverageObservation(x.date, running_1.foldLeft(0.0)(_ + _) / running_1.size, running_2.foldLeft(0.0)(_ + _) / running_2.size, running_3.foldLeft(0.0)(_ + _) / running_3.size)
        }
      })
    }

    if (year == "All") replaceWithMovingAverage(Await.result(db.run(ballparkDailiesTable.filter(_.id like team + "%").sortBy(_.id).result), Inf).map({ x =>
      BattingAverageObservation(x.date, safeRatio((x.LHhits + x.RHhits), (x.LHatBat + x.RHatBat)), safeRatio(x.LHhits, x.LHatBat), safeRatio(x.RHhits, x.RHatBat))
    }).toList)
    else replaceWithMovingAverage(Await.result(db.run(ballparkDailiesTable.filter({ row => (row.id like (team + "%")) && (row.id like (team + year + "%")) }).result), Inf).map({ x =>
      BattingAverageObservation(x.date, safeRatio((x.LHhits + x.RHhits), (x.LHatBat + x.RHatBat)), safeRatio(x.LHhits, x.LHatBat), safeRatio(x.RHhits, x.RHatBat))
    }).toList)
  }

  def ballparkAttendance(team: String, year: String): List[(DateTime, Double)] = {
    val formatter = DateTimeFormat.forPattern("yyyy/MM/dd")
    def dateFromId(id: String): DateTime = {
      formatter.parseDateTime(id.substring(3, 7) + "/" + id.substring(7, 9) + "/" + id.substring(9, 11))
    }
    if (year == "All") Await.result(db.run(gameScoringTable.filter(_.id like team + "%").sortBy(_.id).result), Inf).toList.map({ x => (dateFromId(x.id), x.attendance.toDouble) })
    else Await.result(db.run(gameScoringTable.filter({ row => (row.id like (team + "%")) && (row.id like (team + year + "%")) }).result), Inf).toList.map({ x => (dateFromId(x.id), x.attendance.toDouble) })
  }

  def ballparkConditions(team: String, year: String): List[(DateTime, Double, Double)] = {
    val teamMeta = Await.result(db.run(teamsTable.filter({ x => x.year === "2014" && x.mnemonic === team }).result), Inf).toList
    if (teamMeta.isEmpty) List.empty[(DateTime, Double, Double)]
    else {
      val weather = new Weather(teamMeta.head.zipCode)
      val formatter = DateTimeFormat.forPattern("E h:mm a")
      weather.hourlyForecasts.map({ x =>
        {
          val date = new DateTime(x.FCTTIME.epoch.toLong * 1000)
          (date, x.temp.english.toDouble, x.pop.toDouble)
        }
      })
    }
  }

  def odds(game: Game): GameOdds = {
    Await.result(db.run(gameOddsTable.filter({ x => x.id === game.id }).result), Inf).head
  }

  def schedule(team: String, year: String): List[FullGameInfo] = {
    val lookingOut = (new DateTime).plusMonths(3)
    val schedule = {
      if (year == "All") (gamedayScheduleTable join gameOddsTable on (_.id === _.id)).filter({ x => ((x._1.homeTeam === team) || (x._1.visitingTeam === team)) && x._1.date < lookingOut }).sortBy(_._1.date)
      else (gamedayScheduleTable join gameOddsTable on (_.id === _.id)).filter({ x => ((x._1.homeTeam === team) || (x._1.visitingTeam === team)) && x._1.date < lookingOut }).sortBy(_._1.date)
    }
    Await.result(db.run(schedule.result), Inf).reverse.map { case (x, y) => FullGameInfo(x, y) }.toList
  }

  def injuries(team: String): List[InjuryReport] = {
    Await.result(db.run(injuryReportTable.map(_.reportTime).max.result), Inf).head match {
      case x: DateTime =>
        val reportTime = x
        val injuryReports = for {
          injuries <- injuryReportTable if injuries.reportTime === reportTime
          ids <- idMappingTable if injuries.mlbId === ids.mlbId
        } yield (ids.mlbName, injuries.reportTime, injuries.injuryReportDate, injuries.status, injuries.dueBack, injuries.injury)
        Await.result(db.run(injuryReports.result), Inf).map({ x => InjuryReport(x._1, x._2, x._3, x._4, x._5, x._6) }).toList
      //case None => List()
    }
  }

  def fsPerPa(player: Player, date: DateTime, pitcherHand: String): Double = {
    /*
select a.date, a.id, c.throwsWith, a.fanDuel, a.draftKings, a.draftster, b.pa, a.pitcherIndex from
	hitterFantasyStats a, hitterDailyStats b, players c
where
	a.gameId = b.gameId and
	a.id = b.id and
    c.id = b.pitcherId and
    c.year = '2014' and
    a.pitcherIndex = b.pitcherIndex and
    a.id = 'cabrm001' and a.date like '2014%'
order by date, pitcherIndex
limit 100;
*/
    val query = for {
      dailyStats <- hitterStats if (dailyStats.date <= date && dailyStats.id === player.id)
      hitterFantasy <- hitterFantasyTable if (hitterFantasy.gameId === dailyStats.gameId && hitterFantasy.id === dailyStats.id && hitterFantasy.pitcherIndex === dailyStats.pitcherIndex)
      players <- playersTable if (players.id === hitterFantasy.pitcherId && players.year === "2014" && players.throwsWith === pitcherHand)
    } yield (dailyStats.date, dailyStats.id, players.throwsWith, dailyStats.plateAppearances, hitterFantasy.fanDuel, hitterFantasy.draftKings, hitterFantasy.draftster)
    val rows = Await.result(db.run(query.sortBy({ x => x._1.desc }).take(200).result), Inf).toList
    val lookback = if (pitcherHand == "R") 40 else 20
    val sums = rows.foldLeft((0.0, 0))({ case (x, y) => if (x._2 >= lookback) x else (x._1 + y._5.getOrElse(0.0), x._2 + y._4) })
    if (sums._2 > 0) sums._1 / sums._2.toDouble else 0.0

  }

  def availablePredictionDates: List[String] = {
    Await.result(db.run(sql"""select distinct(substr(gameId, 4, 8)) as date from fantasyPrediction order by date""".as[String]), Inf).toList
  }

  def predictions(date: DateTime, position: String, platform: String): (List[(DateTime, Double, Double)], List[String]) = {
    (List(), List())
  }

}
