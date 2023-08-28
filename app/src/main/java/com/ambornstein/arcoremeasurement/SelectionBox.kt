package com.ambornstein.arcoremeasurement

import android.util.Log
import android.view.MotionEvent
import com.google.ar.core.Pose
import com.google.ar.sceneform.HitTestResult
import com.google.ar.sceneform.Node
import com.google.ar.sceneform.math.Quaternion
import com.google.ar.sceneform.math.Vector3
import com.google.ar.sceneform.rendering.Material
import com.google.ar.sceneform.rendering.MaterialFactory
import com.google.ar.sceneform.rendering.ModelRenderable
import com.google.ar.sceneform.rendering.ShapeFactory
import com.google.ar.sceneform.ux.TransformableNode
import com.ambornstein.arcoremeasurement.Measurement.Companion.arFragment
import com.ambornstein.arcoremeasurement.Util.Companion.floatToVec3
import com.ambornstein.arcoremeasurement.Util.Companion.vec3ToFloat
import com.google.ar.core.Plane
import com.google.ar.sceneform.rendering.Color
import java.util.Vector

class SelectionBox
    (corner1: CornerAnchorNode, corner2: CornerAnchorNode, cornerRenderable: ModelRenderable, plane: Plane){
    private lateinit var topLeftCorner: CornerAnchorNode
    private lateinit var topRightCorner: CornerAnchorNode
    private lateinit var bottomLeftCorner: CornerAnchorNode
    private lateinit var bottomRightCorner: CornerAnchorNode
    private val defaultQuaternion: FloatArray = floatArrayOf(0f,0f,0f,0f)
    private val cornerRenderable: ModelRenderable?
    private var selectedPlane: Plane
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
        selectedPlane = plane
        val xAxis = Vector3.cross(floatToVec3(selectedPlane.centerPose.zAxis)!!,Vector3.back())
        val zAxis = Vector3.cross(floatToVec3(selectedPlane.centerPose.yAxis)!!,Vector3.right())
        if (corner1.worldPosition.x < corner2.worldPosition.x && corner1.worldPosition.z < corner2.worldPosition.z) { // x1 < x2 ; z1 < z2
            bottomLeftCorner = corner1//- 2
            topRightCorner = corner2//  1 -
            topLeftCorner = makeIntersection(corner1, corner2, zAxis)
            bottomRightCorner = makeIntersection(corner2, corner1, zAxis)
        }
        else if (corner1.worldPosition.x > corner2.worldPosition.x && corner1.worldPosition.z > corner2.worldPosition.z) { // x1 > x2 ; z1 > z2
            topRightCorner = corner1//  - 1
            bottomLeftCorner = corner2//2 -
            bottomRightCorner = makeIntersection(corner1, corner2, zAxis)
            topLeftCorner = makeIntersection(corner2, corner1, zAxis)
        }
        else if (corner1.worldPosition.x < corner2.worldPosition.x && corner1.worldPosition.z > corner2.worldPosition.z) { // x1 < x2 ; z1 > z2
            topLeftCorner = corner1//    1 -
            bottomRightCorner = corner2//- 2
            bottomLeftCorner = makeIntersection(corner1, corner2, zAxis)
            topRightCorner = makeIntersection(corner2, corner1, zAxis)
        }
        else if (corner1.worldPosition.x > corner2.worldPosition.x && corner1.worldPosition.z < corner2.worldPosition.z) { // x1 > x2 ; z1 < z2
            bottomRightCorner = corner1//2 -
            topLeftCorner = corner2//    - 1
            topRightCorner = makeIntersection(corner1, corner2, zAxis)
            bottomLeftCorner = makeIntersection(corner2, corner1, zAxis)
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
    private fun makeIntersection(corner1: CornerAnchorNode, corner2: CornerAnchorNode, axis: Vector3) : CornerAnchorNode {
        val length = Vector3.subtract(corner2.worldPosition,corner1.worldPosition)
        val adjustment = axis.scaled(Vector3.dot(axis, length))
        val position = Vector3.add(corner1.worldPosition, adjustment)
        val anchor = arFragment!!.arSceneView.session!!.createAnchor(Pose(vec3ToFloat(position),defaultQuaternion))
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

    private fun lineBetweenPoints (
        anchorNode: CornerAnchorNode?,
        lookRotation: Quaternion,
        distanceMeters: Float) {
        MaterialFactory.makeOpaqueWithColor(arFragment!!.context, Color(1f,1f,1f))
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
        width = kotlin.math.abs(Vector3.subtract(topLeftCorner.worldPosition, topRightCorner.worldPosition).length())
        height = kotlin.math.abs(Vector3.subtract(topLeftCorner.worldPosition, bottomLeftCorner.worldPosition).length())
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