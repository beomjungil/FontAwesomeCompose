package com.guru.fontawesomecomposelib

sealed class FaIconType(val src: Int) {
    class SolidIcon(icon: Int) : FaIconType(icon)

    class RegularIcon(icon: Int) : FaIconType(icon)

    class BrandIcon(icon: Int) : FaIconType(icon)
}
