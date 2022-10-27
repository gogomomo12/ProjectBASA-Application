/***
 * Code from https://blog.cpming.top/p/httpurlconnection-multipart-form-data
 */

package project.basa

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.util.*

class HttpPostMultipart(
    requestURL: String?,
    private val charset: String,
    headers: Map<String?, String?>?
) {
    private val boundary: String
    private val httpConn: HttpURLConnection
    private val outputStream: OutputStream
    private val writer: PrintWriter

    /**
     * Adds a form field to the request
     *
     * @param name  field name
     * @param value field value
     */
    fun addFormField(name: String, value: String?) {
        writer.append("--$boundary").append(LINE)
        writer.append("Content-Disposition: form-data; name=\"$name\"").append(LINE)
        writer.append("Content-Type: text/plain; charset=$charset").append(LINE)
        writer.append(LINE)
        writer.append(value).append(LINE)
        writer.flush()
    }

    /**
     * Adds a upload file section to the request
     *
     * @param fieldName
     * @param uploadFile
     * @throws IOException
     */
    @Throws(IOException::class)
    fun addFilePart(fieldName: String, uploadFile: File) {
        val fileName: String = uploadFile.getName()
        writer.append("--$boundary").append(LINE)
        writer.append("Content-Disposition: form-data; name=\"$fieldName\"; filename=\"$fileName\"")
            .append(
                LINE
            )
        writer.append("Content-Type: " + URLConnection.guessContentTypeFromName(fileName)).append(
            LINE
        )
        writer.append("Content-Transfer-Encoding: binary").append(LINE)
        writer.append(LINE)
        writer.flush()
        val inputStream = FileInputStream(uploadFile)
        val buffer = ByteArray(4096)
        var bytesRead = -1
        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            outputStream.write(buffer, 0, bytesRead)
        }
        outputStream.flush()
        inputStream.close()
        writer.append(LINE)
        writer.flush()
    }

    /**
     * Completes the request and receives response from the server.
     *
     * @return String as response in case the server returned
     * status OK, otherwise an exception is thrown.
     * @throws IOException
     */
    @Throws(IOException::class)
    fun finish(): String {
        var response = ""
        writer.flush()
        writer.append("--$boundary--").append(LINE)
        writer.close()

        // checks server's status code first
        val status: Int = httpConn.getResponseCode()
        if (status == HttpURLConnection.HTTP_OK) {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            var length: Int
            while (httpConn.inputStream.read(buffer).also { length = it } != -1) {
                result.write(buffer, 0, length)
            }
            response = result.toString(charset)
            httpConn.disconnect()
        } else {
            throw IOException("Server returned non-OK status: $status")
        }
        return response
    }

    companion object {
        private const val LINE = "\r\n"
    }

    /**
     * This constructor initializes a new HTTP POST request with content type
     * is set to multipart/form-data
     *
     * @param requestURL
     * @param charset
     * @param headers
     * @throws IOException
     */
    init {
        boundary = UUID.randomUUID().toString()
        val url = URL(requestURL)
        httpConn = url.openConnection() as HttpURLConnection
        httpConn.useCaches = false
        httpConn.doOutput = true // indicates POST method
        httpConn.doInput = true
        httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        if (headers != null && headers.isNotEmpty()) {
            val it = headers.keys.iterator()
            while (it.hasNext()) {
                val key = it.next()
                val value = headers[key]
                httpConn.setRequestProperty(key, value)
            }
        }
        outputStream = httpConn.outputStream
        writer = PrintWriter(OutputStreamWriter(outputStream, charset), true)
    }
}