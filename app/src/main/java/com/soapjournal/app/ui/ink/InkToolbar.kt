package com.soapjournal.app.ui.ink

/**
 * Ink interaction helpers shared by the SOAP writing surfaces.
 * Undo/redo stacks live in [com.soapjournal.app.ui.editor.EntryEditorViewModel].
 */
object InkDefaults {
    const val DefaultStrokeWidth = 4f
    const val MinStrokeWidth = 2f
    const val MaxStrokeWidth = 14f
    const val MinCanvasHeightDp = 1200
}
