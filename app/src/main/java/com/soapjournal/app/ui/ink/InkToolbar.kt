package com.soapjournal.app.ui.ink

/**
 * Ink interaction helpers shared by the SOAP writing surfaces.
 * Undo/redo stacks live in [com.soapjournal.app.ui.editor.EntryEditorViewModel].
 */
object InkDefaults {
    const val DefaultStrokeWidth = 4f
    const val MinStrokeWidth = 2f
    const val MaxStrokeWidth = 14f
    /** One writing viewport; the page grows as ink reaches the bottom. */
    const val MinCanvasHeightDp = 680
    /** Extra paper when the user taps “Add writing space”. */
    const val PageExtendPx = 1400f
    /** Keep this much blank paper below the lowest stroke while writing. */
    const val AutoGrowPaddingPx = 360f
}
