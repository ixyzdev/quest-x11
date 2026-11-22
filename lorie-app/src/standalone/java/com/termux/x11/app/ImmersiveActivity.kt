package com.termux.x11.app

import android.os.Bundle
import android.widget.Switch
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
import com.meta.spatial.toolkit.LayoutXMLPanelRegistration
import com.meta.spatial.toolkit.Panel
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.toolkit.Visible
import com.meta.spatial.vr.VRFeature
import com.termux.x11.MainActivity

class ImmersiveActivity : AppSystemActivity() {
    private var menu: Entity? = null
    private var menuVisible = false

    override fun registerFeatures(): List<SpatialFeature> = listOf(VRFeature(this))

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        systemManager.registerSystem(MenuButtonSystem(::toggleMenu))
    }

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
        LayoutXMLPanelRegistration(
            R.id.menu_panel,
            layoutIdCreator = { _ -> R.layout.immersive_menu },
            settingsCreator = { _ ->
                UIPanelSettings(
                    shape = QuadShapeOptions(width = 0.5f, height = 0.2f),
                    display = DpDisplayOptions(width = 500f, height = 200f),
                )
            },
            panelSetupWithRootView = { rootView, _, _ ->
                rootView.findViewById<Switch>(R.id.passthrough_switch)
                    ?.setOnCheckedChangeListener { _, checked -> scene.enablePassthrough(checked) }
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
        menu = Entity.create(
            listOf(
                Panel(R.id.menu_panel),
                Transform(Pose(Vector3(0f, 0.7f, 1.2f))),
                Grabbable(),
                Visible(false),
            ),
        )
    }

    private fun toggleMenu() {
        menuVisible = !menuVisible
        menu?.setComponent(Visible(menuVisible))
    }
}
