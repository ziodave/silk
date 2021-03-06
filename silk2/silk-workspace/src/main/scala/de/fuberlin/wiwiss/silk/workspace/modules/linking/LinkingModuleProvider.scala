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

package de.fuberlin.wiwiss.silk.workspace.modules.linking

import java.util.logging.{Level, Logger}

import de.fuberlin.wiwiss.silk.config.{LinkSpecification, Prefixes}
import de.fuberlin.wiwiss.silk.evaluation.ReferenceLinksReader
import de.fuberlin.wiwiss.silk.runtime.resource.{ResourceLoader, ResourceManager}
import de.fuberlin.wiwiss.silk.util.Identifier
import de.fuberlin.wiwiss.silk.util.XMLUtils._
import de.fuberlin.wiwiss.silk.workspace.Project
import de.fuberlin.wiwiss.silk.workspace.modules.ModuleProvider

import scala.xml.XML

/**
 * The linking module which encapsulates all linking tasks.
 */
class LinkingModuleProvider extends ModuleProvider[LinkingTask] {

  private val logger = Logger.getLogger(classOf[LinkingModuleProvider].getName)

  override def prefix = "linking"

  /**
   * Loads all tasks of this module.
   */
  def loadTasks(resources: ResourceLoader, project: Project) = {
    for(name <- resources.listChildren) yield
      loadTask(resources.child(name), project)
  }

  /**
   * Loads a specific task in this module.
   */
  private def loadTask(taskResources: ResourceLoader, project: Project) = {
    val linkSpec = LinkSpecification.load(project.resources)(project.config.prefixes)(taskResources.get("linkSpec.xml").load)
    val referenceLinks = ReferenceLinksReader.readReferenceLinks(taskResources.get("alignment.xml").load)

    //Load the cache
    val cache = new LinkingCaches()
    try {
      cache.loadFromXML(XML.load(taskResources.get("cache.xml").load))
    } catch {
    case ex: Exception =>
      logger.log(Level.WARNING, s"Cache for task ${linkSpec.id} in project ${project.name} corrupted. Rebuilding Cache.", ex)
      new LinkingCaches()
    }

    LinkingTask(project, linkSpec, referenceLinks, cache, updateCache = false)
  }

  /**
   * Removes a specific task.
   */
  def removeTask(taskId: Identifier, resources: ResourceManager) = {
    resources.delete(taskId)
  }

  /**
   * Writes an updated task.
   */
  def writeTask(task: LinkingTask, resources: ResourceManager) = {
    //Don't use any prefixes
    implicit val prefixes = Prefixes.empty

    // Write resources
    val taskResources = resources.child(task.name)
    taskResources.put("linkSpec.xml") { os => task.linkSpec.toXML.write(os) }
    taskResources.put("alignment.xml") { os => task.referenceLinks.toXML.write(os) }
    taskResources.put("cache.xml") { os => task.cache.toXML.write(os) }
  }
  
}