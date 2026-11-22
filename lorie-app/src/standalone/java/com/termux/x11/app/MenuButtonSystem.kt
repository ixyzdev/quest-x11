package com.termux.x11.app

import com.meta.spatial.core.Query
import com.meta.spatial.core.SystemBase
import com.meta.spatial.runtime.ButtonBits
import com.meta.spatial.toolkit.AvatarBody
import com.meta.spatial.toolkit.Controller

class MenuButtonSystem(private val onMenuPressed: () -> Unit) : SystemBase() {
    override fun execute() {
        val avatar = Query.where { has(AvatarBody.id) }
            .eval()
            .firstOrNull { it.isLocal() && it.getComponent<AvatarBody>().isPlayerControlled }
            ?: return
        val controller = avatar.getComponent<AvatarBody>().leftHand.tryGetComponent<Controller>() ?: return
        if ((controller.buttonState and controller.changedButtons and ButtonBits.ButtonMenu) != 0)
            onMenuPressed()
    }
}
