package misk.web

import com.squareup.moshi.Moshi
import misk.inject.KAbstractModule
import misk.testing.MiskTest
import misk.testing.MiskTestModule
import misk.web.actions.WebAction
import misk.web.actions.WebActionEntry
import misk.web.jetty.JettyService
import misk.web.mediatype.MediaTypes
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import javax.inject.Inject
import kotlin.test.assertFailsWith

@MiskTest(startService = true)
internal class WebDispatchTest {
  @MiskTestModule
  val module = TestModule()

  data class HelloBye(val message: String)

  @Inject
  lateinit var moshi: Moshi

  @Inject
  lateinit var jettyService: JettyService

  private val helloByeJsonAdapter get() = moshi.adapter(HelloBye::class.java)

  @Test
  fun post() {
    val requestContent = helloByeJsonAdapter.toJson(HelloBye("my friend"))
    val httpClient = OkHttpClient()
    val request = Request.Builder()
        .post(okhttp3.RequestBody.create(MediaTypes.APPLICATION_JSON_MEDIA_TYPE,
            requestContent))
        .url(serverUrlBuilder().encodedPath("/hello").build())
        .build()

    val response = httpClient.newCall(request).execute()
    assertThat(response.code()).isEqualTo(200)

    val responseContent = response.body()!!.source()
    assertThat(helloByeJsonAdapter.fromJson(responseContent)!!.message).isEqualTo(
        "post hello my friend")
  }

  @Test
  fun get() {
    val httpClient = OkHttpClient()
    val request = Request.Builder()
        .get()
        .url(serverUrlBuilder().encodedPath("/hello/my_friend").build())
        .build()

    val response = httpClient.newCall(request).execute()
    assertThat(response.code()).isEqualTo(200)

    val responseContent = response.body()!!.source().readString(Charsets.UTF_8)
    assertThat(helloByeJsonAdapter.fromJson(responseContent)!!.message)
        .isEqualTo("get hello my_friend")
  }

  @Test
  fun getWithPathPrefix() {
    val httpClient = OkHttpClient()
    val request = Request.Builder()
        .get()
        .url(serverUrlBuilder().encodedPath("/path/prefix/hello/my_friend").build())
        .build()

    val response = httpClient.newCall(request).execute()
    assertThat(response.code()).isEqualTo(200)

    val responseContent = response.body()!!.source().readString(Charsets.UTF_8)
    assertThat(helloByeJsonAdapter.fromJson(responseContent)!!.message)
        .isEqualTo("get hello my_friend")
  }

  @Test
  fun getNothing() {
    val httpClient = OkHttpClient()
    val request = Request.Builder()
        .get()
        .url(serverUrlBuilder().encodedPath("/nothing").build())
        .build()

    val response = httpClient.newCall(request).execute()

    // we expect this to fail since we threw an error; what we are trying to avoid is a
    // binding time error due to missing marshaller
    assertThat(response.code()).isEqualTo(500)
  }

  @Test
  fun entryWithSingleSegment() {
    WebActionEntry(GetHello::class, "/good")
  }

  @Test
  fun entryFailsWithTrailingSlash() {
    assertFailsWith<IllegalArgumentException> {
      WebActionEntry(GetHello::class, "/bad/path/")
    }
  }

  @Test
  fun entryFailsWithEmptyPathSegments() {
    assertFailsWith<IllegalArgumentException> {
      WebActionEntry(GetHello::class, "///")
    }
  }

  class TestModule : KAbstractModule() {
    override fun configure() {
      install(WebTestingModule())
      multibind<WebActionEntry>().toInstance(WebActionEntry<PostHello>())
      multibind<WebActionEntry>().toInstance(WebActionEntry<GetHello>())
      multibind<WebActionEntry>().toInstance(WebActionEntry<PostBye>())
      multibind<WebActionEntry>().toInstance(WebActionEntry<GetBye>())
      multibind<WebActionEntry>().toInstance(WebActionEntry<GetNothing>())
      multibind<WebActionEntry>().toInstance(WebActionEntry<GetHello>("/path/prefix"))
    }
  }

  class PostHello : WebAction {
    @Post("/hello")
    @RequestContentType(MediaTypes.APPLICATION_JSON)
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun hello(@misk.web.RequestBody request: HelloBye) =
        HelloBye("post hello ${request.message}")
  }

  class GetHello : WebAction {
    @Get("/hello/{message}")
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun hello(@PathParam("message") message: String) =
        HelloBye("get hello $message")
  }

  class PostBye : WebAction {
    @Post("/bye")
    @RequestContentType(MediaTypes.APPLICATION_JSON)
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun bye(@misk.web.RequestBody request: HelloBye) =
        HelloBye("post bye ${request.message}")
  }

  class GetBye : WebAction {
    @Get("/bye/{message}")
    @ResponseContentType(MediaTypes.APPLICATION_JSON)
    fun bye(@PathParam("message") message: String) =
        HelloBye("get bye $message")
  }

  class GetNothing : WebAction {
    @Get("/nothing")
    fun doNothing(): Nothing {
      throw UnsupportedOperationException("we did nothing")
    }
  }

  private fun serverUrlBuilder(): HttpUrl.Builder {
    return jettyService.httpServerUrl.newBuilder()
  }
}
