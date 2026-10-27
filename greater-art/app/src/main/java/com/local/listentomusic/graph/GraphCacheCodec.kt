package com.local.listentomusic.graph

import java.io.DataInputStream
import java.io.DataOutputStream
import java.security.MessageDigest

/** Version the key whenever similarity weights, candidate selection or layout changes. */
object GraphCacheCodec {
    fun fingerprint(nodes: List<GraphInput>): String {
        val hash = MessageDigest.getInstance("SHA-256")
        hash.update("graph-v1-k6-layout2".toByteArray())
        nodes.sortedBy { it.id }.forEach {
            hash.update(it.id.toByteArray()); hash.update(0.toByte())
            hash.update(it.filename.toByteArray()); hash.update(0.toByte())
        }
        return hash.digest().joinToString("") { "%02x".format(it) }
    }

    fun read(input: DataInputStream, key: String, nodes: List<GraphInput>): LibraryGraph {
        require(input.readUTF() == key && input.readInt() == nodes.size)
        val points = nodes.map { GraphPoint(input.readFloat(), input.readFloat()).also { require(it.x.isFinite() && it.y.isFinite()) } }
        val count = input.readInt(); require(count in 0..nodes.size * GraphBuilder.NEIGHBORS)
        val edges = List(count) {
            GraphEdge(input.readInt(), input.readInt(), input.readFloat()).also {
                require(it.a in nodes.indices && it.b in nodes.indices && it.a < it.b && it.strength > 0 && it.strength <= 1)
            }
        }
        return LibraryGraph(nodes, edges, points)
    }

    fun write(output: DataOutputStream, key: String, graph: LibraryGraph) {
        output.writeUTF(key); output.writeInt(graph.nodes.size)
        graph.points.forEach { output.writeFloat(it.x); output.writeFloat(it.y) }
        output.writeInt(graph.edges.size)
        graph.edges.forEach { output.writeInt(it.a); output.writeInt(it.b); output.writeFloat(it.strength) }
    }
}
