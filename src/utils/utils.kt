package utils

import java.io.File
import java.io.FileInputStream
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

fun readFile(file: String, progressIndicator: ProgressIndicator = ProgressIndicator.empty): List<String> {
    //check for interruption via Thread.interrupt() manually because
    //Files.ReadAllText should support interruption (uses nio channels)
    //but does not because InputStream.read() swallows InterruptedIOException : IOException
    //:-(
    progressIndicator.setMax(File(file).length())
    val input = FileInputStream(file)
    val result = StringBuilder()
    try {
        val byteBuffer = ByteArray(500000)
        var read: Int
        while (true) {
            Thread.currentThread().throwIfInterrupted()
            read = input.read(byteBuffer)
            if (read > 0) {
                progressIndicator.report(progressIndicator.value + read)
                result.append(String(byteBuffer, 0, read))
            } else break
        }
    } finally {
        input.close()
        progressIndicator.done()
    }
    return result.split("\n", "\r\n")
}


fun <T> time(func: () -> T, description: String): T {
    val startTime = System.nanoTime()
    val result = func()
    val endTime = System.nanoTime()
    println("$description in ${(endTime - startTime) / 1000000} ms")
    return result
}

fun splitByCount(content: String, maxLength: Int): List<String> {
    var startIndex = 0
    val result = arrayListOf<String>()
    while (startIndex < content.length) {
        if (startIndex + maxLength <= content.length)
            result.add(content.substring(startIndex, startIndex + maxLength))
        else
            result.add(content.substring(startIndex))
        startIndex += maxLength
    }
    return result
}


fun Thread.throwIfInterrupted() {
    if (this.isInterrupted)
        throw InterruptedException()
}
