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

class IpynbRenderer extends Renderer {
  private[this] val logger = LoggerFactory.getLogger(classOf[IpynbRenderer])

  def render(request: RenderRequest): Html = {
    import request._
    Html(toHtml(filePath, fileContent, branch, repository, enableWikiLink, enableRefsLink)(context))
  }

  def shutdown() : Unit = {
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
    val lang = nb.metadata.language_info.name

    val cellsHtml = nb.cells.map(cell =>
      cell.cell_type match{
        case "code" =>
          val execCount = cell.execution_count
          val source = cell.source.mkString("")
          val rendered = s"""<pre class="prettyprint lang-$lang">$source</pre>"""

          val outputHtml = cell.outputs.map{ o =>
            o.output_type match {
              case "stream" =>
                s"""<div class="ipynb-${o.output_type}">${o.text.map(HtmlFormat.escape).mkString("<br>")}</div>"""
              case "error" =>
                val stacktrace = o.traceback.map(_.mkString("")).getOrElse("")
                s"""<div class="ipynb-error"><span>${o.ename.getOrElse("")}:</span><span>${o.evalue.getOrElse("")}</span>
                   |<div class="ipynb-stacktrace">$stacktrace</div></div>""".stripMargin
              case "execute_result" | "display_data"=>
                val innerHtml = o.data.map(m =>
                  m.keys.map {
                    case key@("text/html" | "text/latex" | "image/svg+xml") =>
                      m.get(key) match {
                        case Some(x: String) => x
                        case Some(x: List[_]) => x.asInstanceOf[List[String]].mkString("")
                        case _ => ""
                      }
                    case key@img if img.startsWith("image/") =>
                      val src = m.get(key) match {
                        case Some(x: String) => x
                        case Some(x: List[_]) => x.asInstanceOf[List[String]].mkString("")
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
                s"""<div class="ipynb-execution-result">$innerHtml</div>"""
              case _ =>
                s"""<div class="ipynb-unknown">Unknown output_type:${o.output_type} value:${o.data}</div>"""
            }
          }.mkString("<br>")
          val countStr = execCount.map(c => s"[$c]").getOrElse("[ ]")
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
    ).map(c => s"""<div class="ipynb-cell">$c</div>""").mkString("")

    s"""<link rel="stylesheet" href="$path/plugin-assets/ipynb/ipynb.css">
       |<script src='https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.0/MathJax.js?config=TeX-MML-AM_CHTML'></script>
       |$cellsHtml""".stripMargin
  }
}

case class CellMetaData(
  collapsed: Option[Boolean]
)

case class CellOutput(
  name: Option[String],
  output_type: String,
  text: Array[String],
  execution_count: Option[Int],
  ename: Option[String],
  evalue: Option[String],
  traceback: Option[Array[String]],
  data: Option[Map[String, Any]]
)

case class Cell(
  cell_type: String,
  execution_count: Option[Int],
  metadata: CellMetaData,
  outputs: Array[CellOutput],
  source: Array[String]
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
  kernelspec: KernelSpec,
  language_info: LanguageInfo
)

case class IPyNotebook(
  metadata: MetaData,
  cells: Array[Cell],
  nbformat: Long,
  nbformat_minor: Long
)