package com.termux.x11.app

import com.meta.spatial.core.Pose
import com.meta.spatial.core.SystemBase
import com.meta.spatial.core.Vector3

class ViewerReadySystem(private val onViewerReady: (Pose) -> Unit) : SystemBase() {
    private var viewerFound = false

    override fun execute() {
        if (viewerFound) return
        val viewerPose = getScene().getViewerPose()
        val viewerNotTrackedYet = viewerPose.t == Vector3(0f)
        if (viewerNotTrackedYet) return
        viewerFound = true
        onViewerReady(viewerPose)
    }
}
