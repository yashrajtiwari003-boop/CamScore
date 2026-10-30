package com.fischerlabs.camscore.platform

import platform.UIKit.UIViewController

/**
 * iOS PlatformContext wrapping the active UIViewController.
 */
actual class PlatformContext(
    val viewController: UIViewController? = null
)
