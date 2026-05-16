package io.github.madlemon.xmptaxatranslator.xmp

import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory


object XmpXmlUtils {

    fun parseXml(filePath: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        return builder.parse(filePath)
    }

    fun findDescription(doc: Document): Element {
        val nodes = doc.getElementsByTagName("rdf:Description")
        return nodes.item(0) as Element
    }
}
