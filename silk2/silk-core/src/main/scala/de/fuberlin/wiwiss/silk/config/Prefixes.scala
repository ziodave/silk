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

package de.fuberlin.wiwiss.silk.config

import xml.Node
import de.fuberlin.wiwiss.silk.util.ValidationException
import scala.language.implicitConversions

/**
 * Holds namespace prefixes.
 */
class Prefixes(val prefixMap: Map[String, String]) {
  override def toString = "Prefixes(" + prefixMap.toString + ")"

  /**
   * Combines two prefix objects.
   */
  def ++(prefixes: Prefixes) = {
    new Prefixes(prefixMap ++ prefixes.prefixMap)
  }

  /**
   * Resolves a qualified name to its full URI.
   *
   * @param name The qualified name e.g. rdf:label or a full URI enclosed in <> brackets.
   * @return The full URI e.g. http://www.w3.org/1999/02/22-rdf-syntax-ns#label
   * @see shorten
   */
  def resolve(name: String) =
    if(name.trim.isEmpty) {
      throw new ValidationException("Value cannot be empty.")
    } else if (name.startsWith("http")) {
      name
    } else if (name.startsWith("<") && name.endsWith(">")) {
      name.substring(1, name.length - 1)
    } else {
      name.split(":", 2) match {
        case Array(prefix, suffix) => prefixMap.get(prefix) match {
          case Some(resolvedPrefix) => resolvedPrefix + suffix
          case None => throw new ValidationException(
            s"Unknown prefix: '$prefix'. Please add the missing prefix to the project " +
             "or use a full URI, e.g., <http:/example.org/name>.")
        }
        case _ => throw new ValidationException(
          s"Expected a prefixed name of the form 'prefix:name', but got '$name'. " +
           "If you want to write a full URI, use angle brackets, e.g., <http:/example.org/name>.")
    }
  }

  /**
   * Tries to shorten a full URI.
   *
   * @param uri The full URI e.g. http://www.w3.org/1999/02/22-rdf-syntax-ns#label
   * @return The qualified name if a prefix was found e.g. rdf:label. The full URI otherwise.
   * @see resolve
   */
  def shorten(uri: String): String = {
    for ((id, namespace) <- prefixMap if uri.startsWith(namespace)) {
      return id + ":" + uri.substring(namespace.length)
    }

    uri
  }

  def toXML = {
    <Prefixes>
      {for ((key, value) <- prefixMap) yield {
        <Prefix id={key} namespace={value}/>
    }}
    </Prefixes>
  }

  def toSparql = {
    var sparql = ""
    for ((key, value) <- prefixMap) {
      sparql += "PREFIX " + key + ": <" + value + "> "
    }
    sparql
  }

}

object Prefixes {
  val empty = new Prefixes(Map.empty)

  implicit def fromMap(map: Map[String, String]) = new Prefixes(map)

  implicit def toMap(prefixes: Prefixes) = prefixes.prefixMap

  def apply(map: Map[String, String]) = new Prefixes(map)

  def fromXML(xml: Node) = {
    new Prefixes((xml \ "Prefix").map(n => ((n \ "@id").text, (n \ "@namespace").text)).toMap)
  }
}