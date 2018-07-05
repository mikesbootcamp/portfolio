package controllers

import java.io.IOException
import java.util.Date

import play.api.mvc._
import shared.{Api, Message}
import upickle.Js
import autowire.AutowireServer
import play.api.libs.concurrent.Execution.Implicits._
import org.commonmark.node._
import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import java.nio.file._

import akka.util.ByteString
import play.api.http.HttpEntity

import scala.util.{Failure, Success, Try}

class Application extends Controller with Api {
  private def postsBasePath: Path = Paths.get("./posts")

  private def generateHtml(markdownPath: Path): String = {
    def generateHtmlFromString(markdownString: String): String = {
      val parser = Parser.builder().build()
      val document = parser.parse(markdownString)
      val renderer = HtmlRenderer.builder().build()
      renderer.render(document)
    }

    val markdownString: Try[String] = Try(
      new String(Files.readAllBytes(markdownPath)))
    markdownString match {
      case Success(markdownString) => generateHtmlFromString(markdownString)
      case Failure(failure) =>
        failure match {
          case _: IOException => "404 to go here"
          case e              => throw e // Throw all non-file not found exceptions.
        }
    }
  }

  def index = Action {
    Result(header = ResponseHeader(200, Map.empty),
           body = HttpEntity.Strict(
             ByteString(generateHtml(postsBasePath.resolve("./index.md"))),
             Some("text/html")))
    //Ok(views.html.index(""))
  }

  def api(path: String) = Action.async { request =>
    val body = request.body.asText.getOrElse("")
    AutowireServer
      .route[Api](this)(
        autowire.Core.Request(
          path.split("/"),
          upickle.json.read(body).asInstanceOf[Js.Obj].value.toMap
        )
      )
      .map(upickle.json.write(_))
      .map { body =>
        Ok(body)
      }
  }

  def greet(name: String) =
    Message(s"Greetings from play server, $name! Time is now ${new Date}")

}
