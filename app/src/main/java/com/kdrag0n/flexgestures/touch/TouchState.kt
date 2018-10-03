package com.kdrag0n.flexgestures.touch

enum class TouchState {
    // finger is not in gesture area or is not on the screen
    NONE,

    // finger just entered gesture area
    DOWN,

    // finger is moving but has not passed gesture threshold
    SWIPE_BEGIN,

    // finger has moved past gesture threshold, causing screenshot
    SWIPE,
}