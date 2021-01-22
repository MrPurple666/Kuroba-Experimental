package com.github.k1rakishou.chan.features.search

import android.content.Context
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.EpoxyRecyclerView
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.core.manager.SiteManager
import com.github.k1rakishou.chan.features.search.epoxy.epoxySearchSiteView
import com.github.k1rakishou.chan.ui.controller.BaseFloatingController
import com.github.k1rakishou.core_themes.ThemeEngine
import com.github.k1rakishou.model.data.descriptor.SiteDescriptor
import javax.inject.Inject

internal class SelectSiteForSearchController(
  context: Context,
  private val selectedSite: SiteDescriptor,
  private val onSiteSelected: (SiteDescriptor) -> Unit
) : BaseFloatingController(context), ThemeEngine.ThemeChangesListener {

  @Inject
  lateinit var themeEngine: ThemeEngine
  @Inject
  lateinit var siteManager: SiteManager

  private lateinit var sitesRecyclerView: EpoxyRecyclerView
  private lateinit var clickableArea: ConstraintLayout

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun getLayoutId(): Int = R.layout.controller_select_site_for_search

  override fun onCreate() {
    super.onCreate()

    sitesRecyclerView = view.findViewById(R.id.sites_supporting_search_recycler_view)

    clickableArea = view.findViewById(R.id.clickable_area)
    clickableArea.setOnClickListener { pop() }

    renderSites()
    themeEngine.addListener(this)
  }

  override fun onDestroy() {
    super.onDestroy()

    themeEngine.removeListener(this)
  }

  override fun onThemeChanged() {
    renderSites()
  }

  private fun renderSites() {
    val sites = mutableListOf<SiteSupportingSearchData>()

    siteManager.viewActiveSitesOrderedWhile { chanSiteData, site ->
      sites += SiteSupportingSearchData(
        site.siteDescriptor(),
        site.icon().url.toString(),
        site.siteDescriptor() == selectedSite
      )

      return@viewActiveSitesOrderedWhile true
    }

    if (sites.isEmpty()) {
      pop()
      return
    }

    val backColor = themeEngine.chanTheme.backColor

    sitesRecyclerView.withModels {
      sites.forEach { siteSupportingSearchData ->
        val siteBackgroundColor = getSiteItemBackgroundColor(siteSupportingSearchData, backColor)

        epoxySearchSiteView {
          id("epoxy_search_site_view")
          bindIcon(siteSupportingSearchData.siteIconUrl)
          bindSiteName(siteSupportingSearchData.siteDescriptor.siteName)
          itemBackgroundColor(siteBackgroundColor)
          bindClickCallback {
            onSiteSelected.invoke(siteSupportingSearchData.siteDescriptor)
            pop()
          }
        }
      }
    }
  }

  private fun getSiteItemBackgroundColor(
    siteSupportingSearchData: SiteSupportingSearchData,
    backColor: Int
  ): Int {
    if (!siteSupportingSearchData.isSelected) {
      return backColor
    }

    return if (ThemeEngine.isDarkColor(backColor)) {
      ThemeEngine.manipulateColor(backColor, 1.3f)
    } else {
      ThemeEngine.manipulateColor(backColor, .7f)
    }
  }

  data class SiteSupportingSearchData(
    val siteDescriptor: SiteDescriptor,
    val siteIconUrl: String,
    val isSelected: Boolean
  )

}