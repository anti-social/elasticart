package dev.evo.elasticart.transport

import java.io.ByteArrayOutputStream
import java.util.zip.GZIPOutputStream

actual class GzipEncoder : RequestEncoder {
    private val buf = ByteArrayOutputStream()
    private val gzipStream = GZIPOutputStream(buf)

    override fun append(value: CharSequence?): Appendable {
        if (value == null) {
            return this
        }
        gzipStream.write(value.toString().toByteArray(Charsets.UTF_8))
        return this
    }

    override fun toByteArray(): ByteArray {
        gzipStream.finish()
        return buf.toByteArray()
    }
}