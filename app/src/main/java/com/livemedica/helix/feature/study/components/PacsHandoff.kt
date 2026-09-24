package com.livemedica.helix.feature.study.components

import com.livemedica.helix.domain.model.Study

/**
 * Seam for the PACS / DICOM viewer hand-off.
 *
 * ── What this is ──────────────────────────────────────────────────────────────────────────────
 * "Open images" is the one action on a study that leaves Helix. In production it resolves a
 * [Study.studyInstanceUid] to a viewer — an installed DICOM app via an `Intent`, an OHIF/Weasis web
 * viewer via a deep link, or the site's own vendor client — and the target differs per deployment.
 *
 * ── Why it is empty ───────────────────────────────────────────────────────────────────────────
 * No viewer endpoint has been supplied, and inventing one would bake a fictional host into the app
 * and make "Open images" look wired when it is not. So [resolve] returns `null`, the button renders
 * disabled with an honest explanation, and nothing about the surrounding screen has to change when
 * a real resolver arrives.
 *
 * ── How to fill it in ─────────────────────────────────────────────────────────────────────────
 * Replace [resolve] with a resolver injected from configuration (facility → viewer base URL, or an
 * installed-package check), returning a [PacsTarget]. The caller in `StudyActions` already handles
 * both outcomes; only this object and its injection point should need to change.
 */
object PacsHandoff {

    /** Where the viewer for a study lives, once a deployment has told us. */
    data class PacsTarget(val label: String, val uri: String)

    /** Returns `null` until a viewer is configured for this deployment. */
    @Suppress("UNUSED_PARAMETER")
    fun resolve(study: Study?): PacsTarget? = null

    /** Shown in place of the action while [resolve] has nothing to offer. */
    const val UNAVAILABLE_REASON = "No imaging viewer is configured for this facility yet."
}
