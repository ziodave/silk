package de.fuberlin.wiwiss.silk.util

import javax.xml.XMLConstants
import javax.xml.parsers.SAXParserFactory
import javax.xml.validation.SchemaFactory
import javax.xml.transform.stream.StreamSource
import java.io._
import xml.parsing.NoBindingFactoryAdapter
import de.fuberlin.wiwiss.silk.util.ValidationException.ValidationError
import xml._
import org.xml.sax.{Attributes, SAXParseException, ErrorHandler, InputSource}

/**
 * Parses an XML input source and validates it against the schema.
 */
class ValidatingXMLReader[T](deserializer: Node => T, schemaPath: String) {
  def apply(xml: Node): T = {
    //Validate
    new XmlReader().read(new InputSource(new StringReader(xml.toString)), schemaPath)

    deserializer(xml)
  }

  def apply(stream: InputStream): T = {
    val node = new XmlReader().read(new InputSource(stream), schemaPath)

    deserializer(node)
  }

  def apply(reader: Reader): T = {
    val node = new XmlReader().read(new InputSource(reader), schemaPath)

    deserializer(node)
  }

  def apply(file: File): T = {
    val inputStream = new FileInputStream(file)
    try {
      apply(inputStream)
    }
    finally {
      inputStream.close()
    }
  }

  /**
   * Reads an XML stream while validating it using a xsd schema file.
   */
  private class XmlReader extends NoBindingFactoryAdapter {
    private var currentErrors = List[String]()
    private var validationErrors = List[ValidationError]()

    def read(inputSource: InputSource, schemaPath: String): Elem = {
      //Load XML Schema
      val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schemaStream = getClass.getClassLoader.getResourceAsStream(schemaPath)
      if (schemaStream == null) throw new ValidationException("XML Schema for Link Specification not found")
      val schema = schemaFactory.newSchema(new StreamSource(schemaStream))

      //Create parser
      val parserFactory = SAXParserFactory.newInstance()
      parserFactory.setNamespaceAware(true)
      parserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      val parser = parserFactory.newSAXParser()

      //Set Error handler
      val xr = parser.getXMLReader
      val vh = schema.newValidatorHandler()
      vh.setErrorHandler(new ErrorHandler {
        def warning(ex: SAXParseException) {}

        def error(ex: SAXParseException) {
          addError(ex)
        }

        def fatalError(ex: SAXParseException) {
          addError(ex)
        }
      })
      vh.setContentHandler(this)
      xr.setContentHandler(vh)

      //Parse XML
      scopeStack.push(TopScope)
      xr.parse(inputSource)
      scopeStack.pop

      //Add errors without an id
      for(error <- currentErrors) {
        validationErrors ::= ValidationError(error)
      }

      //Return result
      if (validationErrors.isEmpty) {
        val xml = rootElem.asInstanceOf[Elem]
        checkUniqueIdentifiers(xml)
        xml
      }
      else {
        throw new ValidationException(validationErrors.reverse)
      }
    }

    override def startElement(uri: String, _localName: String, qname: String, attributes: Attributes) {
      for(idAttribute <- Option(attributes.getValue("id"))) {
        val id = Identifier(idAttribute)

        for(error <- currentErrors) {
          validationErrors ::= ValidationError(error, Some(id), Some(_localName))
        }

        currentErrors = Nil
      }

      super.startElement(uri, _localName, qname, attributes)
    }

    /**
     * Checks if the document contains any duplicated identifiers.
     */
    private def checkUniqueIdentifiers(xml: Elem) {
      val elements = (xml \\ "Aggregate") ++ (xml \\ "Compare") ++ (xml \\ "TransformInput") ++ (xml \\ "Input")
      val ids = elements.map(_ \ "@id").map(_.text).filterNot(_.isEmpty)
      if (ids.distinct.size < ids.size) {
        val duplicatedIds = ids diff ids.distinct
        val errors = duplicatedIds.map(id => ValidationError("Duplicated identifier", Some(Identifier(id))))
        throw new ValidationException(errors)
      }
    }

    /**
     * Formats a XSD validation exception.
     */
    private def addError(ex: SAXParseException) = {
      //The error message without prefixes like "cvc-complex-type.2.4.b:"
      val error = ex.getMessage.split(':').tail.mkString.trim

      currentErrors ::= error
    }
  }

}