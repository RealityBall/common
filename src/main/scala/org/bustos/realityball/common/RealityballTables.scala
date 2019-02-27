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

import org.bustos.realityball.common.RealityballRecords._
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._
import slick.lifted.{ProvenShape, SimpleFunction, TableQuery, Tag}


object RealityballRecords {
  import scala.collection.mutable.Queue

  case class Statistic(val date: DateTime, var total: Double, var rh: Double, var lh: Double)
  case class StatisticInputs(var totalNumer: Double, var totalDenom: Double, var rhNumer: Double, var rhDenom: Double, var lhNumer: Double, var lhDenom: Double)
  case class RunningHitterData(lineupPosition: Queue[Int], ba: Queue[StatisticInputs], obp: Queue[StatisticInputs], slugging: Queue[StatisticInputs], fantasy: Map[String, Queue[Statistic]],
                               strikeOuts: Queue[Statistic], flyBalls: Queue[Statistic], groundBalls: Queue[Statistic], baseOnBalls: Queue[Statistic])
  case class Team(year: String, mnemonic: String, league: String, city: String, name: String, site: String, zipCode: String,
                  mlbComId: String, mlbComName: String, timeZone: String, coversComId: String, coversComName: String, coversComFullName: String)

  case class BattingAverageObservation(date: DateTime, bAvg: Double, lhBAvg: Double, rhBAvg: Double)
  case class BattingAverageSummaries(ba: BattingAverageObservation, obp: BattingAverageObservation, slg: BattingAverageObservation)

  case class Player(id: String, year: String, lastName: String, firstName: String, batsWith: String, throwsWith: String, team: String, position: String)
  case class PlayerSummary(id: String, lineupRegime: Int, RHatBats: Int, LHatBats: Int, games: Int, mlbId: String, brefId: String, espnId: String)
  case class PitcherSummary(id: String, daysSinceLastApp: Int, wins: Int, losses: Int, saves: Int, games: Int, mlbId: String, brefId: String, espnId: String)
  case class PlayerData(meta: Player, appearances: PlayerSummary)
  case class PitcherData(meta: Player, appearances: PitcherSummary)

  case class PitcherDaily(id: String, game: String, date: DateTime, var daysSinceLastApp: Int, opposing: String, var win: Int, var loss: Int, var save: Int,
                          var hits: Int, var walks: Int, var hitByPitch: Int, var strikeOuts: Int, var groundOuts: Int, var flyOuts: Int,
                          var earnedRuns: Int, var outs: Int, var shutout: Boolean, var noHitter: Boolean, var pitches: Int, var balls: Int, var style: String)
  case class Game(id: String, var homeTeam: String, var visitingTeam: String, var site: String, var date: DateTime, var number: Int, var startingHomePitcher: String, var startingVisitingPitcher: String, gamedayUrl: String)
  case class GameConditions(id: String, var startTime: DateTime, var daynight: String, var usedh: Boolean,
                            var temp: Int, var winddir: String, var windspeed: Int, var fieldcond: String, var precip: String, var sky: String)
  case class GamedaySchedule(var id: String, homeTeam: String, visitingTeam: String, site: String, date: DateTime, number: Int,
                             result: String, winningPitcher: String, losingPitcher: String, record: String, var startingHomePitcher: String, var startingVisitingPitcher: String,
                             temp: Int, winddir: String, windspeed: Int, precip: String, sky: String)
  case class GameScoring(id: String, var umphome: String, var ump1b: String, var ump2b: String, var ump3b: String, var howscored: String,
                         var timeofgame: Int, var attendance: Int, var wp: String, var lp: String, var save: String)
  case class GameOdds(var id: String, var visitorML: Int, var homeML: Int, var overUnder: Double, var overUnderML: Int)
  case class FullGameInfo(schedule: GamedaySchedule, odds: GameOdds)
  case class InjuryReport(mlbId: String, reportTime: DateTime, injuryReportdate: DateTime, status: String, dueBack: String, injury: String)

