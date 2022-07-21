package re.notifica

/**
 * Marks declarations that are **internal** in the Notificare API, which means that should not be used outside of
 * `re.notifica` packages, because their signatures and semantics will change between future releases without any
 * warnings and without providing any migration aids.
 */
@Retention(value = AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPEALIAS, AnnotationTarget.PROPERTY)
@RequiresOptIn(
    level = RequiresOptIn.Level.ERROR,
    message = "This is an internal Notificare API that should not be used from outside of the re.notifica " +
        "library group. No compatibility guarantees are provided. It is recommended to report your use-case of an " +
        "internal API to the Notificare issue tracker, so a stable API could be provided instead."
)
public annotation class InternalNotificareApi
