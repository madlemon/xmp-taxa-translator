package io.github.madlemon.xmptaxatranslator.xmp

import io.github.madlemon.xmptaxatranslator.model.XmpTaxonMetadata
import org.w3c.dom.Element

class XmpReader(private val iNaturalistModel: String) {

    fun read(filePath: String): XmpTaxonMetadata {
        val doc = XmpXmlUtils.parseXml(filePath)
        val rdfDescription = XmpXmlUtils.findDescription(doc)

        val hierarchicalSubjects = extractBag(rdfDescription, "lr:hierarchicalSubject")

        return XmpTaxonMetadata(
            detectedSpecies = extractDetectedSpecies(hierarchicalSubjects),
            keywords = extractInatClassification(hierarchicalSubjects)?.split("|")?.toSet() ?: emptySet()
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

    private fun extractDetectedSpecies(hierarchicalSubjects: List<String>): String? {
        return extractInatClassification(hierarchicalSubjects)?.substringAfterLast("|")
    }

    private fun extractInatClassification(hierarchicalSubjects: List<String>): String? =
        hierarchicalSubjects.firstOrNull { it.startsWith(iNaturalistModel) }

}