  case class FantasyPrediction(id: String, date: DateTime, gameId: String, position: String, startingPitcher: String, productionRate: Option[Double], daysSinceProduction: Int,
                               fanduelActual: Option[Double], draftsterActual: Option[Double],
                               fanduelBase: Option[Double], draftsterBase: Option[Double],
                               fanduelVol: Option[Double], draftsterVol: Option[Double],
                               pitcherAdj: Option[Double], parkAdj: Option[Double], baTrendAdj: Option[Double],
                               oddsAdj: Option[Double], overUnder: Option[Double], overUnderML: Option[Double], matchupAdj: Option[Double])
  case class HitterStatsMoving(date: DateTime, id: String, pitcherId: String, pitcherIndex: Int,
                               RHbattingAverageMov: Option[Double], LHbattingAverageMov: Option[Double], battingAverageMov: Option[Double],
                               RHonBasePercentageMov: Option[Double], LHonBasePercentageMov: Option[Double], onBasePercentageMov: Option[Double],
                               RHsluggingPercentageMov: Option[Double], LHsluggingPercentageMov: Option[Double], sluggingPercentageMov: Option[Double],
                               RHstyle: String, LHstyle: String, style: String)
  case class HitterFantasyDaily(date: DateTime, id: String, gameId: String, side: Int, pitcherId: String, pitcherIndex: Int, productionRate: Option[Double], daysSinceProduction: Int,
                                RHfanDuel: Option[Double], LHfanDuel: Option[Double], fanDuel: Option[Double],
                                RHdraftKings: Option[Double], LHdraftKings: Option[Double], draftKings: Option[Double],
                                RHdraftster: Option[Double], LHdraftster: Option[Double], draftster: Option[Double])
  case class HitterFantasy(date: DateTime, id: String, pitcherId: String, pitcherIndex: Int,
                           RHfanDuel: Option[Double], LHfanDuel: Option[Double], fanDuel: Option[Double],
                           RHdraftKings: Option[Double], LHdraftKings: Option[Double], draftKings: Option[Double],
                           RHdraftster: Option[Double], LHdraftster: Option[Double], draftster: Option[Double])

  case class BallparkDaily(var id: String, var date: DateTime,
                           var RHhits: Int, var RHtotalBases: Int, var RHatBat: Int, var RHbaseOnBalls: Int, var RHhitByPitch: Int, var RHsacFly: Int,
                           var LHhits: Int, var LHtotalBases: Int, var LHatBat: Int, var LHbaseOnBalls: Int, var LHhitByPitch: Int, var LHsacFly: Int)
  case class Ballpark(id: String, name: String, aka: String, city: String, state: String, start: String, end: String, league: String, notes: String)

  case class IdMapping(mlbId: String, mlbName: String, mlbTeam: String, mlbPos: String, bats: String, throws: String,
                       brefId: String, brefName: String, espnId: String, espnName: String, retroId: String, retroName: String)
  case class Lineup(mlbId: String, date: DateTime, game: String, team: String, lineupPosition: Int, position: String, acesSalary: Option[Double], draftKingsSalary: Option[Double], fanduelSalary: Option[Double])

  val ballparkDailiesTable = TableQuery[BallparkDailiesTable]
  val ballparkTable = TableQuery[BallparkTable]
  val fantasyPredictionTable = TableQuery[FantasyPredictionTable]
  val gameConditionsTable = TableQuery[GameConditionsTable]
  val gameOddsTable = TableQuery[GameOddsTable]
  val gameScoringTable = TableQuery[GameScoringTable]
  val gamedayScheduleTable = TableQuery[GamedayScheduleTable]
  val gamesTable = TableQuery[GamesTable]
  val hitterFantasyTable = TableQuery[HitterFantasyTable]
  val hitterFantasyMovingTable = TableQuery[HitterFantasyMovingTable]
  val hitterFantasyVolatilityTable = TableQuery[HitterFantasyVolatilityTable]
  val hitterMovingStats = TableQuery[HitterStatsMovingTable]
  val hitterRawLH = TableQuery[HitterRawLHStatsTable]
  val hitterRawRH = TableQuery[HitterRawRHStatsTable]
  val hitterStats = TableQuery[HitterDailyStatsTable]
  val hitterVolatilityStats = TableQuery[HitterStatsVolatilityTable]
  val idMappingTable = TableQuery[IdMappingTable]
  val injuryReportTable = TableQuery[InjuryReportTable]
  val pitcherDailyTable = TableQuery[PitcherDailyTable]
  val lineupsTable = TableQuery[LineupsTable]
  val pitcherFantasy = TableQuery[PitcherFantasyTable]
  val pitcherFantasyMoving = TableQuery[PitcherFantasyMovingTable]
  val pitcherFantasyMovingStats = TableQuery[PitcherFantasyMovingTable]
  val pitcherFantasyStats = TableQuery[PitcherFantasyTable]
  val pitcherStats = TableQuery[PitcherDailyTable]
  val playersTable = TableQuery[PlayersTable]
  val teamsTable = TableQuery[TeamsTable]

  val yearFromDate = SimpleFunction.unary[DateTime, Int]("year")

}

