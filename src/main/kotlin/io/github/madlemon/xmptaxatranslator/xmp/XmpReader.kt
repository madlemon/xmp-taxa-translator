package io.github.madlemon.xmptaxatranslator.xmp

import io.github.madlemon.xmptaxatranslator.model.XmpTaxonMetadata
import org.w3c.dom.Element

class XmpReader {

    fun read(filePath: String): XmpTaxonMetadata {
        val doc = XmpXmlUtils.parseXml(filePath)
        val rdfDescription = XmpXmlUtils.findDescription(doc)

        val iptcKeywords = extractBag(rdfDescription, "Iptc4xmpCore:Keywords")

        val description = extractDescription(rdfDescription)

        return XmpTaxonMetadata(
            description = description,
            iptcKeywords = iptcKeywords.toSet()
        )
    }


    private fun extractBag(parent: Element, tag: String): List<String> {
        val nodes = parent.getElementsByTagName(tag)
        if (nodes.length == 0) return emptyList()

        val bag = nodes.item(0)
        val liNodes = (bag as Element).getElementsByTagName("rdf:li")

        val result = mutableListOf<String>()
        for (i in 0 until liNodes.length) {
            result.add(liNodes.item(i).textContent)
        }
        return result
    }

    private fun extractDescription(parent: Element): String? {
        val nodes = parent.getElementsByTagName("tiff:ImageDescription")
        if (nodes.length == 0) return null

        val alt = nodes.item(0) as Element
        val li = alt.getElementsByTagName("rdf:li")

        if (li.length == 0) return null
        return li.item(0).textContent
    }
}
