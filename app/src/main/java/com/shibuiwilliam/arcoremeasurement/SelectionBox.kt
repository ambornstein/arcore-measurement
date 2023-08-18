package com.shibuiwilliam.arcoremeasurement

import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.Pose
import com.google.ar.sceneform.AnchorNode
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.Scene
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.Renderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.DragGesture
import com.google.ar.sceneform.ux.TransformableNode
import com.shibuiwilliam.arcoremeasurement.Measurement.Companion.arFragment
import java.lang.Math.abs

class SelectionBox
    (corner1: CornerAnchorNode, corner2: CornerAnchorNode, cornerRenderable: ModelRenderable){
    private lateinit var topLeftCorner: CornerAnchorNode
    private lateinit var topRightCorner: CornerAnchorNode
    private lateinit var bottomLeftCorner: CornerAnchorNode
    private lateinit var bottomRightCorner: CornerAnchorNode
    private val defaultQuaternion: FloatArray = floatArrayOf(0f,0f,0f,0f)
    private val cornerRenderable: ModelRenderable?
    var width: Float = 0f
    var height: Float = 0f
    private var areaAdded: Boolean = true

    /** ^
     *  |
     *  z
     *  y x - >
     * This constructor assigns opposing [CornerAnchorNode]s
     *
     */
    init {
        this.cornerRenderable = cornerRenderable
        if (corner1.worldPosition.x < corner2.worldPosition.x && corner1.worldPosition.z < corner2.worldPosition.z) { // x1 < x2 ; z1 < z2
            bottomLeftCorner = corner1//- 2
            topRightCorner = corner2//  1 -
            topLeftCorner = makeIntersection(corner1, corner2)
            bottomRightCorner = makeIntersection(corner2, corner1)
        }
        else if (corner1.worldPosition.x > corner2.worldPosition.x && corner1.worldPosition.z > corner2.worldPosition.z) { // x1 > x2 ; z1 > z2
            topRightCorner = corner1//  - 1
            bottomLeftCorner = corner2//2 -
            bottomRightCorner = makeIntersection(corner1, corner2)
            topLeftCorner = makeIntersection(corner2, corner1)
        }
        else if (corner1.worldPosition.x < corner2.worldPosition.x && corner1.worldPosition.z > corner2.worldPosition.z) { // x1 < x2 ; z1 > z2
            topLeftCorner = corner1//    1 -
            bottomRightCorner = corner2//- 2
            bottomLeftCorner = makeIntersection(corner1, corner2)
            topRightCorner = makeIntersection(corner2, corner1)
        }
        else if (corner1.worldPosition.x > corner2.worldPosition.x && corner1.worldPosition.z < corner2.worldPosition.z) { // x1 > x2 ; z1 < z2
            bottomRightCorner = corner1//2 -
            topLeftCorner = corner2//    - 1
            topRightCorner = makeIntersection(corner1, corner2)
            bottomLeftCorner = makeIntersection(corner2, corner1)
        }
        bottomLeftCorner.setHoriz(bottomRightCorner)
        topLeftCorner.setVert(bottomLeftCorner)
        topRightCorner.setHoriz(topLeftCorner)
        bottomRightCorner.setVert(topRightCorner)
        createTransformableNode(bottomRightCorner)
        createTransformableNode(bottomLeftCorner)
        createTransformableNode(topLeftCorner)
        createTransformableNode(topRightCorner)
        updateDimensions()
        drawLines()
    }

    /**
     * Creates a new [CornerAnchorNode] at the x value of corner1 and the z value of corner2. This point will be orthogonal to both points in order to construct a rectangular network of points.
     */
    private fun makeIntersection(corner1: CornerAnchorNode, corner2: CornerAnchorNode) : CornerAnchorNode {
        val anchor = arFragment!!.arSceneView.session!!.createAnchor(Pose(floatArrayOf(corner1.worldPosition.x,corner1.worldPosition.y,corner2.worldPosition.z),defaultQuaternion))
        return CornerAnchorNode(anchor)
    }

    private fun createTransformableNode(cornerAnchorNode: CornerAnchorNode) {
        val node = TransformableNode(arFragment!!.transformationSystem)
            .apply {
                this.rotationController.isEnabled = false
                this.scaleController.isEnabled = false
                this.translationController.isEnabled = true
                this.renderable = cornerRenderable
                this.setOnTouchListener { hitTestResult: HitTestResult?, motionEvent: MotionEvent? ->
                    if (motionEvent!!.action == MotionEvent.ACTION_UP) {
                        cornerAnchorNode.align()
                        return@setOnTouchListener true
                    } else return@setOnTouchListener false
                }
                setParent(cornerAnchorNode)
            }
    }

    private fun lineBetweenPoints(
        anchorNode: CornerAnchorNode?,
        lookRotation: Quaternion,
        distanceMeters: Float) {
        MaterialFactory.makeOpaqueWithColor(arFragment!!.context, com.google.ar.sceneform.rendering.Color(1f,1f,1f))
            .thenAccept { material: Material? ->
                Log.d("lineBetweenPoints", "distance: $distanceMeters")
                val size = Vector3(.01f, .01f, distanceMeters)
                val center = Vector3(.01f / 2, .01f / 2, distanceMeters / 2)
                val cube = ShapeFactory.makeCube(size, center, material)
                val lineNode = Node()
                lineNode.setParent(anchorNode)
                lineNode.renderable = cube
                lineNode.worldRotation = lookRotation
            }
    }

    private fun drawLines() {
        lineBetweenPoints(topLeftCorner, Quaternion.lookRotation(Vector3.subtract(topLeftCorner.worldPosition,topRightCorner.worldPosition), Vector3.up()), width)
        lineBetweenPoints(topLeftCorner, Quaternion.lookRotation(Vector3.subtract(topLeftCorner.worldPosition,bottomLeftCorner.worldPosition), Vector3.up()), height)
        lineBetweenPoints(bottomRightCorner, Quaternion.lookRotation(Vector3.subtract(bottomRightCorner.worldPosition,bottomLeftCorner.worldPosition), Vector3.up()), width)
        lineBetweenPoints(bottomRightCorner, Quaternion.lookRotation(Vector3.subtract(bottomRightCorner.worldPosition,topRightCorner.worldPosition), Vector3.up()), height)
    }

    private fun updateDimensions() {
        width =
            kotlin.math.abs(topLeftCorner.worldPosition.x - topRightCorner.worldPosition.x)
        height = kotlin.math.abs(topLeftCorner.worldPosition.z - bottomLeftCorner.worldPosition.z)
    }

    fun centerPoint() : FloatArray{
        return floatArrayOf((topLeftCorner.worldPosition.x + bottomRightCorner.worldPosition.x)/2,
            (topLeftCorner.worldPosition.y + bottomRightCorner.worldPosition.y)/2,
        (topLeftCorner.worldPosition.z + bottomRightCorner.worldPosition.z)/2)
    }

    fun clear() {
        arFragment!!.arSceneView.scene.removeChild(topLeftCorner)
        topLeftCorner.unlink()
        topLeftCorner.isEnabled = false
        topLeftCorner.anchor!!.detach()
        topLeftCorner.setParent(null)

        arFragment!!.arSceneView.scene.removeChild(topRightCorner)
        topRightCorner.unlink()
        topRightCorner.isEnabled = false
        topRightCorner.anchor!!.detach()
        topRightCorner.setParent(null)

        arFragment!!.arSceneView.scene.removeChild(bottomLeftCorner)
        bottomLeftCorner.unlink()
        bottomLeftCorner.isEnabled = false
        bottomLeftCorner.anchor!!.detach()
        bottomLeftCorner.setParent(null)

        arFragment!!.arSceneView.scene.removeChild(bottomRightCorner)
        bottomRightCorner.unlink()
        bottomRightCorner.isEnabled = false
        bottomRightCorner.anchor!!.detach()
        bottomRightCorner.setParent(null)
    }
}