class TeamsTable(tag: Tag) extends Table[Team](tag, "teams") {
  def year = column[String]("year", O.Length(100))
  def mnemonic = column[String]("mnemonic", O.Length(100))
  def league = column[String]("league", O.Length(100))
  def city = column[String]("city", O.Length(100))
  def name = column[String]("name", O.Length(100))
  def site = column[String]("site", O.Length(100))
  def zipCode = column[String]("zipCode", O.Length(100))
  def mlbComId = column[String]("mlbComId", O.Length(100))
  def mlbComName = column[String]("mlbComName", O.Length(100))
  def timeZone = column[String]("timeZone", O.Length(100))
  def coversComId = column[String]("coversComId", O.Length(100))
  def coversComName = column[String]("coversComName", O.Length(100))
  def coversComFullName = column[String]("coversComFullName", O.Length(100))

  def * = (year, mnemonic, league, city, name, site, zipCode, mlbComId, mlbComName, timeZone, coversComId, coversComName, coversComFullName) <> (Team.tupled, Team.unapply)
}

class GamesTable(tag: Tag) extends Table[Game](tag, "games") with RealityballJsonProtocol {

  def id = column[String]("id", O.PrimaryKey, O.Length(100))
  def homeTeam = column[String]("homeTeam", O.Length(100))
  def visitingTeam = column[String]("visitingTeam", O.Length(100))
  def site = column[String]("site", O.Length(100))
  def date = column[DateTime]("date")
  def number = column[Int]("number")
  def startingHomePitcher = column[String]("startingHomePitcher", O.Length(100))
  def startingVisitingPitcher = column[String]("startingVisitingPitcher", O.Length(100))
  def gamedayUrl = column[String]("gamedayUrl", O.Length(255))

  def * = (id, homeTeam, visitingTeam, site, date, number, startingHomePitcher, startingVisitingPitcher, gamedayUrl) <> (Game.tupled, Game.unapply)
}

class GameConditionsTable(tag: Tag) extends Table[GameConditions](tag, "gameConditions") with RealityballJsonProtocol {

  def id = column[String]("id", O.PrimaryKey, O.Length(100))
  def startTime = column[DateTime]("startTime")
  def daynight = column[String]("daynight", O.Length(100))
  def usedh = column[Boolean]("usedh")
  def temp = column[Int]("temp")
  def winddir = column[String]("winddir", O.Length(100))
  def windspeed = column[Int]("windspeed")
  def fieldcond = column[String]("fieldcond", O.Length(100))
  def precip = column[String]("precip", O.Length(100))
  def sky = column[String]("sky", O.Length(100))

  def * = (id, startTime, daynight, usedh, temp, winddir, windspeed, fieldcond, precip, sky) <> (GameConditions.tupled, GameConditions.unapply)
}

class GamedayScheduleTable(tag: Tag) extends Table[GamedaySchedule](tag, "gamedaySchedule") with RealityballJsonProtocol {

  def id = column[String]("id", O.PrimaryKey, O.Length(100))
  def homeTeam = column[String]("homeTeam", O.Length(100))
  def visitingTeam = column[String]("visitingTeam", O.Length(100))
  def site = column[String]("site", O.Length(100))
  def date = column[DateTime]("date")
  def number = column[Int]("number")
  def result = column[String]("result", O.Length(100))
  def winningPitcher = column[String]("winningPitcher", O.Length(100))
  def losingPitcher = column[String]("losingPitcher", O.Length(100))
  def record = column[String]("record", O.Length(100))
  def startingHomePitcher = column[String]("startingHomePitcher", O.Length(100))
  def startingVisitingPitcher = column[String]("startingVisitingPitcher", O.Length(100))
  def temp = column[Int]("temp")
  def winddir = column[String]("winddir", O.Length(100))
  def windspeed = column[Int]("windspeed")
  def precip = column[String]("precip", O.Length(100))
  def sky = column[String]("sky", O.Length(100))

  def * = (id, homeTeam, visitingTeam, site, date, number, result, winningPitcher, losingPitcher, record, startingHomePitcher, startingVisitingPitcher, temp, winddir, windspeed, precip, sky) <> (GamedaySchedule.tupled, GamedaySchedule.unapply)
}

class FantasyPredictionTable(tag: Tag) extends Table[FantasyPrediction](tag, "fantasyPrediction") with RealityballJsonProtocol {

