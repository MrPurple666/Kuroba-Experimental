package com.github.k1rakishou.chan.features.image_saver

import android.content.Context
import android.widget.FrameLayout
import com.airbnb.epoxy.EpoxyController
import com.airbnb.epoxy.EpoxyRecyclerView
import com.github.k1rakishou.chan.R
import com.github.k1rakishou.chan.core.di.component.activity.ActivityComponent
import com.github.k1rakishou.chan.features.image_saver.epoxy.epoxyDuplicateImageView
import com.github.k1rakishou.chan.ui.controller.BaseFloatingController
import com.github.k1rakishou.chan.ui.epoxy.epoxyErrorView
import com.github.k1rakishou.chan.ui.epoxy.epoxyLoadingView
import com.github.k1rakishou.chan.ui.epoxy.epoxyTextView
import com.github.k1rakishou.chan.ui.theme.widget.ColorizableButton
import com.github.k1rakishou.chan.utils.setEnabledFast
import com.github.k1rakishou.common.errorMessageOrClassName
import com.github.k1rakishou.fsaf.FileManager
import com.github.k1rakishou.model.repository.ChanPostImageRepository
import com.github.k1rakishou.model.repository.ImageDownloadRequestRepository
import com.github.k1rakishou.persist_state.ImageSaverV2Options
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

class ResolveDuplicateImagesController(
  context: Context,
  private val uniqueId: String,
  private val imageSaverOptionsJson: String
) : BaseFloatingController(context), ResolveDuplicateImagesView {

  @Inject
  lateinit var gson: Gson
  @Inject
  lateinit var fileManager: FileManager
  @Inject
  lateinit var imageDownloadRequestRepository: ImageDownloadRequestRepository
  @Inject
  lateinit var chanPostImageRepository: ChanPostImageRepository
  @Inject
  lateinit var imageSaverV2: ImageSaverV2

  private lateinit var epoxyRecyclerView: EpoxyRecyclerView
  private lateinit var resolveButton: ColorizableButton

  private val resolveDuplicateImagesPresenter by lazy {
    return@lazy ResolveDuplicateImagesPresenter(
      uniqueId,
      gson.fromJson(imageSaverOptionsJson, ImageSaverV2Options::class.java),
      fileManager,
      imageDownloadRequestRepository,
      chanPostImageRepository,
      imageSaverV2
    )
  }

  override fun injectDependencies(component: ActivityComponent) {
    component.inject(this)
  }

  override fun getLayoutId(): Int = R.layout.controller_resolve_duplicate_images

  override fun onCreate() {
    super.onCreate()

    epoxyRecyclerView = view.findViewById(R.id.epoxy_recycler_view)
    resolveButton = view.findViewById(R.id.resolve_button)
    resolveButton.setEnabledFast(false)

    val cancelButton = view.findViewById<ColorizableButton>(R.id.cancel_button)
    val outsideArea = view.findViewById<FrameLayout>(R.id.outside_area)

    cancelButton.setOnClickListener {
      pop()
    }
    outsideArea.setOnClickListener {
      pop()
    }
    resolveButton.setOnClickListener {
      resolveDuplicateImagesPresenter.resolve()
    }

    mainScope.launch(Dispatchers.Main.immediate) {
      resolveDuplicateImagesPresenter.listenForStateUpdates()
        .collect { state -> renderState(state) }
    }

    resolveDuplicateImagesPresenter.onCreate(this)
  }

  override fun onDestroy() {
    super.onDestroy()

    resolveDuplicateImagesPresenter.onDestroy()
  }

  override fun showToastMessage(message: String) {
    showToast(message)
  }

  override fun onDuplicateResolvingCompleted() {
    pop()
  }

  private fun renderState(state: ResolveDuplicateImagesState) {
    epoxyRecyclerView.withModels {
      if (state !is ResolveDuplicateImagesState.Data) {
        resolveButton.setEnabledFast(false)

        when (state) {
          ResolveDuplicateImagesState.Loading -> {
            epoxyLoadingView {
              id("resolve_duplicates_controller_loading_view")
            }
          }
          ResolveDuplicateImagesState.Empty -> {
            epoxyTextView {
              id("resolve_duplicates_controller_empty_view")
              // TODO(KurobaEx v0.6.0): strings
              message("No unresolved duplicate images were found")
            }
          }
          is ResolveDuplicateImagesState.Error -> {
            epoxyErrorView {
              id("resolve_duplicates_controller_error_view")
              errorMessage(state.throwable.errorMessageOrClassName())
            }
          }
          is ResolveDuplicateImagesState.Data -> throw IllegalStateException("Must not be handled here")
        }

        return@withModels
      }

      renderDataState(state)
    }
  }

  private fun EpoxyController.renderDataState(dataState: ResolveDuplicateImagesState.Data) {
    check(dataState.duplicateImages.isNotEmpty()) { "dataState.duplicateImages is empty!" }

    val canEnableResolveButton = dataState.duplicateImages
      .all { duplicateImage ->
        return@all duplicateImage.resolution != ImageSaverV2Options.DuplicatesResolution.AskWhatToDo
      }

    resolveButton.setEnabledFast(canEnableResolveButton)

    dataState.duplicateImages.forEach { duplicateImage ->
      epoxyDuplicateImageView {
        id("epoxy_duplicate_image_view_${duplicateImage.hashCode()}")
        serverImage(duplicateImage.serverImage)
        localImage(duplicateImage.localImage)
        locked(duplicateImage.locked)
        duplicateResolution(duplicateImage.resolution)
        onImageClickListener { clickedDuplicateImage ->
          resolveDuplicateImagesPresenter.onDuplicateImageClicked(clickedDuplicateImage)
        }
      }
    }
  }
}
