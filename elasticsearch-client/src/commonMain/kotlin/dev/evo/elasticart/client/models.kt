package dev.evo.elasticart.client

import kotlinx.serialization.Decoder
import kotlinx.serialization.Encoder
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.Serializer
import kotlinx.serialization.internal.HashMapClassDesc
import kotlinx.serialization.internal.StringDescriptor
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonInput
import kotlinx.serialization.json.JsonObject

fun Decoder.asJsonInput(): JsonInput {
    return this as? JsonInput
        ?: throw SerializationException("This class can be loaded only by Json")
}

fun JsonInput.decodeJsonObject(): JsonObject {
    return this.asJsonInput().decodeJson() as? JsonObject
        ?: throw SerializationException("Expected ${JsonObject::class} but was ${this::class}")
}

@Serializable
data class ClusterInfo(
    val name: String,
    @SerialName("cluster_name")
    val clusterName: String,
    @SerialName("cluster_uuid")
    val clusterUuid: String,
    val version: Version,
    val tagline: String
) {
    @Serializable
    data class Version(
        val number: String,
        @SerialName("build_hash")
        val buildHash: String,
        @SerialName("build_date")
        val buildDate: String?,
        @SerialName("build_snapshot")
        val buildSnapshot: String,
        @SerialName("lucene_version")
        val luceneVersion: String
    )
}

@Serializable
data class Aliases(
    private val indexes: Map<String, IndexAliases>
) : Map<String, Aliases.IndexAliases> by indexes {
    @Serializable
    data class IndexAliases(
        val aliases: Map<String, JsonObject>
    )

    @Serializer(forClass = Aliases::class)
    companion object : KSerializer<Aliases> {
        override fun deserialize(decoder: Decoder): Aliases {
            val input = decoder.asJsonInput()
            val tree = input.decodeJsonObject()
            val indexes = mutableMapOf<String, IndexAliases>()
            for ((index, aliases) in tree) {
                indexes[index] = input.json.fromJson(IndexAliases.serializer(), aliases)
            }
            return Aliases(indexes)
        }

        override fun serialize(encoder: Encoder, obj: Aliases) {
            TODO("not implemented")
        }
    }
}
