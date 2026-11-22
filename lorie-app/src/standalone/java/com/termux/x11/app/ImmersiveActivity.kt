package com.termux.x11.app

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.Switch
import com.meta.spatial.core.Color4
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
import com.meta.spatial.toolkit.Material
import com.meta.spatial.toolkit.Mesh
import com.meta.spatial.toolkit.MeshCollision
import com.meta.spatial.toolkit.Panel
import com.meta.spatial.toolkit.PanelRegistration
import com.meta.spatial.toolkit.QuadShapeOptions
import com.meta.spatial.toolkit.Transform
import com.meta.spatial.toolkit.UIPanelSettings
import com.meta.spatial.toolkit.Visible
import com.meta.spatial.vr.VRFeature
import com.termux.x11.MainActivity

class ImmersiveActivity : AppSystemActivity() {
    private var desktop: Entity? = null
    private var menu: Entity? = null
    private var skybox: Entity? = null
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
                    shape = QuadShapeOptions(width = 0.5f, height = 0.3f),
                    display = DpDisplayOptions(width = 500f, height = 300f),
                )
            },
            panelSetupWithRootView = { rootView, _, _ ->
                rootView.findViewById<Switch>(R.id.passthrough_switch)
                    ?.setOnCheckedChangeListener { _, checked -> showRoom(checked) }
                rootView.findViewById<Button>(R.id.desktop_window_button)
                    ?.setOnClickListener { openDesktopWindowInHome() }
            },
        ),
    )

    override fun onSceneReady() {
        super.onSceneReady()
        scene.setReferenceSpace(ReferenceSpace.LOCAL_FLOOR)
        skybox = Entity.create(
            listOf(
                Mesh(Uri.parse("mesh://skybox"), MeshCollision.NoCollision),
                Material().apply {
                    baseColor = Color4(0.05f, 0.07f, 0.12f, 1f)
                    unlit = true
                },
                Transform(Pose()),
            ),
        )
        desktop = Entity.create(
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
        systemManager.registerSystem(ViewerReadySystem(::placeInFrontOf))
    }

    private fun placeInFrontOf(viewerPose: Pose) {
        val viewerFacing = viewerPose.removePitchAndRoll()
        val desktopPosition = viewerFacing.t + viewerFacing.forward() * 1.5f
        val menuPosition = viewerFacing.t + viewerFacing.forward() * 1.0f + Vector3(0f, -0.5f, 0f)
        desktop?.setComponent(Transform(Pose(desktopPosition, viewerFacing.q)))
        menu?.setComponent(Transform(Pose(menuPosition, viewerFacing.q)))
    }

    private fun showRoom(roomVisible: Boolean) {
        scene.enablePassthrough(roomVisible)
        skybox?.setComponent(Visible(!roomVisible))
    }

    private fun toggleMenu() {
        menuVisible = !menuVisible
        menu?.setComponent(Visible(menuVisible))
    }

    private fun openDesktopWindowInHome() {
        val desktopWindowIntent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_MAIN
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val pendingDesktopWindow = PendingIntent.getActivity(
            this,
            0,
            desktopWindowIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val homeIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_HOME)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra("extra_launch_in_home_pending_intent", pendingDesktopWindow)
        startActivity(homeIntent)
        finishAndRemoveTask()
    }
}
