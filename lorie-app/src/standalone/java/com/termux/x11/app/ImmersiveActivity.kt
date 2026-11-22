package com.termux.x11.app

import com.meta.spatial.core.Entity
import com.meta.spatial.core.Pose
import com.meta.spatial.core.SpatialFeature
import com.meta.spatial.core.Vector3
import com.meta.spatial.isdk.IsdkPanelResize
import com.meta.spatial.isdk.ResizeMode
import com.meta.spatial.runtime.ReferenceSpace
import com.meta.spatial.toolkit.ActivityPanelRegistration
import com.meta.spatial.toolkit.AppSystemActivity
import com.meta.spatial.toolkit.DpDisplayOptions
import com.meta.spatial.toolkit.Grabbable
import com.meta.spatial.toolkit.Panel
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.vr.VRFeature
import com.termux.x11.MainActivity

class ImmersiveActivity : AppSystemActivity() {
    override fun registerFeatures(): List<SpatialFeature> = listOf(VRFeature(this))

    override fun registerPanels(): List<PanelRegistration> = listOf(
        ActivityPanelRegistration(
            R.id.desktop_panel,
            classIdCreator = { MainActivity::class.java },
            settingsCreator = {
                UIPanelSettings(
                    shape = QuadShapeOptions(width = 1.6f, height = 0.96f),
                    display = DpDisplayOptions(width = 1280f, height = 768f),
                )
            },
        ),
    )

    override fun onSceneReady() {
        super.onSceneReady()
        scene.enablePassthrough(true)
        scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
        Entity.create(
            listOf(
                Panel(R.id.desktop_panel),
                Transform(Pose(Vector3(0f, 1.3f, 1.5f))),
                Grabbable(),
                IsdkPanelResize(resizeMode = ResizeMode.Relayout),
            ),
        )
    }
}
