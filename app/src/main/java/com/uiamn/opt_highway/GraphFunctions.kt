package com.uiamn.opt_highway

import android.app.Activity
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class GraphFunctions(activity: Activity) {
    private var jsonObject: JSONObject

    init {
        val inputStream = activity.resources.assets.open("joints_graph.json")
        val br = BufferedReader(InputStreamReader(inputStream))
        val jsonText = br.readText()
        jsonObject = JSONObject(jsonText)
    }

    data class NeighborNode(val name: String, val dist: Double)

    private fun getNeighborNodes(name: String): ArrayList<NeighborNode> {
        val neighborJSONArray = jsonObject.getJSONArray(name)
        var neighborNodes = arrayListOf<NeighborNode>()

        for(i in 0 until neighborJSONArray.length()) {
            val neighborJSONObject = neighborJSONArray.getJSONObject(i)
            val name = neighborJSONObject.getString("name")
            val dist = neighborJSONObject.getDouble("distance")
            neighborNodes.add(NeighborNode(name, dist))
        }

        return neighborNodes
    }

    data class Node(val name: String, var mindist: Double, var preName: String?)

    private fun obtainNextNodeIndexInCandidate(candidate: ArrayList<Node>): Int {
        var minimumDistance = Double.MAX_VALUE
        var nextNodeIndex = 0

        candidate.withIndex().forEach { (i, it) -> if(it.mindist < minimumDistance) {
            minimumDistance = it.mindist
            nextNodeIndex = i
        } }

        return nextNodeIndex
    }

    fun searchMinimumPath(start: String, goal: String): ArrayList<String> {
        var dijkstra = mutableMapOf<String, Node>()

        // 初期化
        for(key in jsonObject.keys()) {
            dijkstra[key] = Node(key, Double.MAX_VALUE, null)
        }

        // Dijkstraする
        var candidate = arrayListOf<Node>()
        var alreadyVisited = arrayListOf<String>()
        dijkstra[start]!!.mindist = 0.0
        dijkstra[start]?.let { candidate.add(it) }

        while(candidate.isNotEmpty()) {
            val index = obtainNextNodeIndexInCandidate(candidate)
            val nextNode = candidate[index]

            if(nextNode.name == goal) break

            val nextNodeDistance = nextNode.mindist

            val neighborNodes = getNeighborNodes(nextNode!!.name)
            neighborNodes.forEach { it ->
                run {
                    if (!alreadyVisited.contains(it.name) && dijkstra[it.name]!!.mindist > nextNodeDistance + it.dist) {
                        dijkstra[it.name]!!.mindist = nextNodeDistance + it.dist
                        dijkstra[it.name]!!.preName = nextNode.name
                        dijkstra[it.name]?.let { it1 -> candidate.add(it1) }
                    }
                }
            }

            alreadyVisited.add(nextNode.name)
            candidate.removeAt(index)
        }

        var node = dijkstra[goal]
        if(node!!.preName == null) return arrayListOf<String>()

        var resultICNames = arrayListOf(goal)
        while(node!!.name != start) {
            node = dijkstra[node.preName]
            if(!node!!.name.endsWith("JCT")) resultICNames.add(node!!.name)
        }

        resultICNames.reverse()

        return resultICNames
    }
}
