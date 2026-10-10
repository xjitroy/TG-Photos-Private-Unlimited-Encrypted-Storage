package app.tgphotos.core.testing

import app.tgphotos.core.model.MediaKind
import app.tgphotos.core.model.PreviewMedia

/** Original non-sensitive design fixtures, never cloud records. */
object PreviewCatalog {
    val media = listOf(
        PreviewMedia(1, "Quiet peaks", MediaKind.PHOTO, true),
        PreviewMedia(2, "Evening light", MediaKind.PHOTO),
        PreviewMedia(3, "Along the coast", MediaKind.VIDEO),
        PreviewMedia(4, "Morning layers", MediaKind.PHOTO),
        PreviewMedia(5, "Blue horizon", MediaKind.PHOTO, true),
        PreviewMedia(6, "A little escape", MediaKind.VIDEO),
        PreviewMedia(7, "Last light", MediaKind.PHOTO),
        PreviewMedia(8, "Wander slowly", MediaKind.PHOTO),
        PreviewMedia(9, "Ocean air", MediaKind.PHOTO),
    )
}
