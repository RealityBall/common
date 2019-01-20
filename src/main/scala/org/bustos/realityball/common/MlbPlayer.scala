package org.bustos.realityball.common

import java.io._

import org.bustos.realityball.common.RealityballConfig._
import org.bustos.realityball.common.RealityballRecords._
import org.openqa.selenium.remote._
import org.scalatest.selenium.Chrome
import org.scalatest.time.{Seconds, Span}
import org.slf4j.LoggerFactory

class MlbPlayer(val mlbId: String) extends Chrome {

  implicitlyWait(Span(20, Seconds))

  val realityballData = new RealityballData
  val logger = LoggerFactory.getLogger(getClass)

  val nameExpression = """(.*) (.*?)[0-9].*""".r
  val justNameExpression = """(.*) (.*?)""".r
  val nameNoNumberExpression = """(.*) (.*?)\|.*""".r
  val positionExpression = """\|(.*)""".r
  val logoTeamName = """.*/images/logos/30x34/(.*)_logo.png""".r
  val batsThrows = """B/T: (.)/(.)""".r

  logger.info("*****************************************")
  logger.info("*** Retrieving player info for " + mlbId + " ***")
  logger.info("*****************************************")

  val fileName = DataRoot + "gamedayPages/players/" + mlbId + ".html"

  if (new File(fileName).exists) {
    val caps = DesiredCapabilities.chrome
    caps.setCapability("chrome.switches", Array("--disable-javascript"))
    go to "file://" + fileName
  } else {
    val host = MlbURL
    go to host + "team/player.jsp?player_id=" + mlbId
  }

  val playerName = {

    def nameTuple(name: RemoteWebElement) = {
      name.getAttribute("textContent").replaceAll("\u00A0","") match {
        case nameExpression(firstName, lastName) => (firstName, lastName)
        case nameNoNumberExpression(firstName, lastName) => (firstName, lastName)
        case justNameExpression(firstName, lastName) => (firstName, lastName)
      }
    }

    find("player_name") match {
      case Some(x) => x.underlying match {
        case name: RemoteWebElement => nameTuple(name)
      }
      case None => {
        find(XPathQuery("""//*[@id="player-header"]/div/div/h1/span[contains(@class, 'player-name')]""")) match {
          case Some(x) => x.underlying match {
            case name: RemoteWebElement => nameTuple(name)
          }
          case None => throw new Exception("Could not find player name")
        }
      }
    }
  }

  val bats = {

    find("player_bats") match {
      case Some(x) => x.underlying match {
        case name: RemoteWebElement => name.getAttribute("textContent")
      }
      case None => {
        find(XPathQuery("""//*[@id="player-header"]/div/div/ul/li[2]""")) match {
          case Some(x) => x.underlying match {
            case name: RemoteWebElement => name .getAttribute("textContent") match {
              case batsThrows(bat, thr) => bat
            }
          }
          case None => throw new Exception("Could not find player bats")
        }
      }
    }
  }

  val throws = {
    find("player_throws") match {
      case Some(x) => x.underlying match {
        case name: RemoteWebElement => name.getAttribute("textContent")
      }
      case None => {
        find(XPathQuery("""//*[@id="player-header"]/div/div/ul/li[2]""")) match {
          case Some(x) => x.underlying match {
            case name: RemoteWebElement => name.getAttribute("textContent") match {
              case batsThrows(bat, thr) => thr
            }
          }
          case None => throw new Exception("Could not find player throws")
        }
      }
    }
  }

  val position = {
    find("player_position") match {
      case Some(x) => x.underlying match {
        case name: RemoteWebElement => name.getAttribute("textContent").replaceAll("\u00A0","") match {
          case positionExpression(positionString) => positionString
        }
      }
      case None => {
        find(XPathQuery("""//*[@id="player-header"]/div/div/ul/li[1]""")) match {
          case Some(x) => x.underlying match {
            case name: RemoteWebElement => name.getAttribute("textContent")
          }
          case None => throw new Exception("Could not find player position")
        }
      }
    }
  }

  val team = {
    find("main_name") match {
      case Some(x) => x.underlying match {
        case panel: RemoteWebElement => {
          panel.findElementByClassName("logo").getAttribute("src") match {
            case logoTeamName(teamName) => teamName.toUpperCase
          }
        }
      }
      case None => {
        find(XPathQuery( """/html/head/meta[22]""")) match {
          case Some(x) => x.underlying match {
            case name: RemoteWebElement => name.getAttribute("content").toUpperCase
          }
          case None => throw new Exception("Could not find team name")
        }
      }
    }
  }

  val player = Player(mlbId, "2015", playerName._2, playerName._1, bats, throws, team, position)

  if (!(new File(fileName)).exists) {
    val writer = new FileWriter(new File(fileName))
    writer.write(pageSource)
    writer.close
  }

  quit
}
