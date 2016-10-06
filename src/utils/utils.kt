package utils

import java.awt.GridBagConstraints
import java.io.FileInputStream
import java.io.InputStreamReader
import java.util.*

fun parseCommandLineArgs(args: List<String>): HashMap<String, Any> {
    var argName: String? = null
    val options = HashMap<String, Any>()
    for (arg in args) {
        if (arg.startsWith("-") || arg.startsWith("/")) {
            if (argName != null)
                options[argName] = true
            argName = arg.substring(1)
        } else if (argName == null)
            throw Exception("Invalid args. Expected argument name before string [$arg]")
        else {
            options[argName] = arg
            argName = null
        }
    }
    if (argName != null)
        options[argName] = true
    return options
}


fun parseIntOrNull(str: String?): Int? {
    if (str == null)
        return null
    try {
        return Integer.parseInt(str)
    } catch (e: NumberFormatException) {
        return null
    }
}

fun readFile(file: String): List<String> {
    val stream = InputStreamReader(FileInputStream(file))
    try {
        return stream.readLines()

    } finally {
        stream.close()
    }
}

fun getGridBagConstraints(configure: (GridBagConstraints) -> Unit): GridBagConstraints {
    val result = GridBagConstraints()
    configure(result)
    return result
}