  def id = column[String]("id", O.Length(100))
  def date = column[DateTime]("date")
  def gameId = column[String]("gameId", O.Length(100))
  def position = column[String]("position", O.Length(100))
  def startingPitcher = column[String]("startingPitcher", O.Length(100))
  def productionRate = column[Option[Double]]("productionRate")
  def daysSinceProduction = column[Int] ("daysSinceProduction")
  def fanduelActual = column[Option[Double]]("fanduelActual")
  //def draftKingsActual = column[Option[Double]]("draftKingsActual")
  def draftsterActual = column[Option[Double]]("draftsterActual")
  def fanduelBase = column[Option[Double]]("fanduelBase")
  //def draftKingsBase = column[Option[Double]]("draftKingsBase")
  def draftsterBase = column[Option[Double]]("draftsterBase")
  def fanduelVol = column[Option[Double]]("fanduelVol")
  //def draftKingsVol = column[Option[Double]]("draftKingsVol")
  def draftsterVol = column[Option[Double]]("draftsterVol")
  def pitcherAdj = column[Option[Double]]("pitcherAdj")
  def parkAdj = column[Option[Double]]("parkAdj")
  def baTrendAdj = column[Option[Double]]("baTrendAdj")
  def oddsAdj = column[Option[Double]]("oddsAdj")
  def overUnder = column[Option[Double]]("overUnder")
  def overUnderML = column[Option[Double]]("overUnderML")
  def matchupAdj = column[Option[Double]]("matchupAdj")

  def pk = index("pk_id_game", (id, gameId))

  def * = (id, date, gameId, position, startingPitcher,
    productionRate, daysSinceProduction,
    fanduelActual, draftsterActual,
    fanduelBase, draftsterBase,
    fanduelVol, draftsterVol,
    pitcherAdj, parkAdj, baTrendAdj, oddsAdj, overUnder, overUnderML, matchupAdj) <> (FantasyPrediction.tupled, FantasyPrediction.unapply)
}

class GameOddsTable(tag: Tag) extends Table[GameOdds](tag, "gameOdds") {
  def id = column[String]("id", O.PrimaryKey, O.Length(100))
  def visitorML = column[Int]("visitorML")
  def homeML = column[Int]("homeML")
  def overUnder = column[Double]("overUnder")
  def overUnderML = column[Int]("overUnderML")

  def * = (id, visitorML, homeML, overUnder, overUnderML) <> (GameOdds.tupled, GameOdds.unapply)
}

class InjuryReportTable(tag: Tag) extends Table[InjuryReport](tag, "InjuryReport") with RealityballJsonProtocol {

  def mlbId = column[String]("mlbId", O.Length(100))
  def reportTime = column[DateTime]("reportTime")
  def injuryReportDate = column[DateTime]("injuryReportDate", O.SqlType("timestamp DEFAULT CURRENT_TIMESTAMP"))
  def status = column[String]("status", O.Length(100))
  def dueBack = column[String]("dueBack", O.Length(100))
  def injury = column[String]("injury", O.Length(100))

  def * = (mlbId, reportTime, injuryReportDate, status, dueBack, injury) <> (InjuryReport.tupled, InjuryReport.unapply)
}

class BallparkDailiesTable(tag: Tag) extends Table[BallparkDaily](tag, "ballparkDailies") with RealityballJsonProtocol {

  def id = column[String]("id", O.Length(100))
  def date = column[DateTime]("date")
  def RHhits = column[Int]("RHhits")
  def RHtotalBases = column[Int]("RHtotalBases")
  def RHatBat = column[Int]("RHatBat")
  def RHbaseOnBalls = column[Int]("RHbaseOnBalls")
  def RHhitByPitch = column[Int]("RHhitByPitch")
  def RHsacFly = column[Int]("RHsacFly")
  def LHhits = column[Int]("LHhits")
  def LHtotalBases = column[Int]("LHtotalBases")
  def LHatBat = column[Int]("LHatBat")
  def LHbaseOnBalls = column[Int]("LHbaseOnBalls")
  def LHhitByPitch = column[Int]("LHhitByPitch")
  def LHsacFly = column[Int]("LHsacFly")

  def pk = index("pk_id_date", (id, date))

  def * = (id, date, RHhits, RHtotalBases, RHatBat, RHbaseOnBalls, RHhitByPitch, RHsacFly, LHhits, LHtotalBases, LHatBat, LHbaseOnBalls, LHhitByPitch, LHsacFly) <> (BallparkDaily.tupled, BallparkDaily.unapply)
}

class BallparkTable(tag: Tag) extends Table[Ballpark](tag, "ballpark") {
  def id = column[String]("id", O.PrimaryKey, O.Length(100))
  def name = column[String]("name", O.Length(100))
  def aka = column[String]("aka", O.Length(100))
  def city = column[String]("city", O.Length(100))
  def state = column[String]("state", O.Length(100))
  def start = column[String]("start", O.Length(100))
  def end = column[String]("end", O.Length(100))
  def league = column[String]("league", O.Length(100))
  def notes = column[String]("notes", O.Length(100))

  def * = (id, name, aka, city, state, start, end, league, notes) <> (Ballpark.tupled, Ballpark.unapply)
}

