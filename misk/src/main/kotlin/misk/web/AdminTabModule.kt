package misk.web

import misk.inject.KAbstractModule
import misk.web.actions.AdminTab
import misk.web.actions.WebActionEntry
import misk.web.proxy.WebProxyAction
import misk.web.proxy.WebProxyEntry

/**
 * AdminTabModule
 *
 * Binds the admin UI framework. Individual tabs should be bound with their other code.
 *
 * Example
 * Config tab is tightly coupled to the config module. Thus binding should be in ConfigWebModule
 */

class AdminTabModule : KAbstractModule() {
  override fun configure() {
    multibind<WebProxyEntry>().toInstance(
        WebProxyEntry("/_admin", "http://localhost:3100/"))
    multibind<WebActionEntry>().toInstance(
        WebActionEntry<WebProxyAction>("/_admin"))

    multibind<WebProxyEntry>().toInstance(
        WebProxyEntry("/_tab/dashboard", "http://localhost:3110/"))
    multibind<WebActionEntry>().toInstance(
        WebActionEntry<WebProxyAction>("/_tab/dashboard"))

    multibind<WebProxyEntry>().toInstance(
        WebProxyEntry("/@misk", "http://localhost:9100/"))
    multibind<WebActionEntry>().toInstance(
        WebActionEntry<WebProxyAction>("/@misk"))

//    Testing
//    multibind<AdminTab>().toInstance(AdminTab(
//        "Dashboard",
//        "dashboard",
//        "/_admin/dashboard"
//    ))

  }
}