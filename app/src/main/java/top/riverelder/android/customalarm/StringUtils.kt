package top.riverelder.android.customalarm

import java.lang.Exception
import java.lang.Integer.parseInt

val REGEX = Regex("\\$\\{(\\w+)?\\}")

fun String.formatBy(map: Map<String, Any>): String = formatBy(
    { map.getOrDefault(it, null) },
    if (map.isNotEmpty()) map.entries.first().value else null,
)

fun String.formatBy(list: List<Any>): String = formatBy(
    { try { parseInt(it) } catch (ignored: Exception) { null } },
    if (list.isNotEmpty()) list.first() else null,
)

fun String.formatBy(argument: Any): String =
    formatBy({ if (it == "0") argument else null }, argument)

fun String.formatBy(getArg: (String) -> Any?, defaultArg: Any?): String {
    var lastIndex = 0
    val builder = StringBuilder()
    for (piece in REGEX.findAll(this, 0)) {
        builder.append(substring(lastIndex, piece.range.first))
        lastIndex = piece.range.last + 1

        val group = piece.groups[1]
        if (group == null) {
            builder.append(defaultArg ?: piece.value)
            continue
        }
        builder.append(getArg(group.value) ?: piece.value)
    }
    builder.append(substring(lastIndex))
    return builder.toString()
}