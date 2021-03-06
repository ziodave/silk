/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.fuberlin.wiwiss.silk.plugins.dataset.rdf.formatters

import de.fuberlin.wiwiss.silk.dataset.Formatter
import de.fuberlin.wiwiss.silk.entity.Link

import scala.xml.NodeSeq

/**
 * Base trait of all formatters using XML.
 */
trait XMLFormatter extends Formatter {

  override final def format(link: Link, predicateUri: String): String = formatXML(link, predicateUri).toString + "\n"

  def formatXML(link: Link, predicateUri: String): NodeSeq
}