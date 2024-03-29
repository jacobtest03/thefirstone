package misk.resources

import com.google.common.collect.ImmutableSet
import misk.resources.ResourceLoader.Backend
import okio.BufferedSource
import okio.ByteString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ResourceLoader is a testable API for loading resources from the classpath, from the filesystem,
 * from memory, or from another [Backend] source.
 *
 * Resource addresses have a scheme name, a colon, and an absolute filesystem-like path:
 * `classpath:/migrations/v1.sql`. Schemes identify backends `classpath:` or `memory:`. Paths start
 * with a slash and have any number of segments.
 *
 * **Classpath resources** use the scheme `classpath:`. The backend reads data from the
 * `src/main/resources` of the project's modules and the contents of all library `.jar` files.
 * Classpath resources are read-only.
 *
 * **Filesystem resources** use the scheme `filesystem:`. The backend reads data from the host
 * machine's local filesystem. It is read-only and does not support [list].
 *
 * **Memory resources** use the scheme `memory:`. The backend starts empty and is populated by calls
 * to [put].
 *
 * Other backends are permitted. They should be registered with a `MapBinder` with the backend
 * scheme like `classpath:` as the key.
 */
@Singleton
class ResourceLoader @Inject constructor(
  @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
  private val backends: java.util.Map<String, Backend>
) {
  init {
    for (prefix in backends.keySet()) {
      require(prefix.matches(Regex("[^/:]+:")))
    }
  }

  /** Return a buffered source for `address`, or null if no such resource exists. */
  fun open(address: String): BufferedSource? {
    checkAddress(address)

    val (scheme, path) = parseAddress(address)
    val backend = backends[scheme] ?: return null
    return backend.open(path)
  }

  /** Writes a resource as UTF-8. Throws if the backend is readonly. */
  fun put(address: String, utf8: String) {
    put(address, ByteString.encodeUtf8(utf8))
  }

  /** Writes a resource. Throws if the backend is readonly. */
  fun put(address: String, data: ByteString) {
    checkAddress(address)

    val (scheme, path) = parseAddress(address)
    val backend = backends[scheme] ?: return
    backend.put(path, data)
  }

  /** Returns true if a resource at `address` exists. */
  fun exists(address: String): Boolean {
    checkAddress(address)

    val (scheme, path) = parseAddress(address)
    val backend = backends[scheme] ?: return false
    return backend.exists(path)
  }

  /** Returns the full path of the resources that are immediate children of `address`. */
  fun list(address: String): List<String> {
    checkAddress(address)

    val (scheme, path) = parseAddress(address)
    val backend = backends[scheme] ?: return listOf()
    return backend.list(path).map { scheme + it }
  }

  /**
   * Return the contents of `address` as a string, or null if no such resource exists. Note that
   * this method decodes the resource on every use. It is the caller's responsibility to cache the
   * result if it is to be loaded frequently.
   */
  fun utf8(address: String): String? {
    val source = open(address) ?: return null
    return source.use { it.readUtf8() }
  }

  private fun checkAddress(address: String) {
    require(address.matches(Regex("([^/:]+:)(/[^/]+)+/?"))) { "unexpected address $address" }
  }

  /**
   * Decodes an address like `classpath:/migrations/v1.sql` into a backend scheme like `classpath:`
   * and a backend-specific path like `/migrations/v1.sql`.
   */
  private fun parseAddress(path: String): Address {
    val colon = path.indexOf(':')
    return Address(path.substring(0, colon + 1), path.substring(colon + 1))
  }

  private data class Address(val scheme: String, val path: String)

  abstract class Backend {
    abstract fun open(path: String): BufferedSource?

    abstract fun exists(path: String): Boolean

    open fun put(path: String, data: ByteString) {
      throw UnsupportedOperationException("cannot put $path; ${this::class} is readonly")
    }

    open fun all(): Set<String> {
      throw UnsupportedOperationException("${this::class} doesn't support all()")
    }

    open fun list(path: String): List<String> {
      val prefix = if (path.endsWith("/")) path else "$path/"
      val result = ImmutableSet.builder<String>()
      for (key in all()) {
        if (!key.startsWith(prefix)) continue
        val slash = key.indexOf('/', prefix.length)
        if (slash == -1) {
          result.add(key)
        } else {
          result.add(key.substring(0, slash))
        }
      }
      return result.build().toList()
    }
  }
}