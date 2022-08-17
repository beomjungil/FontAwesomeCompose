import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import groovy.json.JsonSlurper
import java.io.File

data class IconData(
    val unicode: String,
    val name: String,
    val originalName: String,
    val styles: List<String>
) {
    val propertyName = if (name.first().isDigit()) "`${name}`" else name
}

fun String.kebabToPascalCase(): String = this
    .split("-")
    .joinToString("") {
        it.replaceFirstChar(Char::uppercase)
    }


abstract class GenerateIconsTask : DefaultTask() {
    @Suppress("UNCHECKED_CAST")
    @TaskAction
    fun main() {
        println("Reading metadata...")
        val metadata = File("${project.rootDir}/Font-Awesome/metadata/icons.json")
        val json = JsonSlurper().parseText(metadata.readText()) as Map<String, Map<String, Any>>
        val icons = json.keys
            .map {
                IconData(
                    unicode = "0x${json[it]?.get("unicode")}",
                    name = it.kebabToPascalCase(),
                    originalName = it,
                    styles = json[it]?.get("styles") as List<String>
                )
            }
        val iconsCodes = icons.joinToString("\n\n") {
            generateIconCode(it)
        }
        val iconsMapCodes = generateIconMap(icons)

        writeCode(iconsCodes + "\n\n" + iconsMapCodes)
        println("Successfully generated FaIcons.kt")

        println("Copying fonts to res folder...")
        moveFonts()
        println("Done!")
    }

    private fun generateIconCode(icon: IconData): String {
        val lines = mutableListOf<String>()

        if (icon.styles.contains("solid")) {
            lines.add(
                """
                |    // https://fontawesome.com/icons/${icon.originalName}?style=solid
                |    // Solid icon : ${icon.name}
                |    val ${icon.propertyName} = SolidIcon(${icon.unicode})
                """.trimMargin()
            )
        } else if (icon.styles.contains("brands")) {
            lines.add(
                """
                |    // https://fontawesome.com/icons/${icon.originalName}?style=brands
                |    // Brands icon : ${icon.name}
                |    val ${icon.propertyName} = BrandIcon(${icon.unicode})
                """.trimMargin()
            )
        }
        if (icon.styles.contains("regular")) {
            lines.add(
                """
                |    // https://fontawesome.com/icons/${icon.originalName}?style=regular
                |    // Brands icon : ${icon.name}
                |    val ${icon.propertyName}Regular = RegularIcon(${icon.unicode})
                """.trimMargin()
            )
        }

        return lines.joinToString("\n\n")
    }

    private fun generateIconMap(icons: List<IconData>): String {
        return icons
            .fold(
                mutableMapOf<String, MutableList<IconData>>(
                    "solid" to mutableListOf(),
                    "brands" to mutableListOf(),
                    "regular" to mutableListOf()
                )
            ) { acc, icon ->
                icon.styles.forEach {
                    acc[it]?.add(icon)
                }
                acc
            }
            .entries
            .joinToString("\n\n") { (style, iconList) ->
                val suffix = if (style == "regular") "Regular" else ""
                val iconMap = iconList.joinToString(",\n") {
                    "        \"fa-${it.originalName}\" to ${it.propertyName}$suffix"
                }

                """
                |    private val ${style}IconMap: Map<String, FaIconType> = mapOf(
                |$iconMap
                |    )
                """.trimMargin()
            }
    }

    private fun writeCode(lines: String) {
        val destination =
            File("${project.rootDir}/FontAwesomeComposeLib/src/main/java/com/guru/fontawesomecomposelib/FaIcons.kt")
        val template =
            File("${project.rootDir}/buildSrc/src/main/kotlin/FaIcons.kt.template").readText()


        destination.writeText(template.replace("{{ ICONS }}", lines))
    }

    private fun moveFonts() {
        val ttfDir = "${project.rootDir}/Font-Awesome/webfonts"
        val fontDestinationDir = "${project.rootDir}/FontAwesomeComposeLib/src/main/res/font"
        val brandsFont =
            File("${ttfDir}/fa-brands-400.ttf")
        val brandsFontDestination =
            File("${fontDestinationDir}/fa_brands_400.ttf")
        brandsFont.copyTo(brandsFontDestination, overwrite = true)

        val regularFont =
            File("${ttfDir}/fa-regular-400.ttf")
        val regularFontDestination =
            File("${fontDestinationDir}/fa_regular_400.ttf")
        regularFont.copyTo(regularFontDestination, overwrite = true)

        val solidFont =
            File("${ttfDir}/fa-solid-900.ttf")
        val solidFontDestination =
            File("${fontDestinationDir}/fa_solid_900.ttf")
        solidFont.copyTo(solidFontDestination, overwrite = true)
    }
}