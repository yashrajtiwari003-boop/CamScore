package com.fischerlabs.camscore.platform

import platform.UIKit.UIViewController

/**
 * iOS PlatformContext wrapping the active UIViewController.
 */
actual abstract class PlatformContext {
    open val viewController: UIViewController? = null
}

class IosPlatformContext(
    override val viewController: UIViewController? = null
) : PlatformContext()