class GameScoringTable(tag: Tag) extends Table[GameScoring](tag, "gameScoring") {
  def id = column[String]("id", O.PrimaryKey, O.Length(100))
  def umphome = column[String]("umphome", O.Length(100))
  def ump1b = column[String]("ump1b", O.Length(100))
  def ump2b = column[String]("ump2b", O.Length(100))
  def ump3b = column[String]("ump3b", O.Length(100))
  def howscored = column[String]("howscored", O.Length(100))
  def timeofgame = column[Int]("timeofgame")
  def attendance = column[Int]("attendarnce")
  def wp = column[String]("wp", O.Length(100))
  def lp = column[String]("lp", O.Length(100))
  def save = column[String]("save", O.Length(100))

  def * = (id, umphome, ump1b, ump2b, ump3b, howscored, timeofgame, attendance, wp, lp, save) <> (GameScoring.tupled, GameScoring.unapply)
}

class PitcherDailyTable(tag: Tag) extends Table[PitcherDaily](tag, "pitcherDaily") with RealityballJsonProtocol {

  def id = column[String]("id", O.Length(100))
  def game = column[String]("game", O.Length(100))
  def date = column[DateTime]("date")
  def daysSinceLastApp = column[Int]("daysSinceLastApp")
  def opposing = column[String]("opposing", O.Length(100))

  def win = column[Int]("win")
  def loss = column[Int]("loss")
  def save = column[Int]("save")
  def hits = column[Int]("hits")
  def walks = column[Int]("walks")
  def hitByPitch = column[Int]("hitByPitch")
  def strikeOuts = column[Int]("strikeOuts")
  def groundOuts = column[Int]("groundOuts")
  def flyOuts = column[Int]("flyOuts")
  def earnedRuns = column[Int]("earnedRuns")
  def outs = column[Int]("outs")
  def shutout = column[Boolean]("shutout")
  def noHitter = column[Boolean]("noHitter")
  def pitches = column[Int]("pitches")
  def balls = column[Int]("balls")
  def style = column[String]("style", O.Length(100))

  def pk = index("pk_id_date", (id, game)) // Duplicate issue with Joaquin Benoit on 20100910

  def * = (id, game, date, daysSinceLastApp, opposing, win, loss, save, hits, walks, hitByPitch, strikeOuts, groundOuts, flyOuts, earnedRuns, outs, shutout, noHitter, pitches, balls, style) <> (PitcherDaily.tupled, PitcherDaily.unapply)
}

class LineupsTable(tag: Tag) extends Table[Lineup](tag, "lineups") with RealityballJsonProtocol {

  def mlbId = column[String]("mlbId", O.Length(100))
  def date = column[DateTime]("date")
  def game = column[String]("game", O.Length(100))
  def team = column[String]("team", O.Length(100))
  def lineupPosition = column[Int]("lineupPosition")
  def position = column[String]("position", O.Length(100))
  def acesSalary = column[Option[Double]]("acesSalary")
  def draftKingsSalary = column[Option[Double]]("draftKingsSalary")
  def fanduelSalary = column[Option[Double]]("fanduelSalary")

  def pk = index("pk_id_date", (mlbId, date))

  def * = (mlbId, date, game, team, lineupPosition, position, acesSalary, draftKingsSalary, fanduelSalary) <> (Lineup.tupled, Lineup.unapply)
}

class PlayersTable(tag: Tag) extends Table[Player](tag, "players") {
  def id = column[String]("id", O.Length(100)); def year = column[String]("year", O.Length(100));
  def lastName = column[String]("lastName", O.Length(100))
  def firstName = column[String]("firstName", O.Length(100))
  def batsWith = column[String]("batsWith", O.Length(100))
  def throwsWith = column[String]("throwsWith", O.Length(100))
  def team = column[String]("team", O.Length(100))
  def position = column[String]("position", O.Length(100))

  def pk = primaryKey("pk_id_date", (id, year))

  def * = (id, year, lastName, firstName, batsWith, throwsWith, team, position) <> (Player.tupled, Player.unapply)
}

class IdMappingTable(tag: Tag) extends Table[IdMapping](tag, "idMapping") {
  def mlbId = column[String]("mlbId", O.Length(100)); def mlbName = column[String]("mlbName", O.Length(100)); def mlbTeam = column[String]("mlbTeam", O.Length(100))
  def mlbPos = column[String]("mlbPos", O.Length(100)); def bats = column[String]("bats", O.Length(100)); def throws = column[String]("throws", O.Length(100));
  def brefId = column[String]("brefId", O.Length(100)); def brefName = column[String]("brefName", O.Length(100));
  def espnId = column[String]("espnId", O.Length(100)); def espnName = column[String]("espnName", O.Length(100));
  def retroId = column[String]("retroId", O.Length(100)); def retroName = column[String]("retroName", O.Length(100));

