package io.github.markusmo3.bm

import com.intellij.util.xmlb.XmlSerializer
import io.github.markusmo3.bm.config.BMActionsSchema
import io.github.markusmo3.bm.config.BMActionsSchemaState
import org.codehaus.plexus.util.StringInputStream
import org.jdom.input.DOMBuilder
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.w3c.dom.Document
import javax.xml.parsers.DocumentBuilderFactory

internal class BMActionsSchemaTest {
}

fun main() {
    val schema = BMActionsSchema()
    schema.noStateLoaded()
    val schemaXmlString = toXmlString(schema.state)
    println("=== BEFORE ===")
    println(schemaXmlString)

    val factory = DocumentBuilderFactory.newInstance()
    val documentBuilder = factory.newDocumentBuilder()
    val w3cDocument: Document = documentBuilder.parse(StringInputStream(schemaXmlString))
    val document = DOMBuilder().build(w3cDocument)
    val deserialize = XmlSerializer.deserialize(document.rootElement, BMActionsSchemaState::class.java)
    println("=== AFTER ===")
    val newSchemaXmlString = toXmlString(deserialize)
    println(newSchemaXmlString)

    println("=== MATCH? ===")
    println(schemaXmlString == newSchemaXmlString)
}

private fun toXmlString(schema: BMActionsSchemaState): String? {
    val serialized = XmlSerializer.serialize(schema)
    val xmlOutputter = XMLOutputter()
    xmlOutputter.format = Format.getPrettyFormat()
    val str = xmlOutputter.outputString(serialized)
    return str
}