package uk.co.appsplus.bootstrap.ui.pagination

import android.os.Bundle
import android.view.View
import androidx.annotation.StringRes
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.ListAdapter
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uk.co.appsplus.bootstrap.ui.R

abstract class PagingFragment<T, VM, Adapter>(
    private val pagingAdapter: PagingAdapter,
    private val pagingDirection: PagingDirection,
) : Fragment() where VM : PagingViewModel<T>, Adapter : ListAdapter<T, *> {

    enum class PagingDirection {
        TOP, BOTTOM
    }

    abstract val viewModel: VM
    abstract val concatAdapter: ConcatAdapter
    abstract val pagedAdapter: ListAdapter<T, *>

    private var currentSnackbar: Snackbar? = null
    @StringRes var retryTitle = R.string.bootstrap_retry

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        bindViewModel()
    }

    abstract fun hideRefreshing()

    protected open fun bindViewModel() {
        viewModel.items
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach(pagedAdapter::submitList)
            .launchIn(lifecycleScope)

        viewModel.isRefreshing
            .filter { !it }
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach { hideRefreshing() }
            .launchIn(lifecycleScope)

        viewModel.isPaging
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .onEach {
                if (it) {
                    when (pagingDirection) {
                        PagingDirection.TOP -> concatAdapter.addAdapter(0, pagingAdapter)
                        PagingDirection.BOTTOM -> concatAdapter.addAdapter(pagingAdapter)
                    }
                }
                pagingAdapter.isLoading = it
            }
            .launchIn(lifecycleScope)

        viewModel.pagingError
            .flowWithLifecycle(lifecycle, Lifecycle.State.STARTED)
            .filterNotNull()
            .onEach {
                hideCurrentSnackbar()
                currentSnackbar = when (it) {
                    PagingError.Refresh -> {
                        createFetchErrorSnackbar()
                            ?.apply { addFailureSnackbarActions(this) }
                    }
                    PagingError.Paging -> {
                        createPagingFailedSnackbar()
                            ?.apply { addFailureSnackbarActions(this) }
                    }
                }
                currentSnackbar?.show() ?: run { viewModel.returnToIdle() }
            }
            .launchIn(lifecycleScope)
    }

    private fun addFailureSnackbarActions(snackbar: Snackbar) {
        snackbar
            .addCallback(object : BaseTransientBottomBar.BaseCallback<Snackbar>() {
                override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                    currentSnackbar = null
                    if (event == Snackbar.Callback.DISMISS_EVENT_TIMEOUT) {
                        lifecycleScope.launch {
                            viewModel.returnToIdle()
                        }
                    }
                }
            })
            .setAction(retryTitle) {
                lifecycleScope.launch {
                    viewModel.retry()
                }
            }
    }

    abstract fun createFetchErrorSnackbar(): Snackbar?
    abstract fun createPagingFailedSnackbar(): Snackbar?

    protected fun hideCurrentSnackbar() {
        currentSnackbar?.dismiss()
    }
}
