package gitbucket.ipynb

import gitbucket.core.controller.Context
import gitbucket.core.plugin.{RenderRequest, Renderer}
import gitbucket.core.service.RepositoryService.RepositoryInfo
import gitbucket.core.view.helpers.markdown
import org.json4s._
import org.json4s.jackson.JsonMethods._
import org.json4s.jackson.JsonMethods
import play.twirl.api.{Html, HtmlFormat}
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success, Try}

class IpynbRenderer extends Renderer {
  private[this] val logger = LoggerFactory.getLogger(classOf[IpynbRenderer])

  def render(request: RenderRequest): Html = {
    import request._
    Html(Try(toHtml(filePath, fileContent, branch, repository, enableWikiLink, enableRefsLink)(context)) match {
      case Success(v) => v
      case Failure(e) => s"""<h2>Error</h2><div class="ipynb-error"><pre>$e</pre></div>"""
    })
  }

  def shutdown() : Unit = {
  }

  def getLanguage(nb: IPyNotebook, cell: Cell): String = {
    if (nb.nbformat <= 4) {
      cell.language.getOrElse("")
    } else {
      nb.metadata.language_info.get.name
    }
  }

  def getCellHtml(
    nb: IPyNotebook,
    cell: Cell,
    repository: RepositoryInfo,
    enableWikiLink: Boolean,
    enableRefsLink: Boolean)(implicit context: Context): String = {
    cell.cell_type match{
      case "code" =>
        val execCount = cell.execution_count.getOrElse(cell.prompt_number.getOrElse(-1))
        val source = (cell.source ++ cell.input).mkString("")
        val lang = getLanguage(nb, cell)
        val rendered = s"""<pre class="prettyprint lang-$lang">$source</pre>"""

        val outputHtml = cell.outputs.map{ o =>
          o.output_type match {
            case "stream" =>
              s"""<div class="ipynb-${o.output_type}">${o.text.map(HtmlFormat.escape).mkString("<br>")}</div>"""
            case "error" =>
              val stacktrace = o.traceback.map(_.mkString("")).getOrElse("")
              s"""<div class="ipynb-error alert alert-danger"><span>${o.ename.getOrElse("")}:</span><span>${o.evalue.getOrElse("")}</span>
                 |<div class="ipynb-stacktrace">$stacktrace</div></div>""".stripMargin
            case "execute_result" | "display_data" =>
              val innerHtml = o.data.map(m =>
                m.keys.map {
                  case key@("text/html" | "text/latex" | "image/svg+xml") =>
                    m(key) match {
                      case x: String => x
                      case x: List[_] => x.asInstanceOf[List[String]].mkString("")
                      case _ => ""
                    }
                  case key@img if img.startsWith("image/") =>
                    val src = m(key) match {
                      case x: String => x
                      case x: List[_] => x.asInstanceOf[List[String]].mkString("")
                      case _ => ""
                    }
                    s"""<img src="data:$img;base64,$src">"""
                  case key@("text/plain" | "application/javascript" | _) =>
                    m(key) match {
                      // disable Javascript
                      case x: String => HtmlFormat.escape(x)
                      case x: List[_] => x.asInstanceOf[List[String]].map(HtmlFormat.escape).mkString("<br>")
                      case _ => ""
                    }
                }.mkString("")
              ).mkString("")
              val innerHtmlFormat3 = o.png.map(x => s"""<img src="data:image/png;base64,$x">""").getOrElse("") +
                o.jpeg.map(x => s"""<img src="data:image/jpeg;base64,$x">""").getOrElse("") +
                o.svg.getOrElse("") +
                o.javascript.map(HtmlFormat.escape).getOrElse("") +
                o.json.map(x => s"""<pre class="prettyprint lang-json">$x</pre>""").getOrElse("") +
                o.latex.map(HtmlFormat.escape).getOrElse("") +
                o.html.getOrElse("") +
                o.text.map(HtmlFormat.escape).mkString("")
              s"""<div class="ipynb-execution-result">$innerHtml$innerHtmlFormat3</div>"""
            case "pyout" => // format3
              ""
            case _ =>
              s"""<div class="ipynb-unknown">Unknown output_type:${o.output_type} value:${o.data}</div>"""
          }
        }.mkString("<br>")
        val countStr = execCount match{
          case -1 => "[ ]"
          case x => s"[$x]"
        }
        val outputDiv = if (cell.outputs.length > 0) {
          s"""<div class="ipynb-innercell"><div class="ipynb-prompt ipynb-output-prompt">Out $countStr</div><div class="ipynb-rendered ipynb-rendered-output">$outputHtml</div></div>"""
        }else {
          ""
        }
        s"""<div class="ipynb-innercell"><div class="ipynb-prompt ipynb-input-prompt">In $countStr</div><div class="ipynb-rendered ipynb-rendered-code">$rendered</div></div>$outputDiv"""
      case "markdown" =>
        val md = cell.source.mkString("")
        val rendered = markdown(md, repository, enableWikiLink, enableRefsLink, enableLineBreaks = true)
        s"""<div class="ipynb-innercell"><div clss="ipynb-prompt ipynb-input-prompt"></div><div class="ipynb-prompt ipynb-input-prompt"></div><div calss="ipynb-rendered ipynb-rendered-markdown">$rendered</div></div>"""
      case _ =>
        s"""<div class="ipynb-prompt ipynb-input-prompt">???</div><div class="ipynb-rendered ipynb-rendered-unknown"><code><pre>${cell.source.mkString("")}</pre></code></div>"""
    }
  }