  def pk = primaryKey("pk_mlb_id", (mlbId))

  def * = (mlbId, mlbName, mlbTeam, mlbPos, bats, throws, brefId, brefName, espnId, espnName, retroId, retroName) <> (IdMapping.tupled, IdMapping.unapply)
}

class HitterRawLHStatsTable(tag: Tag) extends Table[(DateTime, String, String, Int, String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)](tag, "hitterRawLHstats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def gameId = column[String]("gameId", O.Length(100)); def side = column[Int]("side");
  def pitcherId = column[String]("pitcherId", O.Length(100)); def pitcherIndex = column[Int]("pitcherIndex")
  def LHatBat = column[Int]("LHatBat")
  def LHsingle = column[Int]("LHsingle")
  def LHdouble = column[Int]("LHdouble")
  def LHtriple = column[Int]("LHtriple")
  def LHhomeRun = column[Int]("LHhomeRun")
  def LHRBI = column[Int]("LHRBI")
  def LHruns = column[Int]("LHruns")
  def LHbaseOnBalls = column[Int]("LHbaseOnBalls")
  def LHhitByPitch = column[Int]("LHhitByPitch")
  def LHsacFly = column[Int]("LHsacFly")
  def LHsacHit = column[Int]("LHsacHit")
  def LHstrikeOut = column[Int]("LHstrikeOut")
  def LHflyBall = column[Int]("LHflyBall")
  def LHgroundBall = column[Int]("LHgroundBall")

  def pk = index("pk_id_date", (id, date)) // First game of double headers are ignored for now

  def * : ProvenShape[(DateTime, String, String, Int, String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)] =
    (date, id, gameId, side, pitcherId, pitcherIndex, LHatBat, LHsingle, LHdouble, LHtriple, LHhomeRun, LHRBI, LHruns, LHbaseOnBalls, LHhitByPitch, LHsacFly, LHsacHit, LHstrikeOut, LHflyBall, LHgroundBall)
}

class HitterRawRHStatsTable(tag: Tag) extends Table[(DateTime, String, String, Int, String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)](tag, "hitterRawRHstats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def gameId = column[String]("gameId", O.Length(100)); def side = column[Int]("side");
  def pitcherId = column[String]("pitcherId", O.Length(100)); def pitcherIndex = column[Int]("pitcherIndex")
  def RHatBat = column[Int]("RHatBat")
  def RHsingle = column[Int]("RHsingle")
  def RHdouble = column[Int]("RHdouble")
  def RHtriple = column[Int]("RHtriple")
  def RHhomeRun = column[Int]("RHhomeRun")
  def RHRBI = column[Int]("RHRBI")
  def RHruns = column[Int]("RHruns")
  def RHbaseOnBalls = column[Int]("RHbaseOnBalls")
  def RHhitByPitch = column[Int]("RHhitByPitch")
  def RHsacFly = column[Int]("RHsacFly")
  def RHsacHit = column[Int]("RHsacHit")
  def RHstrikeOut = column[Int]("RHstrikeOut")
  def RHflyBall = column[Int]("RHflyBall")
  def RHgroundBall = column[Int]("RHgroundBall")

  def pk = index("pk_id_date", (id, date)) // First game of double headers are ignored for now

  def * : ProvenShape[(DateTime, String, String, Int, String, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int, Int)] =
    (date, id, gameId, side, pitcherId, pitcherIndex, RHatBat, RHsingle, RHdouble, RHtriple, RHhomeRun, RHRBI, RHruns, RHbaseOnBalls, RHhitByPitch, RHsacFly, RHsacHit, RHstrikeOut, RHflyBall, RHgroundBall)
}

