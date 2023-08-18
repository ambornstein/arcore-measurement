package com.shibuiwilliam.arcoremeasurement

import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.FrameTime
import com.google.ar.sceneform.Scene
import com.shibuiwilliam.arcoremeasurement.Measurement.Companion.arFragment

class CornerAnchorNode(anchor: Anchor) : AnchorNode(anchor) {
    private var vertNeighbor: CornerAnchorNode? = null
    private var horizNeighbor: CornerAnchorNode? = null

    init {
        AnchorNode(anchor).apply {
            isSmoothed = true
            setParent(arFragment!!.arSceneView.scene)
        }
        arFragment!!.arSceneView.scene.addChild(this)
        arFragment!!.arSceneView.scene.addOnUpdateListener {this}
    }

    fun align () {
        vertNeighbor!!.worldPosition.x = this.worldPosition.x
        horizNeighbor!!.worldPosition.y = this.worldPosition.y
    }
    fun setVert(anchor: CornerAnchorNode) {
        vertNeighbor = anchor
        anchor.vertNeighbor = this
    }
    fun setHoriz(anchor: CornerAnchorNode) {
        horizNeighbor = anchor
        anchor.horizNeighbor = this
    }

    fun unlink() {
        vertNeighbor?.vertNeighbor = null
        vertNeighbor = null
        horizNeighbor?.horizNeighbor = null
        horizNeighbor = null
    }

}