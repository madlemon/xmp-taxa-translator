package io.github.madlemon.xmptaxatranslator.xmp

import io.github.madlemon.xmptaxatranslator.model.TaxonTranslations
import org.w3c.dom.Document
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import java.io.File

private const val DC_NS = "http://purl.org/dc/elements/1.1/"
private const val LR_NS = "http://ns.adobe.com/lightroom/1.0/"
private const val RDF_NS = "http://www.w3.org/1999/02/22-rdf-syntax-ns#"

class DarktableXmpWriter {

    fun write(
        filePath: String,
        translation: TaxonTranslations,
        hierarchyPath: String // e.g. "Inat21|bird|Mäusebussard"
    ) {
        val doc = parseXml(filePath)
        val rdf = findDescription(doc)

        addHierarchy(rdf, hierarchyPath)

        writeDescription(rdf, doc, translation)

        save(doc, filePath)
    }

    // ----------------------------
    // Hierarchical subject
    // ----------------------------

    private fun addHierarchy(parent: Element, path: String) {
        val bag = getOrCreateBag(parent, "lr:hierarchicalSubject")

        val item = parent.ownerDocument.createElement("rdf:li")
        item.textContent = path

        bag.appendChild(item)
    }

    // ----------------------------
    // Description (dc:description)
    // ----------------------------

    private fun writeDescription(
        parent: Element,
        doc: Document,
        t: TaxonTranslations
    ) {
        val desc = getOrCreateAltNode(parent, doc)
        val alt = desc.getElementsByTagName("rdf:Alt").item(0) as Element

        fun removeLang(lang: String) {
            val nodes = alt.getElementsByTagName("rdf:li")
            val toRemove = mutableListOf<Element>()

            for (i in 0 until nodes.length) {
                val el = nodes.item(i) as Element
                if (el.getAttribute("xml:lang") == lang) {
                    toRemove.add(el)
                }
            }

            toRemove.forEach { alt.removeChild(it) }
        }

        fun add(lang: String, value: String?) {
            if (value == null) return

            removeLang(lang)

            val li = doc.createElement("rdf:li")
            li.setAttribute("xml:lang", lang)
            li.textContent = value
            alt.appendChild(li)
        }

        add("x-default", t.preferredCommonName)
        add("en", t.englishCommonName)
        add("la", t.latinName)
    }

    // ----------------------------
    // Helpers
    // ----------------------------

    private fun getOrCreateBag(parent: Element, tag: String): Element {
        val existing = parent.getElementsByTagName(tag)
        if (existing.length > 0) {
            val bag = existing.item(0) as Element
            val rdfBag = bag.getElementsByTagName("rdf:Bag")
            if (rdfBag.length > 0) return rdfBag.item(0) as Element
        }

        val doc = parent.ownerDocument

        val container = doc.createElement(tag)
        val bag = doc.createElement("rdf:Bag")

        container.appendChild(bag)
        parent.appendChild(container)

        return bag
    }

    private fun getOrCreateAltNode(parent: Element, doc: Document): Element {
        val nodes = parent.getElementsByTagName("dc:description")

        if (nodes.length > 0) {
            return nodes.item(0) as Element
        }

        val desc = doc.createElementNS(DC_NS, "dc:description")
        val alt = doc.createElement("rdf:Alt")

        desc.appendChild(alt)
        parent.appendChild(desc)

        return desc
    }

    // ----------------------------
    // XML plumbing
    // ----------------------------

    private fun parseXml(path: String): Document {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        return factory.newDocumentBuilder().parse(path)
    }

    private fun findDescription(doc: Document): Element {
        return doc.getElementsByTagName("rdf:Description").item(0) as Element
    }

    private fun save(doc: Document, filePath: String) {
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.transform(
            DOMSource(doc),
            StreamResult(File(filePath))
        )
    }
}
