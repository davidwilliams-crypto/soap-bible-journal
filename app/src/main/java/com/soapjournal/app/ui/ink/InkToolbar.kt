package com.soapjournal.app.ui.ink

/**
 * Ink interaction helpers shared by the SOAP writing surfaces.
 * Undo/redo stacks live in [com.soapjournal.app.ui.editor.EntryEditorViewModel].
 */
object InkDefaults {
    const val DefaultStrokeWidth = 4f
    const val MinStrokeWidth = 2f
    const val MaxStrokeWidth = 14f
    /** Taller default page so Observation/Application/Prayer have room to write. */
    const val MinCanvasHeightDp = 2200
    /** Extra blank page when the user taps “Add writing space” (~900dp at xxhdpi). */
    const val PageExtendPx = 2700f
    /** Keep this much blank paper below the lowest stroke while writing. */
    const val AutoGrowPaddingPx = 520f
}