  def toHtml(
    filePath: List[String],
    ipynb: String,
    branch: String,
    repository: RepositoryInfo,
    enableWikiLink: Boolean,
    enableRefsLink: Boolean)(implicit context: Context): String = {
    val path = context.baseUrl
    val json = parse(ipynb)

    implicit val formats = DefaultFormats
    val nb = json.extract[IPyNotebook]

    val cellsHtml = (if (nb.nbformat < 4) nb.worksheets.flatMap(_.cells) else nb.cells).map(cell =>
      getCellHtml(nb, cell, repository, enableWikiLink, enableRefsLink)
    ).map(c => s"""<div class="ipynb-cell">$c</div>""").mkString("")

    s"""<link rel="stylesheet" href="$path/plugin-assets/ipynb/ipynb.css">
       |<script src='https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.0/MathJax.js?config=TeX-MML-AM_CHTML'></script>
       |$cellsHtml""".stripMargin
  }
}

case class CellMetaData(
  collapsed: Option[Boolean],
  scrolled: Option[Boolean]
)

case class CellOutput(
  name: Option[String],
  output_type: String,
  text: Array[String],
  html: Option[String],
  png: Option[String],
  jpeg: Option[String],
  svg: Option[String],
  json: Option[String],
  javascript: Option[String],
  latex: Option[String],
  execution_count: Option[Int],
  ename: Option[String],
  evalue: Option[String],
  traceback: Option[Array[String]],
  data: Option[Map[String, Any]]
)

case class Cell(
  cell_type: String,
  execution_count: Option[Int],
  prompt_number: Option[Int],
  metadata: CellMetaData,
  outputs: Array[CellOutput],
  source: Array[String],
  input: Array[String],
  language: Option[String]
)

case class Worksheet(
  cells: Array[Cell],
  metadata: Map[String, Any]
)

case class KernelSpec(
  display_name: String,
  language: String,
  name: String
)

case class CodeMirrorMode(
  name: String,
  version: Int
)

case class LanguageInfo(
  codemirror_mode: CodeMirrorMode,
  file_extension: String,
  mimetype: String,
  name: String,
  nbconvert_exporter: String,
  pygments_lexer: String,
  version: String
)

case class MetaData(
  kernelspec: Option[KernelSpec],
  language_info: Option[LanguageInfo],
  name: Option[String]
)

case class IPyNotebook(
  metadata: MetaData,
  cells: Array[Cell],
  worksheets: Array[Worksheet],
  nbformat: Int,
  nbformat_minor: Int
)
