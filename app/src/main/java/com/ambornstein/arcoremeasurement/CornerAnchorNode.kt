package com.ambornstein.arcoremeasurement

import android.view.MotionEvent
import com.google.ar.core.Anchor
import com.google.ar.sceneform.AnchorNode
import com.ambornstein.arcoremeasurement.Measurement.Companion.arFragment
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene

class CornerAnchorNode(anchor: Anchor) : AnchorNode(anchor) , Node.OnTouchListener{
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
    override fun onTouch(p0: HitTestResult?, p1: MotionEvent?): Boolean {
        if (p1 != null) {
            if (p1.action == MotionEvent.ACTION_UP) {
                align()
                return true
            }
        }
        return false
    }
}