class HitterDailyStatsTable(tag: Tag) extends Table[(DateTime, String, String, Int, Int, Int, String, Int, Int, Int, Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double])](tag, "hitterDailyStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date")
  def id = column[String]("id", O.Length(100))
  def gameId = column[String]("gameId", O.Length(100))
  def side = column[Int]("side")
  def lineupPosition = column[Int]("lineupPosition"); def lineupPositionRegime = column[Int]("lineupPositionRegime")
  def pitcherId = column[String]("pitcherId"); def pitcherIndex = column[Int]("pitcherIndex", O.Length(100))
  def atBats = column[Int]("ab"); def plateAppearances = column[Int]("pa")
  def RHdailyBattingAverage = column[Option[Double]]("RHdailyBattingAverage")
  def LHdailyBattingAverage = column[Option[Double]]("LHdailyBattingAverage")
  def dailyBattingAverage = column[Option[Double]]("dailyBattingAverage")
  def RHbattingAverage = column[Option[Double]]("RHbattingAverage")
  def LHbattingAverage = column[Option[Double]]("LHbattingAverage")
  def battingAverage = column[Option[Double]]("battingAverage")
  def RHonBasePercentage = column[Option[Double]]("RHonBasePercentage")
  def LHonBasePercentage = column[Option[Double]]("LHonBasePercentage")
  def onBasePercentage = column[Option[Double]]("onBasePercentage")
  def RHsluggingPercentage = column[Option[Double]]("RHsluggingPercentage")
  def LHsluggingPercentage = column[Option[Double]]("LHsluggingPercentage")
  def sluggingPercentage = column[Option[Double]]("sluggingPercentage")

  def pk = index("pk_id_date", (id, date)) // First game of double headers are ignored for now
  def gameIdIndex = index("gameId", (gameId))

  def * = (date, id, gameId, side, lineupPosition, lineupPositionRegime, pitcherId, pitcherIndex, atBats, plateAppearances,
    RHdailyBattingAverage, LHdailyBattingAverage, dailyBattingAverage,
    RHbattingAverage, LHbattingAverage, battingAverage,
    RHonBasePercentage, LHonBasePercentage, onBasePercentage,
    RHsluggingPercentage, LHsluggingPercentage, sluggingPercentage)
}

class HitterStatsMovingTable(tag: Tag) extends Table[HitterStatsMoving](tag, "hitterMovingStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def pitcherId = column[String]("pitcherId", O.Length(100)); def pitcherIndex = column[Int]("pitcherIndex")
  def RHbattingAverageMov = column[Option[Double]]("RHbattingAverageMov")
  def LHbattingAverageMov = column[Option[Double]]("LHbattingAverageMov")
  def battingAverageMov = column[Option[Double]]("battingAverageMov")
  def RHonBasePercentageMov = column[Option[Double]]("RHonBasePercentageMov")
  def LHonBasePercentageMov = column[Option[Double]]("LHonBasePercentageMov")
  def onBasePercentageMov = column[Option[Double]]("onBasePercentageMov")
  def RHsluggingPercentageMov = column[Option[Double]]("RHsluggingPercentageMov")
  def LHsluggingPercentageMov = column[Option[Double]]("LHsluggingPercentageMov")
  def sluggingPercentageMov = column[Option[Double]]("sluggingPercentageMov")
  def RHstyle = column[String]("RHstyle")
  def LHstyle = column[String]("LHstyle")
  def style = column[String]("style")

  def pk = index("pk_id_date", (id, date))

  def * = (date, id, pitcherId, pitcherIndex,
    RHbattingAverageMov, LHbattingAverageMov, battingAverageMov,
    RHonBasePercentageMov, LHonBasePercentageMov, onBasePercentageMov,
    RHsluggingPercentageMov, LHsluggingPercentageMov, sluggingPercentageMov,
    RHstyle, LHstyle, style) <> (HitterStatsMoving.tupled, HitterStatsMoving.unapply)
}

class HitterFantasyTable(tag: Tag) extends Table[HitterFantasyDaily](tag, "hitterFantasyStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def gameId = column[String]("gameId", O.Length(100)); def side = column[Int]("side");
  def pitcherId = column[String]("pitcherId", O.Length(100)); def pitcherIndex = column[Int]("pitcherIndex")
  def productionRate = column[Option[Double]]("productionRate")
  def daysSinceProduction = column[Int]("daysSinceProduction")
  def RHfanDuel = column[Option[Double]]("RHfanDuel")
  def LHfanDuel = column[Option[Double]]("LHfanDuel")
  def fanDuel = column[Option[Double]]("fanDuel")
  def RHdraftKings = column[Option[Double]]("RHdraftKings")
  def LHdraftKings = column[Option[Double]]("LHdraftKings")
  def draftKings = column[Option[Double]]("draftKings")
  def RHdraftster = column[Option[Double]]("RHdraftster")
  def LHdraftster = column[Option[Double]]("LHdraftster")
  def draftster = column[Option[Double]]("draftster")

  def pk = index("pk_id_date", (id, date))
  def gameIndex = index("gameId_id", (gameId, id))

  def * = (date, id, gameId, side,
    pitcherId, pitcherIndex, productionRate, daysSinceProduction,
    RHfanDuel, LHfanDuel, fanDuel,
    RHdraftKings, LHdraftKings, draftKings,
    RHdraftster, LHdraftster, draftster) <> (HitterFantasyDaily.tupled, HitterFantasyDaily.unapply)
}

