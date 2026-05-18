package io.github.madlemon.xmptaxatranslator.xmp

import io.github.madlemon.xmptaxatranslator.model.TaxonTranslations
import io.github.madlemon.xmptaxatranslator.model.XmpTaxonMetadata
import org.w3c.dom.Document
import org.w3c.dom.Element

private const val DC_NS = "http://purl.org/dc/elements/1.1/"
private const val TIFF_NS = "http://ns.adobe.com/tiff/1.0/"

class LuminaXmpWriter(private val preferredLocale: String) {
    fun write(
        filePath: String, originalData: XmpTaxonMetadata, translationData: TaxonTranslations
    ) {
        val doc = XmpXmlUtils.parseXml(filePath)
        val rdfDescription = XmpXmlUtils.findDescription(doc)

        val keywords = mutableSetOf<String>().apply {
            addAll(originalData.iptcKeywords)
            translationData.preferredCommonName?.let { add(it) }
            translationData.latinName?.let { add(it) }
        }

        writeKeywords(rdfDescription, keywords)

        val newDescription = buildNewDescription(
            preferred = translationData.preferredCommonName, latin = translationData.latinName
        )

        if (newDescription != null) {
            writeMultilangDescription(
                rdfDescription,
                valueXDefault = translationData.preferredCommonName,
                valueEnglish = translationData.englishCommonName,
                valueTranslated = translationData.preferredCommonName,
                valueLatin = translationData.latinName
            )
        }

        saveXml(doc, filePath)
    }

    private fun writeKeywords(parent: Element, keywords: Set<String>) {
        val bag = parent.getElementsByTagName("lr:hierarchicalSubject")
            .item(0) as Element

        val rdfBag = bag.getElementsByTagName("rdf:Bag")
            .item(0) as Element

        // clear existing
        while (rdfBag.childNodes.length > 0) {
            rdfBag.removeChild(rdfBag.firstChild)
        }

        // add new
        keywords.forEach {
            val li = rdfBag.ownerDocument.createElement("rdf:li")
            li.textContent = it
            rdfBag.appendChild(li)
        }
    }

    private fun buildNewDescription(
        preferred: String?, latin: String?
    ): String? {

        if (preferred == null && latin == null) return null
        if (preferred == null) return latin
        if (latin == null) return preferred

        return "$preferred ($latin)"
    }

    private fun writeMultilangDescription(
        parent: Element,
        valueXDefault: String,
        valueEnglish: String,
        valueTranslated: String,
        valueLatin: String?
    ) {
        val tiffNode = parent.getElementsByTagNameNS(TIFF_NS, "ImageDescription")
            .item(0) as Element

        val dcNode = parent.getElementsByTagNameNS(DC_NS, "description")
            .item(0) as Element

        val altTiff = tiffNode.getElementsByTagName("rdf:Alt")
            .item(0) as Element

        val altDc = dcNode.getElementsByTagName("rdf:Alt")
            .item(0) as Element

        clearAlt(altTiff)
        clearAlt(altDc)

        val doc = parent.ownerDocument

        writeAlt(tiffNode, doc, valueXDefault, valueEnglish, valueTranslated, valueLatin)
        writeAlt(dcNode, doc, valueXDefault, valueEnglish, valueTranslated, valueLatin)
    }

    private fun clearAlt(alt: Element) {
        val nodes = alt.getElementsByTagName("rdf:li")

        while (nodes.length > 0) {
            alt.removeChild(nodes.item(0))
        }
    }

    private fun writeAlt(
        node: Element,
        doc: Document,
        xDefault: String,
        en: String,
        preferred: String,
        la: String?
    ) {
        val alt = node.getElementsByTagName("rdf:Alt").item(0) as Element

        // clear existing
        while (alt.childNodes.length > 0) {
            alt.removeChild(alt.firstChild)
        }

        fun add(lang: String, value: String) {
            val li = doc.createElement("rdf:li")
            li.setAttribute("xml:lang", lang)
            li.textContent = value
            alt.appendChild(li)
        }

        add("x-default", xDefault)
        add("en", en)
        add(preferredLocale, preferred)
        la?.let { add("la", it) }
    }

    private fun saveXml(doc: Document, filePath: String) {
        val transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer()
        val source = javax.xml.transform.dom.DOMSource(doc)
        val result = javax.xml.transform.stream.StreamResult(java.io.File(filePath))
        transformer.transform(source, result)
    }
}