class HitterFantasyMovingTable(tag: Tag) extends Table[HitterFantasy](tag, "hitterFantasyMovingStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def pitcherId = column[String]("pitcherId", O.Length(100)); def pitcherIndex = column[Int]("pitcherIndex")
  def RHfanDuel = column[Option[Double]]("RHfanDuel")
  def LHfanDuel = column[Option[Double]]("LHfanDuel")
  def fanDuel = column[Option[Double]]("fanDuel")
  def RHdraftKings = column[Option[Double]]("RHdraftKings")
  def LHdraftKings = column[Option[Double]]("LHdraftKings")
  def draftKings = column[Option[Double]]("draftKings")
  def RHdraftster = column[Option[Double]]("RHdraftster")
  def LHdraftster = column[Option[Double]]("LHdraftster")
  def draftster = column[Option[Double]]("draftster")

  def pk = index("pk_id_date", (id, date))

  def * = (date, id, pitcherId, pitcherIndex,
    RHfanDuel, LHfanDuel, fanDuel,
    RHdraftKings, LHdraftKings, draftKings,
    RHdraftster, LHdraftster, draftster) <> (HitterFantasy.tupled, HitterFantasy.unapply)
}

class HitterFantasyVolatilityTable(tag: Tag) extends Table[HitterFantasy](tag, "hitterFantasyVolatilityStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def pitcherId = column[String]("pitcherId", O.Length(100)); def pitcherIndex = column[Int]("pitcherIndex")
  def RHfanDuel = column[Option[Double]]("RHfanDuel")
  def LHfanDuel = column[Option[Double]]("LHfanDuel")
  def fanDuel = column[Option[Double]]("fanDuel")
  def RHdraftKings = column[Option[Double]]("RHdraftKings")
  def LHdraftKings = column[Option[Double]]("LHdraftKings")
  def draftKings = column[Option[Double]]("draftKings")
  def RHdraftster = column[Option[Double]]("RHdraftster")
  def LHdraftster = column[Option[Double]]("LHdraftster")
  def draftster = column[Option[Double]]("draftster")

  def pk = index("pk_id_date", (id, date))

  def * = (date, id, pitcherId, pitcherIndex,
    RHfanDuel, LHfanDuel, fanDuel,
    RHdraftKings, LHdraftKings, draftKings,
    RHdraftster, LHdraftster, draftster) <> (HitterFantasy.tupled, HitterFantasy.unapply)
}

class HitterStatsVolatilityTable(tag: Tag) extends Table[(DateTime, String, String, Int, Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double], Option[Double])](tag, "hitterVolatilityStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def pitcherId = column[String]("pitcherId", O.Length(100)); def pitcherIndex = column[Int]("pitcherIndex")
  def RHbattingVolatility = column[Option[Double]]("RHbattingVolatility")
  def LHbattingVolatility = column[Option[Double]]("LHbattingVolatility")
  def battingVolatility = column[Option[Double]]("battingVolatility")
  def RHonBaseVolatility = column[Option[Double]]("RHonBaseVolatility")
  def LHonBaseVolatility = column[Option[Double]]("LHonBaseVolatility")
  def onBaseVolatility = column[Option[Double]]("onBaseVolatility")
  def RHsluggingVolatility = column[Option[Double]]("RHsluggingVolatility")
  def LHsluggingVolatility = column[Option[Double]]("LHsluggingVolatility")
  def sluggingVolatility = column[Option[Double]]("sluggingVolatility")

  def pk = index("pk_id_date", (id, date))

  def * = (date, id, pitcherId, pitcherIndex,
    RHbattingVolatility, LHbattingVolatility, battingVolatility,
    RHonBaseVolatility, LHonBaseVolatility, onBaseVolatility,
    RHsluggingVolatility, LHsluggingVolatility, sluggingVolatility)
}

class PitcherFantasyTable(tag: Tag) extends Table[(DateTime, String, Option[Double], Option[Double], Option[Double])](tag, "pitcherFantasyStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def fanDuel = column[Option[Double]]("fanDuel")
  def draftKings = column[Option[Double]]("draftKings")
  def draftster = column[Option[Double]]("draftster")

  def pk = index("pk_id_date", (id, date))

  def * = (date, id, fanDuel, draftKings, draftster)
}

class PitcherFantasyMovingTable(tag: Tag) extends Table[(DateTime, String, Option[Double], Option[Double], Option[Double])](tag, "pitcherFantasyMovingStats") with RealityballJsonProtocol {

  def date = column[DateTime]("date"); def id = column[String]("id", O.Length(100));
  def fanDuelMov = column[Option[Double]]("fanDuelMov")
  def draftKingsMov = column[Option[Double]]("draftKingsMov")
  def draftsterMov = column[Option[Double]]("draftsterMov")

  def pk = index("pk_id_date", (id, date))

  def * = (date, id, fanDuelMov, draftKingsMov, draftsterMov)
}
